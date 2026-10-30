package com.medistock.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

import java.math.BigDecimal;
import java.util.List;

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
        SalesOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        // 销售状态机：0 草稿 → 1 待出库 → 3 已出库（4 已取消）
        // 必须先「提交」为待出库(1)才能出库，避免草稿未确认直接扣库存
        if (order.getStatus() == null || order.getStatus() != 1) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        List<SalesOrderItem> items = itemMapper.selectList(new LambdaQueryWrapper<SalesOrderItem>()
                .eq(SalesOrderItem::getOrderId, orderId));
        for (SalesOrderItem item : items) {
            // FIFO 扣减 + 过期/负库存拦截 + 并发重试（全在 StockService 内完成）
            stockService.outboundFifo(item.getDrugId(), order.getWarehouseId(), item.getQuantity(),
                    order.getOrderNo(), "销售出库");
            item.setOutboundQuantity(item.getQuantity());
            itemMapper.updateById(item);
        }
        order.setStatus(3);   // 已出库
        orderMapper.updateById(order);
    }
}
