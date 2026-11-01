<template>
  <!-- 客户档案：列表 + 弹窗表单（新增/编辑/删除） -->
  <div>
    <PageCard title="客户档案" desc="基础资料 · 药店 / 诊所 / 医院等销售客户">
      <template #actions>
        <button class="btn btn-primary" @click="openForm()">+ 新增客户</button>
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
    <Modal :visible="formVisible" :title="form.id ? '编辑客户' : '新增客户'" @close="formVisible = false">
      <div class="form-grid">
        <div class="f-item"><label>名称 *</label><input v-model.trim="form.name" placeholder="如 康民大药房"></div>
        <div class="f-item"><label>编码 *</label><input v-model.trim="form.code" placeholder="如 CUS001"></div>
        <div class="f-item"><label>联系人</label><input v-model.trim="form.contactName" placeholder="联系人"></div>
        <div class="f-item"><label>联系电话</label><input v-model.trim="form.contactPhone" placeholder="联系电话"></div>
        <div class="f-item" style="grid-column:span 2"><label>地址</label><input v-model.trim="form.address" placeholder="门店地址"></div>
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
// 客户档案页：列表查询 + 弹窗表单
import { reactive, ref } from 'vue'
import PageCard from '../../components/PageCard.vue'
import DataTable from '../../components/DataTable.vue'
import Modal from '../../components/Modal.vue'
import { getCustomers, createCustomer, updateCustomer, deleteCustomer } from '../../api/base'
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
  return getCustomers({ keyword: params.keyword, status: params.status, page: params.page, pageSize: params.pageSize })
}

// 兜底演示数据（后端未启动时展示）
const mockData = [
  { id: 1, name: '康民大药房', code: 'CUS001', contactName: '陈店长', contactPhone: '13800000001', address: '石家庄市桥西区中山西路 88 号', status: 1, remark: '连锁药店，月结' },
  { id: 2, name: '仁和诊所', code: 'CUS002', contactName: '刘医生', contactPhone: '13800000002', address: '石家庄市长安区建华大街 12 号', status: 1, remark: '' },
  { id: 3, name: '市第一人民医院药房', code: 'CUS003', contactName: '周主任', contactPhone: '13800000003', address: '石家庄市新华区和平西路 215 号', status: 1, remark: '需走招投标流程' },
  { id: 4, name: '百康连锁药房', code: 'CUS004', contactName: '赵经理', contactPhone: '13800000004', address: '石家庄市裕华区槐安东路 66 号', status: 1, remark: '' },
  { id: 5, name: '健康源药房', code: 'CUS005', contactName: '孙店长', contactPhone: '13800000005', address: '石家庄市鹿泉区向阳大街 9 号', status: 0, remark: '欠款超账期，暂停供货' }
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
    if (form.id) await updateCustomer(form.id, form)
    else await createCustomer(form)
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
    await deleteCustomer(row.id)
    notifySuccess('已删除')
    tableRef.value.reload()
  } catch (e) {
    notifyError(e.message || '删除失败')
  } finally {
    deleting.value = false
  }
}
</script>
