<template>
  <!-- 批次管理：以效期为核心的批次台账，按近效期/过期/下限筛选 -->
  <div>
    <PageCard title="批次管理" desc="批次效期 · 近效期 90/30 天、过期与低于下限批次预警">
    </PageCard>

    <!-- 效期筛选（chip 驱动 query.level） -->
    <div class="filters">
      <span class="chip" :class="{ active: query.level === '' }" @click="setLevel('')">全部</span>
      <span class="chip" :class="{ active: query.level === '90' }" @click="setLevel('90')">近效期90天</span>
      <span class="chip" :class="{ active: query.level === '30' }" @click="setLevel('30')">近效期30天</span>
      <span class="chip" :class="{ active: query.level === 'expired' }" @click="setLevel('expired')">已过期</span>
      <span class="chip" :class="{ active: query.level === 'low' }" @click="setLevel('low')">低于下限</span>
    </div>

    <!-- 批次明细表 -->
    <DataTable ref="tableRef" :columns="columns" :fetch="fetchData" :mock="mockData" :query="query" />
  </div>
</template>

<script setup>
// 批次管理页：效期状态根据有效期至与今天的天数差自动判定
import { reactive, ref } from 'vue'
import PageCard from '../../components/PageCard.vue'
import DataTable from '../../components/DataTable.vue'
import { getStockBatches } from '../../api/inventory'

const tableRef = ref()
// 查询条件：level 为效期筛选级别
const query = reactive({ level: '' })

// 计算剩余天数（按自然日取整）
function daysLeft(dateStr) {
  const end = new Date(dateStr.replace(/-/g, '/')).getTime()
  return Math.ceil((end - new Date().getTime()) / 86400000)
}

// 效期状态标签：已过期 / 近效期30天 / 近效期90天 / 正常
function expiryTag(r) {
  const days = daysLeft(r.expiryDate)
  if (days < 0) return { type: 'danger', text: '已过期' }
  if (days <= 30) return { type: 'warn', text: '近效期30天' }
  if (days <= 90) return { type: 'info', text: '近效期90天' }
  return { type: 'ok', text: '正常' }
}

// 列配置
const columns = [
  { title: '药品', key: 'drugName', className: 'name', width: '170px' },
  { title: '生产批号', key: 'batchNo', className: 'mono', width: '130px' },
  { title: '有效期至', key: 'expiryDate', className: 'mono', width: '120px' },
  { title: '效期状态', key: 'status', width: '110px', tag: expiryTag },
  { title: '库存', key: 'quantity', align: 'center' },
  { title: '仓库', key: 'warehouseName' },
  { title: '入库日期', key: 'inDate', className: 'dim', width: '120px' }
]

// 拉取接口：异常时由 DataTable 回退 mock
function fetchData(params) {
  return getStockBatches({ level: params.level, page: params.page, pageSize: params.pageSize })
}

// 点击筛选 chip：更新 level 并回到第一页刷新
function setLevel(level) {
  query.level = level
  tableRef.value.reload()
}

// 兜底演示数据（含 1 条已过期、1 条近效期30、1 条近效期90）
const mockData = [
  { drugName: '阿莫西林胶囊', batchNo: 'A20250812', expiryDate: '2027-08-12', quantity: 850, warehouseName: '常温库', inDate: '2026-08-12' },
  { drugName: '布洛芬缓释胶囊', batchNo: 'B20250903', expiryDate: '2027-09-03', quantity: 640, warehouseName: '常温库', inDate: '2026-09-03' },
  { drugName: '连花清瘟胶囊', batchNo: 'L20261001', expiryDate: '2027-01-05', quantity: 120, warehouseName: '常温库', inDate: '2026-10-01' },
  { drugName: '奥美拉唑肠溶胶囊', batchNo: 'O20261028', expiryDate: '2026-11-20', quantity: 210, warehouseName: '阴凉库', inDate: '2026-10-28' },
  { drugName: '维生素C片', batchNo: 'V20251001', expiryDate: '2026-10-15', quantity: 32, warehouseName: '常温库', inDate: '2025-10-01' },
  { drugName: '藿香正气口服液', batchNo: 'H20261012', expiryDate: '2027-10-12', quantity: 260, warehouseName: '阴凉库', inDate: '2026-10-12' }
]
</script>
