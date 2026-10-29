<template>
  <!-- 配置驱动数据表格：列配置 + 分页；fetch 失败时自动回退 mock 数据（便于无后端演示） -->
  <div>
    <div class="tbl-wrap">
      <table class="tbl">
        <thead>
          <tr>
            <th v-for="c in columns" :key="c.key || c.title" :style="{ width: c.width }">{{ c.title }}</th>
            <th v-if="$slots.actions" style="width:1%">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(r, i) in rows" :key="i">
            <td v-for="c in columns" :key="c.key || c.title" :class="c.className" :style="{ textAlign: c.align }">
              <!-- 三种列渲染：自定义 render / 状态标签 tag / 默认取字段 -->
              <template v-if="c.render">{{ c.render(r) }}</template>
              <template v-else-if="c.tag">
                <span class="tag" :class="c.tag(r).type">{{ c.tag(r).text }}</span>
              </template>
              <template v-else>{{ r[c.key] }}</template>
            </td>
            <td v-if="$slots.actions">
              <div class="row-actions"><slot name="actions" :row="r" /></div>
            </td>
          </tr>
          <tr v-if="!rows.length">
            <td :colspan="columns.length + ($slots.actions ? 1 : 0)" class="empty">{{ loading ? '加载中...' : '暂无数据' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    <!-- 分页 -->
    <div class="pager">
      <span>共 {{ total }} 条 · 第 {{ page }} / {{ totalPages }} 页</span>
      <div class="flex" style="gap:6px">
        <button class="pbtn" :disabled="page <= 1" @click="go(page - 1)">‹</button>
        <button v-for="p in pageList" :key="p" class="pbtn" :class="{ cur: p === page }" @click="go(p)">{{ p }}</button>
        <button class="pbtn" :disabled="page >= totalPages" @click="go(page + 1)">›</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  // 列配置：[{ key, title, width, align, className, render(row), tag(row)->{type,text} }]
  columns: { type: Array, default: () => [] },
  // 远程拉取函数：params => Promise<{list,total}>
  fetch: { type: Function, default: null },
  // 兜底演示数据（fetch 失败时展示）
  mock: { type: Array, default: () => [] },
  // 查询条件对象（变化后自动重新加载）
  query: { type: Object, default: () => ({}) }
})

const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(false)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

// 分页按钮列表（最多展示 7 个）
const pageList = computed(() => {
  const t = totalPages.value
  const p = page.value
  let s = Math.max(1, p - 3)
  let e = Math.min(t, s + 6)
  s = Math.max(1, e - 6)
  const arr = []
  for (let i = s; i <= e; i++) arr.push(i)
  return arr
})

// 加载数据：优先调接口，失败回退 mock（mock 做本地分页）
async function load() {
  loading.value = true
  const params = { page: page.value, pageSize, ...props.query }
  try {
    if (props.fetch) {
      const data = await props.fetch(params)
      rows.value = (data.list || []).map(normalizeRow)
      total.value = Number(data.total || 0)
    } else {
      useMock()
    }
  } catch (e) {
    useMock()
  } finally {
    loading.value = false
  }
}
function useMock() {
  const start = (page.value - 1) * pageSize
  rows.value = props.mock.slice(start, start + pageSize)
  total.value = props.mock.length
}
// 兼容后端字段：row 可能嵌套 userName 等，这里不做强转换，直接透传
function normalizeRow(r) { return r }

function go(p) {
  if (p < 1 || p > totalPages.value || p === page.value) return
  page.value = p
  load()
}

// 查询条件变化时回到第一页并刷新
watch(() => props.query, () => { page.value = 1; load() }, { deep: true })

// 对外暴露刷新方法（父组件筛选后调用 tableRef.reload()）
defineExpose({ reload: load })

load()
</script>
