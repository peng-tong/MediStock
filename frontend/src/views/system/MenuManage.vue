<template>
  <!-- 菜单权限：getMenus 返回树 → 页面内扁平化为一维数组展示 -->
  <div>
    <PageCard title="菜单权限" desc="系统管理 · 菜单 / 按钮级权限点，树转扁平列表展示">
      <template #actions>
        <button class="btn btn-primary" @click="openForm()">+ 新增菜单</button>
      </template>
    </PageCard>

    <DataTable ref="tableRef" :columns="columns" :fetch="fetchData" :mock="mockData">
      <template #actions="{ row }">
        <button class="btn btn-sm" @click="openForm(row)">编辑</button>
        <button class="btn btn-sm-danger" @click="remove(row)">删除</button>
      </template>
    </DataTable>

    <!-- 新增/编辑弹窗 -->
    <Modal :visible="formVisible" :title="form.id ? '编辑菜单' : '新增菜单'" @close="formVisible = false">
      <div class="form-grid">
        <div class="f-item"><label>菜单名称 *</label><input v-model.trim="form.name" placeholder="如 药品档案"></div>
        <div class="f-item"><label>父菜单</label>
          <select v-model="form.parentId">
            <option :value="0">根</option>
            <option v-for="m in menuOptions" :key="m.id" :value="m.id">{{ m.name }}</option>
          </select>
        </div>
        <div class="f-item"><label>路由路径</label><input v-model.trim="form.path" placeholder="如 /base/drugs"></div>
        <div class="f-item"><label>图标</label><input v-model.trim="form.icon" placeholder="SVG path 或留空"></div>
        <div class="f-item"><label>类型</label>
          <select v-model="form.type"><option :value="1">目录</option><option :value="2">菜单</option><option :value="3">按钮</option></select>
        </div>
        <div class="f-item"><label>排序</label><input type="number" v-model="form.sort" placeholder="数字越小越靠前"></div>
        <div class="f-item"><label>状态</label>
          <select v-model="form.status"><option :value="1">显示</option><option :value="0">隐藏</option></select>
        </div>
      </div>
      <template #footer>
        <button class="btn btn-ghost" @click="formVisible = false">取消</button>
        <button class="btn btn-primary" @click="save">保存</button>
      </template>
    </Modal>
  </div>
</template>

<script setup>
// 菜单权限页：菜单树扁平化 + 弹窗表单
import { reactive, ref } from 'vue'
import PageCard from '../../components/PageCard.vue'
import DataTable from '../../components/DataTable.vue'
import Modal from '../../components/Modal.vue'
import { getMenus, createMenu, updateMenu, deleteMenu } from '../../api/system'
import { notifySuccess, notifyError, confirmDelete } from '../../utils/notify'

const tableRef = ref()

// 菜单树扁平化：为每个节点补充 parentName（无父级记 '根'）
function flattenTree(tree, parentName = '') {
  const list = []
  tree.forEach((node) => {
    list.push({ ...node, parentName: parentName || '根' })
    if (node.children && node.children.length) list.push(...flattenTree(node.children, node.name))
  })
  return list
}

// 列配置：render 自定义渲染 / tag 状态标签
const columns = [
  { title: '菜单名称', key: 'name', className: 'name' },
  { title: '父菜单', key: 'parentName', render: (r) => r.parentName || '根' },
  { title: '路由', key: 'path', className: 'mono' },
  { title: '类型', key: 'type', width: '90px',
    tag: (r) => ({ 1: { type: 'info', text: '目录' }, 2: { type: 'gray', text: '菜单' }, 3: { type: 'info', text: '按钮' } })[Number(r.type)] || { type: 'gray', text: '-' } },
  { title: '排序', key: 'sort', align: 'center' },
  { title: '状态', key: 'status', width: '90px',
    tag: (r) => (Number(r.status) === 1 ? { type: 'ok', text: '显示' } : { type: 'gray', text: '隐藏' }) }
]

// 兜底演示数据（6 条扁平数据，父级为 '根' 或 '系统管理'）
const mockData = [
  { id: 1, name: '工作台', parentId: 0, parentName: '根', path: '/dashboard', type: 1, sort: 1, status: 1 },
  { id: 2, name: '药品档案', parentId: 0, parentName: '根', path: '/base/drugs', type: 2, sort: 2, status: 1 },
  { id: 3, name: '采购订单', parentId: 0, parentName: '根', path: '/purchase/orders', type: 2, sort: 3, status: 1 },
  { id: 4, name: '销售开单', parentId: 0, parentName: '根', path: '/sales/orders', type: 2, sort: 4, status: 1 },
  { id: 5, name: '库存查询', parentId: 0, parentName: '根', path: '/inventory/stocks', type: 2, sort: 5, status: 1 },
  { id: 6, name: '用户管理', parentId: 9, parentName: '系统管理', path: '/system/users', type: 2, sort: 6, status: 1 }
]

// 上级菜单下拉选项（'根' 之外的父级候选，来自扁平化后的菜单）
const menuOptions = ref(mockData.map((m) => ({ id: m.id, name: m.name })))

// 拉取菜单树并扁平化；接口异常时回退 mock（同步给出下拉选项后抛出，由 DataTable 兜底）
async function fetchData(params) {
  try {
    const tree = await getMenus()
    const list = flattenTree(tree || [])
    // 同步刷新父菜单下拉选项
    menuOptions.value = list.map((n) => ({ id: n.id, name: n.name }))
    // 本地分页切片（DataTable 分页参数）
    const start = (params.page - 1) * params.pageSize
    return { list: list.slice(start, start + params.pageSize), total: list.length }
  } catch (e) {
    menuOptions.value = mockData.map((m) => ({ id: m.id, name: m.name }))
    throw e
  }
}

// ---------- 弹窗表单 ----------
const formVisible = ref(false)
const form = reactive({ id: null, name: '', parentId: 0, path: '', icon: '', type: 2, sort: 1, status: 1 })

// 打开表单：新增为空表单，编辑回填
function openForm(row) {
  Object.assign(form, row ? { ...row } : { id: null, name: '', parentId: 0, path: '', icon: '', type: 2, sort: 1, status: 1 })
  formVisible.value = true
}

// 保存：新增/编辑走接口
async function save() {
  if (!form.name) return notifyError('菜单名称必填')
  try {
    if (form.id) await updateMenu(form.id, form)
    else await createMenu(form)
    notifySuccess('保存成功')
    formVisible.value = false
    tableRef.value.reload()
  } catch (e) {
    notifyError(e.message || '保存失败')
  }
}

// 删除（先二次确认，防止误点立即物理删除）
async function remove(row) {
  const ok = await confirmDelete(`确定删除菜单「${row.name || row.id}」吗？删除后不可恢复。`)
  if (!ok) return
  try {
    await deleteMenu(row.id)
    notifySuccess('已删除')
    tableRef.value.reload()
  } catch (e) {
    notifyError(e.message || '删除失败')
  }
}
</script>
