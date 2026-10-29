package com.medistock.common.exception;

import lombok.Getter;

/** 业务错误码 */
@Getter
public enum ErrorCode {
    OK(0, "ok"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限操作"),
    NOT_FOUND(404, "资源不存在"),
    CONCURRENT_CONFLICT(409, "系统繁忙，请重试"),
    VALIDATE_ERROR(422, "业务校验失败"),
    INTERNAL_ERROR(500, "系统内部错误"),
    // 业务码 1000 起
    CAPTCHA_INVALID(1001, "验证码错误或已过期"),
    LOGIN_FAILED(1002, "用户名或密码错误"),
    USER_DISABLED(1003, "账号已被禁用"),
    TENANT_DISABLED(1004, "租户已停用"),
    USER_EXISTS(1005, "用户名已存在"),
    DRUG_IN_STOCK(1006, "药品存在库存，禁止删除"),
    ORDER_STATUS_INVALID(1007, "单据状态不允许该操作"),
    STOCK_INSUFFICIENT(1008, "库存不足"),
    BATCH_EXPIRED(1009, "存在过期批次，禁止出库"),
    SUPPLIER_CUSTOMER_REFERENCED(1010, "已被单据引用，禁止删除");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
