package com.medistock.sales.controller;

import com.medistock.sales.entity.SalesOrder;
import com.medistock.sales.entity.SalesOrderItem;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 创建销售单请求：头 + 明细 */
@Data
public class SalesOrderCreateReq {
    @NotNull
    private SalesOrder order;
    @NotNull
    private List<SalesOrderItem> items;
}
