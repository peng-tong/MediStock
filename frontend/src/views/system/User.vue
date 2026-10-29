<template>
  <!-- 用户管理：列表 + 弹窗表单 + 禁用/启用/重置密码操作 -->
  <div>
    <PageCard title="用户管理" desc="系统管理 · 用户账号、角色与登录状态">
      <template #actions>
        <button class="btn btn-primary" @click="openForm()">+ 新增用户</button>
      </template>
    </PageCard>

    <div class="toolbar">
      <input class="f-input" style="width:240px" v-model.trim="query.keyword" placeholder="搜索 用户名 / 姓名 / 手机号" @keydown.enter="tableRef.reload()">
      <select class="f-input" v-model="query.status">
        <option value="">全部状态</option><option value="1">启用</option><option value="0">禁用</option>
      </select>
      <div class="spacer"></div>
      <button class="btn btn-ghost" @click="tableRef.reload()">查询</button>
    </div>

    <DataTable ref="tableRef" :columns="columns" :fetch="fetchData" :mock="mockData" :query="query">
      <template #actions="{ row }">
        <button class="btn btn-sm" @click="openForm(row)">编辑</button>
        <!-- 禁用/启用切换：按当前状态取反 -->
        <button class="btn btn-sm" :class="{ 'btn-sm-danger': Number(row.status) === 1 }" @click="toggleStatus(row)">
          {{ Number(row.status) === 1 ? '禁用' : '启用' }}
        </button>
        <button class="btn btn-sm" @click="resetPassword(row)">重置密码</button>
        <button class="btn btn-sm-danger" @click="remove(row)">删除</button>
      </template>
    </DataTable>

    <!-- 新增/编辑弹窗 -->
    <Modal :visible="formVisible" :title="form.id ? '编辑用户' : '新增用户'" @close="formVisible = false">
      <div class="form-grid">
        <div class="f-item"><label>用户名 *</label><input v-model.trim="form.username" placeholder="登录账号"></div>
        <div class="f-item"><label>姓名 *</label><input v-model.trim="form.realName" placeholder="真实姓名"></div>
        <div class="f-item"><label>手机号</label><input v-model.trim="form.phone" placeholder="手机号"></div>
        <div class="f-item"><label>邮箱</label><input v-model.trim="form.email" placeholder="邮箱"></div>
        <div class="f-item"><label>状态</label>
          <select v-model="form.status"><option :value="1">启用</option><option :value="0">禁用</option></select>
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
// 用户管理页：列表查询 + 弹窗表单 + 状态切换/重置密码
import { reactive, ref } from 'vue'
import PageCard from '../../components/PageCard.vue'
import DataTable from '../../components/DataTable.vue'
import Modal from '../../components/Modal.vue'
import { getUsers, createUser, updateUser, deleteUser, updateUserStatus, resetUserPassword } from '../../api/system'
import { notifySuccess, notifyError } from '../../utils/notify'

const tableRef = ref()
// 查询条件（传给 DataTable，变化自动刷新）
const query = reactive({ keyword: '', status: '' })

// 角色展示：兼容后端返回的角色对象数组 / 角色名数组 / 字符串
function roleText(r) {
  const roles = r.roles
  if (Array.isArray(roles)) return roles.map((x) => x.roleName || x.name || x).join(' / ')
  return roles || '-'
}

// 列配置：render 自定义渲染 / tag 状态标签
const columns = [
  { title: '用户名', key: 'username', className: 'mono name' },
  { title: '姓名', key: 'realName' },
  { title: '手机号', key: 'phone' },
  { title: '角色', key: 'roles', render: roleText },
  { title: '所属租户', key: 'tenantName' },
  { title: '状态', key: 'status', width: '90px',
    tag: (r) => (Number(r.status) === 1 ? { type: 'ok', text: '启用' } : { type: 'danger', text: '禁用' }) },
  { title: '最后登录', key: 'lastLoginTime', className: 'dim' }
]

// 拉取列表：接口异常时由 DataTable 回退 mock
function fetchData(params) {
  return getUsers({ keyword: params.keyword, status: params.status, page: params.page, pageSize: params.pageSize })
}

// 兜底演示数据（后端未启动时展示）
const mockData = [
  { id: 1, username: 'admin', realName: '王建国', phone: '13800000001', roles: [{ roleName: '超管' }], tenantName: '平台', status: 1, lastLoginTime: '2026-10-29 09:12' },
  { id: 2, username: 'zhangwei', realName: '张伟', phone: '13800000002', roles: [{ roleName: '采购' }], tenantName: '康民连锁药业', status: 1, lastLoginTime: '2026-10-28 16:40' },
  { id: 3, username: 'limin', realName: '李敏', phone: '13800000003', roles: '销售', tenantName: '康民连锁药业', status: 1, lastLoginTime: '2026-10-28 11:05' },
  { id: 4, username: 'chenhao', realName: '陈浩', phone: '13800000004', roles: [{ roleName: '库管' }], tenantName: '仁和医疗', status: 0, lastLoginTime: '2026-10-20 15:22' },
  { id: 5, username: 'zhourui', realName: '周瑞', phone: '13800000005', roles: '库管', tenantName: '仁和医疗', status: 1, lastLoginTime: '2026-10-26 10:33' }
]

// ---------- 弹窗表单 ----------
const formVisible = ref(false)
const form = reactive({ id: null, username: '', realName: '', phone: '', email: '', status: 1 })

// 打开表单：新增为空表单，编辑回填
function openForm(row) {
  Object.assign(form, row ? { ...row } : { id: null, username: '', realName: '', phone: '', email: '', status: 1 })
  formVisible.value = true
}

// 保存：新增/编辑走接口
async function save() {
  if (!form.username || !form.realName) return notifyError('用户名与姓名必填')
  try {
    if (form.id) await updateUser(form.id, form)
    else await createUser(form)
    notifySuccess('保存成功')
    formVisible.value = false
    tableRef.value.reload()
  } catch (e) {
    notifyError(e.message || '保存失败')
  }
}

// 禁用/启用切换：状态取反后调接口
async function toggleStatus(row) {
  const next = Number(row.status) === 1 ? 0 : 1
  try {
    await updateUserStatus(row.id, next)
    notifySuccess(next === 1 ? '已启用' : '已禁用')
    tableRef.value.reload()
  } catch (e) {
    notifyError(e.message || '操作失败')
  }
}

// 重置密码：恢复为默认密码
async function resetPassword(row) {
  try {
    await resetUserPassword(row.id)
    notifySuccess(`已重置 ${row.username} 的密码`)
  } catch (e) {
    notifyError(e.message || '重置失败')
  }
}

// 删除
async function remove(row) {
  try {
    await deleteUser(row.id)
    notifySuccess('已删除')
    tableRef.value.reload()
  } catch (e) {
    notifyError(e.message || '删除失败')
  }
}
</script>
