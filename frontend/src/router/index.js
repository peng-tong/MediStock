// 路由表：全部业务页面（静态注册，配合登录守卫）
import { createRouter, createWebHistory } from 'vue-router'
import { userStore } from '../stores/user'
import { TOKEN_KEY } from '../api/request'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/Login.vue'), meta: { title: '登录' } },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '工作台' } },
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
      { path: 'inventory/stocktakes', component: () => import('../views/inventory/Stocktake.vue'), meta: { title: '库存盘点' } },
      { path: 'inventory/transfers', component: () => import('../views/inventory/Transfer.vue'), meta: { title: '库存调拨' } },
      // 预警 / 报表
      { path: 'alert', component: () => import('../views/alert/AlertCenter.vue'), meta: { title: '预警中心' } },
      { path: 'report', component: () => import('../views/report/Report.vue'), meta: { title: '报表中心' } },
      // 系统管理
      { path: 'system/users', component: () => import('../views/system/User.vue'), meta: { title: '用户管理' } },
      { path: 'system/roles', component: () => import('../views/system/Role.vue'), meta: { title: '角色管理' } },
      { path: 'system/menus', component: () => import('../views/system/MenuManage.vue'), meta: { title: '菜单权限' } },
      { path: 'system/params', component: () => import('../views/system/Param.vue'), meta: { title: '参数配置' } },
      { path: 'system/logs', component: () => import('../views/system/Log.vue'), meta: { title: '审计日志' } },
      // 多租户
      { path: 'tenant', component: () => import('../views/tenant/Tenant.vue'), meta: { title: '租户管理' } }
    ]
  },
  // 未匹配路由回工作台
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 登录守卫：无 token 跳登录页；有 token 时后台拉取用户信息/菜单（失败静默）
router.beforeEach((to) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (to.path === '/login') {
    return token ? '/dashboard' : true
  }
  if (!token) return { path: '/login' }
  if (!userStore.user && !userStore.loading) userStore.fetchMe()
  if (to.meta.title) document.title = `${to.meta.title} · MediStock`
  return true
})

export default router
