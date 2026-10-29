// 用户状态：token / 用户信息 / 角色 / 菜单树（localStorage 持久化）
import { reactive } from 'vue'
import { login as apiLogin, getMe, logout as apiLogout } from '../api/auth'

export const userStore = reactive({
  token: localStorage.getItem('ms_token') || '',
  user: JSON.parse(localStorage.getItem('ms_user') || 'null'),
  roles: [],
  menus: null,   // 后端返回的菜单树（为空时界面回退静态菜单）
  loading: false,

  // 登录：成功后拉取用户信息与菜单
  async login(form) {
    const data = await apiLogin(form)
    this.token = data.token
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

  // 登出：清理本地登录态
  async logout() {
    try { await apiLogout() } catch (e) { /* 忽略登出接口异常 */ }
    this.token = ''
    this.user = null
    this.roles = []
    this.menus = null
    localStorage.removeItem('ms_token')
    localStorage.removeItem('ms_user')
  }
})
