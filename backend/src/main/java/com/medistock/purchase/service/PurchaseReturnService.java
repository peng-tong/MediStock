package com.medistock.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medistock.base.mapper.DrugMapper;
import com.medistock.base.mapper.SupplierMapper;
import com.medistock.base.mapper.WarehouseMapper;
import com.medistock.common.config.TenantContext;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.inventory.entity.StockBatch;
import com.medistock.inventory.mapper.StockBatchMapper;
import com.medistock.inventory.service.StockService;
import com.medistock.purchase.entity.PurchaseOrder;
import com.medistock.purchase.entity.PurchaseOrderItem;
import com.medistock.purchase.entity.PurchaseReturn;
import com.medistock.purchase.entity.PurchaseReturnItem;
import com.medistock.purchase.mapper.PurchaseOrderItemMapper;
import com.medistock.purchase.mapper.PurchaseOrderMapper;
import com.medistock.purchase.mapper.PurchaseReturnItemMapper;
import com.medistock.purchase.mapper.PurchaseReturnMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** 采购退货：退货即冲减批次库存（乐观锁保证并发安全） */
@Service
public class PurchaseReturnService extends ServiceImpl<PurchaseReturnMapper, PurchaseReturn> {

    private final PurchaseReturnItemMapper itemMapper;
    private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderItemMapper orderItemMapper;
    private final StockBatchMapper batchMapper;
    private final DrugMapper drugMapper;
    private final WarehouseMapper warehouseMapper;
    private final SupplierMapper supplierMapper;
    private final StockService stockService;

    public PurchaseReturnService(PurchaseReturnMapper returnMapper, PurchaseReturnItemMapper itemMapper,
                                 PurchaseOrderMapper orderMapper, PurchaseOrderItemMapper orderItemMapper,
                                 StockBatchMapper batchMapper, DrugMapper drugMapper,
                                 WarehouseMapper warehouseMapper, SupplierMapper supplierMapper,
                                 StockService stockService) {
        // ServiceImpl 在 MP 3.5.7 无带参构造器，直接为 protected baseMapper 赋值（同 Phase 1 UserService 修正方式）
        this.baseMapper = returnMapper;
        this.itemMapper = itemMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.batchMapper = batchMapper;
        this.drugMapper = drugMapper;
        this.warehouseMapper = warehouseMapper;
        this.supplierMapper = supplierMapper;
        this.stockService = stockService;
    }

    /** 数量/单价/金额上限（1 万亿），仅用于拦截恶意超大值与 DECIMAL(18,4) 溢出 */
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000000");

    @Transactional
    public Long createReturn(PurchaseReturn ret, List<PurchaseReturnItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "明细不能为空");
        }
        // 供应商必填且必须存在（selectById 受租户拦截器过滤，跨租户返回 null）：
        // 此前未校验，允许把其他租户的 supplierId 写进本租户退货单，列表回显供应商为空
        if (ret.getSupplierId() == null) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "供应商不能为空");
        }
        if (supplierMapper.selectById(ret.getSupplierId()) == null) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "供应商不存在或不属于当前租户");
        }
        // 备注长度上限（与表结构 VARCHAR(255) 对齐，防止超长文本落库）
        if (ret.getRemark() != null && ret.getRemark().length() > 255) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "备注不能超过255字");
        }
        // S12 缺必填：退货仓库必填，避免空参走到库存层 selectById(null) 抛 500
        if (ret.getWarehouseId() == null) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "退货仓库不能为空");
        }
        // 问题 77：仓库必须存在且属于当前租户（selectById 受租户拦截器过滤，跨租户返回 null）
        if (warehouseMapper.selectById(ret.getWarehouseId()) == null) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "仓库不存在或不属于当前租户");
        }
        // S8/S9 关联采购单校验（仅当指定了采购单时执行；orderId 为 null 属直接退货场景，跳过单据校验）
        if (ret.getOrderId() != null) {
            PurchaseOrder order = orderMapper.selectById(ret.getOrderId());
            if (order == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "采购单不存在");
            }
            // 仅已入库(3)的采购单允许退货，草稿/待审核单一律拦截
            if (order.getStatus() == null || order.getStatus() != 3) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "仅已入库的采购单可退货");
            }
            // 退货仓库必须与采购单入库仓库一致（防跨仓误冲）
            if (ret.getWarehouseId() == null || !ret.getWarehouseId().equals(order.getWarehouseId())) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "仓库与采购单不一致");
            }
        }
        // S5/S7 逐行校验（必须放在金额乘法之前，杜绝负金额单与 NPE）
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseReturnItem item : items) {
            if (item == null) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "明细不能为空");
            }
            if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "退货数量必须大于 0");
            }
            if (item.getReturnPrice() == null || item.getReturnPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "退货单价不能为负数");
            }
            // 数量/单价/金额上限：金额列为 DECIMAL(18,4)，恶意超大值会导致溢出或精度丢失
            if (item.getQuantity().compareTo(MAX_AMOUNT) > 0
                    || item.getReturnPrice().compareTo(MAX_AMOUNT) > 0) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "数量/单价超出合理范围");
            }
            if (item.getQuantity().multiply(item.getReturnPrice()).compareTo(MAX_AMOUNT) > 0) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "金额超出合理范围");
            }
            // S12 缺必填：每行必须指定药品与退货批次，否则库存层 selectById(null) 抛 500
            if (item.getDrugId() == null || item.getBatchId() == null) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "退货必须指定药品与批次");
            }
            // 问题 77：药品必须存在且属于当前租户（防止跨租户引用药品）
            if (drugMapper.selectById(item.getDrugId()) == null) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "药品不存在或不属于当前租户");
            }
            // S9 归属校验：批次必须存在，且该批次（批号）须属于本采购单（入库时 purchase_order_item.batch_no 已回填）
            if (ret.getOrderId() != null) {
                StockBatch batch = batchMapper.selectById(item.getBatchId());
                if (batch == null) {
                    throw new BusinessException(ErrorCode.NOT_FOUND, "批次不存在");
                }
                Long matched = orderItemMapper.selectCount(new LambdaQueryWrapper<PurchaseOrderItem>()
                        .eq(PurchaseOrderItem::getOrderId, ret.getOrderId())
                        .eq(PurchaseOrderItem::getDrugId, item.getDrugId())
                        .eq(PurchaseOrderItem::getBatchNo, batch.getBatchNo()));
                if (matched == null || matched <= 0) {
                    throw new BusinessException(ErrorCode.VALIDATE_ERROR, "退货批次不属于该采购单");
                }
            }
            total = total.add(item.getReturnPrice().multiply(item.getQuantity()));
        }
        // 整单退货金额上限（多行累加兜底）
        if (total.compareTo(MAX_AMOUNT) > 0) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "退货金额超出合理范围");
        }
        // 退货单号同样取最大主键 +1，保证删除/并发下不复用（与采购单号口径一致）
        Long maxId = baseMapper.selectMaxIdIncludingDeleted();
        long seq = (maxId == null ? 0L : maxId) + 1;
        ret.setReturnNo("PR" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%03d", seq));
        ret.setTotalAmount(total);
        ret.setStatus(1);   // 创建即完成（简化 P0）
        // 超管（tenantId=0）时租户拦截器不注入 tenant_id，必须显式补默认值（防 NOT NULL 违反）
        ret.setTenantId(TenantContext.getTenantId() == null ? 0L : TenantContext.getTenantId());
        this.save(ret);
        for (PurchaseReturnItem item : items) {
            item.setReturnId(ret.getId());
            item.setAmount(item.getReturnPrice().multiply(item.getQuantity()));
            item.setTenantId(ret.getTenantId());
            itemMapper.insert(item);
            // 冲减指定批次
            stockService.deductBatch(item.getDrugId(), ret.getWarehouseId(), item.getBatchId(),
                    item.getQuantity(), ret.getReturnNo(), "采购退货");
        }
        return ret.getId();
    }
}
