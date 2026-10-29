package com.medistock.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medistock.common.config.UserContext;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.sales.entity.SalesOrder;
import com.medistock.sales.entity.SalesOrderItem;
import com.medistock.sales.mapper.SalesOrderItemMapper;
import com.medistock.sales.mapper.SalesOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class SalesOrderService extends ServiceImpl<SalesOrderMapper, SalesOrder> {

    private final SalesOrderItemMapper itemMapper;

    public SalesOrderService(SalesOrderMapper salesOrderMapper,
                             SalesOrderItemMapper itemMapper) {
        // ServiceImpl 在 MP 3.5.7 无带参构造器，直接为 protected baseMapper 赋值（同 Phase 4 PurchaseOrderService）
        this.baseMapper = salesOrderMapper;
        this.itemMapper = itemMapper;
    }

    /** 创建销售单（头 + 明细）：行金额 = 单价×数量 − 行折扣，总金额 = Σ行金额 */
    @Transactional
    public Long createOrder(SalesOrder order, List<SalesOrderItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (SalesOrderItem item : items) {
            BigDecimal discount = item.getDiscountAmount() == null ? BigDecimal.ZERO : item.getDiscountAmount();
            BigDecimal lineAmount = item.getSalePrice().multiply(item.getQuantity()).subtract(discount);
            item.setAmount(lineAmount);
            total = total.add(lineAmount);
        }
        order.setOrderNo("SO" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%03d", this.count(null) + 1));
        order.setTotalAmount(total);
        order.setStatus(0);                 // 草稿（P1 审核前）
        order.setCreateBy(UserContext.getUserId());
        this.save(order);
        for (SalesOrderItem item : items) {
            item.setOrderId(order.getId());
            item.setOutboundQuantity(BigDecimal.ZERO);
            itemMapper.insert(item);
        }
        return order.getId();
    }

    /** 详情：头 + 明细 */
    public SalesOrder detail(Long id) {
        SalesOrder order = this.getById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        order.setItems(listItems(id));
        return order;
    }

    public List<SalesOrderItem> listItems(Long orderId) {
        return itemMapper.selectList(new LambdaQueryWrapper<SalesOrderItem>()
                .eq(SalesOrderItem::getOrderId, orderId));
    }

    /** 删除：仅草稿 */
    public void deleteOrder(Long id) {
        SalesOrder order = this.getById(id);
        if (order == null) return;
        if (order.getStatus() != 0) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        itemMapper.delete(new LambdaQueryWrapper<SalesOrderItem>().eq(SalesOrderItem::getOrderId, id));
        this.removeById(id);
    }

    public Page<SalesOrder> pageQuery(int page, int pageSize, String orderNo, Integer status) {
        LambdaQueryWrapper<SalesOrder> qw = new LambdaQueryWrapper<>();
        if (orderNo != null && !orderNo.isBlank()) qw.like(SalesOrder::getOrderNo, orderNo);
        if (status != null) qw.eq(SalesOrder::getStatus, status);
        qw.orderByDesc(SalesOrder::getId);
        return this.page(new Page<>(page, pageSize), qw);
    }
}
