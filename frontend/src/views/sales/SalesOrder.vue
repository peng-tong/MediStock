<template>
  <div>
    <PageCard title="销售开单" desc="选择客户 / 药品 / 批号 / 数量 / 折扣 · 保存后按批次自动出库">
      <template #actions>
        <button class="btn btn-primary" @click="openForm()">+ 新建销售单</button>
      </template>
    </PageCard>

    <DataTable ref="tableRef" :columns="columns" :fetch="fetchData" :mock="mockData" :query="query">
      <template #actions="{ row }">
        <!-- 待出库状态可直接出库（FIFO 按批次扣减） -->
        <button v-if="[1, 2].includes(Number(row.status))" class="btn btn-sm" @click="outbound(row)">出库</button>
        <button class="btn btn-sm" @click="openForm(row)">编辑</button>
        <button class="btn btn-sm-danger" @click="remove(row)">删除</button>
      </template>
    </DataTable>

    <!-- 新建 / 编辑销售单大弹窗 -->
    <Modal :visible="formVisible" :title="form.id ? '编辑销售单' : '新建销售单'" width="880px" @close="formVisible = false">
      <!-- 单据头：客户 / 开单日期 / 结算方式 / 销售员 / 出库仓库 / 单据编号 -->
      <div class="form-grid">
        <div class="f-item">
          <label>客户 *</label>
          <select v-model="form.customerId">
            <option :value="null" disabled>请选择客户</option>
            <option v-for="c in customerOptions" :key="c.id" :value="c.id">{{ c.name }}</option>
          </select>
        </div>
        <div class="f-item"><label>开单日期</label><input type="date" v-model="form.saleDate"></div>
        <div class="f-item">
          <label>结算方式</label>
          <select v-model="form.settlementType">
            <option value="现结">现结</option>
            <option value="月结">月结</option>
          </select>
        </div>
        <div class="f-item"><label>销售员</label><input v-model.trim="form.salesman" placeholder="销售员"></div>
        <div class="f-item">
          <label>出库仓库</label>
          <select v-model="form.warehouseId">
            <option :value="null" disabled>请选择仓库</option>
            <option v-for="w in warehouseOptions" :key="w.id" :value="w.id">{{ w.name }}</option>
          </select>
        </div>
        <div class="f-item"><label>单据编号</label><input :value="form.orderNo" disabled></div>
      </div>

      <!-- 明细行表：药品 / 批号 / 有效期 / 数量 / 单价 / 折扣 / 金额 -->
      <div class="tbl-wrap" style="margin-top:16px">
        <table class="tbl">
          <thead>
            <tr><th>药品</th><th>选择批号</th><th>有效期</th><th>数量</th><th>单价</th><th>折扣</th><th>金额</th><th style="width:1%"></th></tr>
          </thead>
          <tbody>
            <tr v-for="(it, i) in form.items" :key="i">
              <td>
                <select v-model="it.drugId" style="height:32px;border:1.5px solid var(--line);border-radius:8px;padding:0 8px;font-size:12.5px;width:180px">
                  <option :value="null" disabled>请选择药品</option>
                  <option v-for="d in drugOptions" :key="d.id" :value="d.id">{{ d.genericName }} {{ d.spec || '' }}</option>
                </select>
              </td>
              <td>
                <select v-model="it.batchNo" style="height:32px;border:1.5px solid var(--line);border-radius:8px;padding:0 8px;font-size:12.5px;width:150px">
                  <option value="" disabled>选择批号</option>
                  <option v-for="b in batchOptions" :key="b.batchNo" :value="b.batchNo">{{ b.batchNo }}</option>
                </select>
              </td>
              <td class="mono">{{ expiryOf(it.batchNo) }}</td>
              <td><input type="number" min="0" v-model.number="it.quantity" style="width:64px;height:32px;border:1.5px solid var(--line);border-radius:8px;padding:0 8px;text-align:center"></td>
              <td><input type="number" min="0" v-model.number="it.salePrice" style="width:76px;height:32px;border:1.5px solid var(--line);border-radius:8px;padding:0 8px;text-align:center"></td>
              <td><input type="number" min="0" step="0.01" v-model.number="it.discount" placeholder="如 0.95" style="width:64px;height:32px;border:1.5px solid var(--line);border-radius:8px;padding:0 8px;text-align:center"></td>
              <td style="font-weight:600;color:var(--primary-dark)">{{ fmtMoney(lineAmount(it)) }}</td>
              <td><button class="btn btn-sm-danger" @click="removeRow(i)">删除</button></td>
            </tr>
          </tbody>
        </table>
      </div>
      <div style="margin-top:10px">
        <button class="btn btn-ghost" style="height:30px" @click="addRow">+ 添加一行</button>
      </div>

      <!-- 底部合计：数量 / 优惠 / 应收（实时计算） -->
      <div style="display:flex;align-items:center;gap:28px;margin-top:16px;justify-content:flex-end">
        <div><span class="dim">数量合计</span><b style="display:block;font-size:15px;color:var(--ink)">{{ totals.qty }} 盒</b></div>
        <div><span class="dim">优惠金额</span><b style="display:block;font-size:15px;color:var(--ink)">- {{ fmtMoney(totals.discount) }}</b></div>
        <div><span class="dim">应收合计</span><b style="display:block;font-size:22px;color:var(--primary-dark)">{{ fmtMoney(totals.receivable) }}</b></div>
      </div>
      <!-- FIFO 出库规则提示 -->
      <div class="note" style="margin-top:14px">出库自动按效期优先 FIFO 扣减批次，过期批次自动拦截，负库存禁止出库</div>

      <template #footer>
        <button class="btn btn-ghost" @click="formVisible = false">取消</button>
        <button class="btn btn-primary" @click="save">保存</button>
      </template>
    </Modal>
  </div>
</template>

<script setup>
// 销售开单页（重点）：列表 + 大弹窗（单据头 + 药品/批号/折扣明细行 + 实时合计）
import { computed, reactive, ref } from 'vue'
import PageCard from '../../components/PageCard.vue'
import DataTable from '../../components/DataTable.vue'
import Modal from '../../components/Modal.vue'
import {
  getSalesOrders, createSalesOrder, updateSalesOrder, deleteSalesOrder, outboundSalesOrder, getCustomerOptions
} from '../../api/sales'
import { getWarehouseOptions, getDrugOptions } from '../../api/purchase'
import { notifySuccess, notifyError } from '../../utils/notify'

const tableRef = ref()
const query = reactive({})

// 金额千分位格式化
function fmtMoney(n) {
  return '¥ ' + (Number(n) || 0).toLocaleString('zh-CN')
}
// 今日日期 YYYY-MM-DD
function todayStr() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
// 状态映射：0 草稿 / 1 待出库 / 3 已出库 / 4 已取消
function statusTag(s) {
  const map = {
    0: { type: 'gray', text: '草稿' },
    1: { type: 'warn', text: '待出库' },
    3: { type: 'ok', text: '已出库' },
    4: { type: 'gray', text: '已取消' }
  }
  return map[s] || { type: 'gray', text: '未知' }
}

// 列配置
const columns = [
  { title: '销售单号', key: 'orderNo', className: 'mono name', width: '170px' },
  { title: '客户', key: 'customerName' },
  { title: '金额', key: 'totalAmount', render: (r) => fmtMoney(r.totalAmount) },
  { title: '优惠', key: 'discountAmount', render: (r) => `- ${fmtMoney(r.discountAmount)}` },
  { title: '状态', key: 'status', width: '90px', tag: (r) => statusTag(Number(r.status)) },
  { title: '日期', key: 'createTime', className: 'dim' },
  { title: '开单人', key: 'createBy', className: 'dim' }
]

// 拉取列表（失败时 DataTable 自动回退 mock）
function fetchData(params) {
  return getSalesOrders({ page: params.page, pageSize: params.pageSize })
}

// 兜底演示数据 5 条
const mockData = [
  { id: 3, orderNo: 'SO-20261029-003', customerName: '康民大药房', totalAmount: 3690, discountAmount: 142, status: 0, createTime: '2026-10-29', createBy: '王建国' },
  { id: 1, orderNo: 'SO-20261028-001', customerName: '仁和诊所', totalAmount: 5280, discountAmount: 0, status: 3, createTime: '2026-10-28', createBy: '王建国' },
  { id: 2, orderNo: 'SO-20261027-002', customerName: '市第一人民医院药房', totalAmount: 12400, discountAmount: 0, status: 1, createTime: '2026-10-27', createBy: '李敏' },
  { id: 4, orderNo: 'SO-20261026-004', customerName: '百康连锁药房', totalAmount: 2682, discountAmount: 0, status: 1, createTime: '2026-10-26', createBy: '李敏' },
  { id: 5, orderNo: 'SO-20261025-005', customerName: '康民大药房', totalAmount: 1008, discountAmount: 0, status: 4, createTime: '2026-10-25', createBy: '王建国' }
]

// ---------- 下拉数据（接口失败回退固定数组） ----------
const customerOptions = ref([])
const warehouseOptions = ref([])
const drugOptions = ref([])
// 静态批次 mock 数组（演示用）
const batchOptions = [
  { batchNo: 'A20250812', expiryDate: '2027-08-12' },
  { batchNo: 'B20250903', expiryDate: '2027-09-03' },
  { batchNo: 'L20251002', expiryDate: '2026-11-20' }
]

async function loadOptions() {
  try {
    const data = await getCustomerOptions()
    customerOptions.value = (data && data.list) || []
  } catch (e) {
    customerOptions.value = [
      { id: 1, name: '康民大药房' }, { id: 2, name: '仁和诊所' },
      { id: 3, name: '市第一人民医院药房' }, { id: 4, name: '百康连锁药房' }
    ]
  }
  try {
    const data = await getWarehouseOptions()
    warehouseOptions.value = (data && data.list) || []
  } catch (e) {
    warehouseOptions.value = [{ id: 1, name: '常温库' }, { id: 2, name: '阴凉库' }, { id: 3, name: '冷藏库 2~8℃' }]
  }
  try {
    const data = await getDrugOptions()
    drugOptions.value = (data && data.list) || []
  } catch (e) {
    drugOptions.value = [
      { id: 1, genericName: '阿莫西林胶囊', spec: '0.25g×24粒' },
      { id: 2, genericName: '布洛芬缓释胶囊', spec: '0.3g×20粒' },
      { id: 3, genericName: '连花清瘟胶囊', spec: '0.35g×48粒' },
      { id: 4, genericName: '奥美拉唑肠溶胶囊', spec: '20mg×14粒' },
      { id: 5, genericName: '二甲双胍缓释片', spec: '0.5g×60片' }
    ]
  }
}
loadOptions()

// ---------- 弹窗表单 ----------
const formVisible = ref(false)
const form = reactive({ id: null, customerId: null, warehouseId: null, saleDate: todayStr(), settlementType: '现结', salesman: '', orderNo: '', remark: '', items: [] })

// 单据编号自动生成：SO-日期-序号
function genOrderNo() {
  return `SO-${todayStr().replace(/-/g, '')}-001`
}

function openForm(row) {
  Object.assign(form, {
    id: row ? row.id : null,
    customerId: row ? row.customerId ?? null : null,
    warehouseId: row ? row.warehouseId ?? null : null,
    saleDate: row ? (row.createTime || todayStr()) : todayStr(),
    settlementType: row ? row.settlementType || '现结' : '现结',
    salesman: row ? row.salesman || '' : '',
    orderNo: genOrderNo(),
    remark: row ? row.remark || '' : '',
    items: row && row.items && row.items.length
      ? row.items.map(it => ({ drugId: it.drugId, batchNo: it.batchNo || '', quantity: it.quantity, salePrice: it.salePrice ?? 0, discount: 1 }))
      : [{ drugId: null, batchNo: '', quantity: 1, salePrice: 0, discount: 1 }]
  })
  formVisible.value = true
}

function addRow() {
  form.items.push({ drugId: null, batchNo: '', quantity: 1, salePrice: 0, discount: 1 })
}
function removeRow(i) {
  if (form.items.length > 1) form.items.splice(i, 1)
}

// 折扣系数：0.95 直接作系数；>1 视为百分比（如 95 → 0.95）
function discountFactor(d) {
  const n = Number(d)
  if (!n) return 1
  return n >= 1 ? n / 100 : n
}
// 选中批号对应的有效期
function expiryOf(batchNo) {
  const b = batchOptions.find(x => x.batchNo === batchNo)
  return b ? b.expiryDate : '-'
}
// 单行金额 = 数量 × 单价 × 折扣系数（保留 2 位）
function lineAmount(it) {
  return Math.round((Number(it.quantity) || 0) * (Number(it.salePrice) || 0) * discountFactor(it.discount) * 100) / 100
}

// 底部合计：数量 / 优惠金额 / 应收合计（实时计算）
const totals = computed(() => {
  let qty = 0, receivable = 0, discount = 0
  form.items.forEach(it => {
    const q = Number(it.quantity) || 0
    const p = Number(it.salePrice) || 0
    const f = discountFactor(it.discount)
    const amt = q * p * f
    qty += q
    receivable += amt
    discount += q * p - amt
  })
  return { qty, receivable, discount }
})

// 保存销售单
async function save() {
  if (!form.customerId) return notifyError('请选择客户')
  if (!form.items.length || form.items.some(it => !it.drugId)) return notifyError('请至少填写一行药品明细')
  const payload = {
    customerId: form.customerId,
    warehouseId: form.warehouseId,
    saleDate: form.saleDate,
    settlementType: form.settlementType,
    salesman: form.salesman,
    remark: form.remark,
    discountAmount: Math.round(totals.value.discount * 100) / 100,
    items: form.items.map(it => ({
      drugId: it.drugId,
      quantity: Number(it.quantity) || 0,
      salePrice: Number(it.salePrice) || 0,
      discountAmount: Math.round((Number(it.quantity) || 0) * (Number(it.salePrice) || 0) * (1 - discountFactor(it.discount)) * 100) / 100
    }))
  }
  try {
    if (form.id) await updateSalesOrder(form.id, payload)
    else await createSalesOrder(payload)
    notifySuccess('保存成功')
    formVisible.value = false
    tableRef.value.reload()
  } catch (e) {
    notifyError(e.message || '保存失败')
  }
}

// 直接出库（FIFO 按批次扣减）
async function outbound(row) {
  try {
    await outboundSalesOrder(row.id)
    notifySuccess('出库成功，已按效期优先 FIFO 扣减批次')
    tableRef.value.reload()
  } catch (e) {
    notifyError(e.message || '出库失败')
  }
}

// 删除销售单
async function remove(row) {
  try {
    await deleteSalesOrder(row.id)
    notifySuccess('已删除')
    tableRef.value.reload()
  } catch (e) {
    notifyError(e.message || '删除失败')
  }
}
</script>
