package com.medistock.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medistock.common.config.UserContext;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.purchase.entity.PurchaseOrder;
import com.medistock.purchase.entity.PurchaseOrderItem;
import com.medistock.purchase.mapper.PurchaseOrderItemMapper;
import com.medistock.purchase.mapper.PurchaseOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PurchaseOrderService extends ServiceImpl<PurchaseOrderMapper, PurchaseOrder> {

    private final PurchaseOrderItemMapper itemMapper;

    public PurchaseOrderService(PurchaseOrderMapper purchaseOrderMapper,
                                PurchaseOrderItemMapper itemMapper) {
        // ServiceImpl 在 MP 3.5.7 无带参构造器，直接为 protected baseMapper 赋值（同 Phase 1 UserService 修正方式）
        this.baseMapper = purchaseOrderMapper;
        this.itemMapper = itemMapper;
    }

    /** 创建采购单（头 + 明细），金额按明细合计 */
    @Transactional
    public Long createOrder(PurchaseOrder order, List<PurchaseOrderItem> items) {
        BigDecimal total = items.stream()
                .map(i -> i.getPurchasePrice().multiply(i.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setOrderNo("PO" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%03d", this.count(null) + 1));
        order.setTotalAmount(total);
        order.setStatus(0);                 // 草稿（P1 审核前）
        order.setCreateBy(UserContext.getUserId());
        this.save(order);
        for (PurchaseOrderItem item : items) {
            item.setOrderId(order.getId());
            item.setAmount(item.getPurchasePrice().multiply(item.getQuantity()));
            item.setInboundQuantity(BigDecimal.ZERO);
            itemMapper.insert(item);
        }
        return order.getId();
    }

    /** 详情：头 + 明细 */
    public PurchaseOrder detail(Long id) {
        PurchaseOrder order = this.getById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        order.setItems(listItems(id));
        return order;
    }

    public List<PurchaseOrderItem> listItems(Long orderId) {
        return itemMapper.selectList(new LambdaQueryWrapper<PurchaseOrderItem>()
                .eq(PurchaseOrderItem::getOrderId, orderId));
    }

    /** 删除：仅草稿 */
    public void deleteOrder(Long id) {
        PurchaseOrder order = this.getById(id);
        if (order == null) return;
        if (order.getStatus() != 0) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        itemMapper.delete(new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getOrderId, id));
        this.removeById(id);
    }

    public Page<PurchaseOrder> pageQuery(int page, int pageSize, String orderNo, Integer status) {
        LambdaQueryWrapper<PurchaseOrder> qw = new LambdaQueryWrapper<>();
        if (orderNo != null && !orderNo.isBlank()) qw.like(PurchaseOrder::getOrderNo, orderNo);
        if (status != null) qw.eq(PurchaseOrder::getStatus, status);
        qw.orderByDesc(PurchaseOrder::getId);
        return this.page(new Page<>(page, pageSize), qw);
    }
}
