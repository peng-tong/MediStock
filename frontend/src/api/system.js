// 系统模块接口（用户 / 角色 / 菜单 / 租户）
import request from './request'

// ---------- 用户管理 ----------
export const getUsers = (params) => request.get('/users', { params })
export const createUser = (data) => request.post('/users', data)
export const updateUser = (id, data) => request.put(`/users/${id}`, data)
export const deleteUser = (id) => request.delete(`/users/${id}`)
export const updateUserStatus = (id, status) => request.put(`/users/${id}/status`, { status })
export const resetUserPassword = (id) => request.put(`/users/${id}/password/reset`)
export const assignUserRoles = (id, roleIds) => request.put(`/users/${id}/roles`, { roleIds })

// ---------- 角色管理 ----------
export const getRoles = (params) => request.get('/roles', { params })
export const createRole = (data) => request.post('/roles', data)
export const updateRole = (id, data) => request.put(`/roles/${id}`, data)
export const deleteRole = (id) => request.delete(`/roles/${id}`)
export const assignRoleMenus = (id, menuIds) => request.put(`/roles/${id}/menus`, { menuIds })

// ---------- 菜单管理 ----------
export const getMenus = () => request.get('/menus')
export const createMenu = (data) => request.post('/menus', data)
export const updateMenu = (id, data) => request.put(`/menus/${id}`, data)
export const deleteMenu = (id) => request.delete(`/menus/${id}`)

// ---------- 租户管理（仅超管） ----------
export const getTenants = (params) => request.get('/tenants', { params })
export const createTenant = (data) => request.post('/tenants', data)
export const updateTenant = (id, data) => request.put(`/tenants/${id}`, data)
export const updateTenantStatus = (id, status) => request.put(`/tenants/${id}/status`, { status })
export const deleteTenant = (id) => request.delete(`/tenants/${id}`)
