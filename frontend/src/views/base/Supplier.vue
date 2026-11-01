<template>
  <!-- 供应商档案：列表 + 弹窗表单（新增/编辑/删除） -->
  <div>
    <PageCard title="供应商档案" desc="基础资料 · 合作厂商 / 联系人 / 联系方式">
      <template #actions>
        <button class="btn btn-primary" @click="openForm()">+ 新增供应商</button>
      </template>
    </PageCard>

    <div class="toolbar">
      <input class="f-input" style="width:240px" v-model.trim="query.keyword" placeholder="搜索 名称 / 编码 / 联系人" @keydown.enter="tableRef.reload()">
      <select class="f-input" v-model="query.status">
        <option value="">全部状态</option><option value="1">启用</option><option value="0">停用</option>
      </select>
      <div class="spacer"></div>
      <button class="btn btn-ghost" @click="tableRef.reload()">查询</button>
    </div>

    <DataTable ref="tableRef" :columns="columns" :fetch="fetchData" :mock="mockData" :query="query">
      <template #actions="{ row }">
        <button class="btn btn-sm" @click="openForm(row)">编辑</button>
        <button class="btn btn-sm-danger" :disabled="deleting" @click="remove(row)">删除</button>
      </template>
    </DataTable>

    <!-- 新增/编辑弹窗 -->
    <Modal :visible="formVisible" :title="form.id ? '编辑供应商' : '新增供应商'" @close="formVisible = false">
      <div class="form-grid">
        <div class="f-item"><label>名称 *</label><input v-model.trim="form.name" placeholder="如 华北制药"></div>
        <div class="f-item"><label>编码 *</label><input v-model.trim="form.code" placeholder="如 SUP001"></div>
        <div class="f-item"><label>联系人</label><input v-model.trim="form.contactName" placeholder="联系人"></div>
        <div class="f-item"><label>联系电话</label><input v-model.trim="form.contactPhone" placeholder="联系电话"></div>
        <div class="f-item" style="grid-column:span 2"><label>地址</label><input v-model.trim="form.address" placeholder="公司地址"></div>
        <div class="f-item"><label>状态</label>
          <select v-model="form.status"><option :value="1">启用</option><option :value="0">停用</option></select>
        </div>
        <div class="f-item" style="grid-column:span 3"><label>备注</label><input v-model.trim="form.remark" placeholder="备注"></div>
      </div>
      <template #footer>
        <button class="btn btn-ghost" @click="formVisible = false">取消</button>
        <button class="btn btn-primary" :disabled="saving" @click="save">保存</button>
      </template>
    </Modal>
  </div>
</template>

<script setup>
// 供应商档案页：列表查询 + 弹窗表单
import { reactive, ref } from 'vue'
import PageCard from '../../components/PageCard.vue'
import DataTable from '../../components/DataTable.vue'
import Modal from '../../components/Modal.vue'
import { getSuppliers, createSupplier, updateSupplier, deleteSupplier } from '../../api/base'
import { notifySuccess, notifyError, confirmDelete } from '../../utils/notify'

const tableRef = ref()
// 查询条件（传给 DataTable，变化自动刷新）
const query = reactive({ keyword: '', status: '' })

// 列配置：render 自定义渲染 / tag 状态标签
const columns = [
  { title: '名称', key: 'name', className: 'name' },
  { title: '编码', key: 'code', className: 'mono' },
  { title: '联系人', key: 'contactName' },
  { title: '联系电话', key: 'contactPhone' },
  { title: '地址', key: 'address' },
  { title: '状态', key: 'status', width: '90px',
    tag: (r) => (Number(r.status) === 1 ? { type: 'ok', text: '启用' } : { type: 'gray', text: '停用' }) }
]

// 拉取列表：接口异常时由 DataTable 回退 mock
function fetchData(params) {
  return getSuppliers({ keyword: params.keyword, status: params.status, page: params.page, pageSize: params.pageSize })
}

// 兜底演示数据（后端未启动时展示）
const mockData = [
  { id: 1, name: '华北制药', code: 'SUP001', contactName: '王经理', contactPhone: '0311-88886666', address: '河北省石家庄市长安区', status: 1, remark: '青霉素类原料主要供应商' },
  { id: 2, name: '中美史克', code: 'SUP002', contactName: '李经理', contactPhone: '022-88885555', address: '天津市西青区', status: 1, remark: '' },
  { id: 3, name: '以岭药业', code: 'SUP003', contactName: '赵经理', contactPhone: '0311-88884444', address: '河北省石家庄市高新区', status: 1, remark: '连花清瘟胶囊专供' },
  { id: 4, name: '阿斯利康', code: 'SUP004', contactName: '张经理', contactPhone: '021-88883333', address: '上海市静安区', status: 1, remark: '' },
  { id: 5, name: '石药集团', code: 'SUP005', contactName: '刘经理', contactPhone: '0311-88882222', address: '河北省石家庄市裕华区', status: 0, remark: '暂停合作，待资质复审' }
]

// ---------- 弹窗表单 ----------
const formVisible = ref(false)
// 保存/删除进行中标记：按钮禁用 + 方法入口拦截，防止重复点击产生重复提交
const saving = ref(false)
const deleting = ref(false)
const form = reactive({ id: null, name: '', code: '', contactName: '', contactPhone: '', address: '', status: 1, remark: '' })

// 打开表单：新增为空表单，编辑回填
function openForm(row) {
  Object.assign(form, row ? { ...row } : { id: null, name: '', code: '', contactName: '', contactPhone: '', address: '', status: 1, remark: '' })
  formVisible.value = true
}

// 保存：新增/编辑走接口
async function save() {
  if (saving.value) return
  if (!form.name || !form.code) return notifyError('名称与编码必填')
  saving.value = true
  try {
    if (form.id) await updateSupplier(form.id, form)
    else await createSupplier(form)
    notifySuccess('保存成功')
    formVisible.value = false
    tableRef.value.reload()
  } catch (e) {
    notifyError(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 删除（先二次确认，防止误点立即物理删除）
async function remove(row) {
  if (deleting.value) return
  deleting.value = true
  try {
    const ok = await confirmDelete(`确定删除「${row.name || row.id}」吗？删除后不可恢复。`)
    if (!ok) return
    await deleteSupplier(row.id)
    notifySuccess('已删除')
    tableRef.value.reload()
  } catch (e) {
    notifyError(e.message || '删除失败')
  } finally {
    deleting.value = false
  }
}
</script>
