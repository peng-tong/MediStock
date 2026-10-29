package com.medistock.purchase.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.inventory.service.StockService;
import com.medistock.purchase.entity.PurchaseReturn;
import com.medistock.purchase.entity.PurchaseReturnItem;
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
    private final StockService stockService;

    public PurchaseReturnService(PurchaseReturnMapper returnMapper, PurchaseReturnItemMapper itemMapper,
                                 StockService stockService) {
        // ServiceImpl 在 MP 3.5.7 无带参构造器，直接为 protected baseMapper 赋值（同 Phase 1 UserService 修正方式）
        this.baseMapper = returnMapper;
        this.itemMapper = itemMapper;
        this.stockService = stockService;
    }

    @Transactional
    public Long createReturn(PurchaseReturn ret, List<PurchaseReturnItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "明细不能为空");
        }
        BigDecimal total = items.stream()
                .map(i -> i.getReturnPrice().multiply(i.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ret.setReturnNo("PR" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%03d", this.count(null) + 1));
        ret.setTotalAmount(total);
        ret.setStatus(1);   // 创建即完成（简化 P0）
        this.save(ret);
        for (PurchaseReturnItem item : items) {
            item.setReturnId(ret.getId());
            item.setAmount(item.getReturnPrice().multiply(item.getQuantity()));
            itemMapper.insert(item);
            // 冲减指定批次
            stockService.deductBatch(item.getDrugId(), ret.getWarehouseId(), item.getBatchId(),
                    item.getQuantity(), ret.getReturnNo(), "采购退货");
        }
        return ret.getId();
    }
}
