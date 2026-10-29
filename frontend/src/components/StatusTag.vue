<template>
  <!-- 状态标签：type 对应全局样式中的 tag 颜色 -->
  <span class="tag" :class="type">{{ text }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  text: { type: String, default: '' },
  // 可选值：ok / warn / danger / info / gray
  type: { type: String, default: 'gray' }
})
</script>
