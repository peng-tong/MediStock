// 轻量消息提示：页面操作后的成功/失败反馈
let timer = null
export function notify(message, type = 'success') {
  // 复用已有提示则先移除
  const old = document.querySelector('.ms-toast')
  if (old) old.remove()
  const el = document.createElement('div')
  el.className = 'ms-toast ' + type
  el.textContent = message
  document.body.appendChild(el)
  clearTimeout(timer)
  timer = setTimeout(() => el.remove(), 2200)
}
export const notifySuccess = (m) => notify(m, 'success')
export const notifyError = (m) => notify(m, 'error')

// 注入 toast 样式（全局唯一）
const style = document.createElement('style')
style.textContent = `
.ms-toast{position:fixed;top:24px;left:50%;transform:translateX(-50%);z-index:2000;
  padding:11px 22px;border-radius:12px;font-size:13.5px;color:#fff;box-shadow:0 10px 30px rgba(23,56,63,.25);
  animation:msfade .25s ease}
.ms-toast.success{background:#16A34A}
.ms-toast.error{background:#DC2626}
@keyframes msfade{from{opacity:0;transform:translate(-50%,-10px)}to{opacity:1;transform:translate(-50%,0)}}`
document.head.appendChild(style)
