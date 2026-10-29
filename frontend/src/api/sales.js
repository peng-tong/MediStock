// 销售模块接口（销售订单 / 出库 / 退货）
import request from './request'

// ---------- 销售订单 ----------
export const getSalesOrders = (params) => request.get('/sales/orders', { params })
export const getSalesOrder = (id) => request.get(`/sales/orders/${id}`)
export const createSalesOrder = (data) => request.post('/sales/orders', data)
export const updateSalesOrder = (id, data) => request.put(`/sales/orders/${id}`, data)
export const deleteSalesOrder = (id) => request.delete(`/sales/orders/${id}`)
// 销售出库（FIFO 按批次扣减）
export const outboundSalesOrder = (id, data) => request.post(`/sales/orders/${id}/outbound`, data)

// ---------- 销售退货 ----------
export const getSalesReturns = (params) => request.get('/sales/returns', { params })
export const getSalesReturn = (id) => request.get(`/sales/returns/${id}`)
export const createSalesReturn = (data) => request.post('/sales/returns', data)

// ---------- 基础下拉数据 ----------
export const getCustomerOptions = () => request.get('/customers', { params: { page: 1, pageSize: 100 } })
