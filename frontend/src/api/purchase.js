// 采购模块接口（采购订单 / 入库 / 退货）
import request from './request'

// ---------- 采购订单 ----------
export const getPurchaseOrders = (params) => request.get('/purchases/orders', { params })
export const getPurchaseOrder = (id) => request.get(`/purchases/orders/${id}`)
export const createPurchaseOrder = (data) => request.post('/purchases/orders', data)
export const updatePurchaseOrder = (id, data) => request.put(`/purchases/orders/${id}`, data)
export const deletePurchaseOrder = (id) => request.delete(`/purchases/orders/${id}`)
// 采购入库：{ items: [{itemId, quantity, batchNo, expiryDate}] }
export const inboundPurchaseOrder = (id, data) => request.post(`/purchases/orders/${id}/inbound`, data)

// ---------- 采购退货 ----------
export const getPurchaseReturns = (params) => request.get('/purchases/returns', { params })
export const getPurchaseReturn = (id) => request.get(`/purchases/returns/${id}`)
export const createPurchaseReturn = (data) => request.post('/purchases/returns', data)

// ---------- 基础下拉数据（供单据选择） ----------
export const getSupplierOptions = () => request.get('/suppliers', { params: { page: 1, pageSize: 100 } })
export const getDrugOptions = () => request.get('/drugs', { params: { page: 1, pageSize: 100 } })
export const getWarehouseOptions = () => request.get('/warehouses', { params: { page: 1, pageSize: 100 } })
