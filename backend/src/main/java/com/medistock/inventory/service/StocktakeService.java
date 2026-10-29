package com.medistock.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medistock.common.config.UserContext;
import com.medistock.common.constant.BizConstant;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.inventory.entity.StockBatch;
import com.medistock.inventory.entity.Stocktake;
import com.medistock.inventory.entity.StocktakeItem;
import com.medistock.inventory.mapper.StockBatchMapper;
import com.medistock.inventory.mapper.StocktakeItemMapper;
import com.medistock.inventory.mapper.StocktakeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** 盘点流程：按仓库快照 → 录实盘 → 确认后逐批次调整库存 */
@Service
public class StocktakeService extends ServiceImpl<StocktakeMapper, Stocktake> {

    private final StocktakeItemMapper itemMapper;
    private final StockBatchMapper batchMapper;
    private final StockService stockService;

    public StocktakeService(StocktakeMapper stocktakeMapper, StocktakeItemMapper itemMapper,
                            StockBatchMapper batchMapper, StockService stockService) {
        // ServiceImpl 在 MP 3.5.7 无带参构造器，直接为 protected baseMapper 赋值（同 Phase 1 UserService 修正方式）
        this.baseMapper = stocktakeMapper;
        this.itemMapper = itemMapper;
        this.batchMapper = batchMapper;
        this.stockService = stockService;
    }

    /** 创建盘点单：快照该仓库所有批次为账面数 */
    @Transactional
    public Long createStocktake(Long warehouseId, String remark) {
        Stocktake st = new Stocktake();
        st.setTakeNo("ST" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%03d", this.count(null) + 1));
        st.setWarehouseId(warehouseId);
        st.setStatus(0);
        st.setCreateBy(UserContext.getUserId());
        st.setRemark(remark);
        this.save(st);

        List<StockBatch> batches = batchMapper.selectList(new LambdaQueryWrapper<StockBatch>()
                .eq(StockBatch::getWarehouseId, warehouseId));
        for (StockBatch b : batches) {
            StocktakeItem item = new StocktakeItem();
            item.setTakeId(st.getId());
            item.setDrugId(b.getDrugId());
            item.setBatchId(b.getId());
            item.setBookQuantity(b.getQuantity());
            // actualQuantity 保持 null：未录入实盘（确认前由 confirm 校验拦截，防漏录直接确认）
            item.setDiffQuantity(BigDecimal.ZERO);
            itemMapper.insert(item);
        }
        return st.getId();
    }

    /** 录入实盘数量（仅允许 status==0 的进行中盘点单） */
    public void updateActual(Long itemId, BigDecimal actualQuantity) {
        // 入口防呆：实盘数量为空时直接拦截，避免后续 NPE（bookQuantity 相减）
        if (actualQuantity == null) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "实盘数量不能为空");
        }
        StocktakeItem item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        Stocktake st = this.getById(item.getTakeId());
        if (st == null || st.getStatus() != 0) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        item.setActualQuantity(actualQuantity);
        item.setDiffQuantity(actualQuantity.subtract(item.getBookQuantity()));
        itemMapper.updateById(item);
    }

    /** 确认盘点：先校验全部明细已录入实盘，再对每条差异调用 StockService.adjustBatch（含乐观锁），生成盘点调整流水 */
    @Transactional
    public void confirm(Long takeId) {
        Stocktake st = this.getById(takeId);
        if (st == null || st.getStatus() != 0) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        List<StocktakeItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<StocktakeItem>().eq(StocktakeItem::getTakeId, takeId));
        // 防漏录：存在未录入实盘的明细时禁止确认
        for (StocktakeItem item : items) {
            if (item.getActualQuantity() == null) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "存在未录入实盘的明细");
            }
        }
        for (StocktakeItem item : items) {
            if (item.getDiffQuantity().compareTo(BigDecimal.ZERO) != 0) {
                stockService.adjustBatch(item.getDrugId(), st.getWarehouseId(), item.getBatchId(),
                        item.getDiffQuantity(), st.getTakeNo(), "盘点调整");
            }
        }
        st.setStatus(1);
        this.updateById(st);
    }
}
