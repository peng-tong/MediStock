// 路由表：全部业务页面（静态注册，配合登录守卫）
import { createRouter, createWebHistory } from 'vue-router'
import { userStore, isSuperAdmin } from '../stores/user'
import { TOKEN_KEY } from '../api/request'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/Login.vue'), meta: { title: '登录' } },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    children: [
      // 工作台页面为纯静态假数据且无后端统计接口，已屏蔽（详见 docs/屏蔽功能清单.md）
      { path: '', redirect: '/base/drugs' },
      // 基础资料
      { path: 'base/drugs', component: () => import('../views/base/Drug.vue'), meta: { title: '药品档案' } },
      { path: 'base/categories', component: () => import('../views/base/DrugCategory.vue'), meta: { title: '药品分类' } },
      { path: 'base/suppliers', component: () => import('../views/base/Supplier.vue'), meta: { title: '供应商档案' } },
      { path: 'base/customers', component: () => import('../views/base/Customer.vue'), meta: { title: '客户档案' } },
      { path: 'base/warehouses', component: () => import('../views/base/Warehouse.vue'), meta: { title: '仓库档案' } },
      // 采购管理
      { path: 'purchase/orders', component: () => import('../views/purchase/PurchaseOrder.vue'), meta: { title: '采购订单' } },
      { path: 'purchase/inbound', component: () => import('../views/purchase/PurchaseInbound.vue'), meta: { title: '采购入库' } },
      { path: 'purchase/returns', component: () => import('../views/purchase/PurchaseReturn.vue'), meta: { title: '采购退货' } },
      // 销售管理
      { path: 'sales/orders', component: () => import('../views/sales/SalesOrder.vue'), meta: { title: '销售开单' } },
      { path: 'sales/outbound', component: () => import('../views/sales/SalesOutbound.vue'), meta: { title: '销售出库' } },
      { path: 'sales/returns', component: () => import('../views/sales/SalesReturn.vue'), meta: { title: '销售退货' } },
      // 库存管理
      { path: 'inventory/stocks', component: () => import('../views/inventory/StockQuery.vue'), meta: { title: '库存查询' } },
      { path: 'inventory/batches', component: () => import('../views/inventory/Batch.vue'), meta: { title: '批次管理' } },
      { path: 'inventory/flows', component: () => import('../views/inventory/Flow.vue'), meta: { title: '库存流水' } },
      // 库存盘点：已屏蔽（无菜单入口，路由同时移除；后端接口保留）
      // 预警
      { path: 'alert', component: () => import('../views/alert/AlertCenter.vue'), meta: { title: '预警中心' } },
      // 系统管理（角色管理 / 菜单权限已屏蔽：无实际权限功能）
      { path: 'system/users', component: () => import('../views/system/User.vue'), meta: { title: '用户管理' } },
      // 多租户
      { path: 'tenant', component: () => import('../views/tenant/Tenant.vue'), meta: { title: '租户管理' } }
    ]
  },
  // 未匹配路由回药品档案（工作台已屏蔽）
  { path: '/:pathMatch(.*)*', redirect: '/base/drugs' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 登录守卫：无 token 跳登录页；有 token 时后台拉取用户信息/菜单（失败静默）
router.beforeEach((to) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (to.path === '/login') {
    return token ? '/base/drugs' : true
  }
  if (!token) return { path: '/login' }
  if (!userStore.user && !userStore.loading) userStore.fetchMe()
  // 多租户模块属权限限制：非平台超管（tenantId≠0）不可访问
  if (to.path.startsWith('/tenant') && !isSuperAdmin()) return { path: '/base/drugs' }
  if (to.meta.title) document.title = `${to.meta.title} · MediStock`
  return true
})

export default router
