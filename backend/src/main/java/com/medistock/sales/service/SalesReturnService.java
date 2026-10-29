package com.medistock.sales.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medistock.inventory.service.StockService;
import com.medistock.sales.entity.SalesReturn;
import com.medistock.sales.entity.SalesReturnItem;
import com.medistock.sales.mapper.SalesReturnItemMapper;
import com.medistock.sales.mapper.SalesReturnMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** 销售退货：退货即回补指定批次库存 */
@Service
public class SalesReturnService extends ServiceImpl<SalesReturnMapper, SalesReturn> {

    private final SalesReturnItemMapper itemMapper;
    private final StockService stockService;

    public SalesReturnService(SalesReturnMapper returnMapper, SalesReturnItemMapper itemMapper,
                              StockService stockService) {
        // ServiceImpl 在 MP 3.5.7 无带参构造器，直接为 protected baseMapper 赋值（同 Phase 4 PurchaseReturnService）
        this.baseMapper = returnMapper;
        this.itemMapper = itemMapper;
        this.stockService = stockService;
    }

    @Transactional
    public Long createReturn(SalesReturn ret, List<SalesReturnItem> items) {
        BigDecimal total = items.stream()
                .map(i -> i.getReturnPrice().multiply(i.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ret.setReturnNo("SR" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%03d", this.count(null) + 1));
        ret.setTotalAmount(total);
        ret.setStatus(1);   // 创建即完成（简化 P0）
        this.save(ret);
        for (SalesReturnItem item : items) {
            item.setReturnId(ret.getId());
            item.setAmount(item.getReturnPrice().multiply(item.getQuantity()));
            itemMapper.insert(item);
            // 回补指定批次（过期批次可退，逻辑由上层单据控制）
            stockService.restockBatch(item.getDrugId(), ret.getWarehouseId(), item.getBatchId(),
                    item.getQuantity(), ret.getReturnNo(), "销售退货");
        }
        return ret.getId();
    }
}
