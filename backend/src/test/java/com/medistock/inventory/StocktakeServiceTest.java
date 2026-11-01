package com.medistock.inventory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medistock.common.config.TenantContext;
import com.medistock.common.exception.BusinessException;
import com.medistock.inventory.entity.Stock;
import com.medistock.inventory.entity.StockBatch;
import com.medistock.inventory.entity.Stocktake;
import com.medistock.inventory.entity.StocktakeItem;
import com.medistock.inventory.mapper.StockMapper;
import com.medistock.inventory.mapper.StocktakeItemMapper;
import com.medistock.inventory.service.StockService;
import com.medistock.inventory.service.StocktakeService;
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

@SpringBootTest
@ActiveProfiles("test")
class StocktakeServiceTest {

    @Autowired private StocktakeService stocktakeService;
    @Autowired private StockService stockService;
    @Autowired private StockMapper stockMapper;
    @Autowired private StocktakeItemMapper itemMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    /** 盘点测试专用数据（与并发测试的 9001/9002 区分开） */
    private static final Long DRUG_ID = 9101L;
    private static final Long WH_ID = 9102L;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(1L);
        // 物理删除：MP 逻辑删除（deleted=1）后行仍占据唯一索引，重跑测试会撞唯一键，故用 JdbcTemplate 直接 DELETE
        jdbcTemplate.update("DELETE FROM inv_stock_batch WHERE drug_id = ?", DRUG_ID);
        jdbcTemplate.update("DELETE FROM inv_stock WHERE drug_id = ?", DRUG_ID);
        jdbcTemplate.update("DELETE FROM inv_flow WHERE drug_id = ?", DRUG_ID);
        // 清理历史盘点单及其明细（按仓库维度），避免 take_no 序号与明细残留
        jdbcTemplate.update("DELETE FROM inv_stocktake_item WHERE take_id IN (SELECT id FROM inv_stocktake WHERE warehouse_id = ?)", WH_ID);
        jdbcTemplate.update("DELETE FROM inv_stocktake WHERE warehouse_id = ?", WH_ID);
    }

    @Test
    void confirm_adjustsStockByDiff() {
        TenantContext.setTenantId(1L);
        try {
            // 先入库 100（盘盈场景：账面 100，实盘 120 → 差异 +20）
            stockService.inbound(DRUG_ID, WH_ID, "B9", LocalDate.now().plusDays(100),
                    new BigDecimal("100"), "PO-T", "盘点测试准备");

            Long takeId = stocktakeService.createStocktake(WH_ID, "测试盘点");
            StocktakeItem item = itemMapper.selectOne(new LambdaQueryWrapper<StocktakeItem>()
                    .eq(StocktakeItem::getTakeId, takeId).eq(StocktakeItem::getDrugId, DRUG_ID));
            assertThat(item.getBookQuantity()).isEqualByComparingTo("100");

            // 第五轮起 updateActual 增加 takeId 入参，校验明细必须属于该盘点单
            stocktakeService.updateActual(takeId, item.getId(), new BigDecimal("120"));
            stocktakeService.confirm(takeId);

            // 差异 +20 已同步到汇总库存
            Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                    .eq(Stock::getDrugId, DRUG_ID).eq(Stock::getWarehouseId, WH_ID));
            assertThat(stock.getQuantity()).isEqualByComparingTo("120");

            Stocktake st = stocktakeService.getById(takeId);
            assertThat(st.getStatus()).isEqualTo(1);
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void confirm_withoutActual_isRejected() {
        TenantContext.setTenantId(1L);
        try {
            // 先入库 100 产生账面数
            stockService.inbound(DRUG_ID, WH_ID, "B9", LocalDate.now().plusDays(100),
                    new BigDecimal("100"), "PO-T", "盘点测试准备");

            Long takeId = stocktakeService.createStocktake(WH_ID, "测试盘点");
            // 不调用 updateActual 直接确认 → 防漏录拦截
            assertThatThrownBy(() -> stocktakeService.confirm(takeId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("存在未录入实盘的明细");

            // 确认被拒后盘点单仍为进行中
            Stocktake st = stocktakeService.getById(takeId);
            assertThat(st.getStatus()).isEqualTo(0);
        } finally {
            TenantContext.clear();
        }
    }
}
