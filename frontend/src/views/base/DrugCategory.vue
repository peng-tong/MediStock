<template>
  <!-- 药品分类：列表 + 弹窗表单（新增/编辑/删除） -->
  <div>
    <PageCard title="药品分类" desc="基础资料 · 处方药 / OTC / 中成药 / 保健品 / 医疗器械">
      <template #actions>
        <button class="btn btn-primary" @click="openForm()">+ 新增分类</button>
      </template>
    </PageCard>

    <div class="toolbar">
      <input class="f-input" style="width:240px" v-model.trim="query.keyword" placeholder="搜索 分类名称 / 编码" @keydown.enter="tableRef.reload()">
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
    <Modal :visible="formVisible" :title="form.id ? '编辑分类' : '新增分类'" @close="formVisible = false">
      <div class="form-grid">
        <div class="f-item"><label>分类名称 *</label><input v-model.trim="form.name" placeholder="如 处方药"></div>
        <div class="f-item"><label>编码 *</label><input v-model.trim="form.code" placeholder="如 RX"></div>
        <div class="f-item"><label>排序</label><input type="number" v-model="form.sort" placeholder="数字越小越靠前"></div>
        <div class="f-item"><label>状态</label>
          <select v-model="form.status"><option :value="1">启用</option><option :value="0">停用</option></select>
        </div>
      </div>
      <template #footer>
        <button class="btn btn-ghost" @click="formVisible = false">取消</button>
        <button class="btn btn-primary" :disabled="saving" @click="save">保存</button>
      </template>
    </Modal>
  </div>
</template>

<script setup>
// 药品分类页：列表查询 + 弹窗表单
import { reactive, ref } from 'vue'
import PageCard from '../../components/PageCard.vue'
import DataTable from '../../components/DataTable.vue'
import Modal from '../../components/Modal.vue'
import { getCategories, createCategory, updateCategory, deleteCategory } from '../../api/base'
import { notifySuccess, notifyError, confirmDelete } from '../../utils/notify'

const tableRef = ref()
// 查询条件（传给 DataTable，变化自动刷新）
const query = reactive({ keyword: '', status: '' })

// 列配置：render 自定义渲染 / tag 状态标签
const columns = [
  { title: '分类名称', key: 'name', className: 'name' },
  { title: '编码', key: 'code', className: 'mono' },
  { title: '排序', key: 'sort', align: 'center' },
  { title: '状态', key: 'status', width: '90px',
    tag: (r) => (Number(r.status) === 1 ? { type: 'ok', text: '启用' } : { type: 'gray', text: '停用' }) }
]

// 拉取列表：接口异常时由 DataTable 回退 mock
function fetchData(params) {
  return getCategories({ keyword: params.keyword, status: params.status, page: params.page, pageSize: params.pageSize })
}

// 兜底演示数据（后端未启动时展示）
const mockData = [
  { id: 1, name: '处方药', code: 'RX', sort: 1, status: 1 },
  { id: 2, name: 'OTC', code: 'OTC', sort: 2, status: 1 },
  { id: 3, name: '中成药', code: 'TCM', sort: 3, status: 1 },
  { id: 4, name: '保健品', code: 'HCP', sort: 4, status: 1 },
  { id: 5, name: '医疗器械', code: 'MED', sort: 5, status: 0 }
]

// ---------- 弹窗表单 ----------
const formVisible = ref(false)
// 保存/删除进行中标记：按钮禁用 + 方法入口拦截，防止重复点击产生重复提交
const saving = ref(false)
const deleting = ref(false)
const form = reactive({ id: null, name: '', code: '', sort: 1, status: 1 })

// 打开表单：新增为空表单，编辑回填
function openForm(row) {
  Object.assign(form, row ? { ...row } : { id: null, name: '', code: '', sort: 1, status: 1 })
  formVisible.value = true
}

// 保存：新增/编辑走接口
async function save() {
  if (saving.value) return
  if (!form.name || !form.code) return notifyError('分类名称与编码必填')
  saving.value = true
  try {
    if (form.id) await updateCategory(form.id, form)
    else await createCategory(form)
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
    await deleteCategory(row.id)
    notifySuccess('已删除')
    tableRef.value.reload()
  } catch (e) {
    notifyError(e.message || '删除失败')
  } finally {
    deleting.value = false
  }
}
</script>
