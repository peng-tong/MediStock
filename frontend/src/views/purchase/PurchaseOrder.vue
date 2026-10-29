<template>
  <div>
    <PageCard title="采购订单" desc="采购单 · 审核通过后验收入库并生成批次库存">
      <template #actions>
        <button class="btn btn-primary" @click="openForm()">+ 新建采购单</button>
      </template>
    </PageCard>

    <!-- 状态筛选 chips（全部 / 待审核 / 已入库 / 已驳回） -->
    <div class="filters">
      <span class="chip" :class="{ active: query.status === '' }" @click="query.status = ''">全部</span>
      <span class="chip" :class="{ active: query.status === '1' }" @click="query.status = '1'">待审核</span>
      <span class="chip" :class="{ active: query.status === '3' }" @click="query.status = '3'">已入库</span>
      <span class="chip" :class="{ active: query.status === '4' }" @click="query.status = '4'">已驳回</span>
    </div>

    <DataTable ref="tableRef" :columns="columns" :fetch="fetchData" :mock="mockData" :query="query">
      <template #actions="{ row }">
        <!-- 待审核 / 待入库状态可跳转采购入库流程 -->
        <button v-if="[1, 2].includes(Number(row.status))" class="btn btn-sm" @click="router.push('/purchase/inbound')">入库</button>
        <button class="btn btn-sm" @click="openForm(row)">编辑</button>
        <button class="btn btn-sm-danger" @click="remove(row)">删除</button>
      </template>
    </DataTable>

    <!-- 新建 / 编辑采购单弹窗 -->
    <Modal :visible="formVisible" :title="form.id ? '编辑采购单' : '新建采购单'" @close="formVisible = false">
      <!-- 单据头：供应商 / 入库仓库 / 采购日期 / 备注 -->
      <div class="form-grid">
        <div class="f-item">
          <label>供应商 *</label>
          <select v-model="form.supplierId">
            <option :value="null" disabled>请选择供应商</option>
            <option v-for="s in supplierOptions" :key="s.id" :value="s.id">{{ s.name }}</option>
          </select>
        </div>
        <div class="f-item">
          <label>入库仓库</label>
          <select v-model="form.warehouseId">
            <option :value="null" disabled>请选择仓库</option>
            <option v-for="w in warehouseOptions" :key="w.id" :value="w.id">{{ w.name }}</option>
          </select>
        </div>
        <div class="f-item"><label>采购日期</label><input type="date" v-model="form.purchaseDate"></div>
        <div class="f-item" style="grid-column:span 3"><label>备注</label><input v-model.trim="form.remark" placeholder="备注说明"></div>
      </div>

      <!-- 明细行表：药品 / 数量 / 单价 / 金额自动 -->
      <div class="tbl-wrap" style="margin-top:16px">
        <table class="tbl">
          <thead>
            <tr><th>药品</th><th>数量</th><th>单价</th><th>金额</th><th style="width:1%"></th></tr>
          </thead>
          <tbody>
            <tr v-for="(it, i) in form.items" :key="i">
              <td>
                <select v-model="it.drugId" style="height:32px;border:1.5px solid var(--line);border-radius:8px;padding:0 8px;font-size:12.5px;width:200px">
                  <option :value="null" disabled>请选择药品</option>
                  <option v-for="d in drugOptions" :key="d.id" :value="d.id">{{ d.genericName }} {{ d.spec || '' }}</option>
                </select>
              </td>
              <td><input type="number" min="0" v-model.number="it.quantity" style="width:70px;height:32px;border:1.5px solid var(--line);border-radius:8px;padding:0 8px;text-align:center"></td>
              <td><input type="number" min="0" v-model.number="it.price" style="width:80px;height:32px;border:1.5px solid var(--line);border-radius:8px;padding:0 8px;text-align:center"></td>
              <td class="name">{{ fmtMoney((Number(it.quantity) || 0) * (Number(it.price) || 0)) }}</td>
              <td><button class="btn btn-sm-danger" @click="removeRow(i)">删除</button></td>
            </tr>
          </tbody>
        </table>
      </div>
      <div style="margin-top:10px">
        <button class="btn btn-ghost" style="height:30px" @click="addRow">+ 添加一行</button>
      </div>

      <!-- 底部合计 -->
      <div style="display:flex;align-items:center;gap:10px;margin-top:16px;justify-content:flex-end">
        <span class="dim">合计金额</span>
        <b style="font-size:20px;color:var(--primary-dark)">{{ fmtMoney(formTotal) }}</b>
      </div>

      <template #footer>
        <button class="btn btn-ghost" @click="formVisible = false">取消</button>
        <button class="btn btn-primary" @click="save">保存</button>
      </template>
    </Modal>
  </div>
</template>

<script setup>
// 采购订单页：列表（状态 chips 筛选）+ 新建/编辑采购单弹窗（单据头 + 明细行）
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageCard from '../../components/PageCard.vue'
import DataTable from '../../components/DataTable.vue'
import Modal from '../../components/Modal.vue'
import {
  getPurchaseOrders, createPurchaseOrder, updatePurchaseOrder, deletePurchaseOrder,
  getSupplierOptions, getWarehouseOptions, getDrugOptions
} from '../../api/purchase'
import { notifySuccess, notifyError } from '../../utils/notify'

const router = useRouter()
const tableRef = ref()
// 查询条件：status 传给 DataTable，chips 点击后自动刷新
const query = reactive({ status: '' })

// 金额千分位格式化：¥ 22,600
function fmtMoney(n) {
  return '¥ ' + (Number(n) || 0).toLocaleString('zh-CN')
}
// 今日日期 YYYY-MM-DD
function todayStr() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
// 状态映射：0 草稿 / 1 待审核 / 2 待入库 / 3 已入库 / 4 已取消
function statusTag(s) {
  const map = {
    0: { type: 'gray', text: '草稿' },
    1: { type: 'warn', text: '待审核' },
    2: { type: 'warn', text: '待入库' },
    3: { type: 'ok', text: '已入库' },
    4: { type: 'gray', text: '已取消' }
  }
  return map[s] || { type: 'gray', text: '未知' }
}

// 列配置
const columns = [
  { title: '采购单号', key: 'orderNo', className: 'mono name', width: '170px' },
  { title: '供应商', key: 'supplierName' },
  { title: '药品明细', key: 'drugNames', render: (r) => r.drugNames || (r.items || []).map(i => i.drugName).join('、') || '-' },
  { title: '金额', key: 'totalAmount', render: (r) => fmtMoney(r.totalAmount) },
  { title: '数量', key: 'quantity', render: (r) => `${Number(r.quantity) || 0} 盒` },
  { title: '状态', key: 'status', width: '90px', tag: (r) => statusTag(Number(r.status)) },
  { title: '采购日期', key: 'createTime', className: 'dim' },
  { title: '经办人', key: 'createBy', className: 'dim' }
]

// 拉取列表（失败时 DataTable 自动回退 mock）
function fetchData(params) {
  return getPurchaseOrders({ status: params.status, page: params.page, pageSize: params.pageSize })
}

// 兜底演示数据：1 条已入库(3) + 2 条待审核(1) + 1 条已驳回(4) + 1 条待入库(2)
const mockData = [
  { id: 1, orderNo: 'PO-20261028-001', supplierName: '华北制药', drugNames: '阿莫西林胶囊 ×500盒、阿司匹林肠溶片 ×100盒', totalAmount: 22600, quantity: 600, status: 1, createTime: '2026-10-28', createBy: '张伟' },
  { id: 2, orderNo: 'PO-20261027-002', supplierName: '太极集团', drugNames: '藿香正气口服液 ×300盒', totalAmount: 9900, quantity: 300, status: 3, createTime: '2026-10-27', createBy: '张伟' },
  { id: 3, orderNo: 'PO-20261026-003', supplierName: '恒瑞医药', drugNames: '二甲双胍缓释片 ×400盒', totalAmount: 12400, quantity: 400, status: 1, createTime: '2026-10-26', createBy: '李敏' },
  { id: 4, orderNo: 'PO-20261025-004', supplierName: '石药集团', drugNames: '阿司匹林肠溶片 ×600盒', totalAmount: 8400, quantity: 600, status: 4, createTime: '2026-10-25', createBy: '张伟' },
  { id: 5, orderNo: 'PO-20261024-005', supplierName: '扬子江药业', drugNames: '奥美拉唑肠溶胶囊 ×200盒', totalAmount: 15200, quantity: 200, status: 2, createTime: '2026-10-24', createBy: '李敏' }
]

// ---------- 下拉数据（接口失败回退固定数组） ----------
const supplierOptions = ref([])
const warehouseOptions = ref([])
const drugOptions = ref([])

async function loadOptions() {
  try {
    const data = await getSupplierOptions()
    supplierOptions.value = (data && data.list) || []
  } catch (e) {
    // 后端不可用时的固定供应商列表
    supplierOptions.value = [
      { id: 1, name: '华北制药' }, { id: 2, name: '太极集团' }, { id: 3, name: '恒瑞医药' },
      { id: 4, name: '石药集团' }, { id: 5, name: '扬子江药业' }
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
// 明细行：每行 { drugId, quantity, price }，金额由数量*单价实时算出
const form = reactive({ id: null, supplierId: null, warehouseId: null, purchaseDate: todayStr(), remark: '', items: [] })

// 新增为空表单，编辑时回填单据头与明细
function openForm(row) {
  Object.assign(form, {
    id: row ? row.id : null,
    supplierId: row ? row.supplierId ?? null : null,
    warehouseId: row ? row.warehouseId ?? null : null,
    purchaseDate: row ? (row.createTime || todayStr()) : todayStr(),
    remark: row ? row.remark || '' : '',
    items: row && row.items && row.items.length
      ? row.items.map(it => ({ drugId: it.drugId, quantity: it.quantity, price: it.purchasePrice ?? it.price ?? 0 }))
      : [{ drugId: null, quantity: 1, price: 0 }]
  })
  formVisible.value = true
}

// 明细行增删
function addRow() {
  form.items.push({ drugId: null, quantity: 1, price: 0 })
}
function removeRow(i) {
  if (form.items.length > 1) form.items.splice(i, 1)
}

// 合计金额
const formTotal = computed(() => form.items.reduce((s, it) => s + (Number(it.quantity) || 0) * (Number(it.price) || 0), 0))

// 保存：新增/编辑走接口
async function save() {
  if (!form.supplierId) return notifyError('请选择供应商')
  if (!form.items.length || form.items.some(it => !it.drugId)) return notifyError('请至少填写一行药品明细')
  const payload = {
    supplierId: form.supplierId,
    warehouseId: form.warehouseId,
    purchaseDate: form.purchaseDate,
    remark: form.remark,
    items: form.items.map(it => ({
      drugId: it.drugId,
      quantity: Number(it.quantity) || 0,
      purchasePrice: Number(it.price) || 0
    }))
  }
  try {
    if (form.id) await updatePurchaseOrder(form.id, payload)
    else await createPurchaseOrder(payload)
    notifySuccess('保存成功')
    formVisible.value = false
    tableRef.value.reload()
  } catch (e) {
    notifyError(e.message || '保存失败')
  }
}

// 删除采购单
async function remove(row) {
  try {
    await deletePurchaseOrder(row.id)
    notifySuccess('已删除')
    tableRef.value.reload()
  } catch (e) {
    notifyError(e.message || '删除失败')
  }
}
</script>
