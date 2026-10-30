// 静态菜单配置：与《功能需求清单》模块一致
// 登录成功后可被后端 /api/auth/me 返回的菜单树覆盖；后端异常时用本配置兜底
// icon 为内联 SVG path，渲染时包在 <svg viewBox="0 0 24 24" stroke="currentColor"> 中
// 说明：工作台、角色管理、菜单权限、库存调拨、报表中心、参数配置、审计日志 因无对应后端功能已屏蔽，
// 详见 docs/屏蔽功能清单.md
export const staticMenus = [
  { name: '基础资料', icon: '<rect x="3" y="4" width="18" height="16" rx="2"/><path d="M3 9h18"/>', children: [
    { name: '药品档案', path: '/base/drugs',
      icon: '<path d="M9 3h6a4 4 0 0 1 4 4v10a4 4 0 0 1-4 4H9a4 4 0 0 1-4-4V7a4 4 0 0 1 4-4Z"/><path d="M9 3v18M13 3v18"/><path d="M5 12h14"/>' },
    { name: '药品分类', path: '/base/categories',
      icon: '<rect x="3" y="4" width="18" height="16" rx="2"/><path d="M3 9h18M7 14h2m2 0h2"/>' },
    { name: '供应商档案', path: '/base/suppliers',
      icon: '<path d="M3 21h18"/><path d="M5 21V8l7-5 7 5v13"/>' },
    { name: '客户档案', path: '/base/customers',
      icon: '<circle cx="9" cy="8" r="3.5"/><path d="M3 20c1-3 3.5-4.5 6-4.5s5 1.5 6 4.5"/><path d="M17 9l2 2 4-4"/>' },
    { name: '仓库档案', path: '/base/warehouses',
      icon: '<path d="M3 9l9-5 9 5v11H3z"/><path d="M7 13h3m4 0h3M7 17h3m4 0h3"/>' }
  ] },

  { name: '采购管理', icon: '<path d="M3 4h18l-6 8v6l-4 2v-8z"/>', children: [
    { name: '采购订单', path: '/purchase/orders', icon: '<path d="M3 4h18l-6 8v6l-4 2v-8z"/>' },
    { name: '采购入库', path: '/purchase/inbound', icon: '<path d="M4 9l8-5 8 5v11H4z"/><path d="M9 13h6"/>' },
    { name: '采购退货', path: '/purchase/returns', icon: '<path d="M3 4h18l-6 8v6l-4 2v-8z"/><path d="M13 20h-2M15 12l-6 6"/>' }
  ] },

  { name: '销售管理', icon: '<path d="M12 3v18M12 3l4 4M12 3L8 7"/>', children: [
    { name: '销售开单', path: '/sales/orders', icon: '<path d="M12 3v18M12 3l4 4M12 3L8 7"/><path d="M7 9h10l2 3H5z"/><path d="M5 12h14l1 3H4z"/>' },
    { name: '销售出库', path: '/sales/outbound', icon: '<path d="M4 9l8-5 8 5v11H4z"/><path d="M12 13v4"/>' },
    { name: '销售退货', path: '/sales/returns', icon: '<path d="M4 9l8-5 8 5v11H4z"/><path d="M9 14h6"/>' }
  ] },

  { name: '库存管理', icon: '<path d="M4 9l8-5 8 5v11H4z"/><path d="M12 9v11"/>', children: [
    { name: '库存查询', path: '/inventory/stocks', icon: '<path d="M4 9l8-5 8 5v11H4z"/><path d="M4 9h16"/><path d="M12 9v11"/>' },
    { name: '批次管理', path: '/inventory/batches', icon: '<path d="M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h6v6h-6z"/>' },
    { name: '库存流水', path: '/inventory/flows', icon: '<path d="M3 12a9 9 0 1 1 18 0v3a2 2 0 0 1-2 2h-1V12a6 6 0 0 0-12 0v5H5a2 2 0 0 1-2-2z"/>' }
    // 库存盘点：按需求屏蔽（保留后端接口与页面文件，仅移除入口）
  ] },

  { name: '预警管理', icon: '<path d="M12 3L2.5 19h19z"/><path d="M12 9.5v4.5M12 17.5v.1"/>', children: [
    { name: '预警中心', path: '/alert', icon: '<path d="M12 3L2.5 19h19z"/><path d="M12 9.5v4.5M12 17.5v.1"/>' }
  ] },

  { name: '系统管理', icon: '<circle cx="12" cy="12" r="3"/><path d="M19 12a7 7 0 0 0-14 0"/>', children: [
    { name: '用户管理', path: '/system/users', icon: '<circle cx="9" cy="8" r="3.5"/><path d="M3 20c1-3 3.5-4.5 6-4.5s5 1.5 6 4.5"/><path d="M16 11h4M18 9v4"/>' }
  ] },

  // superAdminOnly：权限限制，仅平台超管（tenantId=0）可见，非超管登录时不渲染该模块
  { name: '多租户', superAdminOnly: true, icon: '<rect x="3" y="4" width="8" height="8" rx="1.5"/><rect x="13" y="4" width="8" height="8" rx="1.5"/><rect x="3" y="14" width="8" height="8" rx="1.5"/><rect x="13" y="14" width="8" height="8" rx="1.5"/>', children: [
    { name: '租户管理', path: '/tenant', icon: '<rect x="3" y="4" width="8" height="8" rx="1.5"/><rect x="13" y="4" width="8" height="8" rx="1.5"/><rect x="3" y="14" width="8" height="8" rx="1.5"/><rect x="13" y="14" width="8" height="8" rx="1.5"/>' }
  ] }
]
