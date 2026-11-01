<template>
  <!-- 预警中心：效期预警（已过期 / 近效期）+ 低库存预警，支持一键处理 -->
  <div>
    <PageCard title="预警中心" desc="效期预警 · 近效期/过期药品与低于下限库存提示">
    </PageCard>

    <!-- 顶部统计卡为原型静态数据，后端暂无汇总接口，先屏蔽 -->

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
import { ref } from 'vue'
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

// 级别标签：同 C2 效期规则（当天到期按已过期处理）；无效期风险时按汇总库存判断低库存
function levelOf(r) {
  const days = daysLeft(r.expiryDate)
  if (days <= 0) return { type: 'danger', text: '已过期' }
  if (days <= 30) return { type: 'warn', text: '近效期30天' }
  if (days <= 90) return { type: 'info', text: '近效期90天' }
  if (isLowStock(r)) return { type: 'warn', text: '低于下限' }
  return { type: 'ok', text: '正常' }
}

// 剩余天数渲染：今天到期/已过期红色，近效期琥珀色，其余灰色
function remainRender(r) {
  const days = daysLeft(r.expiryDate)
  let color, text
  if (days === 0) { color = 'var(--danger)'; text = '今天到期' }
  else if (days < 0) { color = 'var(--danger)'; text = `已过期 ${days} 天` }
  else if (days <= 90) { color = 'var(--warn)'; text = `${days} 天` }
  else { color = 'var(--muted)'; text = `${days} 天` }
  return `<span style="color:${color};font-weight:600">${text}</span>`
}

// 列配置
const columns = [
  { title: '药品', key: 'drugName', className: 'name', width: '170px' },
  { title: '批号', key: 'batchNo', className: 'mono', width: '130px' },
  { title: '有效期至', key: 'expiryDate', className: 'mono', width: '120px' },
  // html: true 让 DataTable 用 v-html 渲染带颜色的天数
  { title: '剩余天数', key: 'remain', width: '110px', html: true, render: remainRender },
  { title: '库存', key: 'quantity', align: 'center', width: '80px' },
  { title: '级别', key: 'level', width: '110px', tag: levelOf },
  { title: '建议', key: 'suggestion', className: 'dim', render: suggestionOf }
]

// 单页上限：后端 PageUtil 会把 pageSize 钳制到 100，请求 500 会静默丢失第 101 条之后的数据
const PAGE_SIZE = 100
const MAX_PAGES = 50

// 逐页拉取全部批次（后端分页上限内），保证预警统计基于全量数据
async function fetchAllBatches() {
  const first = await getStockBatches({ page: 1, pageSize: PAGE_SIZE })
  const all = (first && first.list) || []
  const total = Number((first && first.total) || 0)
  const pages = Math.min(Math.ceil(total / PAGE_SIZE), MAX_PAGES)
  for (let p = 2; p <= pages; p++) {
    const data = await getStockBatches({ page: p, pageSize: PAGE_SIZE })
    all.push(...((data && data.list) || []))
  }
  return all
}

// 拉取接口：全量拉取批次后本地处理（去重 + 按药品汇总）再按页切片，满足 DataTable 分页契约
function fetchData(params) {
  return fetchAllBatches().then((list) => {
    const all = processRows(list)
    const start = (params.page - 1) * params.pageSize
    return { list: all.slice(start, start + params.pageSize), total: all.length }
  })
}

// 预处理：按 drugId+batchNo 去重（同一 药品+批号 只保留第一行）；
// 跨仓库/批次按 drugId 汇总 quantity，行内附带 _drugTotal（药品总库存）与 _minStock（库存下限），
// 低库存判断与库存页「按汇总库存判断」口径一致
function processRows(list) {
  const seen = new Set()
  const totals = new Map()
  const rows = []
  for (const r of list) {
    const k = r.drugId ?? r.drugName
    // 先累计全部批次数量（含跨仓库同批号），再决定是否展示该行：
    // 此前先按 drugId+batchNo 去重再汇总，导致同批号多仓库存漏算、低库存误判
    totals.set(k, (totals.get(k) || 0) + (Number(r.quantity) || 0))
    const key = `${k}-${r.batchNo}`
    if (seen.has(key)) continue
    seen.add(key)
    rows.push(r)
  }
  rows.forEach((r) => {
    r._drugTotal = totals.get(r.drugId ?? r.drugName)
    r._minStock = r.minStock
  })
  return rows
}

// 低库存判断：药品汇总库存 _drugTotal 小于下限 _minStock 即视为低库存
function isLowStock(r) {
  return Number(r._drugTotal) < Number(r._minStock)
}

// 兜底演示数据（复用原型药品/批号，覆盖 过期 / 30天 / 90天 / 低库存 四类）
const mockData = [
  { drugName: '布洛芬缓释胶囊', batchNo: 'B20250903', expiryDate: '2026-10-15', quantity: 120, minStock: 80, suggestion: '已过期药品严禁销售，立即下架并报损' },
  { drugName: '奥美拉唑肠溶胶囊', batchNo: 'O20261028', expiryDate: '2026-11-20', quantity: 210, minStock: 50, suggestion: '近效期 30 天内，建议优先促销或调配' },
  { drugName: '连花清瘟胶囊', batchNo: 'L20261001', expiryDate: '2027-01-05', quantity: 120, minStock: 60, suggestion: '近效期 90 天内，关注周转速度' },
  { drugName: '阿莫西林胶囊', batchNo: 'A20250812', expiryDate: '2027-08-12', quantity: 32, minStock: 80, suggestion: '低于库存下限，建议尽快补货' }
]

// 建议文案：后端批次接口无该字段，按剩余天数推导（当天到期视为已过期）
function suggestionOf(r) {
  const days = daysLeft(r.expiryDate)
  if (days <= 0) return '已过期药品严禁销售，立即下架并报损'
  if (days <= 30) return '近效期 30 天内，建议优先促销或调配'
  if (days <= 90) return '近效期 90 天内，关注周转速度'
  if (isLowStock(r)) return '低于库存下限，建议尽快补货'
  return '正常'
}

// 按行状态决定操作按钮文案：过期报损 / 低库存生成采购 / 其余处理
function actionLabel(r) {
  const days = daysLeft(r.expiryDate)
  if (days <= 0) return '报损处理'
  if (isLowStock(r)) return '生成采购'
  return '处理'
}

// 演示处理：统一提示已处理
function handle(row) {
  notifySuccess(`已处理（演示）· ${row.drugName}`)
}
</script>
