<template>
  <div>
    <PageCard title="销售出库" desc="按批次 FIFO 自动扣减 · 过期拦截 · 负库存拦截"></PageCard>

    <!-- 状态筛选 chips（全部 / 待出库 / 已出库） -->
    <div class="filters">
      <span class="chip" :class="{ active: query.status === '' }" @click="query.status = ''">全部</span>
      <span class="chip" :class="{ active: query.status === '1' }" @click="query.status = '1'">待出库</span>
      <span class="chip" :class="{ active: query.status === '3' }" @click="query.status = '3'">已出库</span>
    </div>

    <DataTable ref="tableRef" :columns="columns" :fetch="fetchData" :mock="mockData" :query="query">
      <template #actions="{ row }">
        <!-- 仅待出库状态可确认出库 -->
        <button v-if="Number(row.status) === 1" class="btn btn-sm" @click="outbound(row)">确认出库</button>
        <span v-else class="dim" style="font-size:12px">已出库</span>
      </template>
    </DataTable>
  </div>
</template>

<script setup>
// 销售出库页：待出库销售单列表，确认后按 FIFO 自动扣减批次
import { reactive, ref } from 'vue'
import PageCard from '../../components/PageCard.vue'
import DataTable from '../../components/DataTable.vue'
import { getSalesOrders, outboundSalesOrder } from '../../api/sales'
import { notifySuccess, notifyError } from '../../utils/notify'

const tableRef = ref()
// 查询条件：status 传给 DataTable，chips 点击后自动刷新
const query = reactive({ status: '' })

// 金额千分位格式化
function fmtMoney(n) {
  return '¥ ' + (Number(n) || 0).toLocaleString('zh-CN')
}
// 状态映射：1 待出库 / 3 已出库
function statusTag(s) {
  const map = {
    1: { type: 'warn', text: '待出库' },
    3: { type: 'ok', text: '已出库' }
  }
  return map[s] || { type: 'gray', text: '未知' }
}

// 列配置
const columns = [
  { title: '销售单号', key: 'orderNo', className: 'mono name', width: '170px' },
  { title: '客户', key: 'customerName' },
  { title: '药品明细', key: 'drugNames', render: (r) => r.drugNames || (r.items || []).map(i => i.drugName).join('、') || '-' },
  { title: '金额', key: 'totalAmount', render: (r) => fmtMoney(r.totalAmount) },
  { title: '状态', key: 'status', width: '90px', tag: (r) => statusTag(Number(r.status)) },
  { title: '日期', key: 'createTime', className: 'dim', render: (r) => String(r.createTime || '').slice(0, 10) },
  { title: '开单人', key: 'createByName', className: 'dim' }
]

// 拉取列表（失败时 DataTable 自动回退 mock）
function fetchData(params) {
  return getSalesOrders({ status: params.status, page: params.page, pageSize: params.pageSize })
}

// 兜底演示数据 4 条（3 待出库 + 1 已出库）
const mockData = [
  { id: 2, orderNo: 'SO-20261029-002', customerName: '康民大药房', drugNames: '阿莫西林胶囊 ×120盒', totalAmount: 2682, status: 1, createTime: '2026-10-29', createBy: '王建国' },
  { id: 3, orderNo: 'SO-20261027-002', customerName: '市第一人民医院药房', drugNames: '二甲双胍缓释片 ×400盒', totalAmount: 12400, status: 1, createTime: '2026-10-27', createBy: '李敏' },
  { id: 4, orderNo: 'SO-20261026-003', customerName: '百康连锁药房', drugNames: '连花清瘟胶囊 ×150盒', totalAmount: 6450, status: 1, createTime: '2026-10-26', createBy: '李敏' },
  { id: 1, orderNo: 'SO-20261028-001', customerName: '仁和诊所', drugNames: '布洛芬缓释胶囊 ×200盒', totalAmount: 5280, status: 3, createTime: '2026-10-28', createBy: '王建国' }
]

// 确认出库：调接口后刷新列表
async function outbound(row) {
  try {
    await outboundSalesOrder(row.id)
    notifySuccess('出库成功，已按效期优先 FIFO 扣减批次')
    tableRef.value.reload()
  } catch (e) {
    notifyError(e.message || '出库失败')
  }
}
</script>
