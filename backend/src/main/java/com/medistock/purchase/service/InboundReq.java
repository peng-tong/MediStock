package com.medistock.purchase.service;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 采购入库请求 */
@Data
public class InboundReq {
    private List<Item> items;

    @Data
    public static class Item {
        private Long itemId;        // 采购单明细 id
        private BigDecimal quantity; // 实收数量
        private String batchNo;     // 生产批号
        private LocalDate expiryDate; // 有效期至
    }
}
