// 库存模块接口（实时库存 / 批次 / 流水 / 盘点）
import request from './request'

// ---------- 库存查询 ----------
export const getStocks = (params) => request.get('/inventory/stocks', { params })
// 批次库存查询（按 药品 + 批次 + 有效期排序）
export const getStockBatches = (params) => request.get('/inventory/stocks/batches', { params })
// 库存流水查询
export const getFlows = (params) => request.get('/inventory/flows', { params })

// ---------- 库存盘点 ----------
export const getStocktakes = (params) => request.get('/inventory/stocktakes', { params })
export const getStocktake = (id) => request.get(`/inventory/stocktakes/${id}`)
export const createStocktake = (data) => request.post('/inventory/stocktakes', data)
// 录入实盘数量：{ items: [{id, actualQuantity}] }
export const updateStocktakeItems = (id, data) => request.put(`/inventory/stocktakes/${id}/items`, data)
// 确认盘点：生成盘点差异流水并同步库存
export const confirmStocktake = (id) => request.post(`/inventory/stocktakes/${id}/confirm`)
export const deleteStocktake = (id) => request.delete(`/inventory/stocktakes/${id}`)
