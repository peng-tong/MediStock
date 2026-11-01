package com.medistock.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medistock.sales.entity.SalesOrder;
import org.apache.ibatis.annotations.Select;

public interface SalesOrderMapper extends BaseMapper<SalesOrder> {

    /**
     * 当前租户销售单最大主键（自定义 SQL 不受逻辑删除过滤，已删除单据也计入）。
     * 用于生成单调递增、删除后也不会复用的业务单号（此前按 count+1 生成，删草稿后会出现重号）。
     */
    @Select("SELECT COALESCE(MAX(id), 0) FROM sales_order")
    Long selectMaxIdIncludingDeleted();
}
