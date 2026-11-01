package com.medistock.common.util;

import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;

/**
 * 分页参数规约工具：
 * - page / pageSize 小于 1 属于非法参数，直接返回 400（此前静默钳制为 1，调用方感知不到参数错误）
 * - pageSize 超过 100 仍做上限钳制（兼容预警中心等一次性拉取较多数据的页面，防恶意超大值拖垮数据库）
 */
public final class PageUtil {
    private PageUtil() {}

    /** 页码规约：必须 ≥ 1，非法直接报 400 */
    public static int page(int page) {
        if (page < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "page 必须大于等于 1");
        }
        return page;
    }

    /** 每页条数规约：必须 ≥ 1（非法报 400），上限 100 防恶意超大 pageSize 拖垮数据库/接口 */
    public static int pageSize(int pageSize) {
        if (pageSize < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "pageSize 必须大于等于 1");
        }
        return Math.min(pageSize, 100);
    }
}
