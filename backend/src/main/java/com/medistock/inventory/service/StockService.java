package com.medistock.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medistock.common.config.TenantContext;
import com.medistock.common.config.UserContext;
import com.medistock.common.constant.BizConstant;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.inventory.entity.InventoryFlow;
import com.medistock.inventory.entity.Stock;
import com.medistock.inventory.entity.StockBatch;
import com.medistock.inventory.mapper.InventoryFlowMapper;
import com.medistock.inventory.mapper.StockBatchMapper;
import com.medistock.inventory.mapper.StockMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 库存并发原语层：所有库存变动（入库/出库/退货/盘点）的唯一入口。
 *
 * 并发策略（0.4 节）：乐观锁 @Version + 事务重试。
 * - 事务内：读最新批次 → 计算 → UPDATE（自动带 version 条件）
 * - 冲突：OptimisticLockingFailureException（版本不一致）或
 *        DuplicateKeyException（并发插入同唯一键）→ 回滚 → 外层重试重读
 * - 最多 MAX_RETRY 次，仍失败抛"系统繁忙"
 */
@Slf4j
@Service
public class StockService {

    private final StockMapper stockMapper;
    private final StockBatchMapper batchMapper;
    private final InventoryFlowMapper flowMapper;
    private final TransactionTemplate transactionTemplate;

    public StockService(StockMapper stockMapper, StockBatchMapper batchMapper,
                        InventoryFlowMapper flowMapper, PlatformTransactionManager transactionManager) {
        this.stockMapper = stockMapper;
        this.batchMapper = batchMapper;
        this.flowMapper = flowMapper;
        // 私有 TransactionTemplate：嵌套事务（savepoint）语义不变，但隔离对容器单例传播级别的副作用
        // （若直接复用容器装配的单例并 setPropagationBehavior，会污染其他调用方的事务行为）
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
    }

    private static final AtomicLong SEQ = new AtomicLong();

    // ==================== 对外原语 ====================

    /** 采购入库：同批号存在则累加，不存在则新建批次（正数） */
    public void inbound(Long drugId, Long warehouseId, String batchNo, LocalDate expiryDate,
                        BigDecimal qty, String businessNo, String remark) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "入库数量必须大于 0");
        }
        retry(() -> transactionTemplate.execute(status -> {
            StockBatch batch = findBatch(drugId, warehouseId, batchNo);
            if (batch == null) {
                batch = newBatch(drugId, warehouseId, batchNo, expiryDate, qty);
            } else {
                batch.setQuantity(batch.getQuantity().add(qty));
                updateWithVersion(batchMapper, batch);   // 乐观锁生效
            }
            adjustStock(drugId, warehouseId, qty, businessNo,
                    BizConstant.FLOW_INBOUND, batch, remark);
            return null;
        }));
    }

    /** 销售出库：按 FIFO（有效期升序）扣减指定数量（正数），不足抛库存不足 */
    public void outboundFifo(Long drugId, Long warehouseId, BigDecimal qty,
                             String businessNo, String remark) {
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "出库数量必须大于 0");
        }
        retry(() -> transactionTemplate.execute(status -> {
            BigDecimal remaining = qty;
            // 未过期批次按有效期升序（FIFO）
            List<StockBatch> batches = batchMapper.selectList(new LambdaQueryWrapper<StockBatch>()
                    .eq(StockBatch::getDrugId, drugId)
                    .eq(StockBatch::getWarehouseId, warehouseId)
                    .gt(StockBatch::getExpiryDate, LocalDate.now())   // 过期拦截：过期批次不可出库
                    .orderByAsc(StockBatch::getExpiryDate)
                    .orderByAsc(StockBatch::getId));
            for (StockBatch b : batches) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
                BigDecimal take = b.getQuantity().min(remaining);
                b.setQuantity(b.getQuantity().subtract(take));
                updateWithVersion(batchMapper, b);    // 乐观锁生效
                adjustStock(drugId, warehouseId, take.negate(), businessNo,
                        BizConstant.FLOW_OUTBOUND, b, remark);
                remaining = remaining.subtract(take);
            }
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT);   // 负库存拦截
            }
            return null;
        }));
    }

    /** 销售退货回补：累加指定批次（正数） */
    public void restockBatch(Long drugId, Long warehouseId, Long batchId, BigDecimal qty,
                             String businessNo, String remark) {
        retry(() -> transactionTemplate.execute(status -> {
            StockBatch batch = batchMapper.selectById(batchId);
            if (batch == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "批次不存在");
            }
            // 批次归属校验：防跨药品/跨仓库误冲（脏数据或参数错配时直接拦截）
            if (!batch.getDrugId().equals(drugId) || !batch.getWarehouseId().equals(warehouseId)) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "批次与药品/仓库不匹配");
            }
            batch.setQuantity(batch.getQuantity().add(qty));
            updateWithVersion(batchMapper, batch);
            adjustStock(drugId, warehouseId, qty, businessNo,
                    BizConstant.FLOW_SALES_RETURN, batch, remark);
            return null;
        }));
    }

    /** 采购退货冲减：扣减指定批次（正数），不足抛异常 */
    public void deductBatch(Long drugId, Long warehouseId, Long batchId, BigDecimal qty,
                            String businessNo, String remark) {
        retry(() -> transactionTemplate.execute(status -> {
            StockBatch batch = batchMapper.selectById(batchId);
            if (batch == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "批次不存在");
            }
            // 批次归属校验：防跨药品/跨仓库误冲（脏数据或参数错配时直接拦截）
            if (!batch.getDrugId().equals(drugId) || !batch.getWarehouseId().equals(warehouseId)) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "批次与药品/仓库不匹配");
            }
            if (batch.getQuantity().compareTo(qty) < 0) {
                throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT);
            }
            batch.setQuantity(batch.getQuantity().subtract(qty));
            updateWithVersion(batchMapper, batch);
            adjustStock(drugId, warehouseId, qty.negate(), businessNo,
                    BizConstant.FLOW_PURCHASE_RETURN, batch, remark);
            return null;
        }));
    }

    /** 盘点差异调整：盘盈为正、盘亏为负，直接作用于指定批次 */
    public void adjustBatch(Long drugId, Long warehouseId, Long batchId, BigDecimal diffQty,
                            String businessNo, String remark) {
        retry(() -> transactionTemplate.execute(status -> {
            StockBatch batch = batchMapper.selectById(batchId);
            if (batch == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "批次不存在");
            }
            // 批次归属校验：防跨药品/跨仓库误调（脏数据或参数错配时直接拦截，与 restockBatch/deductBatch 口径一致）
            if (!batch.getDrugId().equals(drugId) || !batch.getWarehouseId().equals(warehouseId)) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "批次与药品/仓库不匹配");
            }
            if (batch.getQuantity().add(diffQty).compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "盘亏超过批次库存");
            }
            batch.setQuantity(batch.getQuantity().add(diffQty));
            updateWithVersion(batchMapper, batch);
            adjustStock(drugId, warehouseId, diffQty, businessNo,
                    BizConstant.FLOW_STOCKTAKE, batch, remark);
            return null;
        }));
    }

    // ==================== 内部实现 ====================

    /**
     * 乐观锁更新：MP 3.5.7 的 updateById 在 version 冲突（影响 0 行）时仅返回 false 而不抛异常，
     * 必须显式检查返回值，冲突时抛 OptimisticLockingFailureException 交由外层 retry 重读重试。
     */
    private <T> void updateWithVersion(BaseMapper<T> mapper, T entity) {
        if (mapper.updateById(entity) == 0) {
            throw new OptimisticLockingFailureException("乐观锁版本冲突");
        }
    }

    /** 事务模板包装的重试循环（乐观锁冲突 / 唯一键冲突 → 重读重试） */
    private void retry(Runnable action) {
        int attempt = 0;
        while (true) {
            try {
                action.run();
                return;
            } catch (OptimisticLockingFailureException | DuplicateKeyException e) {
                // 仅收窄为乐观锁版本冲突与唯一键冲突（并发同唯一键插入），
                // 避免把 NOT NULL 违反等真实数据错误误当并发冲突重试后误报"系统繁忙"
                log.info("库存并发冲突，第 {} 次重试", attempt + 1);
                if (++attempt >= BizConstant.MAX_RETRY) {
                    throw new BusinessException(ErrorCode.CONCURRENT_CONFLICT);
                }
            }
        }
    }

    /** 更新汇总库存 + 写流水（同一事务内） */
    private void adjustStock(Long drugId, Long warehouseId, BigDecimal changeQty, String businessNo,
                             int businessType, StockBatch batch, String remark) {
        Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getDrugId, drugId)
                .eq(Stock::getWarehouseId, warehouseId));
        if (stock == null) {
            stock = new Stock();
            stock.setDrugId(drugId);
            stock.setWarehouseId(warehouseId);
            stock.setQuantity(changeQty);
            stock.setAvailableQuantity(changeQty);
            // 超管（tenantId=0）时租户拦截器不注入 tenant_id，必须显式补默认值（防 NOT NULL 违反）
            stock.setTenantId(TenantContext.getTenantId() == null ? 0L : TenantContext.getTenantId());
            stockMapper.insert(stock);   // 并发插入时唯一索引兜底
        } else {
            stock.setQuantity(stock.getQuantity().add(changeQty));
            stock.setAvailableQuantity(stock.getAvailableQuantity().add(changeQty));
            updateWithVersion(stockMapper, stock);   // 乐观锁生效
        }
        insertFlow(businessType, businessNo, drugId, warehouseId, batch,
                changeQty, remark);
    }

    private StockBatch findBatch(Long drugId, Long warehouseId, String batchNo) {
        return batchMapper.selectOne(new LambdaQueryWrapper<StockBatch>()
                .eq(StockBatch::getDrugId, drugId)
                .eq(StockBatch::getWarehouseId, warehouseId)
                .eq(StockBatch::getBatchNo, batchNo));
    }

    private StockBatch newBatch(Long drugId, Long warehouseId, String batchNo,
                                LocalDate expiryDate, BigDecimal qty) {
        StockBatch b = new StockBatch();
        b.setDrugId(drugId);
        b.setWarehouseId(warehouseId);
        b.setBatchNo(batchNo);
        b.setExpiryDate(expiryDate);
        b.setQuantity(qty);
        b.setInDate(LocalDate.now());
        b.setStatus(0);
        b.setTenantId(TenantContext.getTenantId() == null ? 0L : TenantContext.getTenantId());
        batchMapper.insert(b);
        return b;
    }

    private void insertFlow(int businessType, String businessNo, Long drugId, Long warehouseId,
                            StockBatch batch, BigDecimal changeQty, String remark) {
        InventoryFlow flow = new InventoryFlow();
        flow.setFlowNo("FL" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%06d", SEQ.incrementAndGet()));
        flow.setBusinessType(businessType);
        flow.setBusinessNo(businessNo);
        flow.setDrugId(drugId);
        flow.setWarehouseId(warehouseId);
        flow.setBatchId(batch.getId());
        flow.setChangeQuantity(changeQty);
        flow.setBeforeQuantity(batch.getQuantity().subtract(changeQty));
        flow.setAfterQuantity(batch.getQuantity());
        flow.setOperator(UserContext.getUserId());
        flow.setRemark(remark);
        flow.setTenantId(TenantContext.getTenantId() == null ? 0L : TenantContext.getTenantId());
        flowMapper.insert(flow);
    }
}
