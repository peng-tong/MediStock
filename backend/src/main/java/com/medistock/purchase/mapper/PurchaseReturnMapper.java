package com.medistock.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medistock.purchase.entity.PurchaseReturn;
import org.apache.ibatis.annotations.Select;

public interface PurchaseReturnMapper extends BaseMapper<PurchaseReturn> {

    /** 当前租户退货单最大主键（含逻辑删除），用于生成不复用的退货单号 */
    @Select("SELECT COALESCE(MAX(id), 0) FROM purchase_return")
    Long selectMaxIdIncludingDeleted();
}
