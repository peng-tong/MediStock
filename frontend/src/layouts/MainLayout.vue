<template>
  <!-- 主布局：左侧菜单（后端菜单优先，静态菜单兜底）+ 顶栏 + 内容区 -->
  <div class="shell">
    <aside class="sidebar">
      <div class="brand-logo">
        <span class="mark">✚</span> MediStock
      </div>
      <nav class="nav">
        <!-- 单条目（如工作台） -->
        <div v-for="m in flatItems" :key="m.name" class="nav-item" :class="{ active: isActive(m.path) }"
             @click="m.path && router.push(m.path)">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" v-html="m.icon"></svg>
          <span>{{ m.name }}</span>
        </div>
        <!-- 分组目录：手风琴折叠，点击模块名展开/收起 -->
        <div v-for="g in groups" :key="g.name" class="nav-group">
          <div class="group-head" role="button" :class="{ open: isOpen(g.name) }" @click="toggle(g.name)">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" v-html="g.icon"></svg>
            <span>{{ g.name }}</span>
            <svg class="chev" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 6l6 6-6 6"/></svg>
          </div>
          <div v-show="isOpen(g.name)" class="group-body">
            <div v-for="c in g.children" :key="c.name" class="nav-item sub" role="button" :class="{ active: isActive(c.path) }"
                 @click="c.path && router.push(c.path)">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" v-html="c.icon"></svg>
              <span>{{ c.name }}</span>
            </div>
          </div>
        </div>
      </nav>
      <div class="side-foot">MediStock v1.0</div>
    </aside>

    <div class="main-col">
      <header class="topbar">
        <div class="breadcrumb">
          {{ currentTitle }} <span class="cur">· {{ userStore.user ? userStore.user.realName || userStore.user.username : '' }}</span>
        </div>
        <div class="search-box">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4-4"/></svg>
          <input type="text" placeholder="全局搜索：药品 / 单号 / 批号 ..." @keydown.enter="router.push('/inventory/stocks')">
        </div>
        <div class="header-right">
          <div class="icon-btn" title="预警通知" @click="router.push('/alert')">
            <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M6 9a6 6 0 1 1 12 0c0 5 2 6 2 6H4s2-1 2-6"/><path d="M10 20a2 2 0 0 0 4 0"/></svg>
            <span class="dot"></span>
          </div>
          <div class="user-meta">
            <b>{{ userStore.user ? userStore.user.realName || userStore.user.username : '未登录' }}</b>
            <!-- 角色展示：真实角色优先；未分配时不再用「超级管理员」兜底误导 -->
            <span>{{ displayRole }}</span>
          </div>
          <!-- 头像下拉：修改密码 / 退出登录（退出前二次确认，避免误点直接登出） -->
          <div class="avatar-wrap">
            <el-dropdown trigger="click" @command="onUserCommand">
              <div class="avatar">{{ firstChar }}</div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="password">修改密码</el-dropdown-item>
                  <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </header>
      <div class="content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { userStore, isSuperAdmin } from '../stores/user'
import { staticMenus } from '../menu'
import { changePassword } from '../api/auth'
import { notifySuccess, notifyError, confirmDelete } from '../utils/notify'

const router = useRouter()
const route = useRoute()

// 当前显示的菜单：后端菜单树优先，空/异常时用静态菜单
// superAdminOnly 的模块属权限限制，非平台超管登录时不渲染（多租户模块）
const menus = computed(() => {
  const raw = (userStore.menus && Array.isArray(userStore.menus) && userStore.menus.length)
    ? userStore.menus
    : staticMenus
  return raw.filter((m) => !m.superAdminOnly || isSuperAdmin())
})

// 顶层单条目（无 children 且有 path）
const flatItems = computed(() => menus.value.filter((m) => m.path))
// 分组目录
const groups = computed(() => menus.value.filter((m) => m.children && m.children.length).map((m) => ({ ...m, children: m.children })))

function isActive(path) { return route.path === path }

// ===== 手风琴折叠状态：记录已展开的模块名 =====
const openNames = reactive(new Set())
function isOpen(name) { return openNames.has(name) }
function toggle(name) { openNames.has(name) ? openNames.delete(name) : openNames.add(name) }
// 根据当前路由自动展开所属模块（路由切换时保证激活模块可见）
watch(
  () => route.path,
  (p) => {
    const g = groups.value.find((m) => m.children.some((c) => c.path === p))
    if (g) openNames.add(g.name)
  },
  { immediate: true }
)

// 顶栏标题：取当前路由对应菜单名
const currentTitle = computed(() => {
  const p = route.path
  for (const m of menus.value) {
    if (m.path === p) return m.name
    if (m.children) {
      const c = m.children.find((x) => x.path === p)
      if (c) return m.name + ' / ' + c.name
    }
  }
  return '工作台'
})

const firstChar = computed(() => {
  const u = userStore.user
  const n = (u && (u.realName || u.username)) || '药'
  return n.slice(0, 1)
})

// 顶栏角色展示：真实角色优先；无角色记录时按租户身份显示，杜绝「超级管理员」兜底误导
const displayRole = computed(() => {
  if (userStore.roles && userStore.roles.length) return userStore.roles.join(' / ')
  return isSuperAdmin() ? '平台管理员' : '未分配角色'
})

// 头像下拉命令：修改密码 / 退出登录（退出前二次确认）
async function onUserCommand(cmd) {
  if (cmd === 'logout') {
    // 退出登录弹窗：不复用「删除确认」文案，用独立的退出确认提示，避免误导
    const ok = await ElMessageBox.confirm('确定要退出登录吗？', '退出登录', {
      confirmButtonText: '退出登录', cancelButtonText: '取消', type: 'warning', distinguishCancelAndClose: true
    }).then(() => true).catch(() => false)
    if (!ok) return
    await userStore.logout()
    router.push('/login')
    return
  }
  if (cmd === 'password') {
    try {
      // 两步弹窗收集 原密码 → 新密码，再调用改密接口
      const old = await ElMessageBox.prompt('请输入当前登录密码', '修改密码', {
        inputType: 'password', confirmButtonText: '下一步', cancelButtonText: '取消'
      })
      const fresh = await ElMessageBox.prompt('请输入新密码（至少 6 位）', '修改密码', {
        inputType: 'password', confirmButtonText: '确认修改', cancelButtonText: '取消'
      })
      if (!fresh.value || fresh.value.length < 6) return notifyError('新密码至少 6 位')
      await changePassword({ oldPassword: old.value, newPassword: fresh.value })
      // 后端改密后已拉黑当前 token：提示后清理登录态并跳转登录页，避免后续请求 401
      notifySuccess('密码已修改，请重新登录')
      userStore.clearLocal()
      router.push('/login')
    } catch (e) {
      // 用户取消弹窗（cancel/close）静默；其余为接口错误
      if (e !== 'cancel' && e !== 'close') notifyError((e && e.message) || '修改密码失败')
    }
  }
}
</script>

<style scoped>
/* ===== 主布局（对齐风格一原型） ===== */
.shell{display:grid;grid-template-columns:248px 1fr;height:100vh;overflow:hidden}
.sidebar{background:#fff;border-right:1px solid var(--line);display:flex;flex-direction:column;padding:20px 14px;min-height:0}
.brand-logo{display:flex;align-items:center;gap:12px;font-size:17px;font-weight:700;letter-spacing:1px;color:var(--ink);padding:6px 10px 18px;border-bottom:1px dashed var(--line);flex:none}
.brand-logo .mark{width:38px;height:38px;border-radius:12px;background:var(--primary-soft);color:var(--primary);display:grid;place-items:center;font-size:20px}
/* 菜单区：内容超出时可上下滚动，但隐藏滚动条 */
.nav{flex:1;min-height:0;overflow-y:auto;overflow-x:hidden;padding-top:14px;
  scrollbar-width:none;-ms-overflow-style:none;overscroll-behavior:contain}
.nav::-webkit-scrollbar{width:0;height:0;display:none}
.nav-group{margin-bottom:6px}
/* 折叠头：模块行（图标 + 名称 + 展开箭头） */
.group-head{display:flex;align-items:center;gap:10px;padding:9px 12px;margin:2px 0;border-radius:10px;color:var(--body);cursor:pointer;transition:.18s;font-size:13.5px;font-weight:600;user-select:none}
.group-head svg:first-child{width:17px;height:17px;flex:none}
.group-head:hover{background:var(--surface-2);color:var(--primary)}
.group-head.open{color:var(--primary-dark);background:var(--surface-2)}
.group-head .chev{width:14px;height:14px;margin-left:auto;flex:none;transition:transform .2s;color:var(--muted)}
.group-head.open .chev{transform:rotate(90deg);color:var(--primary)}
/* 子菜单区 */
.group-body{padding-left:6px;animation:fadeIn .15s ease}
@keyframes fadeIn{from{opacity:0;transform:translateY(-2px)}to{opacity:1;transform:none}}
.group-body .nav-item.sub{padding-left:16px;font-weight:400}
.nav-item{display:flex;align-items:center;gap:10px;padding:9px 12px;margin:2px 0;border-radius:10px;color:var(--body);cursor:pointer;transition:.18s;font-size:13.5px;user-select:none}
.nav-item svg{width:17px;height:17px;flex:none}
.nav-item:hover{background:var(--surface-2);color:var(--primary)}
.nav-item.active{background:var(--primary-soft);color:var(--primary-dark);font-weight:600}
.side-foot{padding:12px 10px 0;border-top:1px dashed var(--line);font-size:12px;color:var(--muted)}
.main-col{display:flex;flex-direction:column;overflow:hidden}
.topbar{background:#fff;border-bottom:1px solid var(--line);padding:14px 28px;display:flex;align-items:center;gap:20px}
.breadcrumb{font-size:13px;color:var(--ink);font-weight:600}
.breadcrumb .cur{color:var(--primary);font-weight:400;font-size:12px}
.search-box{display:flex;align-items:center;gap:10px;background:var(--surface-2);border:1.5px solid var(--line);border-radius:999px;padding:0 16px;height:40px;transition:.2s}
.search-box:focus-within{border-color:var(--primary);background:#fff}
.search-box svg{color:var(--muted);flex:none}
.search-box input{border:none;outline:none;flex:1;background:transparent;font-size:13.5px;color:var(--ink);min-width:220px}
.header-right{margin-left:auto;display:flex;align-items:center;gap:14px}
.icon-btn{position:relative;width:40px;height:40px;border-radius:12px;border:1.5px solid var(--line);background:var(--surface-2);display:grid;place-items:center;color:var(--body);cursor:pointer;transition:.2s}
.icon-btn:hover{border-color:var(--primary);color:var(--primary)}
.icon-btn .dot{position:absolute;top:8px;right:9px;width:7px;height:7px;border-radius:50%;background:var(--danger);box-shadow:0 0 0 2px #fff}
.user-meta b{display:block;color:var(--ink);font-size:13px}
.user-meta span{font-size:11px;color:var(--muted)}
.avatar{width:40px;height:40px;border-radius:50%;background:linear-gradient(135deg,#0F766E,#14B8A6);color:#fff;display:grid;place-items:center;font-weight:700;font-size:15px;cursor:pointer}
.content{flex:1;overflow-y:auto;padding:26px 28px 44px}
/* 窄屏/移动端：侧栏收窄为图标栏，隐藏全局搜索与用户文字，保证内容区可用 */
@media (max-width: 900px) {
  .shell{grid-template-columns:72px 1fr}
  .sidebar{padding:14px 8px}
  .brand-logo{justify-content:center;padding:6px 0 14px}
  .brand-logo span:not(.mark){display:none}
  .nav-item span,.group-head span,.group-head .chev,.side-foot{display:none}
  .nav-item,.group-head{justify-content:center;padding:10px 0}
  .search-box{display:none}
  .user-meta{display:none}
  .topbar{padding:12px 14px;gap:10px}
  .content{padding:16px 14px 32px}
}
</style>
