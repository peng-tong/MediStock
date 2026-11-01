<template>
  <!-- 角色管理：列表 + 弹窗表单（新增/编辑/删除） -->
  <div>
    <PageCard title="角色管理" desc="系统管理 · 角色编码与备注，权限点配置后续接入">
      <template #actions>
        <button class="btn btn-primary" @click="openForm()">+ 新增角色</button>
      </template>
    </PageCard>

    <div class="toolbar">
      <input class="f-input" style="width:240px" v-model.trim="query.keyword" placeholder="搜索 角色名称 / 编码" @keydown.enter="tableRef.reload()">
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
    <Modal :visible="formVisible" :title="form.id ? '编辑角色' : '新增角色'" @close="formVisible = false">
      <div class="form-grid">
        <div class="f-item"><label>角色名称 *</label><input v-model.trim="form.name" placeholder="如 库管"></div>
        <div class="f-item"><label>编码 *</label><input v-model.trim="form.code" placeholder="如 STOCK_KEEPER"></div>
        <div class="f-item"><label>状态</label>
          <select v-model="form.status"><option :value="1">启用</option><option :value="0">停用</option></select>
        </div>
        <div class="f-item" style="grid-column:span 3"><label>备注</label><input v-model.trim="form.remark" placeholder="角色职责说明"></div>
      </div>
      <template #footer>
        <button class="btn btn-ghost" @click="formVisible = false">取消</button>
        <button class="btn btn-primary" @click="save">保存</button>
      </template>
    </Modal>
  </div>
</template>

<script setup>
// 角色管理页：列表查询 + 弹窗表单
import { reactive, ref } from 'vue'
import PageCard from '../../components/PageCard.vue'
import DataTable from '../../components/DataTable.vue'
import Modal from '../../components/Modal.vue'
import { getRoles, createRole, updateRole, deleteRole } from '../../api/system'
import { notifySuccess, notifyError, confirmDelete } from '../../utils/notify'

const tableRef = ref()
// 查询条件（传给 DataTable，变化自动刷新）
const query = reactive({ keyword: '' })

// 列配置：render 自定义渲染 / tag 状态标签
const columns = [
  { title: '角色名称', key: 'name', className: 'name' },
  { title: '编码', key: 'code', className: 'mono' },
  { title: '状态', key: 'status', width: '90px',
    tag: (r) => (Number(r.status) === 1 ? { type: 'ok', text: '启用' } : { type: 'gray', text: '停用' }) },
  { title: '备注', key: 'remark' }
]

// 拉取列表：接口异常时由 DataTable 回退 mock
function fetchData(params) {
  return getRoles({ keyword: params.keyword, page: params.page, pageSize: params.pageSize })
}

// 兜底演示数据（后端未启动时展示）
const mockData = [
  { id: 1, name: '超管', code: 'SUPER_ADMIN', status: 1, remark: '系统最高权限，可管理所有租户' },
  { id: 2, name: '租户管理员', code: 'TENANT_ADMIN', status: 1, remark: '租户内全部权限' },
  { id: 3, name: '库管', code: 'STOCK_KEEPER', status: 1, remark: '采购入库 / 库存管理' },
  { id: 4, name: '销售', code: 'SALES', status: 0, remark: '销售开单 / 客户档案' }
]

// ---------- 弹窗表单 ----------
const formVisible = ref(false)
const form = reactive({ id: null, name: '', code: '', status: 1, remark: '' })

// 打开表单：新增为空表单，编辑回填
function openForm(row) {
  Object.assign(form, row ? { ...row } : { id: null, name: '', code: '', status: 1, remark: '' })
  formVisible.value = true
}

// 保存：新增/编辑走接口
async function save() {
  if (!form.name || !form.code) return notifyError('角色名称与编码必填')
  try {
    if (form.id) await updateRole(form.id, form)
    else await createRole(form)
    notifySuccess('保存成功')
    formVisible.value = false
    tableRef.value.reload()
  } catch (e) {
    notifyError(e.message || '保存失败')
  }
}

// 删除（先二次确认，防止误点立即物理删除）
async function remove(row) {
  const ok = await confirmDelete(`确定删除角色「${row.name || row.id}」吗？删除后不可恢复。`)
  if (!ok) return
  try {
    await deleteRole(row.id)
    notifySuccess('已删除')
    tableRef.value.reload()
  } catch (e) {
    notifyError(e.message || '删除失败')
  }
}
</script>
