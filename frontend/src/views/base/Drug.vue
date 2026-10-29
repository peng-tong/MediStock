<template>
  <!-- 药品档案：列表 + 弹窗表单（新增/编辑/删除） —— 列表页示范模板 -->
  <div>
    <PageCard title="药品档案" desc="基础资料 · 通用名 / 规格 / 剂型 / 批准文号 / 存储条件">
      <template #actions>
        <button class="btn btn-ghost">Excel 导入</button>
        <button class="btn btn-primary" @click="openForm()">+ 新增药品</button>
      </template>
    </PageCard>

    <div class="toolbar">
      <input class="f-input" style="width:240px" v-model.trim="query.keyword" placeholder="搜索 通用名 / 商品名 / 条码" @keydown.enter="tableRef.reload()">
      <select class="f-input" v-model="query.dosageForm">
        <option value="">全部剂型</option><option>胶囊</option><option>片剂</option><option>口服液</option>
      </select>
      <select class="f-input" v-model="query.status">
        <option value="">全部状态</option><option value="1">启用</option><option value="0">停用</option>
      </select>
      <div class="spacer"></div>
      <button class="btn btn-ghost" @click="tableRef.reload()">查询</button>
    </div>

    <DataTable ref="tableRef" :columns="columns" :fetch="fetchData" :mock="mockData" :query="query">
      <template #actions="{ row }">
        <button class="btn btn-sm" @click="openForm(row)">编辑</button>
        <button class="btn btn-sm-danger" @click="remove(row)">删除</button>
      </template>
    </DataTable>

    <!-- 新增/编辑弹窗 -->
    <Modal :visible="formVisible" :title="form.id ? '编辑药品' : '新增药品'" @close="formVisible = false">
      <div class="form-grid">
        <div class="f-item"><label>通用名 *</label><input v-model.trim="form.genericName" placeholder="如 阿莫西林"></div>
        <div class="f-item"><label>商品名</label><input v-model.trim="form.tradeName" placeholder="商品名"></div>
        <div class="f-item"><label>规格 *</label><input v-model.trim="form.spec" placeholder="如 0.25g×24粒"></div>
        <div class="f-item"><label>剂型</label>
          <select v-model="form.dosageForm"><option>胶囊</option><option>片剂</option><option>口服液</option><option>颗粒剂</option></select>
        </div>
        <div class="f-item"><label>生产厂家</label><input v-model.trim="form.manufacturer" placeholder="生产厂家"></div>
        <div class="f-item"><label>批准文号</label><input v-model.trim="form.approvalNo" placeholder="国药准字..."></div>
        <div class="f-item"><label>条码</label><input v-model.trim="form.barcode" placeholder="商品条码"></div>
        <div class="f-item"><label>存储条件</label>
          <select v-model="form.storageCondition"><option>常温 ≤30℃</option><option>阴凉 ≤20℃</option><option>冷藏 2~8℃</option></select>
        </div>
        <div class="f-item"><label>库存下限</label><input type="number" v-model="form.minStock" placeholder="预警用"></div>
        <div class="f-item"><label>状态</label>
          <select v-model="form.status"><option :value="1">启用</option><option :value="0">停用</option></select>
        </div>
        <div class="f-item" style="grid-column:span 2"><label>备注</label><input v-model.trim="form.remark" placeholder="备注"></div>
      </div>
      <template #footer>
        <button class="btn btn-ghost" @click="formVisible = false">取消</button>
        <button class="btn btn-primary" @click="save">保存</button>
      </template>
    </Modal>
  </div>
</template>

<script setup>
// 药品档案页：列表查询 + 弹窗表单
import { reactive, ref } from 'vue'
import PageCard from '../../components/PageCard.vue'
import DataTable from '../../components/DataTable.vue'
import Modal from '../../components/Modal.vue'
import StatusTag from '../../components/StatusTag.vue'
import { getDrugs, createDrug, updateDrug, deleteDrug } from '../../api/base'
import { notifySuccess, notifyError } from '../../utils/notify'

const tableRef = ref()
// 查询条件（传给 DataTable，变化自动刷新）
const query = reactive({ keyword: '', dosageForm: '', status: '' })

// 列配置：render 自定义渲染 / tag 状态标签
const columns = [
  { title: '药品名称', key: 'genericName', className: 'name', width: '140px',
    render: (r) => (r.tradeName ? `${r.genericName}（${r.tradeName}）` : r.genericName) },
  { title: '规格 / 剂型', key: 'spec', render: (r) => `${r.spec || '-'} · ${r.dosageForm || '-'}` },
  { title: '生产厂家', key: 'manufacturer' },
  { title: '批准文号', key: 'approvalNo', className: 'mono' },
  { title: '存储条件', key: 'storageCondition' },
  { title: '库存下限', key: 'minStock', align: 'center' },
  { title: '状态', key: 'status', width: '90px',
    tag: (r) => (Number(r.status) === 1 ? { type: 'ok', text: '启用' } : { type: 'gray', text: '停用' }) }
]

// 拉取列表：接口异常时由 DataTable 回退 mock
function fetchData(params) {
  return getDrugs({ keyword: params.keyword, dosageForm: params.dosageForm, status: params.status, page: params.page, pageSize: params.pageSize })
}

// 兜底演示数据（后端未启动时展示）
const mockData = [
  { id: 1, genericName: '阿莫西林胶囊', tradeName: '阿莫仙', spec: '0.25g×24粒', dosageForm: '胶囊', manufacturer: '华北制药', approvalNo: '国药准字H13021786', storageCondition: '常温 ≤30℃', minStock: 100, status: 1 },
  { id: 2, genericName: '布洛芬缓释胶囊', tradeName: '芬必得', spec: '0.3g×20粒', dosageForm: '胶囊', manufacturer: '中美史克', approvalNo: '国药准字H10970393', storageCondition: '常温 ≤30℃', minStock: 80, status: 1 },
  { id: 3, genericName: '连花清瘟胶囊', tradeName: '连花清瘟', spec: '0.35g×48粒', dosageForm: '胶囊', manufacturer: '以岭药业', approvalNo: '国药准字Z20040063', storageCondition: '常温 ≤30℃', minStock: 60, status: 1 },
  { id: 4, genericName: '奥美拉唑肠溶胶囊', spec: '20mg×14粒', dosageForm: '胶囊', manufacturer: '阿斯利康', approvalNo: '国药准字H20030920', storageCondition: '阴凉 ≤20℃', minStock: 50, status: 1 },
  { id: 5, genericName: '二甲双胍缓释片', tradeName: '格华止', spec: '0.5g×60片', dosageForm: '片剂', manufacturer: '扬子江药业', approvalNo: '国药准字H20040127', storageCondition: '常温 ≤30℃', minStock: 120, status: 1 },
  { id: 6, genericName: '氯雷他定片', tradeName: '开瑞坦', spec: '10mg×6片', dosageForm: '片剂', manufacturer: '拜耳医药', approvalNo: '国药准字H10970410', storageCondition: '常温 ≤30℃', minStock: 60, status: 0 }
]

// ---------- 弹窗表单 ----------
const formVisible = ref(false)
const form = reactive({ id: null, genericName: '', tradeName: '', spec: '', dosageForm: '胶囊', manufacturer: '', approvalNo: '', barcode: '', storageCondition: '常温 ≤30℃', minStock: 50, status: 1, remark: '' })

// 打开表单：新增为空表单，编辑回填
function openForm(row) {
  Object.assign(form, row ? { ...row } : { id: null, genericName: '', tradeName: '', spec: '', dosageForm: '胶囊', manufacturer: '', approvalNo: '', barcode: '', storageCondition: '常温 ≤30℃', minStock: 50, status: 1, remark: '' })
  formVisible.value = true
}

// 保存：新增/编辑走接口
async function save() {
  if (!form.genericName || !form.spec) return notifyError('通用名与规格必填')
  try {
    if (form.id) await updateDrug(form.id, form)
    else await createDrug(form)
    notifySuccess('保存成功')
    formVisible.value = false
    tableRef.value.reload()
  } catch (e) {
    notifyError(e.message || '保存失败')
  }
}

// 删除
async function remove(row) {
  try {
    await deleteDrug(row.id)
    notifySuccess('已删除')
    tableRef.value.reload()
  } catch (e) {
    notifyError(e.message || '删除失败')
  }
}
</script>
