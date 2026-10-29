package com.medistock.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.inventory.service.StockService;
import com.medistock.purchase.entity.PurchaseOrder;
import com.medistock.purchase.entity.PurchaseOrderItem;
import com.medistock.purchase.mapper.PurchaseOrderItemMapper;
import com.medistock.purchase.mapper.PurchaseOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 采购入库：验收入库，自动生成批次库存。
 * 并发策略：由 StockService.inbound（乐观锁+重试）保证累加正确。
 */
@Service
@RequiredArgsConstructor
public class PurchaseInboundService {

    private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderItemMapper itemMapper;
    private final StockService stockService;

    /**
     * 入库：items 为明细项 + 实收数量/批号/有效期
     * 简化 P0：一单一次整单入库；多次入库为 P1 口子（inboundQuantity 字段已留）
     */
    @Transactional
    public void inbound(Long orderId, java.util.List<InboundReq.Item> items) {
        PurchaseOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (order.getStatus() != 0) {   // 草稿可直接入库；P1 审核流改为 2-已审核后入库
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        for (InboundReq.Item in : items) {
            PurchaseOrderItem item = itemMapper.selectOne(new LambdaQueryWrapper<PurchaseOrderItem>()
                    .eq(PurchaseOrderItem::getId, in.getItemId())
                    .eq(PurchaseOrderItem::getOrderId, orderId));
            if (item == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "明细不存在");
            }
            if (in.getQuantity().compareTo(item.getQuantity()) > 0) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "入库数量超过采购数量");
            }
            // 核心：生成/累加批次库存（乐观锁保证并发安全）
            stockService.inbound(item.getDrugId(), order.getWarehouseId(),
                    in.getBatchNo(), in.getExpiryDate(), in.getQuantity(),
                    order.getOrderNo(), "采购入库");
            // 回填批次信息
            item.setBatchNo(in.getBatchNo());
            item.setExpiryDate(in.getExpiryDate());
            item.setInboundQuantity(in.getQuantity());
            itemMapper.updateById(item);
        }
        order.setStatus(3);   // 已入库
        orderMapper.updateById(order);
    }
}
