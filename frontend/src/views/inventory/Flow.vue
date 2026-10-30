<template>
  <!-- 库存流水：入库/出库/退货/盘点调整等业务变动的可追溯台账 -->
  <div>
    <PageCard title="库存流水" desc="全量库存变动流水 · 入库 / 出库 / 退货 / 盘点调整，全程可追溯">
    </PageCard>

    <!-- 业务类型筛选（chip 驱动 query.bizType） -->
    <div class="filters">
      <span class="chip" :class="{ active: query.bizType === '' }" @click="setType('')">全部</span>
      <span class="chip" :class="{ active: query.bizType === 1 }" @click="setType(1)">采购入库</span>
      <span class="chip" :class="{ active: query.bizType === 3 }" @click="setType(3)">销售出库</span>
      <span class="chip" :class="{ active: query.bizType === 4 }" @click="setType(4)">销售退货</span>
      <span class="chip" :class="{ active: query.bizType === 2 }" @click="setType(2)">采购退货</span>
      <span class="chip" :class="{ active: query.bizType === 5 }" @click="setType(5)">盘点调整</span>
    </div>

    <!-- 流水明细表 -->
    <DataTable ref="tableRef" :columns="columns" :fetch="fetchData" :mock="mockData" :query="query" />
  </div>
</template>

<script setup>
// 库存流水页：变动数量按正负着色（入库绿 / 出库红）
import { reactive, ref } from 'vue'
import PageCard from '../../components/PageCard.vue'
import DataTable from '../../components/DataTable.vue'
import { getFlows } from '../../api/inventory'

const tableRef = ref()
// 查询条件：bizType 为业务类型（1采购入库 2采购退货 3销售出库 4销售退货 5盘点调整）
const query = reactive({ bizType: '' })

// 业务类型标签映射
const TYPE_MAP = {
  1: { type: 'info', text: '采购入库' },
  2: { type: 'warn', text: '采购退货' },
  3: { type: 'danger', text: '销售出库' },
  4: { type: 'ok', text: '销售退货' },
  5: { type: 'gray', text: '盘点调整' }
}

// 列配置：变动数量正数绿色 +n、负数红色 n
const columns = [
  { title: '流水号', key: 'flowNo', className: 'mono name', width: '170px' },
  { title: '业务类型', key: 'businessType', width: '100px', tag: (r) => TYPE_MAP[Number(r.businessType)] || { type: 'gray', text: '未知' } },
  { title: '业务单号', key: 'businessNo', className: 'mono', width: '150px' },
  { title: '药品', key: 'drugName', className: 'name' },
  { title: '仓库', key: 'warehouseName' },
  { title: '批号', key: 'batchNo', className: 'mono dim', width: '130px' },
  // html: true 表示 render 返回带样式的 HTML（由 DataTable 用 v-html 渲染）
  { title: '变动数量', key: 'changeQuantity', align: 'center', width: '90px', html: true,
    render: (r) => {
      const n = Number(r.changeQuantity)
      const color = n > 0 ? '#16A34A' : n < 0 ? '#DC2626' : 'inherit'
      const prefix = n > 0 ? '+' : ''
      return `<span style="color:${color};font-weight:600">${prefix}${n}</span>`
    } },
  { title: '操作人', key: 'operatorName', className: 'dim' },
  { title: '时间', key: 'createTime', className: 'dim', width: '150px',
    render: (r) => String(r.createTime || '').slice(0, 16).replace('T', ' ') }
]

// 拉取接口：异常时由 DataTable 回退 mock
function fetchData(params) {
  return getFlows({ bizType: params.bizType, page: params.page, pageSize: params.pageSize })
}

// 点击筛选 chip：更新 bizType 并回到第一页刷新
function setType(t) {
  query.bizType = t
  tableRef.value.reload()
}

// 兜底演示数据（覆盖入库 / 出库 / 退货 / 盘点调整）
const mockData = [
  { flowNo: 'FL-20261029-021', businessType: 1, businessNo: 'PO-20261028-005', drugName: '阿莫西林胶囊', warehouseName: '常温库', batchNo: 'A20250812', changeQuantity: 300, operator: '王建国', createTime: '2026-10-29 09:12' },
  { flowNo: 'FL-20261029-020', businessType: 3, businessNo: 'SO-20261029-012', drugName: '布洛芬缓释胶囊', warehouseName: '常温库', batchNo: 'B20250903', changeQuantity: -120, operator: '李敏', createTime: '2026-10-29 08:40' },
  { flowNo: 'FL-20261028-019', businessType: 4, businessNo: 'SO-20261028-008', drugName: '藿香正气口服液', warehouseName: '阴凉库', batchNo: 'H20261012', changeQuantity: 15, operator: '李敏', createTime: '2026-10-28 16:05' },
  { flowNo: 'FL-20261028-018', businessType: 5, businessNo: 'PC-20261027-002', drugName: '维生素C片', warehouseName: '常温库', batchNo: 'V20251001', changeQuantity: -20, operator: '陈浩', createTime: '2026-10-28 10:22' },
  { flowNo: 'FL-20261027-017', businessType: 2, businessNo: 'PO-20261025-003', drugName: '连花清瘟胶囊', warehouseName: '常温库', batchNo: 'L20261001', changeQuantity: -40, operator: '王建国', createTime: '2026-10-27 15:30' },
  { flowNo: 'FL-20261026-016', businessType: 1, businessNo: 'PO-20261024-001', drugName: '奥美拉唑肠溶胶囊', warehouseName: '阴凉库', batchNo: 'O20261028', changeQuantity: 210, operator: '王建国', createTime: '2026-10-26 11:00' }
]
</script>
