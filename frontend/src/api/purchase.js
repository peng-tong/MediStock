// 采购模块接口（采购订单 / 入库 / 退货）
import request from './request'

// ---------- 采购订单 ----------
export const getPurchaseOrders = (params) => request.get('/purchases/orders', { params })
export const getPurchaseOrder = (id) => request.get(`/purchases/orders/${id}`)
// 后端契约为 { order, items }，页面里是扁平结构，统一在此处包装
export const createPurchaseOrder = (data) => request.post('/purchases/orders', {
  order: { supplierId: data.supplierId, warehouseId: data.warehouseId, remark: data.remark },
  items: data.items
})
export const updatePurchaseOrder = (id, data) => request.put(`/purchases/orders/${id}`, {
  order: { supplierId: data.supplierId, warehouseId: data.warehouseId, remark: data.remark },
  items: data.items
})
export const deletePurchaseOrder = (id) => request.delete(`/purchases/orders/${id}`)
// 提交采购单：草稿 → 待审核（之后才可入库）
export const submitPurchaseOrder = (id) => request.post(`/purchases/orders/${id}/submit`)
// 审核通过：待审核 → 待入库；驳回：待审核 → 已驳回
export const approvePurchaseOrder = (id) => request.post(`/purchases/orders/${id}/approve`)
export const rejectPurchaseOrder = (id) => request.post(`/purchases/orders/${id}/reject`)
// 采购入库：{ items: [{itemId, quantity, batchNo, expiryDate}] }
export const inboundPurchaseOrder = (id, data) => request.post(`/purchases/orders/${id}/inbound`, data)

// ---------- 采购退货 ----------
export const getPurchaseReturns = (params) => request.get('/purchases/returns', { params })
export const getPurchaseReturn = (id) => request.get(`/purchases/returns/${id}`)
export const createPurchaseReturn = (data) => request.post('/purchases/returns', {
  purchaseReturn: { supplierId: data.supplierId, warehouseId: data.warehouseId, remark: data.remark },
  items: data.items
})

// ---------- 基础下拉数据（供单据选择） ----------
export const getSupplierOptions = () => request.get('/suppliers', { params: { page: 1, pageSize: 100 } })
export const getDrugOptions = () => request.get('/drugs', { params: { page: 1, pageSize: 100 } })
export const getWarehouseOptions = () => request.get('/warehouses', { params: { page: 1, pageSize: 100 } })
