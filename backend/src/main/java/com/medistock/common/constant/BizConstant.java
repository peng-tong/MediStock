package com.medistock.common.constant;

/** 业务常量 */
public final class BizConstant {
    private BizConstant() {}

    /** 租户拦截器忽略表：平台级表，无 tenant_id */
    public static final String IGNORE_TABLE_TENANT = "sys_tenant";
    public static final String IGNORE_TABLE_MENU = "sys_menu";

    /** 库存流水业务类型 */
    public static final int FLOW_INBOUND = 1;      // 采购入库
    public static final int FLOW_PURCHASE_RETURN = 2; // 采购退货
    public static final int FLOW_OUTBOUND = 3;     // 销售出库
    public static final int FLOW_SALES_RETURN = 4; // 销售退货
    public static final int FLOW_STOCKTAKE = 5;    // 盘点调整
    public static final int FLOW_TRANSFER = 6;     // 调拨（P1 预留）

    /** 乐观锁最大重试次数 */
    public static final int MAX_RETRY = 5;

    /** 单号前缀 */
    public static final String PREFIX_PO = "PO";  // 采购单
    public static final String PREFIX_PR = "PR";  // 采购退货
    public static final String PREFIX_SO = "SO";  // 销售单
    public static final String PREFIX_SR = "SR";  // 销售退货
    public static final String PREFIX_ST = "ST";  // 盘点单
}
