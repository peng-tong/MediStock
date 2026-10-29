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
            <span>{{ userStore.roles[0] || '超级管理员' }}</span>
          </div>
          <div class="avatar" @click="logout">{{ firstChar }}</div>
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
import { userStore } from '../stores/user'
import { staticMenus } from '../menu'

const router = useRouter()
const route = useRoute()

// 当前显示的菜单：后端菜单树优先，空/异常时用静态菜单
const menus = computed(() => {
  if (userStore.menus && Array.isArray(userStore.menus) && userStore.menus.length) return userStore.menus
  return staticMenus
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

// 退出登录
async function logout() {
  await userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
/* ===== 主布局（对齐风格一原型） ===== */
.shell{display:grid;grid-template-columns:248px 1fr;height:100vh;overflow:hidden}
.sidebar{background:#fff;border-right:1px solid var(--line);display:flex;flex-direction:column;padding:20px 14px}
.brand-logo{display:flex;align-items:center;gap:12px;font-size:17px;font-weight:700;letter-spacing:1px;color:var(--ink);padding:6px 10px 18px;border-bottom:1px dashed var(--line)}
.brand-logo .mark{width:38px;height:38px;border-radius:12px;background:var(--primary-soft);color:var(--primary);display:grid;place-items:center;font-size:20px}
.nav{flex:1;overflow-y:auto;padding-top:14px}
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
</style>
