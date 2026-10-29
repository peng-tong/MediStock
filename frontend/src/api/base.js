// 基础资料模块接口（药品 / 分类 / 供应商 / 客户 / 仓库）
import request from './request'

// ---------- 药品分类 ----------
export const getCategories = (params) => request.get('/drug-categories', { params })
export const getCategoryTree = () => request.get('/drug-categories/tree')
export const createCategory = (data) => request.post('/drug-categories', data)
export const updateCategory = (id, data) => request.put(`/drug-categories/${id}`, data)
export const deleteCategory = (id) => request.delete(`/drug-categories/${id}`)

// ---------- 药品 ----------
export const getDrugs = (params) => request.get('/drugs', { params })
export const createDrug = (data) => request.post('/drugs', data)
export const updateDrug = (id, data) => request.put(`/drugs/${id}`, data)
export const deleteDrug = (id) => request.delete(`/drugs/${id}`)

// ---------- 供应商 ----------
export const getSuppliers = (params) => request.get('/suppliers', { params })
export const createSupplier = (data) => request.post('/suppliers', data)
export const updateSupplier = (id, data) => request.put(`/suppliers/${id}`, data)
export const deleteSupplier = (id) => request.delete(`/suppliers/${id}`)

// ---------- 客户 ----------
export const getCustomers = (params) => request.get('/customers', { params })
export const createCustomer = (data) => request.post('/customers', data)
export const updateCustomer = (id, data) => request.put(`/customers/${id}`, data)
export const deleteCustomer = (id) => request.delete(`/customers/${id}`)

// ---------- 仓库 ----------
export const getWarehouses = (params) => request.get('/warehouses', { params })
export const createWarehouse = (data) => request.post('/warehouses', data)
export const updateWarehouse = (id, data) => request.put(`/warehouses/${id}`, data)
export const deleteWarehouse = (id) => request.delete(`/warehouses/${id}`)
