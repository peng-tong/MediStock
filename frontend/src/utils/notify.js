// 轻量消息提示：基于 Element Plus ElMessage，页面操作后的成功/失败反馈
import { ElMessage, ElMessageBox } from 'element-plus'

export function notify(message, type = 'success') {
  ElMessage({ message, type, duration: 2200, grouping: true, showClose: false })
}
export const notifySuccess = (m) => notify(m, 'success')
export const notifyError = (m) => notify(m, 'error')

// 删除二次确认：任何删除操作先弹确认框，确认后才执行；返回 Promise<boolean>
export function confirmDelete(content = '删除后不可恢复，确定要删除吗？') {
  return ElMessageBox.confirm(content, '删除确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning',
    distinguishCancelAndClose: true
  }).then(() => true).catch(() => false)
}
