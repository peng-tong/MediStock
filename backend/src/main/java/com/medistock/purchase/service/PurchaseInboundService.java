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
        // S6 空明细拦截：无明细的入库请求直接拒绝，避免单据被置为已入库
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "入库明细不能为空");
        }
        PurchaseOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        // 仅「审核通过 待入库(2)」可入库，草稿/待审核/已驳回均不可入库
        if (order.getStatus() == null || order.getStatus() != 2) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "仅审核通过的采购单可入库");
        }
        // 先一次性加载本单全部明细，后续按 id 查找并校验覆盖完整性
        java.util.Map<Long, PurchaseOrderItem> orderItemMap = itemMapper.selectList(
                        new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getOrderId, orderId))
                .stream().collect(java.util.stream.Collectors.toMap(PurchaseOrderItem::getId, i -> i));
        // 已处理明细 id 集合：防止同一明细 id 在请求中出现多次导致库存重复累加
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (InboundReq.Item in : items) {
            // S6 明细元素校验：元素为 null 或缺少明细 id 时拒绝（防 NPE）
            if (in == null || in.getItemId() == null) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "明细 id 不能为空");
            }
            // 重复明细拦截：同一 itemId 只允许出现一次（否则同批次会被反复加库存）
            if (!seen.add(in.getItemId())) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "同一采购明细不能重复入库");
            }
            // 逐项校验：数量必须为正数、批号必填（防脏数据入库）
            if (in.getQuantity() == null || in.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "入库数量必须大于 0");
            }
            if (in.getBatchNo() == null || in.getBatchNo().isBlank()) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "批号不能为空");
            }
            // 批号长度与表结构列宽一致，避免超长批号落库
            if (in.getBatchNo().length() > 64) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "批号不能超过64字");
            }
            // 效期必填 + 过期拦截：有效期早于今天的批次禁止入库（药品行业硬约束，前端仅提示、后端强制）
            if (in.getExpiryDate() == null) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "有效期不能为空");
            }
            if (in.getExpiryDate().isBefore(java.time.LocalDate.now())) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "已过期批次禁止入库");
            }
            PurchaseOrderItem item = orderItemMap.get(in.getItemId());
            if (item == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "明细不存在");
            }
            if (in.getQuantity().compareTo(item.getQuantity()) > 0) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "入库数量超过采购数量");
            }
            // P0 为「一单一次整单入库」：数量必须等于采购数量。
            // 否则单据会被置为已入库但存在欠收，已入库数量与状态不一致
            if (in.getQuantity().compareTo(item.getQuantity()) != 0) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR,
                        "本系统仅支持整单一次入库，入库数量须等于采购数量");
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
        // 覆盖完整性：请求必须包含本单全部明细，避免漏项入库后单据仍被标记为已入库
        if (seen.size() != orderItemMap.size()) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "入库明细必须覆盖采购单全部明细");
        }
        order.setStatus(3);   // 已入库
        orderMapper.updateById(order);
    }
}
