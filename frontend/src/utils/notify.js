// 轻量消息提示：基于 Element Plus ElMessage，页面操作后的成功/失败反馈
import { ElMessage } from 'element-plus'

export function notify(message, type = 'success') {
  ElMessage({ message, type, duration: 2200, grouping: true, showClose: false })
}
export const notifySuccess = (m) => notify(m, 'success')
export const notifyError = (m) => notify(m, 'error')
