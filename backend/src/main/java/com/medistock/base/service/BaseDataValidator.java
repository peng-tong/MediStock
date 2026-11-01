package com.medistock.base.service;

import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;

import java.math.BigDecimal;

/**
 * 基础资料公共校验工具：
 * 前端表单对必填/长度/库存上下限已有约束，后端接口必须同样拦截，
 * 否则绕过页面直接调接口即可写入空白名称、超长文本、负库存阈值等脏数据。
 */
public final class BaseDataValidator {

    /** 库存上下限合理上限：与采购/销售明细的数量上限保持一致，防止超大数值落库后精度丢失 */
    private static final BigDecimal MAX_STOCK = new BigDecimal("100000000");

    private BaseDataValidator() {}

    /**
     * 必填文本：去首尾空格后不得为空，且不得超过列宽；返回清洗后的值供实体落库。
     * 对应前端带 * 的字段（如药品通用名/规格、供应商名称/编码）。
     */
    public static String requireText(String value, String field, int maxLength) {
        String trimmed = value == null ? null : value.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, field + "不能为空");
        }
        checkLength(trimmed, field, maxLength);
        return trimmed;
    }

    /** 可选文本：去掉首尾空格，空白统一存 null，超长直接拒绝（防止 1000+ 脏数据撑破列表展示） */
    public static String optionalText(String value, String field, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        checkLength(trimmed, field, maxLength);
        return trimmed;
    }

    /** 状态枚举校验：基础资料只允许 0-停用 / 1-启用；null 表示未传，保留数据库默认值 */
    public static void checkStatus(Integer status) {
        if (status != null && status != 0 && status != 1) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "状态值不合法");
        }
    }

    /** 可选的非负数值：拒绝负数与超大值（库存下限/上限等阈值字段） */
    public static BigDecimal optionalNonNegative(BigDecimal value, String field) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, field + "不能为负数");
        }
        if (value.compareTo(MAX_STOCK) > 0) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, field + "超出合理范围");
        }
        return value;
    }

    /** 库存上下限组合校验：上限不得小于下限，避免预警/补货口径倒置 */
    public static void checkStockRange(BigDecimal minStock, BigDecimal maxStock) {
        if (minStock != null && maxStock != null && maxStock.compareTo(minStock) < 0) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "库存上限不能小于库存下限");
        }
    }

    private static void checkLength(String value, String field, int maxLength) {
        if (value.length() > maxLength) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR,
                    field + "不能超过 " + maxLength + " 个字符");
        }
    }
}
