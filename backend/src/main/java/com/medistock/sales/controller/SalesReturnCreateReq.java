package com.medistock.sales.controller;

import com.medistock.sales.entity.SalesReturn;
import com.medistock.sales.entity.SalesReturnItem;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 创建销售退货单请求：头 + 明细 */
@Data
public class SalesReturnCreateReq {
    @NotNull
    private SalesReturn salesReturn;
    @NotNull
    private List<SalesReturnItem> items;
}
