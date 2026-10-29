<template>
  <!-- 通用弹窗：标题 + 内容插槽 + 底部操作插槽 -->
  <teleport to="body">
    <div v-if="visible" class="modal-mask" @click.self="close">
      <div class="modal" :style="{ maxWidth: width }">
        <div class="modal-head">
          <b>{{ title }}</b>
          <button class="modal-close" @click="close">✕</button>
        </div>
        <div class="modal-body">
          <slot />
        </div>
        <div class="modal-foot">
          <slot name="footer" />
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup>
// 受控弹窗：visible 由父组件控制
defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: '' },
  width: { type: String, default: '640px' }
})
const emit = defineEmits(['close'])
function close() { emit('close') }
</script>
