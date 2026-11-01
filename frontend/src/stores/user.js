// 用户状态：token / 用户信息 / 角色 / 菜单树（localStorage 持久化）
import { reactive } from 'vue'
import { login as apiLogin, getMe, logout as apiLogout } from '../api/auth'

/**
 * 解析 JWT payload（仅用于前端展示与菜单过滤，不做签名校验）
 * 后端 token 内置 userId / tenantId / username；tenantId=0 即平台超管
 */
function decodeJwtPayload(token) {
  try {
    const part = token.split('.')[1]
    if (!part) return {}
    // JWT 使用 base64url，需转成标准 base64 再补齐 padding
    const b64 = part.replace(/-/g, '+').replace(/_/g, '/')
    const padded = b64 + '='.repeat((4 - (b64.length % 4)) % 4)
    // 用 TextDecoder 解码，避免用户名中文乱码
    const json = new TextDecoder().decode(Uint8Array.from(atob(padded), (c) => c.charCodeAt(0)))
    return JSON.parse(json)
  } catch (e) {
    return {}
  }
}

// 初始登录态来自 localStorage，同时解析出所属租户
const initialToken = localStorage.getItem('ms_token') || ''
const initialPayload = decodeJwtPayload(initialToken)

export const userStore = reactive({
  token: initialToken,
  tenantId: initialPayload.tenantId ?? null, // 0=平台超管；其它为普通租户
  user: JSON.parse(localStorage.getItem('ms_user') || 'null'),
  roles: [],
  menus: null,   // 后端返回的菜单树（为空时界面回退静态菜单）
  loading: false,

  // 登录：成功后拉取用户信息与菜单
  async login(form) {
    const data = await apiLogin(form)
    this.token = data.token
    this.tenantId = decodeJwtPayload(data.token).tenantId ?? null
    localStorage.setItem('ms_token', data.token)
    await this.fetchMe()
  },

  // 拉取当前用户 + 角色 + 菜单（后端异常时静默，保证界面可用）
  async fetchMe() {
    if (this.loading) return
    this.loading = true
    try {
      const me = await getMe()
      this.user = me.userInfo || me.user || me
      this.roles = me.roles || []
      this.menus = me.menus || null
      localStorage.setItem('ms_user', JSON.stringify(this.user))
    } catch (e) {
      /* 后端未就绪时忽略，静态菜单兜底 */
    } finally {
      this.loading = false
    }
  },

  // 仅清理本地登录态（用于改密成功后 token 已失效的场景，无需再调登出接口）
  clearLocal() {
    this.token = ''
    this.tenantId = null
    this.user = null
    this.roles = []
    this.menus = null
    localStorage.removeItem('ms_token')
    localStorage.removeItem('ms_user')
  },

  // 登出：清理本地登录态
  async logout() {
    try { await apiLogout() } catch (e) { /* 忽略登出接口异常 */ }
    this.clearLocal()
  }
})

/** 是否平台超管（tenantId=0）：仅超管可访问多租户模块、可指定用户所属租户 */
export function isSuperAdmin() {
  return Number(userStore.tenantId) === 0
}
