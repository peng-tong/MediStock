<template>
  <!-- 租户管理：三列卡片网格 + 创建弹窗 + 启用/停用/删除 -->
  <div>
    <PageCard title="租户管理" desc="共享表 + tenant_id 行级隔离 · 超管/租户管理员两级管理">
      <template #actions>
        <button class="btn btn-primary" @click="openForm()">+ 创建租户</button>
      </template>
    </PageCard>

    <!-- 租户卡片网格 -->
    <div class="t-grid">
      <div class="card t-card" v-for="t in list" :key="t.id">
        <div class="t-head">
          <b class="t-name">{{ t.name }}</b>
          <span class="tag" :class="Number(t.status) === 1 ? 'ok' : 'gray'">{{ Number(t.status) === 1 ? '启用' : '停用' }}</span>
        </div>
        <div class="t-rows">
          <div class="t-row"><span class="muted">联系人</span><span>{{ t.contactName || '-' }}</span></div>
          <div class="t-row"><span class="muted">联系电话</span><span>{{ t.contactPhone || '-' }}</span></div>
          <div class="t-row"><span class="muted">套餐</span><span>{{ t.planName || '旗舰版' }}</span></div>
          <div class="t-row">
            <span class="muted">用户数</span>
            <div class="flex" style="gap:8px">
              <!-- 用户数进度条：满员高亮警告色 -->
              <div class="usage"><div class="usage-bar" :class="{ full: isFull(t) }" :style="{ width: usagePercent(t) + '%' }"></div></div>
              <span class="usage-txt" :class="{ 'text-warn': isFull(t) }">{{ t.usedCount || 0 }} / {{ t.maxUserCount }}</span>
            </div>
          </div>
        </div>
        <div class="t-actions">
          <button class="btn btn-sm" @click="toggleStatus(t)">{{ Number(t.status) === 1 ? '停用' : '启用' }}</button>
          <button class="btn btn-sm-danger" @click="remove(t)">删除</button>
        </div>
      </div>
    </div>

    <!-- 创建租户弹窗 -->
    <Modal :visible="formVisible" title="创建租户" @close="formVisible = false">
      <div class="form-grid">
        <div class="f-item"><label>租户名称 *</label><input v-model.trim="form.name" placeholder="如 康民连锁药业"></div>
        <div class="f-item"><label>租户编码 *</label><input v-model.trim="form.code" placeholder="如 KM01"></div>
        <div class="f-item"><label>最大用户数</label><input type="number" v-model="form.maxUserCount" placeholder="如 10"></div>
        <div class="f-item"><label>联系人</label><input v-model.trim="form.contactName" placeholder="联系人"></div>
        <div class="f-item"><label>联系电话</label><input v-model.trim="form.contactPhone" placeholder="联系电话"></div>
      </div>
      <template #footer>
        <button class="btn btn-ghost" @click="formVisible = false">取消</button>
        <button class="btn btn-primary" @click="save">创建</button>
      </template>
    </Modal>
  </div>
</template>

<script setup>
// 租户管理页：卡片网格 + 创建/启用停用/删除
import { reactive, ref } from 'vue'
import PageCard from '../../components/PageCard.vue'
import Modal from '../../components/Modal.vue'
import { getTenants, createTenant, updateTenantStatus, deleteTenant } from '../../api/system'
import { notifySuccess, notifyError, confirmDelete } from '../../utils/notify'

// 租户列表（非表格页，自行维护）
const list = ref([])

// 拉取租户列表：接口异常时回退 mock
async function load() {
  try {
    const data = await getTenants({ page: 1, pageSize: 100 })
    list.value = (data && data.list) || []
  } catch (e) {
    list.value = mockData
  }
}

// 兜底演示数据（后端未启动时展示）
const mockData = [
  { id: 1, name: '康民连锁药业', code: 'KM01', planName: '旗舰版', usedCount: 8, maxUserCount: 10, contactName: '陈经理', contactPhone: '13800000001', status: 1 },
  { id: 2, name: '仁和医疗', code: 'RH01', planName: '专业版', usedCount: 5, maxUserCount: 5, contactName: '刘主管', contactPhone: '13800000002', status: 1 },
  { id: 3, name: '百康医药', code: 'BK01', planName: '标准版', usedCount: 0, maxUserCount: 3, contactName: '周经理', contactPhone: '13800000003', status: 0 }
]

// 用户数进度百分比（超上限截断到 100%）
function usagePercent(t) {
  const max = Number(t.maxUserCount) || 0
  const used = Number(t.usedCount) || 0
  if (!max) return 0
  return Math.min(100, Math.round((used / max) * 100))
}
// 是否满员（触发警告色）
function isFull(t) {
  const max = Number(t.maxUserCount) || 0
  return max > 0 && Number(t.usedCount) >= max
}

// ---------- 创建弹窗 ----------
const formVisible = ref(false)
const form = reactive({ name: '', code: '', maxUserCount: 10, contactName: '', contactPhone: '' })

// 打开创建弹窗（租户仅支持创建，不支持编辑）
function openForm() {
  Object.assign(form, { name: '', code: '', maxUserCount: 10, contactName: '', contactPhone: '' })
  formVisible.value = true
}

// 创建租户
async function save() {
  if (!form.name || !form.code) return notifyError('租户名称与编码必填')
  try {
    await createTenant(form)
    notifySuccess('创建成功')
    formVisible.value = false
    load()
  } catch (e) {
    notifyError(e.message || '创建失败')
  }
}

// 启用/停用切换：状态取反后调接口
async function toggleStatus(t) {
  const next = Number(t.status) === 1 ? 0 : 1
  try {
    await updateTenantStatus(t.id, next)
    notifySuccess(next === 1 ? '已启用' : '已停用')
    load()
  } catch (e) {
    notifyError(e.message || '操作失败')
  }
}

// 删除（先二次确认，防止误点立即物理删除）
async function remove(t) {
  const ok = await confirmDelete(`确定删除租户「${t.name || t.id}」吗？删除后不可恢复。`)
  if (!ok) return
  try {
    await deleteTenant(t.id)
    notifySuccess('已删除')
    load()
  } catch (e) {
    notifyError(e.message || '删除失败')
  }
}

// 初始化加载
load()
</script>

<style scoped>
/* 租户卡片三列网格 */
.t-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:18px}
.t-card{display:flex;flex-direction:column;margin-bottom:0}
.t-head{display:flex;align-items:center;justify-content:space-between;margin-bottom:14px}
.t-name{font-size:16px;font-weight:700;color:var(--ink)}
.t-rows{display:flex;flex-direction:column;gap:10px;flex:1}
.t-row{display:flex;align-items:center;justify-content:space-between;font-size:13px}
.t-row .muted{font-size:12.5px}
/* 用户数进度条 */
.usage{width:130px;height:8px;border-radius:999px;background:var(--surface-2);border:1px solid var(--line);overflow:hidden}
.usage-bar{height:100%;border-radius:999px;background:var(--primary);transition:.3s}
.usage-bar.full{background:var(--warn)}
.usage-txt{font-size:12px;color:var(--body);white-space:nowrap}
.t-actions{display:flex;gap:8px;margin-top:16px;padding-top:14px;border-top:1px dashed var(--line)}
</style>
