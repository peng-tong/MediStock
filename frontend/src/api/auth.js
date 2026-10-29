// 认证相关接口（对应后端 system/auth 模块）
import request from './request'

// 获取图形验证码：返回 { key, image(base64) }
export const getCaptcha = () => request.get('/auth/captcha')

// 登录：{username, password, captchaKey, captchaCode} → { token, userInfo }
export const login = (data) => request.post('/auth/login', data)

// 登出
export const logout = () => request.post('/auth/logout')

// 当前用户信息 + 角色 + 菜单树
export const getMe = () => request.get('/auth/me')

// 修改密码 { oldPassword, newPassword }
export const changePassword = (data) => request.put('/auth/password', data)
