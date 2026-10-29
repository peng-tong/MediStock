# MediStock 前端（Vue3 + Vite）实施计划

> **For agentic workers:** 按用户要求：**不做功能测试、不测后端接口**（信任其可用）；只完成界面 + 路由跳转，最后 `npm run build` 做编译级校验与简单代码审查。
> **Goal:** 基于已选定「风格一 · 洁净医药风」实现药品进销存系统前端（Vue3 + Vite + Vue Router + Axios）。
> **Architecture:** 前端独立于 backend 之外的 `frontend/` 目录；设计令牌（颜色/圆角/阴影）集中在 `global.css`；列表页统一用配置驱动的 `DataTable` 组件，表单用 `Modal` 组件，最大限度复用、减少代码量。
> **Tech Stack:** Vue3.5 + Vite6 + vue-router4 + axios1.7（不引入 UI 组件库，自绘风格一致）。
> 参考：《功能需求清单.md》《后端设计文档.md》（API 路径与字段以其为准）。

## Global Constraints
- 技术栈 Vue3 + Vite；不使用第三方 UI 库，全部组件自绘，视觉严格对齐「风格一」原型（青绿 `#0F766E`、圆角 16、卡片阴影）。
- 统一响应体 `{code,message,data}`，`code===0` 为成功；`PageResult{list,total}`。
- 鉴权 `Authorization: Bearer <token>`，token 存 localStorage。
- 代码带中文注释（用户规则）。
- **不写任何单元/集成测试**；不启动后端联调。

## 文件结构
```
frontend/
├─ index.html
├─ package.json / vite.config.js（dev 代理 /api → http://localhost:8080）
└─ src/
   ├─ main.js / App.vue
   ├─ assets/styles/global.css        # 设计令牌 + 通用样式（按钮/表格/表单/标签/卡片）
   ├─ api/
   │  ├─ request.js                   # axios 实例 + token 拦截器 + code 解包 + 401 登出
   │  ├─ auth.js / base.js / purchase.js / sales.js / inventory.js / system.js
   ├─ router/index.js                 # 全量路由 + 登录守卫
   ├─ stores/user.js                  # 简单响应式 store（token/user/roles/menus）
   ├─ menu.js                         # 静态菜单配置（含权限标识），登录后优先用后端菜单，失败回退静态
   ├─ components/
   │  ├─ PageCard.vue / DataTable.vue / Modal.vue / StatusTag.vue
   ├─ layouts/MainLayout.vue          # 侧栏(动态菜单)+顶栏+内容区
   └─ views/
      ├─ Login.vue / Dashboard.vue
      ├─ base/    Drug / DrugCategory / Supplier / Customer / Warehouse
      ├─ purchase/PurchaseOrder / PurchaseInbound / PurchaseReturn
      ├─ sales/   SalesOrder / SalesOutbound / SalesReturn
      ├─ inventory/StockQuery / Batch / Flow / Stocktake / Transfer(占位)
      ├─ alert/   AlertCenter
      ├─ report/  Report
      ├─ system/  User / Role / Menu / Param / Log
      └─ tenant/  Tenant
```

## Tasks
- [ ] **T1 脚手架**：手写 package.json + vite.config.js + index.html，`npm install` 成功，`npm run dev` 可启动。
- [ ] **T2 设计系统 + 底座**：global.css（风格一令牌）；api/request.js 与各域 api；stores/user.js；menu.js；router（全路由 + 守卫）。
- [ ] **T3 布局 + 登录 + 工作台**：MainLayout（侧栏动态菜单、顶栏用户信息）；Login（账号/密码/验证码，走 /api/auth/login + /me）；Dashboard（统计卡 + SVG 图表 + 预警/流水列表）。
- [ ] **T4 通用组件**：PageCard / DataTable（列配置 + 分页 + 搜索）+ Modal + StatusTag，供各列表页复用。
- [ ] **T5 base + system + tenant 页面**（子代理 A）：药品/分类/供应商/客户/仓库/用户/角色/菜单/租户，列表 + 弹窗表单。
- [ ] **T6 purchase + sales 页面**（子代理 B）：采购订单/入库/退货；销售开单(多行明细+批号选择)/出库/退货。
- [ ] **T7 inventory + alert + report 页面**（子代理 C）：库存查询/批次/流水/盘点(实盘差异联动)/调拨占位；预警中心；报表。
- [ ] **T8 收尾**：`npm run build` 编译校验全部页面；浏览器点一遍路由跳转（仅导航 smoke test）；提交说明。

## 收尾验证
- `npm run build` 必须 0 报错。
- dev server 下逐路由切换无白屏（仅验证路由/组件挂载，不验证业务数据）。
- 后端接口不做任何联调测试。
