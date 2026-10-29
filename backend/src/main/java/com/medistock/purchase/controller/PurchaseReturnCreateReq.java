package com.medistock.purchase.controller;

import com.medistock.purchase.entity.PurchaseReturn;
import com.medistock.purchase.entity.PurchaseReturnItem;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 创建采购退货单请求：头 + 明细 */
@Data
public class PurchaseReturnCreateReq {
    @NotNull
    private PurchaseReturn purchaseReturn;
    @NotNull
    private List<PurchaseReturnItem> items;
}
