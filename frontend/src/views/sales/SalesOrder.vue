<template>
  <div>
    <PageCard title="销售开单" desc="选择客户 / 药品 / 批号 / 数量 / 折扣 · 保存后按批次自动出库">
      <template #actions>
        <button class="btn btn-primary" @click="openForm()">+ 新建销售单</button>
      </template>
    </PageCard>

    <DataTable ref="tableRef" :columns="columns" :fetch="fetchData" :mock="mockData" :query="query">
      <template #actions="{ row }">
        <!-- 草稿先提交为待出库，之后才可出库 -->
        <button v-if="Number(row.status) === 0" class="btn btn-sm" @click="submit(row)">提交</button>
        <!-- 待出库(1)才可出库（FIFO 按批次扣减）；草稿需先提交 -->
        <button v-if="Number(row.status) === 1" class="btn btn-sm" @click="outbound(row)">出库</button>
        <button v-if="Number(row.status) === 0" class="btn btn-sm" @click="openForm(row)">编辑</button>
        <button v-if="Number(row.status) === 0" class="btn btn-sm-danger" @click="remove(row)">删除</button>
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
          <select v-model="form.warehouseId" @change="refreshAllBatches">
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
            <!-- 批号不再由用户选择：后端出库按效期优先 FIFO 自动扣批次，这里展示真实可用批次供参考 -->
            <tr><th>药品</th><th>可出库批次（效期优先）</th><th>数量</th><th>单价</th><th>折扣</th><th>金额</th><th style="width:1%"></th></tr>
          </thead>
          <tbody>
            <tr v-for="(it, i) in form.items" :key="i">
              <td>
                <select v-model="it.drugId" style="height:32px;border:1.5px solid var(--line);border-radius:8px;padding:0 8px;font-size:12.5px;width:180px" @change="loadBatches(it)">
                  <option :value="null" disabled>请选择药品</option>
                  <option v-for="d in drugOptions" :key="d.id" :value="d.id">{{ d.genericName }} {{ d.spec || '' }}</option>
                </select>
              </td>
              <td style="font-size:12px;line-height:1.6;color:var(--muted);white-space:normal;max-width:240px">
                <div>{{ batchText(it) }}</div>
                <!-- 可用库存合计（仅未过期批次）；超量仅提示，保存不拦截，出库时后端会拦截 -->
                <div v-if="it.drugId" style="margin-top:4px" :style="{ color: overStock(it) ? 'var(--danger)' : 'var(--muted)' }">
                  可用库存 {{ availableQty(it) }} 盒<template v-if="overStock(it)"> · 已超出，出库时将被拦截</template>
                </div>
              </td>
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
  getSalesOrders, getSalesOrder, createSalesOrder, updateSalesOrder, deleteSalesOrder, submitSalesOrder, outboundSalesOrder, getCustomerOptions
} from '../../api/sales'
import { getWarehouseOptions, getDrugOptions } from '../../api/purchase'
import { getStockBatches } from '../../api/inventory'
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
  { title: '日期', key: 'createTime', className: 'dim', render: (r) => String(r.createTime || '').slice(0, 10) },
  { title: '开单人', key: 'createByName', className: 'dim' }
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

// 新增为空表单；编辑时先请求详情接口，列表行不含明细，需回查后再回填
async function openForm(row) {
  let detail = row
  if (row) {
    try {
      detail = await getSalesOrder(row.id) || row
    } catch (e) {
      detail = row // 详情接口异常时退回列表行
    }
  }
  const items = (detail && detail.items) || []
  Object.assign(form, {
    id: detail ? detail.id : null,
    customerId: detail ? detail.customerId ?? null : null,
    warehouseId: detail ? detail.warehouseId ?? null : null,
    saleDate: detail ? String(detail.createTime || todayStr()).slice(0, 10) : todayStr(),
    settlementType: detail ? detail.settlementType || '现结' : '现结',
    salesman: detail ? detail.salesman || '' : '',
    orderNo: genOrderNo(),
    remark: detail ? detail.remark || '' : '',
    items: items.length
      ? items.map(it => ({ drugId: it.drugId, quantity: it.quantity, salePrice: it.salePrice ?? 0, discount: 1, batches: [] }))
      : [{ drugId: null, quantity: 1, salePrice: 0, discount: 1, batches: [] }]
  })
  formVisible.value = true
  // 编辑回填后按已有药品加载真实批次库存
  form.items.forEach(it => loadBatches(it))
}

function addRow() {
  form.items.push({ drugId: null, quantity: 1, salePrice: 0, discount: 1, batches: [] })
}
function removeRow(i) {
  if (form.items.length > 1) form.items.splice(i, 1)
}

// 折扣系数：0.95 直接作系数；>1 视为百分比（如 95 → 0.95）
function discountFactor(d) {
  const n = Number(d)
  // 空值 / 1 表示无折扣；0<n<=1 视为系数（0.95 = 95 折）；>1 视为百分比（95 = 95 折）
  if (!n) return 1
  return n > 1 ? n / 100 : n
}
// 选中药品/仓库后，从批次库存接口加载真实可出库批次（仅展示，出库仍由后端 FIFO 扣减）
async function loadBatches(it) {
  it.batches = []
  if (!it.drugId) return
  try {
    const data = await getStockBatches({
      drugId: it.drugId,
      warehouseId: form.warehouseId || undefined,
      page: 1,
      pageSize: 100
    })
    const today = todayStr()
    it.batches = ((data && data.list) || [])
      // 过滤零库存与已过期批次，并按有效期升序（即 FIFO 出库顺序）
      .filter(b => (Number(b.quantity) || 0) > 0 && String(b.expiryDate || '').slice(0, 10) >= today)
      .sort((a, b) => String(a.expiryDate || '').localeCompare(String(b.expiryDate || '')))
  } catch (e) {
    it.batches = []
  }
}

// 仓库变更时，重新拉取所有明细行的批次（批次按仓库维度存放）
function refreshAllBatches() {
  form.items.forEach(it => loadBatches(it))
}

// 可用库存合计（仅统计未过期批次，与后端出库过滤条件保持一致）
function availableQty(it) {
  return (it.batches || []).reduce((s, b) => s + (Number(b.quantity) || 0), 0)
}

// 明细数量是否超过可用库存（仅作界面提示，保存不做拦截）
function overStock(it) {
  return !!it.drugId && (Number(it.quantity) || 0) > availableQty(it)
}

// 批次展示文案：显示前两个 FIFO 批次（批号/有效期/可用数量）
function batchText(it) {
  if (!it.drugId) return '-'
  if (!it.batches || !it.batches.length) return '无可用批次'
  const list = it.batches.slice(0, 2)
    .map(b => `${b.batchNo}（${String(b.expiryDate || '').slice(0, 10)}，${Number(b.quantity) || 0}盒）`)
  return list.join('；') + (it.batches.length > 2 ? ` 等 ${it.batches.length} 批` : '')
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

// 提交销售单：草稿 → 待出库，提交后不可再编辑/删除，可执行出库
async function submit(row) {
  try {
    await submitSalesOrder(row.id)
    notifySuccess('已提交，等待出库')
    tableRef.value.reload()
  } catch (e) {
    notifyError(e.message || '提交失败')
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
