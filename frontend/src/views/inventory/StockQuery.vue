<template>
  <!-- 库存查询：全仓库实时库存一览 + 库存下限预警标识 -->
  <div>
    <PageCard title="库存查询" desc="实时库存 · 批号维度汇总 / 低于下限自动预警">
    </PageCard>

    <!-- 顶部统计卡为原型静态数据，后端暂无汇总接口，先屏蔽 -->

    <!-- 查询工具栏 -->
    <div class="toolbar">
      <input class="f-input" style="width:240px" v-model.trim="query.keyword" placeholder="搜索 药品名称 / 批号" @keydown.enter="tableRef.reload()">
      <select class="f-input" v-model="query.warehouseId">
        <option value="">全部仓库</option>
        <!-- 仓库选项来自后端真实仓库档案，避免与库内实际 ID 不一致 -->
        <option v-for="w in warehouseOptions" :key="w.id" :value="w.id">{{ w.name }}</option>
      </select>
      <button class="btn btn-ghost" @click="search">查询</button>
      <!-- 清空筛选：同时回到第一页，避免筛选后停留在超出总页数的空白页 -->
      <button class="btn btn-ghost" @click="resetQuery">重置</button>
    </div>

    <!-- 库存明细表 -->
    <DataTable ref="tableRef" :columns="columns" :fetch="fetchData" :mock="mockData" :query="query" />
  </div>
</template>

<script setup>
// 库存查询页：列表 + 统计卡 + 下限预警标签
import { reactive, ref, onMounted } from 'vue'
import PageCard from '../../components/PageCard.vue'
import DataTable from '../../components/DataTable.vue'
import { getStocks } from '../../api/inventory'
import { getWarehouses } from '../../api/base'

const tableRef = ref()
// 查询条件（传给 DataTable，变化自动刷新）
const query = reactive({ keyword: '', warehouseId: '' })

// 仓库筛选下拉：从后端仓库档案加载真实列表（硬编码 1/2/3 与实际库 ID 不一致会导致筛出 0 条）
const warehouseOptions = ref([])
onMounted(async () => {
  try {
    const data = await getWarehouses({ page: 1, pageSize: 100 })
    warehouseOptions.value = (data && data.list) || []
  } catch (e) {
    warehouseOptions.value = []
  }
})

// HTML 转义：药品名来自用户档案，直接拼入 v-html 会有存储型 XSS 风险
function escapeHtml(v) {
  return String(v ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

// 列配置：数量低于下限时在药品名后追加预警标签
const columns = [
  { title: '药品', key: 'drugName', className: 'name', width: '180px', html: true,
    // html:true 让 DataTable 用 v-html 渲染低于下限标签；药品名必须先转义，防止存储型 XSS
    render: (r) => Number(r.quantity) < Number(r.minStock)
      ? `${escapeHtml(r.drugName)}<span class="tag warn" style="margin-left:6px">低于下限</span>` : escapeHtml(r.drugName) },
  { title: '规格', key: 'spec', className: 'dim' },
  { title: '仓库', key: 'warehouseName' },
  { title: '库存数量', key: 'quantity', align: 'center' },
  { title: '可用数量', key: 'availableQuantity', align: 'center' },
  { title: '库存下限', key: 'minStock', className: 'dim', align: 'center' },
  { title: '更新时间', key: 'updateTime', className: 'dim', width: '150px',
    render: (r) => String(r.updateTime || '').slice(0, 16).replace('T', ' ') }
]

// 查询：从第一页开始检索，避免在后续页点击查询出现「暂无数据」的误解
function search() {
  tableRef.value && tableRef.value.reset()
}

// 重置筛选条件并回到第一页
function resetQuery() {
  query.keyword = ''
  query.warehouseId = ''
  search()
}

// 拉取接口：异常时由 DataTable 回退 mock
function fetchData(params) {
  return getStocks({ keyword: params.keyword, warehouseId: params.warehouseId, page: params.page, pageSize: params.pageSize })
}

// 兜底演示数据（含 1 条低于下限：维生素C 32 < 80）
const mockData = [
  { drugName: '阿莫西林胶囊', spec: '0.25g×24粒', warehouseName: '常温库', quantity: 850, availableQuantity: 845, minStock: 100, updateTime: '2026-10-29 09:12' },
  { drugName: '布洛芬缓释胶囊', spec: '0.3g×20粒', warehouseName: '常温库', quantity: 640, availableQuantity: 620, minStock: 80, updateTime: '2026-10-29 08:40' },
  { drugName: '连花清瘟胶囊', spec: '0.35g×48粒', warehouseName: '常温库', quantity: 120, availableQuantity: 120, minStock: 60, updateTime: '2026-10-28 16:05' },
  { drugName: '奥美拉唑肠溶胶囊', spec: '20mg×14粒', warehouseName: '阴凉库', quantity: 210, availableQuantity: 210, minStock: 50, updateTime: '2026-10-28 10:22' },
  { drugName: '维生素C片', spec: '0.1g×100片', warehouseName: '常温库', quantity: 32, availableQuantity: 32, minStock: 80, updateTime: '2026-10-27 15:30' },
  { drugName: '藿香正气口服液', spec: '10ml×10支', warehouseName: '阴凉库', quantity: 260, availableQuantity: 258, minStock: 40, updateTime: '2026-10-26 11:00' }
]
</script>
