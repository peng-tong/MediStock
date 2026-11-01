package com.medistock.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medistock.base.mapper.DrugMapper;
import com.medistock.base.mapper.SupplierMapper;
import com.medistock.base.mapper.WarehouseMapper;
import com.medistock.common.config.TenantContext;
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
    private final SupplierMapper supplierMapper;
    private final WarehouseMapper warehouseMapper;
    private final DrugMapper drugMapper;
    private final com.medistock.common.service.NameResolver nameResolver;

    public PurchaseOrderService(PurchaseOrderMapper purchaseOrderMapper,
                                PurchaseOrderItemMapper itemMapper,
                                SupplierMapper supplierMapper,
                                WarehouseMapper warehouseMapper,
                                DrugMapper drugMapper,
                                com.medistock.common.service.NameResolver nameResolver) {
        // ServiceImpl 在 MP 3.5.7 无带参构造器，直接为 protected baseMapper 赋值（同 Phase 1 UserService 修正方式）
        this.baseMapper = purchaseOrderMapper;
        this.itemMapper = itemMapper;
        this.supplierMapper = supplierMapper;
        this.warehouseMapper = warehouseMapper;
        this.drugMapper = drugMapper;
        this.nameResolver = nameResolver;
    }

    /** 创建采购单（头 + 明细），金额按明细合计 */
    @Transactional
    public Long createOrder(PurchaseOrder order, List<PurchaseOrderItem> items) {
        // S11/S13 头部校验：供应商/仓库必填且存在、备注长度受限（防外键悬空与脏数据落库）
        validateHeader(order);
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "明细不能为空");
        }
        BigDecimal total = items.stream()
                .map(i -> {
                    validateLine(i);
                    return i.getPurchasePrice().multiply(i.getQuantity());
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 订单总额上限：防多行超大明细累加溢出 DECIMAL(18,4)（单行上限之外再做整单兜底）
        checkTotalAmount(total);
        // 单号序号取当前租户最大主键 +1（含已删除），避免删除草稿后 count 回退导致与既有单号重复
        order.setOrderNo(nextOrderNo());
        order.setTotalAmount(total);
        order.setStatus(0);                 // 草稿（P1 审核前）
        order.setCreateBy(UserContext.getUserId());
        // 超管（tenantId=0）时租户拦截器不注入 tenant_id，必须显式补默认值（防 NOT NULL 违反）
        order.setTenantId(TenantContext.getTenantId() == null ? 0L : TenantContext.getTenantId());
        this.save(order);
        for (PurchaseOrderItem item : items) {
            item.setOrderId(order.getId());
            item.setAmount(item.getPurchasePrice().multiply(item.getQuantity()));
            item.setInboundQuantity(BigDecimal.ZERO);
            item.setTenantId(TenantContext.getTenantId() == null ? 0L : TenantContext.getTenantId());
            itemMapper.insert(item);
        }
        return order.getId();
    }

    /** 更新采购单：仅草稿可改，明细全量重建（头字段与金额重算） */
    @Transactional
    public void updateOrder(Long id, PurchaseOrder order, List<PurchaseOrderItem> items) {
        PurchaseOrder old = this.getById(id);
        if (old == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        // 仅草稿（状态 0）允许修改，已审核/已入库单据不可回改
        if (old.getStatus() == null || old.getStatus() != 0) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        // S11/S13 头部校验：更新后的供应商/仓库必须存在、备注长度受限
        validateHeader(order);
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "明细不能为空");
        }
        BigDecimal total = items.stream()
                .map(i -> {
                    validateLine(i);
                    return i.getPurchasePrice().multiply(i.getQuantity());
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 编辑与新增共用整单金额上限校验
        checkTotalAmount(total);
        // 头：只允许改业务字段，单号/租户/创建人等保持不变
        old.setSupplierId(order.getSupplierId());
        old.setWarehouseId(order.getWarehouseId());
        old.setRemark(order.getRemark());
        old.setTotalAmount(total);
        this.updateById(old);
        // 明细：先删后插，保持 id 自增与租户一致
        itemMapper.delete(new LambdaQueryWrapper<PurchaseOrderItem>()
                .eq(PurchaseOrderItem::getOrderId, id));
        for (PurchaseOrderItem item : items) {
            item.setId(null);
            item.setOrderId(id);
            item.setAmount(item.getPurchasePrice().multiply(item.getQuantity()));
            item.setInboundQuantity(BigDecimal.ZERO);
            item.setTenantId(old.getTenantId());
            itemMapper.insert(item);
        }
    }

    /** 提交采购单：草稿(0) → 待审核/待入库(1)，仅草稿可提交 */
    @Transactional
    public void submitOrder(Long id) {
        PurchaseOrder old = this.getById(id);
        if (old == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (old.getStatus() == null || old.getStatus() != 0) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        old.setStatus(1);
        this.updateById(old);
    }

    /** 审核通过：待审核(1) → 待入库(2)，审核通过后不可再编辑/删除 */
    @Transactional
    public void approveOrder(Long id) {
        PurchaseOrder old = this.getById(id);
        if (old == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (old.getStatus() == null || old.getStatus() != 1) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        old.setStatus(2);
        this.updateById(old);
    }

    /** 驳回：待审核(1) → 已驳回(4) */
    @Transactional
    public void rejectOrder(Long id) {
        PurchaseOrder old = this.getById(id);
        if (old == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (old.getStatus() == null || old.getStatus() != 1) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        old.setStatus(4);
        this.updateById(old);
    }

    /** 详情：头 + 明细（回填供应商名/经办人/药品名与规格，供入库验收页直接展示） */
    public PurchaseOrder detail(Long id) {
        PurchaseOrder order = this.getById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        List<PurchaseOrderItem> items = listItems(id);
        // 明细按 drugId 批量回填 药品通用名 / 规格 / 存储条件（一次查询，避免 N+1）
        java.util.Map<Long, com.medistock.base.entity.Drug> drugs = nameResolver.drugs(
                items.stream().map(PurchaseOrderItem::getDrugId).toList());
        for (PurchaseOrderItem it : items) {
            com.medistock.base.entity.Drug d = drugs.get(it.getDrugId());
            if (d == null) continue;
            it.setDrugName(d.getGenericName());
            it.setSpec(d.getSpec());
            it.setStorageCondition(d.getStorageCondition());
        }
        order.setItems(items);
        // 头部展示字段
        if (order.getSupplierId() != null) {
            order.setSupplierName(nameResolver.supplierNames(List.of(order.getSupplierId()))
                    .get(order.getSupplierId()));
        }
        if (order.getCreateBy() != null) {
            order.setCreateByName(nameResolver.userNames(List.of(order.getCreateBy()))
                    .get(order.getCreateBy()));
        }
        order.setQuantity(items.stream()
                .map(i -> i.getQuantity() == null ? BigDecimal.ZERO : i.getQuantity())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return order;
    }

    public List<PurchaseOrderItem> listItems(Long orderId) {
        return itemMapper.selectList(new LambdaQueryWrapper<PurchaseOrderItem>()
                .eq(PurchaseOrderItem::getOrderId, orderId));
    }

    /** 生成采购单号：PO + 日期 + 3 位以上序号（序号跟随最大主键单调递增，永不回退复用） */
    private String nextOrderNo() {
        Long maxId = baseMapper.selectMaxIdIncludingDeleted();
        long seq = (maxId == null ? 0L : maxId) + 1;
        return "PO" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%03d", seq);
    }

    /** 头部防呆校验：供应商/仓库必填且存在、备注长度上限（S11 外键悬空 / S13 超长备注） */
    private void validateHeader(PurchaseOrder order) {
        if (order.getSupplierId() == null) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "供应商不能为空");
        }
        if (supplierMapper.selectById(order.getSupplierId()) == null) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "供应商不存在");
        }
        if (order.getWarehouseId() == null) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "仓库不能为空");
        }
        if (warehouseMapper.selectById(order.getWarehouseId()) == null) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "仓库不存在");
        }
        if (order.getRemark() != null && order.getRemark().length() > 500) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "备注不能超过500字");
        }
    }

    /** 明细行防呆校验：药品必填且归属当前租户、数量必须为正、单价非负 */
    private void validateLine(PurchaseOrderItem item) {
        // S12 明细元素可能为 null（防 NPE）
        if (item == null) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "明细不能为空");
        }
        // 问题 77：明细药品必须存在且属于当前租户（selectById 受租户拦截器过滤，跨租户返回 null）
        if (item.getDrugId() == null) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "明细药品不能为空");
        }
        if (drugMapper.selectById(item.getDrugId()) == null) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "药品不存在或不属于当前租户");
        }
        if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "明细数量必须大于 0");
        }
        if (item.getPurchasePrice() == null || item.getPurchasePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "明细单价不能为负数");
        }
        // S13 数量/单价上限：防 1e20 等超大值落库（REAL 精度风险）
        if (item.getQuantity().compareTo(new BigDecimal("100000000")) > 0
                || item.getPurchasePrice().compareTo(new BigDecimal("100000000")) > 0) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "数量/单价超出合理范围");
        }
        // 单行金额上限：数量×单价可达 1e16，已超出 DECIMAL(18,4) 的整数位容量，必须单独拦截
        if (item.getQuantity().multiply(item.getPurchasePrice()).compareTo(MAX_TOTAL_AMOUNT) > 0) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "金额超出合理范围");
        }
    }

    /** 金额上限（1 万亿）：远大于正常医药采购额，仅用于拦截溢出/恶意超大值 */
    private static final BigDecimal MAX_TOTAL_AMOUNT = new BigDecimal("1000000000000");

    /** 整单金额上限校验（明细逐行校验后的兜底，防多行累加溢出） */
    private void checkTotalAmount(BigDecimal total) {
        if (total.compareTo(MAX_TOTAL_AMOUNT) > 0) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "订单金额超出合理范围");
        }
    }

    /** 删除：仅草稿（级联删明细 + 删头原子） */
    @Transactional
    public void deleteOrder(Long id) {
        PurchaseOrder order = this.getById(id);
        // 删除不存在/跨租户单据统一 404（此前 return 静默成功，返回 200 ok）
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "采购单不存在");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        itemMapper.delete(new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getOrderId, id));
        this.removeById(id);
    }

    public Page<PurchaseOrder> pageQuery(int page, int pageSize, String orderNo, String status) {
        LambdaQueryWrapper<PurchaseOrder> qw = new LambdaQueryWrapper<>();
        if (orderNo != null && !orderNo.isBlank()) qw.like(PurchaseOrder::getOrderNo, orderNo);
        // status 支持逗号分隔多值："1,2" -> 待审核+待入库
        if (status != null && !status.isBlank()) {
            List<Integer> statuses;
            try {
                statuses = java.util.Arrays.stream(status.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Integer::valueOf)
                        .toList();
            } catch (NumberFormatException e) {
                // 问题 78：status=abc 此前直接抛 NumberFormatException 被兜底为 500，改为明确的 400
                throw new BusinessException(ErrorCode.BAD_REQUEST, "status 参数不合法");
            }
            if (!statuses.isEmpty()) {
                qw.in(PurchaseOrder::getStatus, statuses);
            }
        }
        qw.orderByDesc(PurchaseOrder::getId);
        Page<PurchaseOrder> p = this.page(new Page<>(page, pageSize), qw);
        enrich(p.getRecords());
        return p;
    }

    /** 列表展示回填：供应商名称 / 药品明细摘要 / 明细总数量 / 经办人姓名 */
    private void enrich(List<PurchaseOrder> records) {
        if (records == null || records.isEmpty()) return;
        java.util.List<Long> orderIds = records.stream().map(PurchaseOrder::getId).toList();
        // 一次查出所有明细，按单分组
        java.util.Map<Long, List<PurchaseOrderItem>> itemMap = itemMapper.selectList(
                        new LambdaQueryWrapper<PurchaseOrderItem>()
                                .in(PurchaseOrderItem::getOrderId, orderIds))
                .stream().collect(java.util.stream.Collectors.groupingBy(PurchaseOrderItem::getOrderId));
        java.util.Map<Long, String> suppliers = nameResolver.supplierNames(
                records.stream().map(PurchaseOrder::getSupplierId).toList());
        java.util.Map<Long, String> users = nameResolver.userNames(
                records.stream().map(PurchaseOrder::getCreateBy).toList());
        // 明细涉及药品一次查名称
        java.util.List<Long> drugIds = itemMap.values().stream().flatMap(List::stream)
                .map(PurchaseOrderItem::getDrugId).distinct().toList();
        java.util.Map<Long, String> drugs = nameResolver.drugNames(drugIds);
        for (PurchaseOrder o : records) {
            o.setSupplierName(suppliers.get(o.getSupplierId()));
            o.setCreateByName(users.get(o.getCreateBy()));
            java.util.List<PurchaseOrderItem> its = itemMap.getOrDefault(o.getId(), List.of());
            o.setQuantity(its.stream().map(i -> i.getQuantity() == null ? BigDecimal.ZERO : i.getQuantity())
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            o.setDrugNames(its.stream().map(i -> {
                String name = drugs.getOrDefault(i.getDrugId(), "药品#" + i.getDrugId());
                String qty = i.getQuantity() == null ? "0" : i.getQuantity().stripTrailingZeros().toPlainString();
                return name + " ×" + qty;
            }).collect(java.util.stream.Collectors.joining("、")));
        }
    }
}
