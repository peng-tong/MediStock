<template>
  <!-- 工作台：经营概况看板（统计卡 + 趋势图 + 分类占比 + 预警/流水） -->
  <div>
    <PageCard title="工作台" desc="2026年10月29日 周四 · 经营概况一览">
      <template #actions>
        <button class="btn btn-ghost">导出报表</button>
        <button class="btn btn-primary" @click="router.push('/sales/orders')">+ 新建销售单</button>
      </template>
    </PageCard>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-ico teal">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M3 3h18M5 3v18h14V3"/><path d="M7 12h3l2-6 2 10 2-6h3"/></svg>
        </div>
        <div><div class="lbl">今日销售额</div><div class="val">¥ 86,420</div><div class="trend up">▲ 12.5% 较昨日</div></div>
      </div>
      <div class="stat-card">
        <div class="stat-ico blue">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="4" y="4" width="8" height="8" rx="1.5"/><rect x="13" y="4" width="8" height="8" rx="1.5"/><rect x="4" y="13" width="8" height="8" rx="1.5"/><rect x="13" y="13" width="8" height="8" rx="1.5"/></svg>
        </div>
        <div><div class="lbl">库存总额（含批）</div><div class="val">¥ 1,284,560</div><div class="trend up">1,423 个批次在库</div></div>
      </div>
      <div class="stat-card">
        <div class="stat-ico amber">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 3L2.5 19h19z"/><path d="M12 9.5v4.5M12 17.5v.1"/></svg>
        </div>
        <div><div class="lbl">待处理预警</div><div class="val">12 条</div><div class="trend warn">近效期 8 · 低库存 4</div></div>
      </div>
      <div class="stat-card">
        <div class="stat-ico red">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M3 4h18l-6 8v6l-4 2v-8z"/></svg>
        </div>
        <div><div class="lbl">待审核采购单</div><div class="val">3 单</div><div class="trend down">合计 ¥ 46,100</div></div>
      </div>
    </div>

    <div class="grid-2">
      <div class="card">
        <div class="card-title">近 7 日销售趋势<span class="more">查看月报 ›</span></div>
        <svg viewBox="0 0 420 170" style="width:100%;height:auto">
          <defs>
            <linearGradient id="g1" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0" stop-color="#0F766E" stop-opacity=".25"/><stop offset="1" stop-color="#0F766E" stop-opacity="0"/>
            </linearGradient>
          </defs>
          <g stroke="#E3ECEE"><line x1="40" y1="20" x2="40" y2="150"/><line x1="40" y1="62" x2="420" y2="62"/><line x1="40" y1="105" x2="420" y2="105"/><line x1="40" y1="150" x2="420" y2="150"/></g>
          <path d="M40 128 L106 118 L172 122 L238 96 L304 102 L370 60 L420 70 L420 150 L40 150 Z" fill="url(#g1)"/>
          <path d="M40 128 L106 118 L172 122 L238 96 L304 102 L370 60 L420 70" fill="none" stroke="#0F766E" stroke-width="2.5" stroke-linecap="round"/>
          <g fill="#0F766E">
            <circle cx="40" cy="128" r="3.5"/><circle cx="106" cy="118" r="3.5"/><circle cx="172" cy="122" r="3.5"/><circle cx="238" cy="96" r="3.5"/><circle cx="304" cy="102" r="3.5"/><circle cx="370" cy="60" r="3.5"/><circle cx="420" cy="70" r="3.5"/>
          </g>
          <g fill="#93A6AB" font-size="10">
            <text x="40" y="165" text-anchor="middle">23</text><text x="106" y="165" text-anchor="middle">24</text><text x="172" y="165" text-anchor="middle">25</text><text x="238" y="165" text-anchor="middle">26</text><text x="304" y="165" text-anchor="middle">27</text><text x="370" y="165" text-anchor="middle">28</text><text x="420" y="165" text-anchor="middle">29</text>
          </g>
          <g fill="#0F766E" font-size="10" font-weight="600"><text x="370" y="52" text-anchor="end">¥86,420</text></g>
        </svg>
      </div>
      <div class="card">
        <div class="card-title">库存分类占比<span class="more">详情 ›</span></div>
        <div style="display:flex;align-items:center;gap:22px">
          <svg viewBox="0 0 120 120" width="150">
            <circle cx="60" cy="60" r="42" fill="none" stroke="#E3ECEE" stroke-width="16"/>
            <circle cx="60" cy="60" r="42" fill="none" stroke="#0F766E" stroke-width="16" stroke-dasharray="99 264" transform="rotate(-90 60 60)"/>
            <circle cx="60" cy="60" r="42" fill="none" stroke="#38BDF8" stroke-width="16" stroke-dasharray="70 264" stroke-dashoffset="-99" transform="rotate(-90 60 60)"/>
            <circle cx="60" cy="60" r="42" fill="none" stroke="#F59E0B" stroke-width="16" stroke-dasharray="57 264" stroke-dashoffset="-169" transform="rotate(-90 60 60)"/>
            <circle cx="60" cy="60" r="42" fill="none" stroke="#34D399" stroke-width="16" stroke-dasharray="34 264" stroke-dashoffset="-226" transform="rotate(-90 60 60)"/>
            <text x="60" y="58" text-anchor="middle" font-size="17" font-weight="700" fill="#17383F">1,423</text>
            <text x="60" y="72" text-anchor="middle" font-size="9" fill="#93A6AB">批次总数</text>
          </svg>
          <div style="flex:1">
            <div v-for="c in categoryPercents" :key="c.name" class="mini-item">
              <div class="info"><b>{{ c.name }}</b><span>{{ c.desc }}</span></div>
              <div class="right"><b>{{ c.percent }}%</b></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="grid-2">
      <div class="card">
        <div class="card-title">效期预警 · 重点关注<span class="more">全部预警 ›</span></div>
        <div class="mini-list">
          <div v-for="a in alerts" :key="a.name" class="mini-item">
            <div class="info"><b>{{ a.name }}</b><span>{{ a.batch }}</span></div>
            <div class="right"><span class="tag" :class="a.type">{{ a.level }}</span><b :class="'text-' + (a.type === 'danger' ? 'danger' : 'warn')" style="font-size:13px">{{ a.qty }}</b></div>
          </div>
        </div>
      </div>
      <div class="card">
        <div class="card-title">最新库存流水<span class="more">全部流水 ›</span></div>
        <div class="mini-list">
          <div v-for="f in flows" :key="f.no" class="mini-item">
            <div class="info"><b>{{ f.title }}</b><span>{{ f.detail }}</span></div>
            <div class="right"><b :style="{ color: f.dir > 0 ? 'var(--ok)' : 'var(--danger)' }">{{ f.dir > 0 ? '+' : '' }}{{ f.qty }}</b><span>{{ f.time }}</span></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// 工作台展示页：当前为静态演示数据（对接后端后替换为对应统计接口）
import { useRouter } from 'vue-router'
import PageCard from '../components/PageCard.vue'

const router = useRouter()

// 库存分类占比
const categoryPercents = [
  { name: '处方药', desc: '抗生素 / 慢病 / 消化', percent: 38 },
  { name: 'OTC 非处方', desc: '解热镇痛 / 抗过敏', percent: 27 },
  { name: '中成药', desc: '感冒类 / 胃肠类', percent: 22 },
  { name: '慢病特药', desc: '心脑血管等', percent: 13 }
]
// 效期预警
const alerts = [
  { name: '氯雷他定片 10mg×6片', batch: '批号 L20250730 · 已于 2026-10-15 过期', level: '已过期', type: 'danger', qty: '45 盒' },
  { name: '连花清瘟胶囊 0.35g×48粒', batch: '批号 L20251002 · 2026-11-20 到期（剩 22 天）', level: '近效期', type: 'warn', qty: '120 盒' },
  { name: '藿香正气口服液 10ml×10支', batch: '批号 H20251012 · 2027-10-12 到期（剩 373 天）', level: '90天内', type: 'info', qty: '260 盒' }
]
// 最近流水
const flows = [
  { title: '销售出库 · SO-20261029-002', detail: '阿莫西林胶囊 · 批号 A20250812 · 王建国', dir: -1, qty: '120 盒', time: '10:24' },
  { title: '采购入库 · PO-20261027-002', detail: '藿香正气口服液 · 批号 H20251012 · 张伟', dir: 1, qty: '300 盒', time: '昨天 16:02' },
  { title: '销售退货 · SR-20261026-005', detail: '布洛芬缓释胶囊 · 批号 B20250903 · 李敏', dir: 1, qty: '15 盒', time: '10-26 14:30' }
]
</script>

<style scoped>
.grid-2{display:grid;grid-template-columns:1.4fr 1fr;gap:18px}
.grid-2 + .grid-2{grid-template-columns:1fr 1fr}
</style>
