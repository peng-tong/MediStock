<template>
  <!-- 通用弹窗：基于 Element Plus Dialog，保留原有 visible / title / footer 插槽用法 -->
  <el-dialog
    :model-value="visible"
    :title="title"
    :width="width"
    class="ms-dialog"
    append-to-body
    align-center
    destroy-on-close
    @update:model-value="onUpdate"
  >
    <slot />
    <template #footer>
      <slot name="footer" />
    </template>
  </el-dialog>
</template>

<script setup>
// 受控弹窗：visible 由父组件控制；关闭（ESC / 遮罩 / 右上角 ×）时通知父组件
defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: '' },
  width: { type: String, default: '640px' }
})
const emit = defineEmits(['close'])
function onUpdate(v) {
  if (!v) emit('close')
}
</script>
