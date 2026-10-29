package com.medistock.purchase.controller;

import com.medistock.purchase.entity.PurchaseOrder;
import com.medistock.purchase.entity.PurchaseOrderItem;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 创建采购单请求：头 + 明细 */
@Data
public class PurchaseOrderCreateReq {
    @NotNull
    private PurchaseOrder order;
    @NotNull
    private List<PurchaseOrderItem> items;
}
