package com.medistock.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.inventory.service.StockService;
import com.medistock.sales.entity.SalesOrder;
import com.medistock.sales.entity.SalesOrderItem;
import com.medistock.sales.mapper.SalesOrderItemMapper;
import com.medistock.sales.mapper.SalesOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 销售出库：按批次 FIFO 扣减。
 * - 过期拦截：FIFO 查询已过滤过期批次（expiryDate > 今天）
 * - 负库存拦截：可用不足抛"库存不足"
 * - 并发安全：由 StockService.outboundFifo（乐观锁+重试）保证
 */
@Service
@RequiredArgsConstructor
public class SalesOutboundService {

    private final SalesOrderMapper orderMapper;
    private final SalesOrderItemMapper itemMapper;
    private final StockService stockService;

    @Transactional
    public void outbound(Long orderId) {
        /*
         * 并发防护：用「status=1 → 3」的条件更新做原子占位（CAS），只有抢占成功的请求才扣库存。
         * 注意必须把这条 UPDATE 作为事务内第一条语句：若先 selectById 再 update，
         * SQLite WAL 下读快照升级为写事务会遇到 SQLITE_BUSY_SNAPSHOT（database is locked）返回 500。
         */
        int claimed = orderMapper.update(null, new LambdaUpdateWrapper<SalesOrder>()
                .eq(SalesOrder::getId, orderId)
                .eq(SalesOrder::getStatus, 1)
                .set(SalesOrder::getStatus, 3));
        if (claimed == 0) {
            // 抢占失败：区分「单据不存在/跨租户」与「状态不允许」（已出库、草稿等）
            SalesOrder exists = orderMapper.selectById(orderId);
            if (exists == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND);
            }
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        SalesOrder order = orderMapper.selectById(orderId);
        java.util.List<SalesOrderItem> items = itemMapper.selectList(new LambdaQueryWrapper<SalesOrderItem>()
                .eq(SalesOrderItem::getOrderId, orderId));
        for (SalesOrderItem item : items) {
            // FIFO 扣减 + 过期/负库存拦截 + 并发重试（全在 StockService 内完成）
            stockService.outboundFifo(item.getDrugId(), order.getWarehouseId(), item.getQuantity(),
                    order.getOrderNo(), "销售出库");
            item.setOutboundQuantity(item.getQuantity());
            itemMapper.updateById(item);
        }
    }
}
