// 库存模块接口（实时库存 / 批次 / 流水 / 盘点）
import request from './request'

// ---------- 库存查询 ----------
export const getStocks = (params) => request.get('/inventory/stocks', { params })
// 批次库存查询（按 药品 + 批次 + 有效期排序）
export const getStockBatches = (params) => request.get('/inventory/stocks/batches', { params })
// 库存流水查询（页面用 bizType，后端参数名为 businessType，这里统一映射）
export const getFlows = (params) => {
  const { bizType, ...rest } = params || {}
  return request.get('/inventory/flows', {
    params: { ...rest, businessType: bizType === '' ? undefined : bizType }
  })
}

// ---------- 库存盘点 ----------
export const getStocktakes = (params) => request.get('/inventory/stocktakes', { params })
export const getStocktake = (id) => request.get(`/inventory/stocktakes/${id}`)
// 后端创建盘点单用 query 参数（warehouseId / remark），页面传的是对象，这里适配
export const createStocktake = (data) => request.post('/inventory/stocktakes', null, {
  params: { warehouseId: data.warehouseId, remark: data.remark }
})
// 后端为逐条录入（PUT /{id}/items/{itemId}?actualQuantity=...），页面按批提交，这里逐条串行调用
export const updateStocktakeItems = async (id, data) => {
  for (const it of data.items || []) {
    await request.put(`/inventory/stocktakes/${id}/items/${it.id}`, null, {
      params: { actualQuantity: it.actualQuantity }
    })
  }
  return { code: 0 }
}
// 确认盘点：生成盘点差异流水并同步库存
export const confirmStocktake = (id) => request.post(`/inventory/stocktakes/${id}/confirm`)
export const deleteStocktake = (id) => request.delete(`/inventory/stocktakes/${id}`)
