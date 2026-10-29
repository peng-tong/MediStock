package com.medistock.base;

import com.medistock.base.entity.Drug;
import com.medistock.base.mapper.DrugMapper;
import com.medistock.base.service.DrugService;
import com.medistock.common.config.TenantContext;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.inventory.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * deleteDrug 防呆校验测试（回归：多仓库存行下 selectOne 抛 TooManyResultsException 的缺陷）：
 * 1. 同一药品在两个仓库各有库存 → 禁止删除
 * 2. 同一药品两个仓库库存全部出清 → 允许删除
 */
@SpringBootTest
@ActiveProfiles("test")
class DrugServiceTest {

    @Autowired private DrugService drugService;
    @Autowired private StockService stockService;
    @Autowired private DrugMapper drugMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    /** 多仓测试专用数据（与既有测试的 9001/9101 区分开） */
    private static final Long DRUG_ID = 9401L;
    private static final Long WH_1 = 9402L;
    private static final Long WH_2 = 9403L;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(1L);
        // 物理删除：MP 逻辑删除（deleted=1）后行仍占据唯一索引，重跑测试会撞唯一键，故用 JdbcTemplate 直接 DELETE
        jdbcTemplate.update("DELETE FROM inv_stock_batch WHERE drug_id = ?", DRUG_ID);
        jdbcTemplate.update("DELETE FROM inv_stock WHERE drug_id = ?", DRUG_ID);
        jdbcTemplate.update("DELETE FROM inv_flow WHERE drug_id = ?", DRUG_ID);
        // base_drug 无引用（测试库），可直接物理删除
        jdbcTemplate.update("DELETE FROM base_drug WHERE id = ?", DRUG_ID);
    }

    /** 建一个固定 id 的药品档案 */
    private void saveDrug() {
        Drug drug = new Drug();
        drug.setId(DRUG_ID);
        drug.setTenantId(1L);
        drug.setGenericName("多仓测试药品");
        drug.setStatus(1);
        drugService.save(drug);
    }

    /** 两个仓库各入库 10（批号各自，产生两条 inv_stock 行） */
    private void inboundBothWarehouses() {
        stockService.inbound(DRUG_ID, WH_1, "B9401A", LocalDate.now().plusDays(90),
                new BigDecimal("10"), "PO-T1", "多仓入库");
        stockService.inbound(DRUG_ID, WH_2, "B9401B", LocalDate.now().plusDays(90),
                new BigDecimal("10"), "PO-T2", "多仓入库");
    }

    @Test
    void deleteDrug_withStockInMultipleWarehouses_isRejected() {
        TenantContext.setTenantId(1L);
        try {
            inboundBothWarehouses();
            saveDrug();

            // 任一仓库有库存即拦截删除（修复前 selectOne 多仓会抛 TooManyResultsException → 500）
            assertThatThrownBy(() -> drugService.deleteDrug(DRUG_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.DRUG_IN_STOCK.getMessage());
            // 未被删除
            assertThat(drugMapper.selectById(DRUG_ID)).isNotNull();
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void deleteDrug_noStock_succeeds() {
        TenantContext.setTenantId(1L);
        try {
            inboundBothWarehouses();
            // 两个仓库库存全部出清
            stockService.outboundFifo(DRUG_ID, WH_1, new BigDecimal("10"), "SO-T1", "出清");
            stockService.outboundFifo(DRUG_ID, WH_2, new BigDecimal("10"), "SO-T2", "出清");
            saveDrug();

            // 无库存 → 允许删除（逻辑删除后查不到）
            drugService.deleteDrug(DRUG_ID);
            assertThat(drugMapper.selectById(DRUG_ID)).isNull();
        } finally {
            TenantContext.clear();
        }
    }
}
