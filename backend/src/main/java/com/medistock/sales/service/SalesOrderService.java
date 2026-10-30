package com.medistock.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medistock.common.config.TenantContext;
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
    private final com.medistock.common.service.NameResolver nameResolver;

    public SalesOrderService(SalesOrderMapper salesOrderMapper,
                             SalesOrderItemMapper itemMapper,
                             com.medistock.common.service.NameResolver nameResolver) {
        // ServiceImpl 在 MP 3.5.7 无带参构造器，直接为 protected baseMapper 赋值（同 Phase 4 PurchaseOrderService）
        this.baseMapper = salesOrderMapper;
        this.itemMapper = itemMapper;
        this.nameResolver = nameResolver;
    }

    /** 创建销售单（头 + 明细）：行金额 = 单价×数量 − 行折扣，总金额 = Σ行金额 */
    @Transactional
    public Long createOrder(SalesOrder order, List<SalesOrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "明细不能为空");
        }
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
        // 超管（tenantId=0）时租户拦截器不注入 tenant_id，必须显式补默认值（防 NOT NULL 违反）
        order.setTenantId(TenantContext.getTenantId() == null ? 0L : TenantContext.getTenantId());
        this.save(order);
        for (SalesOrderItem item : items) {
            item.setOrderId(order.getId());
            item.setOutboundQuantity(BigDecimal.ZERO);
            item.setTenantId(order.getTenantId());
            itemMapper.insert(item);
        }
        return order.getId();
    }

    /** 更新销售单：仅草稿可改，明细全量重建（头字段与金额重算） */
    @Transactional
    public void updateOrder(Long id, SalesOrder order, List<SalesOrderItem> items) {
        SalesOrder old = this.getById(id);
        if (old == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        // 仅草稿（状态 0）允许修改，已出库单据不可回改
        if (old.getStatus() == null || old.getStatus() != 0) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "明细不能为空");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (SalesOrderItem item : items) {
            BigDecimal discount = item.getDiscountAmount() == null ? BigDecimal.ZERO : item.getDiscountAmount();
            BigDecimal lineAmount = item.getSalePrice().multiply(item.getQuantity()).subtract(discount);
            item.setAmount(lineAmount);
            total = total.add(lineAmount);
        }
        // 头：只允许改业务字段，单号/租户/创建人等保持不变
        old.setCustomerId(order.getCustomerId());
        old.setWarehouseId(order.getWarehouseId());
        old.setDiscountAmount(order.getDiscountAmount());
        old.setPaidAmount(order.getPaidAmount());
        old.setRemark(order.getRemark());
        old.setTotalAmount(total);
        this.updateById(old);
        // 明细：先删后插
        itemMapper.delete(new LambdaQueryWrapper<SalesOrderItem>()
                .eq(SalesOrderItem::getOrderId, id));
        for (SalesOrderItem item : items) {
            item.setId(null);
            item.setOrderId(id);
            item.setOutboundQuantity(BigDecimal.ZERO);
            item.setTenantId(old.getTenantId());
            itemMapper.insert(item);
        }
    }

    /** 提交销售单：草稿(0) → 待出库(1)，仅草稿可提交 */
    @Transactional
    public void submitOrder(Long id) {
        SalesOrder old = this.getById(id);
        if (old == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (old.getStatus() == null || old.getStatus() != 0) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        old.setStatus(1);
        this.updateById(old);
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

    /** 删除：仅草稿（级联删明细 + 删头原子） */
    @Transactional
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
        Page<SalesOrder> p = this.page(new Page<>(page, pageSize), qw);
        // 列表展示回填：客户名称 / 开单人姓名 / 药品明细摘要（批量查询避免 N+1）
        List<SalesOrder> records = p.getRecords();
        if (records.isEmpty()) return p;
        java.util.Map<Long, String> customers = nameResolver.customerNames(
                records.stream().map(SalesOrder::getCustomerId).toList());
        java.util.Map<Long, String> users = nameResolver.userNames(
                records.stream().map(SalesOrder::getCreateBy).toList());
        // 一次查出所有明细，按单分组，再批量查药品名
        java.util.Map<Long, List<SalesOrderItem>> itemMap = itemMapper.selectList(
                        new LambdaQueryWrapper<SalesOrderItem>()
                                .in(SalesOrderItem::getOrderId, records.stream().map(SalesOrder::getId).toList()))
                .stream().collect(java.util.stream.Collectors.groupingBy(SalesOrderItem::getOrderId));
        java.util.Map<Long, String> drugs = nameResolver.drugNames(
                itemMap.values().stream().flatMap(List::stream).map(SalesOrderItem::getDrugId).distinct().toList());
        records.forEach(o -> {
            o.setCustomerName(customers.get(o.getCustomerId()));
            o.setCreateByName(users.get(o.getCreateBy()));
            List<SalesOrderItem> its = itemMap.getOrDefault(o.getId(), List.of());
            o.setQuantity(its.stream().map(i -> i.getQuantity() == null ? BigDecimal.ZERO : i.getQuantity())
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            o.setDrugNames(its.stream().map(i -> {
                String name = drugs.getOrDefault(i.getDrugId(), "药品#" + i.getDrugId());
                String qty = i.getQuantity() == null ? "0" : i.getQuantity().stripTrailingZeros().toPlainString();
                return name + " ×" + qty;
            }).collect(java.util.stream.Collectors.joining("、")));
        });
        return p;
    }
}
