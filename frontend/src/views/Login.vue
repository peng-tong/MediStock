<template>
  <!-- 登录页：左侧品牌区 + 右侧登录表单（图形验证码） -->
  <div id="login">
    <div class="login-brand">
      <span class="deco d1"></span><span class="deco d2"></span><span class="deco d3"></span>
      <div class="brand-logo"><span class="mark">✚</span> MediStock</div>
      <h1>药品进销存<br>智能管理系统</h1>
      <p class="slogan">覆盖 基础资料 · 采购 · 销售 · 库存 · 批次效期 · 预警 · 报表<br>全流程数字化的药品经营一体化平台</p>
      <div class="stat-strip">
        <div><b>98%</b><span>库存数据准确率</span></div>
        <div><b>7×24h</b><span>批次效期实时监控</span></div>
        <div><b>GSP</b><span>经营合规管理</span></div>
      </div>
    </div>
    <div class="login-panel">
      <div class="login-card">
        <h2>欢迎登录</h2>
        <p class="sub">请使用管理员分配的账号进入系统</p>
        <div class="field">
          <label>账号</label>
          <div class="input">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="8" r="4"/><path d="M4 21c1.5-3.5 4.5-5 8-5s6.5 1.5 8 5"/></svg>
            <!-- autocomplete 交给浏览器密码管理器，避免预填固定账号干扰自动填充 -->
            <input type="text" v-model.trim="form.username" placeholder="请输入账号" autocomplete="username" @keydown.enter="submit">
          </div>
        </div>
        <div class="field">
          <label>密码</label>
          <div class="input">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg>
            <input :type="pwdVisible ? 'text' : 'password'" v-model="form.password" placeholder="请输入密码"
                   autocomplete="current-password" @keydown.enter="submit">
            <!-- 密码可见切换：点击眼睛图标在明文/密文之间切换 -->
            <button type="button" class="pwd-toggle" :title="pwdVisible ? '隐藏密码' : '显示密码'" @click="pwdVisible = !pwdVisible">
              <svg v-if="!pwdVisible" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7S1 12 1 12z"/><circle cx="12" cy="12" r="3"/></svg>
              <svg v-else width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 3l18 18"/><path d="M10.5 5.2A11.8 11.8 0 0 1 12 5c7 0 11 7 11 7a17 17 0 0 1-2.2 2.8"/><path d="M6.7 6.8A17.4 17.4 0 0 0 1 12s4 7 11 7a11 11 0 0 0 4.2-.8"/></svg>
            </button>
          </div>
        </div>
        <!-- 验证码区域：后端 captcha.enabled=false 时整块隐藏（测试问题 82） -->
        <div v-if="captchaEnabled" class="field">
          <label>验证码</label>
          <div class="captcha-row">
            <div class="input" style="flex:1">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 12h6M9 16h6M7 8h10"/></svg>
              <input type="text" v-model.trim="form.captchaCode" placeholder="输入右侧字符" @keydown.enter="submit">
            </div>
            <img v-if="captchaImg" class="captcha-img" :src="captchaImg" title="点击刷新" @click="refreshCaptcha" alt="验证码">
            <div v-else class="captcha-box" @click="refreshCaptcha">加载中</div>
          </div>
        </div>
        <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
        <button class="btn btn-primary login-btn" :disabled="loading" @click="submit">{{ loading ? '登录中...' : '登 录' }}</button>
        <p class="tips">面试演示 Demo · 演示账号 admin / 123456（可自行切换角色）</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { userStore } from '../stores/user'
import { getCaptcha } from '../api/auth'

const router = useRouter()
// 账号不再硬编码预填，避免与浏览器自动填充/密码管理器冲突
const form = reactive({ username: '', password: '', captchaKey: '', captchaCode: '' })
const captchaImg = ref('')
const captchaEnabled = ref(false)   // 验证码开关（以接口返回为准）
const pwdVisible = ref(false)       // 密码可见切换
const loading = ref(false)
const errorMsg = ref('')

// 拉取图形验证码（key + base64 图片 + enabled 开关）
async function refreshCaptcha() {
  try {
    const data = await getCaptcha()
    form.captchaKey = data.key
    captchaImg.value = data.image
    captchaEnabled.value = data.enabled !== false
  } catch (e) {
    captchaImg.value = ''
  }
}

// 登录：成功跳转药品档案（工作台页面已屏蔽）
async function submit() {
  if (!form.username || !form.password) { errorMsg.value = '请输入账号与密码'; return }
  loading.value = true
  errorMsg.value = ''
  try {
    await userStore.login({ ...form })
    router.push('/base/drugs')
  } catch (e) {
    errorMsg.value = e.message || '登录失败，请稍后重试'
    // 仅开启验证码时刷新（关闭时无需请求验证码接口）
    if (captchaEnabled.value) refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(refreshCaptcha)
</script>

<style scoped>
/* ===== 登录页（风格一 · 洁净医药） ===== */
#login{position:fixed;inset:0;z-index:100;display:grid;grid-template-columns:1.1fr 1fr}
.login-brand{position:relative;overflow:hidden;display:flex;flex-direction:column;justify-content:center;padding:64px;
  background:linear-gradient(150deg,#0E4F4A 0%,#0F766E 55%,#14B8A6 100%);color:#fff}
.login-brand .deco{position:absolute;border-radius:50%;background:rgba(255,255,255,.08)}
.login-brand .d1{width:340px;height:340px;top:-120px;right:-80px}
.login-brand .d2{width:180px;height:180px;bottom:80px;left:-60px;background:rgba(255,255,255,.06)}
.login-brand .d3{width:90px;height:90px;bottom:260px;right:120px;background:rgba(255,255,255,.12)}
.brand-logo{display:flex;align-items:center;gap:12px;font-size:24px;font-weight:700;letter-spacing:1px}
.brand-logo .mark{width:46px;height:46px;border-radius:14px;background:#fff;color:var(--primary);display:grid;place-items:center;font-size:26px;box-shadow:0 4px 12px rgba(0,0,0,.18)}
.login-brand h1{margin-top:36px;font-size:38px;line-height:1.3;font-weight:700;letter-spacing:2px}
.login-brand .slogan{margin-top:16px;font-size:16px;opacity:.85;letter-spacing:1px;line-height:1.8}
.login-brand .stat-strip{margin-top:56px;display:flex;gap:40px}
.login-brand .stat-strip b{display:block;font-size:26px;font-weight:700}
.login-brand .stat-strip span{font-size:13px;opacity:.8}
.login-panel{background:#fff;display:flex;align-items:center;justify-content:center;padding:40px}
.login-card{width:100%;max-width:400px}
.login-card h2{font-size:26px;color:var(--ink);letter-spacing:1px}
.login-card .sub{margin:8px 0 32px;color:var(--muted);font-size:13px}
.field{margin-bottom:18px}
.field label{display:block;font-size:13px;color:var(--ink);margin-bottom:8px;font-weight:600}
.input{display:flex;align-items:center;gap:10px;border:1.5px solid var(--line);border-radius:12px;padding:0 14px;height:46px;background:#FBFDFD;transition:.2s}
.input:focus-within{border-color:var(--primary);background:#fff;box-shadow:0 0 0 3px rgba(15,118,110,.12)}
.input svg{flex:none;color:var(--muted)}
.input input{border:none;outline:none;flex:1;background:transparent;color:var(--ink);font-size:14px}
/* 密码可见切换按钮：无边框，仅图标 */
.pwd-toggle{border:none;background:transparent;color:var(--muted);cursor:pointer;display:grid;place-items:center;padding:0;flex:none}
.pwd-toggle:hover{color:var(--primary)}
.captcha-row{display:flex;gap:12px}
.captcha-img{width:120px;height:46px;border-radius:12px;border:1.5px dashed var(--line);cursor:pointer;object-fit:cover}
.captcha-box{width:120px;height:46px;border-radius:12px;border:1.5px dashed var(--line);display:grid;place-items:center;
  font-size:13px;color:var(--muted);cursor:pointer;background:var(--surface-2)}
.error{color:var(--danger);font-size:12.5px;margin:-6px 0 12px}
.login-btn{width:100%;height:46px;margin-top:8px;font-size:15px;letter-spacing:2px}
.tips{margin-top:22px;font-size:12px;color:var(--muted);text-align:center;line-height:1.9}
/* 窄屏/移动端：单列布局，隐藏装饰性品牌区，保证表单可用 */
@media (max-width: 860px) {
  #login{grid-template-columns:1fr}
  .login-brand{display:none}
  .login-panel{padding:24px 18px}
  .login-card{max-width:420px}
}
</style>
