package com.medistock.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medistock.base.mapper.CustomerMapper;
import com.medistock.base.mapper.DrugMapper;
import com.medistock.base.mapper.WarehouseMapper;
import com.medistock.common.config.TenantContext;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.inventory.entity.StockBatch;
import com.medistock.inventory.mapper.StockBatchMapper;
import com.medistock.inventory.service.StockService;
import com.medistock.sales.entity.SalesOrder;
import com.medistock.sales.entity.SalesOrderItem;
import com.medistock.sales.entity.SalesReturn;
import com.medistock.sales.entity.SalesReturnItem;
import com.medistock.sales.mapper.SalesOrderItemMapper;
import com.medistock.sales.mapper.SalesOrderMapper;
import com.medistock.sales.mapper.SalesReturnItemMapper;
import com.medistock.sales.mapper.SalesReturnMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 销售退货：退货即回补指定批次库存，可关联来源销售单（退不能超过卖） */
@Service
public class SalesReturnService extends ServiceImpl<SalesReturnMapper, SalesReturn> {

    private final SalesReturnItemMapper itemMapper;
    private final StockService stockService;
    private final SalesOrderMapper orderMapper;
    private final SalesOrderItemMapper orderItemMapper;
    private final DrugMapper drugMapper;
    private final WarehouseMapper warehouseMapper;
    private final CustomerMapper customerMapper;
    private final StockBatchMapper batchMapper;

    public SalesReturnService(SalesReturnMapper returnMapper, SalesReturnItemMapper itemMapper,
                              StockService stockService, SalesOrderMapper orderMapper,
                              SalesOrderItemMapper orderItemMapper, DrugMapper drugMapper,
                              WarehouseMapper warehouseMapper, CustomerMapper customerMapper,
                              StockBatchMapper batchMapper) {
        // ServiceImpl 在 MP 3.5.7 无带参构造器，直接为 protected baseMapper 赋值（同 Phase 4 PurchaseReturnService）
        this.baseMapper = returnMapper;
        this.itemMapper = itemMapper;
        this.stockService = stockService;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.drugMapper = drugMapper;
        this.warehouseMapper = warehouseMapper;
        this.customerMapper = customerMapper;
        this.batchMapper = batchMapper;
    }

    @Transactional
    public Long createReturn(SalesReturn ret, List<SalesReturnItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "明细不能为空");
        }
        // 备注长度与表结构列宽一致（VARCHAR(255)），防超长文本绕过前端落库
        if (ret.getRemark() != null && ret.getRemark().length() > 255) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "备注不能超过255字");
        }
        // 问题 77：退货仓库/客户必须存在且属于当前租户
        // customer_id 为 NOT NULL 列：缺失会导致 SQLite 约束 500，必须在 Service 层强制必填（与仓库一致）
        if (ret.getWarehouseId() == null) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "退货仓库不能为空");
        }
        if (warehouseMapper.selectById(ret.getWarehouseId()) == null) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "仓库不存在或不属于当前租户");
        }
        if (ret.getCustomerId() == null) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "退货客户不能为空");
        }
        if (customerMapper.selectById(ret.getCustomerId()) == null) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "客户不存在或不属于当前租户");
        }
        for (SalesReturnItem item : items) {
            if (item == null) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "明细不能为空");
            }
            // 防呆校验：数量必须为正、退货单价非负，杜绝负数退货
            if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "退货数量必须大于 0");
            }
            if (item.getReturnPrice() == null || item.getReturnPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "退货单价不能为负数");
            }
            // 数量/单价及单行金额上限：防超大值落库导致金额失真（SQLite 不校验 DECIMAL 精度）
            if (item.getQuantity().compareTo(MAX_QUANTITY_PRICE) > 0
                    || item.getReturnPrice().compareTo(MAX_QUANTITY_PRICE) > 0) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "退货数量/单价超出合理范围");
            }
            if (item.getReturnPrice().multiply(item.getQuantity()).compareTo(MAX_TOTAL_AMOUNT) > 0) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "退货金额超出合理范围");
            }
            // 问题 77：药品与批次必填且归属当前租户，批次须与药品/仓库匹配（防止跨租户、跨药品、跨仓回补）
            if (item.getDrugId() == null || item.getBatchId() == null) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "退货必须指定药品与批次");
            }
            if (drugMapper.selectById(item.getDrugId()) == null) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "药品不存在或不属于当前租户");
            }
            StockBatch batch = batchMapper.selectById(item.getBatchId());
            if (batch == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "批次不存在或不属于当前租户");
            }
            if (!batch.getDrugId().equals(item.getDrugId()) || !batch.getWarehouseId().equals(ret.getWarehouseId())) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "批次与药品/仓库不匹配");
            }
        }
        // 退不能超过卖：关联销售单时校验（未关联则跳过，支持直接退货）
        validateAgainstOrder(ret.getOrderId(), items);
        BigDecimal total = items.stream()
                .map(i -> i.getReturnPrice().multiply(i.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 整单金额上限兜底：防多行累加溢出
        if (total.compareTo(MAX_TOTAL_AMOUNT) > 0) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "退货金额超出合理范围");
        }
        ret.setReturnNo("SR" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%03d", this.count(null) + 1));
        ret.setTotalAmount(total);
        ret.setStatus(1);   // 创建即完成（简化 P0）
        // 超管（tenantId=0）时租户拦截器不注入 tenant_id，必须显式补默认值（防 NOT NULL 违反）
        ret.setTenantId(TenantContext.getTenantId() == null ? 0L : TenantContext.getTenantId());
        this.save(ret);
        for (SalesReturnItem item : items) {
            item.setReturnId(ret.getId());
            item.setAmount(item.getReturnPrice().multiply(item.getQuantity()));
            item.setTenantId(ret.getTenantId());
            itemMapper.insert(item);
            // 回补指定批次（过期批次可退，逻辑由上层单据控制）
            stockService.restockBatch(item.getDrugId(), ret.getWarehouseId(), item.getBatchId(),
                    item.getQuantity(), ret.getReturnNo(), "销售退货");
        }
        return ret.getId();
    }

    /** 退不能超过卖：退货数量 ≤ 来源销售单该药品出库数量 − 该单累计已退数量 */
    private void validateAgainstOrder(Long orderId, List<SalesReturnItem> items) {
        if (orderId == null) {
            return;  // 未关联销售单的直接退货不校验（系统允许）
        }
        SalesOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "关联销售单不存在");
        }
        if (order.getStatus() == null || order.getStatus() != 3) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "仅已出库的销售单可退货");
        }
        // 该销售单各药品的出库数量
        Map<Long, BigDecimal> outbound = orderItemMapper.selectList(
                        new LambdaQueryWrapper<SalesOrderItem>().eq(SalesOrderItem::getOrderId, orderId))
                .stream().collect(Collectors.toMap(SalesOrderItem::getDrugId,
                        i -> i.getOutboundQuantity() == null ? BigDecimal.ZERO : i.getOutboundQuantity(),
                        BigDecimal::add));
        // 该销售单已关联退货的累计数量（按药品）
        List<SalesReturn> rets = this.list(new LambdaQueryWrapper<SalesReturn>()
                .eq(SalesReturn::getOrderId, orderId)
                .eq(SalesReturn::getStatus, 1));
        List<Long> returnIds = rets.stream().map(SalesReturn::getId).toList();
        Map<Long, BigDecimal> returned = returnIds.isEmpty() ? Map.of()
                : itemMapper.selectList(new LambdaQueryWrapper<SalesReturnItem>()
                                .in(SalesReturnItem::getReturnId, returnIds))
                        .stream().collect(Collectors.toMap(SalesReturnItem::getDrugId,
                                i -> i.getQuantity() == null ? BigDecimal.ZERO : i.getQuantity(),
                                BigDecimal::add));
        /*
         * 同一药品可能被拆成多行明细（如不同批号/单价）。
         * 必须先把本次请求按药品汇总再与可退数量比较，否则每行单独校验都能通过，
         * 合计退货量会超过已出库量（历史缺陷：一次请求两行各退 6，实际只卖出 10）。
         */
        Map<Long, BigDecimal> requested = items.stream()
                .collect(Collectors.toMap(SalesReturnItem::getDrugId,
                        i -> i.getQuantity() == null ? BigDecimal.ZERO : i.getQuantity(),
                        BigDecimal::add));
        for (Map.Entry<Long, BigDecimal> e : requested.entrySet()) {
            BigDecimal maxReturn = outbound.getOrDefault(e.getKey(), BigDecimal.ZERO)
                    .subtract(returned.getOrDefault(e.getKey(), BigDecimal.ZERO));
            if (e.getValue().compareTo(maxReturn) > 0) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR,
                        "退货数量超过该销售单可退数量（最多可退 " + maxReturn.stripTrailingZeros().toPlainString() + "）");
            }
        }
    }

    /** 数量/单价上限（1 亿）：仅用于拦截溢出/恶意超大值 */
    private static final BigDecimal MAX_QUANTITY_PRICE = new BigDecimal("100000000");

    /** 金额上限（1 万亿）：用于拦截单行/整单金额溢出 */
    private static final BigDecimal MAX_TOTAL_AMOUNT = new BigDecimal("1000000000000");
}
