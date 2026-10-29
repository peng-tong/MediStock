<template>
  <!-- 预警中心：效期预警（已过期 / 近效期）+ 低库存预警，支持一键处理 -->
  <div>
    <PageCard title="预警中心" desc="效期预警 · 近效期/过期药品与低于下限库存提示">
    </PageCard>

    <!-- 预警统计卡（数量由 mock 明细实时统计） -->
    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-ico red">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 3L2.5 19h19z"/><path d="M12 9.5v4.5M12 17.5v.1"/></svg>
        </div>
        <div>
          <div class="lbl">已过期</div>
          <div class="val">{{ stats.expired }}</div>
          <div class="trend down">立即报损下架</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-ico amber">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>
        </div>
        <div>
          <div class="lbl">近效期30天</div>
          <div class="val">{{ stats.near30 }}</div>
          <div class="trend warn">优先促销或调配</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-ico blue">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>
        </div>
        <div>
          <div class="lbl">近效期90天</div>
          <div class="val">{{ stats.near90 }}</div>
          <div class="trend" style="color:var(--info)">关注周转速度</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-ico teal">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 9l8-5 8 5v11H4z"/><path d="M9 13h6"/></svg>
        </div>
        <div>
          <div class="lbl">低于下限</div>
          <div class="val">{{ stats.lowStock }}</div>
          <div class="trend" style="color:var(--primary)">建议尽快补货</div>
        </div>
      </div>
    </div>

    <!-- 效期预警明细 -->
    <div class="card">
      <div class="card-title">效期预警明细<span class="more">按剩余天数排序</span></div>
      <DataTable ref="tableRef" :columns="columns" :fetch="fetchData" :mock="mockData">
        <template #actions="{ row }">
          <button class="btn btn-sm" @click="handle(row)">{{ actionLabel(row) }}</button>
        </template>
      </DataTable>
    </div>
  </div>
</template>

<script setup>
// 预警中心页：剩余天数分级（同批次管理规则），操作按钮为演示处理
import { computed, ref } from 'vue'
import PageCard from '../../components/PageCard.vue'
import DataTable from '../../components/DataTable.vue'
import { getStockBatches } from '../../api/inventory'
import { notifySuccess } from '../../utils/notify'

const tableRef = ref()

// 计算剩余天数（按自然日取整）
function daysLeft(dateStr) {
  const end = new Date(dateStr.replace(/-/g, '/')).getTime()
  return Math.ceil((end - new Date().getTime()) / 86400000)
}

// 级别标签：同 C2 效期规则
function levelOf(r) {
  const days = daysLeft(r.expiryDate)
  if (days < 0) return { type: 'danger', text: '已过期' }
  if (days <= 30) return { type: 'warn', text: '近效期30天' }
  if (days <= 90) return { type: 'info', text: '近效期90天' }
  return { type: 'ok', text: '正常' }
}

// 剩余天数渲染：已过期显示红色负数，近效期显示琥珀色
function remainRender(r) {
  const days = daysLeft(r.expiryDate)
  const color = days < 0 ? 'var(--danger)' : days <= 90 ? 'var(--warn)' : 'var(--muted)'
  const text = days < 0 ? `已过期 ${days} 天` : `${days} 天`
  return `<span style="color:${color};font-weight:600">${text}</span>`
}

// 列配置
const columns = [
  { title: '药品', key: 'drugName', className: 'name', width: '170px' },
  { title: '批号', key: 'batchNo', className: 'mono', width: '130px' },
  { title: '有效期至', key: 'expiryDate', className: 'mono', width: '120px' },
  { title: '剩余天数', key: 'remain', width: '110px', render: remainRender },
  { title: '库存', key: 'quantity', align: 'center', width: '80px' },
  { title: '级别', key: 'level', width: '110px', tag: levelOf },
  { title: '建议', key: 'suggestion', className: 'dim' }
]

// 拉取接口：失败回退 mock
function fetchData(params) {
  return getStockBatches({ page: params.page, pageSize: params.pageSize })
}

// 兜底演示数据（复用原型药品/批号，覆盖 过期 / 30天 / 90天 / 低库存 四类）
const mockData = [
  { drugName: '布洛芬缓释胶囊', batchNo: 'B20250903', expiryDate: '2026-10-15', quantity: 120, minStock: 80, suggestion: '已过期药品严禁销售，立即下架并报损' },
  { drugName: '奥美拉唑肠溶胶囊', batchNo: 'O20261028', expiryDate: '2026-11-20', quantity: 210, minStock: 50, suggestion: '近效期 30 天内，建议优先促销或调配' },
  { drugName: '连花清瘟胶囊', batchNo: 'L20261001', expiryDate: '2027-01-05', quantity: 120, minStock: 60, suggestion: '近效期 90 天内，关注周转速度' },
  { drugName: '阿莫西林胶囊', batchNo: 'A20250812', expiryDate: '2027-08-12', quantity: 32, minStock: 80, suggestion: '低于库存下限，建议尽快补货' }
]

// 预警统计：按 mock 明细实时统计，保证与展示数据一致
const stats = computed(() => {
  let expired = 0, near30 = 0, near90 = 0, lowStock = 0
  mockData.forEach(r => {
    const days = daysLeft(r.expiryDate)
    if (days < 0) expired++
    else if (days <= 30) near30++
    else if (days <= 90) near90++
    if (Number(r.quantity) < Number(r.minStock)) lowStock++
  })
  return { expired, near30, near90, lowStock }
})

// 按行状态决定操作按钮文案：过期报损 / 低库存生成采购 / 其余处理
function actionLabel(r) {
  const days = daysLeft(r.expiryDate)
  if (days < 0) return '报损处理'
  if (Number(r.quantity) < Number(r.minStock)) return '生成采购'
  return '处理'
}

// 演示处理：统一提示已处理
function handle(row) {
  notifySuccess(`已处理（演示）· ${row.drugName}`)
}
</script>
