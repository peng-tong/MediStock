// 销售模块接口（销售订单 / 出库 / 退货）
import request from './request'

// ---------- 销售订单 ----------
export const getSalesOrders = (params) => request.get('/sales/orders', { params })
export const getSalesOrder = (id) => request.get(`/sales/orders/${id}`)
// 后端契约为 { order, items }，页面里是扁平结构，统一在此处包装
export const createSalesOrder = (data) => request.post('/sales/orders', {
  order: {
    customerId: data.customerId, warehouseId: data.warehouseId, remark: data.remark,
    discountAmount: data.discountAmount, paidAmount: data.paidAmount
  },
  items: data.items
})
export const updateSalesOrder = (id, data) => request.put(`/sales/orders/${id}`, {
  order: {
    customerId: data.customerId, warehouseId: data.warehouseId, remark: data.remark,
    discountAmount: data.discountAmount, paidAmount: data.paidAmount
  },
  items: data.items
})
export const deleteSalesOrder = (id) => request.delete(`/sales/orders/${id}`)
// 提交销售单：草稿 → 待出库（之后才可出库）
export const submitSalesOrder = (id) => request.post(`/sales/orders/${id}/submit`)
// 销售出库（FIFO 按批次扣减）
export const outboundSalesOrder = (id, data) => request.post(`/sales/orders/${id}/outbound`, data)

// ---------- 销售退货 ----------
export const getSalesReturns = (params) => request.get('/sales/returns', { params })
export const getSalesReturn = (id) => request.get(`/sales/returns/${id}`)
export const createSalesReturn = (data) => request.post('/sales/returns', {
  salesReturn: {
    orderId: data.orderId, customerId: data.customerId,
    warehouseId: data.warehouseId, remark: data.remark
  },
  items: data.items
})

// ---------- 基础下拉数据 ----------
export const getCustomerOptions = () => request.get('/customers', { params: { page: 1, pageSize: 100 } })
