<template>
  <!-- 报表中心：销售/采购 TOP5 与库存汇总（P1 阶段全静态演示数据） -->
  <div>
    <PageCard title="报表中心" desc="进销存报表 · 数据为演示样例">
    </PageCard>

    <!-- 核心指标卡（静态演示） -->
    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-ico teal">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M3 17l6-6 4 4 8-8"/><path d="M21 7h-4v4"/></svg>
        </div>
        <div>
          <div class="lbl">本月销售额</div>
          <div class="val">¥ 286,500</div>
          <div class="trend up">较上月 +12.6%</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-ico blue">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M3 4h18l-6 8v6l-4 2v-8z"/></svg>
        </div>
        <div>
          <div class="lbl">本月采购额</div>
          <div class="val">¥ 128,400</div>
          <div class="trend" style="color:var(--info)">较上月 +5.2%</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-ico amber">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 9l8-5 8 5v11H4z"/><path d="M4 9h16"/><path d="M12 9v11"/></svg>
        </div>
        <div>
          <div class="lbl">库存总额</div>
          <div class="val">¥ 386,200</div>
          <div class="trend" style="color:var(--muted)">在库金额合计</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-ico red">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M6 9a6 6 0 1 1 12 0c0 5 2 6 2 6H4s2-1 2-6"/><path d="M10 20a2 2 0 0 0 4 0"/></svg>
        </div>
        <div>
          <div class="lbl">预警数</div>
          <div class="val">6</div>
          <div class="trend down">效期 / 低库存预警</div>
        </div>
      </div>
    </div>

    <!-- 销售 / 采购 TOP5 双栏 -->
    <div class="grid-2">
      <div class="card">
        <div class="card-title">销售明细 TOP5</div>
        <div class="tbl-wrap">
          <table class="tbl">
            <thead><tr><th>药品</th><th>销售数量</th><th>金额</th></tr></thead>
            <tbody>
              <tr v-for="(r, i) in saleTop" :key="i">
                <td class="name">{{ r.name }}</td>
                <td>{{ r.qty }} 盒</td>
                <td>¥ {{ r.amount }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <div class="card">
        <div class="card-title">采购明细 TOP5</div>
        <div class="tbl-wrap">
          <table class="tbl">
            <thead><tr><th>药品</th><th>采购数量</th><th>金额</th></tr></thead>
            <tbody>
              <tr v-for="(r, i) in purchaseTop" :key="i">
                <td class="name">{{ r.name }}</td>
                <td>{{ r.qty }} 盒</td>
                <td>¥ {{ r.amount }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 库存汇总 -->
    <div class="card">
      <div class="card-title">库存汇总<span class="more">按药品分类统计</span></div>
      <div class="tbl-wrap">
        <table class="tbl">
          <thead><tr><th>分类</th><th>批次数量</th><th>库存金额</th></tr></thead>
          <tbody>
            <tr v-for="(r, i) in stockSummary" :key="i">
              <td class="name">{{ r.category }}</td>
              <td>{{ r.batches }} 批</td>
              <td>¥ {{ r.amount }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
// 报表中心：P1 阶段无专用接口，全部使用静态演示数据
import PageCard from '../../components/PageCard.vue'

// 销售 TOP5 演示数据
const saleTop = [
  { name: '阿莫西林胶囊', qty: 1240, amount: '45,880' },
  { name: '连花清瘟胶囊', qty: 860, amount: '38,700' },
  { name: '奥美拉唑肠溶胶囊', qty: 650, amount: '22,750' },
  { name: '藿香正气口服液', qty: 540, amount: '17,820' },
  { name: '布洛芬缓释胶囊', qty: 720, amount: '10,080' }
]

// 采购 TOP5 演示数据
const purchaseTop = [
  { name: '阿莫西林胶囊', qty: 1500, amount: '55,500' },
  { name: '连花清瘟胶囊', qty: 600, amount: '27,000' },
  { name: '奥美拉唑肠溶胶囊', qty: 400, amount: '14,000' },
  { name: '布洛芬缓释胶囊', qty: 800, amount: '11,200' },
  { name: '维生素C片', qty: 500, amount: '4,500' }
]

// 库存汇总演示数据（按分类）
const stockSummary = [
  { category: '处方药', batches: 42, amount: '168,500' },
  { category: 'OTC', batches: 38, amount: '96,200' },
  { category: '中成药', batches: 25, amount: '74,800' },
  { category: '慢病特药', batches: 18, amount: '46,700' }
]
</script>

<style scoped>
/* 双栏报表布局 */
.grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
</style>
