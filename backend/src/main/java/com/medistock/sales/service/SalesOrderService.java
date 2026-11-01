package com.medistock.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medistock.base.mapper.CustomerMapper;
import com.medistock.base.mapper.DrugMapper;
import com.medistock.base.mapper.WarehouseMapper;
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
    private final CustomerMapper customerMapper;
    private final WarehouseMapper warehouseMapper;
    private final DrugMapper drugMapper;
    private final com.medistock.common.service.NameResolver nameResolver;

    public SalesOrderService(SalesOrderMapper salesOrderMapper,
                             SalesOrderItemMapper itemMapper,
                             CustomerMapper customerMapper,
                             WarehouseMapper warehouseMapper,
                             DrugMapper drugMapper,
                             com.medistock.common.service.NameResolver nameResolver) {
        // ServiceImpl 在 MP 3.5.7 无带参构造器，直接为 protected baseMapper 赋值（同 Phase 4 PurchaseOrderService）
        this.baseMapper = salesOrderMapper;
        this.itemMapper = itemMapper;
        this.customerMapper = customerMapper;
        this.warehouseMapper = warehouseMapper;
        this.drugMapper = drugMapper;
        this.nameResolver = nameResolver;
    }

    /** 创建销售单（头 + 明细）：行金额 = 单价×数量 − 行折扣，总金额 = Σ行金额 */
    @Transactional
    public Long createOrder(SalesOrder order, List<SalesOrderItem> items) {
        // 问题 77：客户/仓库必须存在且属于当前租户（防止引用其他租户基础资料）
        validateHeader(order);
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "明细不能为空");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (SalesOrderItem item : items) {
            // 防元素为 null 导致 NPE：明细数组里的元素逐个判空
            if (item == null) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "明细不能为空");
            }
            // 数据防呆：数量必须为正数、单价非负，禁止负数单产生负金额
            validateLine(item);
            BigDecimal discount = item.getDiscountAmount() == null ? BigDecimal.ZERO : item.getDiscountAmount();
            BigDecimal lineAmount = item.getSalePrice().multiply(item.getQuantity()).subtract(discount);
            item.setAmount(lineAmount);
            total = total.add(lineAmount);
        }
        // 整单金额上限兜底：防多行超大明细累加后超出 DECIMAL(18,4) 容量（SQLite 不报错但数据已失真）
        checkTotalAmount(total);
        // 折扣口径统一：订单头 discountAmount 由明细折扣汇总得出（不再接受两套口径并存），
        // 保证同一笔单在列表「金额/优惠」两列展示前后一致
        order.setDiscountAmount(lineDiscountSum(items));
        // 单号序号取当前租户最大主键 +1（含已删除），避免删草稿后 count 回退导致与既有单号重复
        order.setOrderNo(nextOrderNo());
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
        // 问题 77：更新后客户/仓库同样必须存在且属于当前租户
        validateHeader(order);
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "明细不能为空");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (SalesOrderItem item : items) {
            // 防元素为 null 导致 NPE：明细数组里的元素逐个判空
            if (item == null) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "明细不能为空");
            }
            validateLine(item);
            BigDecimal discount = item.getDiscountAmount() == null ? BigDecimal.ZERO : item.getDiscountAmount();
            BigDecimal lineAmount = item.getSalePrice().multiply(item.getQuantity()).subtract(discount);
            item.setAmount(lineAmount);
            total = total.add(lineAmount);
        }
        // 编辑与新增共用整单金额上限校验，防超大金额落库
        checkTotalAmount(total);
        order.setDiscountAmount(lineDiscountSum(items));
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

    /** 头部校验：客户/仓库必填且存在（selectById 受租户拦截器过滤，跨租户/不存在均返回 null） */
    private void validateHeader(SalesOrder order) {
        if (order.getCustomerId() == null) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "客户不能为空");
        }
        if (customerMapper.selectById(order.getCustomerId()) == null) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "客户不存在或不属于当前租户");
        }
        if (order.getWarehouseId() == null) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "仓库不能为空");
        }
        if (warehouseMapper.selectById(order.getWarehouseId()) == null) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "仓库不存在或不属于当前租户");
        }
        // 备注长度与表结构列宽一致（VARCHAR(255)），防止超长文本绕过前端落库
        if (order.getRemark() != null && order.getRemark().length() > 255) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "备注不能超过255字");
        }
    }

    /** 明细行防呆校验：药品必填且归属当前租户、数量必须为正、单价非负、折扣非负，杜绝负数单与负金额 */
    private void validateLine(SalesOrderItem item) {
        // 问题 77：明细药品必须存在且属于当前租户
        if (item.getDrugId() == null) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "明细药品不能为空");
        }
        if (drugMapper.selectById(item.getDrugId()) == null) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "药品不存在或不属于当前租户");
        }
        if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "明细数量必须大于 0");
        }
        if (item.getSalePrice() == null || item.getSalePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "明细单价不能为负数");
        }
        if (item.getDiscountAmount() != null && item.getDiscountAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "明细折扣不能为负数");
        }
        // 数量/单价上限：防 1e18 等超大值落库（SQLite 不校验 DECIMAL 精度，会存入失真金额）
        if (item.getQuantity().compareTo(MAX_QUANTITY_PRICE) > 0
                || item.getSalePrice().compareTo(MAX_QUANTITY_PRICE) > 0) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "数量/单价超出合理范围");
        }
        // 折扣超金额拦截：折扣非空时不得大于 单价×数量，否则行金额为负（前面已保证 quantity/salePrice 非空，可直接相乘）
        if (item.getDiscountAmount() != null
                && item.getDiscountAmount().compareTo(item.getSalePrice().multiply(item.getQuantity())) > 0) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "明细折扣不能超过单价×数量");
        }
        // 单行金额上限：数量×单价可达 1e16，超出 DECIMAL(18,4) 整数位容量，必须单独拦截
        if (item.getSalePrice().multiply(item.getQuantity()).compareTo(MAX_TOTAL_AMOUNT) > 0) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "金额超出合理范围");
        }
    }

    /** 数量/单价上限（1 亿）：远大于正常医药销售数量，仅用于拦截溢出/恶意超大值 */
    private static final BigDecimal MAX_QUANTITY_PRICE = new BigDecimal("100000000");

    /** 金额上限（1 万亿）：用于拦截单行/整单金额溢出 */
    private static final BigDecimal MAX_TOTAL_AMOUNT = new BigDecimal("1000000000000");

    /** 整单金额上限校验（逐行校验后的兜底，防多行累加溢出） */
    private void checkTotalAmount(BigDecimal total) {
        if (total.compareTo(MAX_TOTAL_AMOUNT) > 0) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "订单金额超出合理范围");
        }
    }

    /** 生成销售单号：SO + 日期 + 3 位以上序号（序号跟随最大主键单调递增，永不回退复用） */
    private String nextOrderNo() {
        Long maxId = baseMapper.selectMaxIdIncludingDeleted();
        long seq = (maxId == null ? 0L : maxId) + 1;
        return "SO" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%03d", seq);
    }

    /** 明细折扣合计：订单头 discountAmount 统一由明细汇总（单一口径，杜绝同单两价） */
    private BigDecimal lineDiscountSum(List<SalesOrderItem> items) {
        return items.stream()
                .map(i -> i.getDiscountAmount() == null ? BigDecimal.ZERO : i.getDiscountAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 删除：仅草稿（级联删明细 + 删头原子） */
    @Transactional
    public void deleteOrder(Long id) {
        SalesOrder order = this.getById(id);
        // 删除不存在/跨租户单据统一 404（此前 return 静默成功，返回 200 ok）
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "销售单不存在");
        }
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
