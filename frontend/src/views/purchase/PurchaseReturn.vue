<template>
  <div>
    <PageCard title="采购退货" desc="退货给供应商 · 冲减对应批次库存">
      <template #actions>
        <button class="btn btn-primary" @click="openForm()">+ 新建退货单</button>
      </template>
    </PageCard>

    <DataTable ref="tableRef" :columns="columns" :fetch="fetchData" :mock="mockData" :query="query"></DataTable>

    <!-- 新建采购退货单弹窗 -->
    <Modal :visible="formVisible" title="新建退货单" @close="formVisible = false">
      <!-- 单据头：供应商 / 退货仓库 -->
      <div class="form-grid">
        <div class="f-item">
          <label>供应商 *</label>
          <select v-model="form.supplierId">
            <option :value="null" disabled>请选择供应商</option>
            <option v-for="s in supplierOptions" :key="s.id" :value="s.id">{{ s.name }}</option>
          </select>
        </div>
        <div class="f-item">
          <label>退货仓库</label>
          <select v-model="form.warehouseId">
            <option :value="null" disabled>请选择仓库</option>
            <option v-for="w in warehouseOptions" :key="w.id" :value="w.id">{{ w.name }}</option>
          </select>
        </div>
        <div class="f-item" style="grid-column:span 3"><label>备注</label><input v-model.trim="form.remark" placeholder="退货原因说明"></div>
      </div>

      <!-- 明细行：药品 + 批号 + 数量 + 退货单价 + 金额自动 -->
      <div class="tbl-wrap" style="margin-top:16px">
        <table class="tbl">
          <thead>
            <tr><th>药品</th><th>选择批号</th><th>数量</th><th>退货单价</th><th>金额</th><th style="width:1%"></th></tr>
          </thead>
          <tbody>
            <tr v-for="(it, i) in form.items" :key="i">
              <td>
                <select v-model="it.drugId" style="height:32px;border:1.5px solid var(--line);border-radius:8px;padding:0 8px;font-size:12.5px;width:190px">
                  <option :value="null" disabled>请选择药品</option>
                  <option v-for="d in drugOptions" :key="d.id" :value="d.id">{{ d.genericName }} {{ d.spec || '' }}</option>
                </select>
              </td>
              <td>
                <select v-model="it.batchNo" style="height:32px;border:1.5px solid var(--line);border-radius:8px;padding:0 8px;font-size:12.5px;width:170px">
                  <option value="" disabled>选择批号</option>
                  <option v-for="b in batchOptions" :key="b.batchNo" :value="b.batchNo">{{ b.batchNo }}（效期 {{ b.expiryDate }}）</option>
                </select>
              </td>
              <td><input type="number" min="0" v-model.number="it.quantity" style="width:70px;height:32px;border:1.5px solid var(--line);border-radius:8px;padding:0 8px;text-align:center"></td>
              <td><input type="number" min="0" v-model.number="it.returnPrice" style="width:80px;height:32px;border:1.5px solid var(--line);border-radius:8px;padding:0 8px;text-align:center"></td>
              <td class="name">{{ fmtMoney((Number(it.quantity) || 0) * (Number(it.returnPrice) || 0)) }}</td>
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
// 采购退货页：退货列表 + 新建退货单弹窗（供应商 / 仓库 / 药品批号明细行）
import { computed, reactive, ref } from 'vue'
import PageCard from '../../components/PageCard.vue'
import DataTable from '../../components/DataTable.vue'
import Modal from '../../components/Modal.vue'
import {
  getPurchaseReturns, createPurchaseReturn,
  getSupplierOptions, getWarehouseOptions, getDrugOptions
} from '../../api/purchase'
import { notifySuccess, notifyError } from '../../utils/notify'

const tableRef = ref()
const query = reactive({})

// 金额千分位格式化
function fmtMoney(n) {
  return '¥ ' + (Number(n) || 0).toLocaleString('zh-CN')
}
// 状态映射：0 草稿 / 1 已完成 / 2 已取消
function statusTag(s) {
  const map = {
    0: { type: 'warn', text: '草稿' },
    1: { type: 'ok', text: '已完成' },
    2: { type: 'gray', text: '已取消' }
  }
  return map[s] || { type: 'gray', text: '未知' }
}

// 列配置
const columns = [
  { title: '退货单号', key: 'returnNo', className: 'mono name', width: '170px' },
  { title: '供应商', key: 'supplierName' },
  { title: '金额', key: 'totalAmount', render: (r) => fmtMoney(r.totalAmount) },
  { title: '状态', key: 'status', width: '90px', tag: (r) => statusTag(Number(r.status)) },
  { title: '日期', key: 'createTime', className: 'dim' },
  { title: '经办人', key: 'createBy', className: 'dim' }
]

// 拉取列表（失败时 DataTable 自动回退 mock）
function fetchData(params) {
  return getPurchaseReturns({ page: params.page, pageSize: params.pageSize })
}

// 兜底演示数据 3 条
const mockData = [
  { id: 1, returnNo: 'PR-20261028-001', supplierName: '华北制药', totalAmount: 1850, status: 1, createTime: '2026-10-28', createBy: '张伟' },
  { id: 2, returnNo: 'PR-20261026-002', supplierName: '恒瑞医药', totalAmount: 620, status: 0, createTime: '2026-10-26', createBy: '李敏' },
  { id: 3, returnNo: 'PR-20261024-003', supplierName: '石药集团', totalAmount: 420, status: 2, createTime: '2026-10-24', createBy: '张伟' }
]

// ---------- 下拉数据（接口失败回退固定数组） ----------
const supplierOptions = ref([])
const warehouseOptions = ref([])
const drugOptions = ref([])
// 静态批次数组（演示用）
const batchOptions = [
  { batchNo: 'A20250812', expiryDate: '2027-08-12' },
  { batchNo: 'B20250903', expiryDate: '2027-09-03' },
  { batchNo: 'L20251002', expiryDate: '2026-11-20' }
]

async function loadOptions() {
  try {
    const data = await getSupplierOptions()
    supplierOptions.value = (data && data.list) || []
  } catch (e) {
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
const form = reactive({ supplierId: null, warehouseId: null, remark: '', items: [] })

function openForm() {
  Object.assign(form, { supplierId: null, warehouseId: null, remark: '', items: [{ drugId: null, batchNo: '', quantity: 1, returnPrice: 0 }] })
  formVisible.value = true
}

function addRow() {
  form.items.push({ drugId: null, batchNo: '', quantity: 1, returnPrice: 0 })
}
function removeRow(i) {
  if (form.items.length > 1) form.items.splice(i, 1)
}

// 合计金额
const formTotal = computed(() => form.items.reduce((s, it) => s + (Number(it.quantity) || 0) * (Number(it.returnPrice) || 0), 0))

// 保存退货单
async function save() {
  if (!form.supplierId) return notifyError('请选择供应商')
  if (!form.items.length || form.items.some(it => !it.drugId || !it.batchNo)) return notifyError('请至少填写一行 药品 + 批号 明细')
  const payload = {
    supplierId: form.supplierId,
    warehouseId: form.warehouseId,
    remark: form.remark,
    items: form.items.map(it => ({
      drugId: it.drugId,
      batchNo: it.batchNo,
      quantity: Number(it.quantity) || 0,
      returnPrice: Number(it.returnPrice) || 0
    }))
  }
  try {
    await createPurchaseReturn(payload)
    notifySuccess('退货单已保存')
    formVisible.value = false
    tableRef.value.reload()
  } catch (e) {
    notifyError(e.message || '保存失败')
  }
}
</script>
