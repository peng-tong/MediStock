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
              <!-- 三种列渲染：自定义 render（支持 html 标记）/ 状态标签 tag / 默认取字段 -->
              <template v-if="c.render">
                <!-- html 列用于带颜色的数字/天数；内容均由页面自身生成，不含用户输入 -->
                <span v-if="c.html" v-html="c.render(r)"></span>
                <template v-else>{{ c.render(r) }}</template>
              </template>
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
            <td :colspan="columns.length + ($slots.actions ? 1 : 0)" class="empty">
              <!-- 接口失败：显示错误提示与重试按钮，避免静默展示假数据误导用户 -->
              <template v-if="loadError">
                <span style="color:var(--danger)">加载失败：{{ loadError }}</span>
                <button class="btn btn-sm" style="margin-left:10px" @click="reload">重试</button>
              </template>
              <template v-else>{{ loading ? '加载中...' : '暂无数据' }}</template>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <!-- 分页：改用 Element Plus 分页组件，风格与全站统一 -->
    <div class="pager">
      <span>共 {{ total }} 条</span>
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        :pager-count="7"
        hide-on-single-page
        @current-change="go"
      />
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
  // 兜底演示数据（仅当 mockOnError=true 时在接口失败后展示，默认不回退假数据）
  mock: { type: Array, default: () => [] },
  // 接口失败时是否回退 mock（默认 false：失败显示报错，避免假单据误导）
  mockOnError: { type: Boolean, default: false },
  // 查询条件对象（变化后自动重新加载）
  query: { type: Object, default: () => ({}) }
})

const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(false)
const loadError = ref('')
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

// 加载数据：优先调接口；失败默认展示错误提示，仅 mockOnError=true 时回退 mock（mock 做本地分页）
async function load() {
  loading.value = true
  const params = { page: page.value, pageSize, ...props.query }
  try {
    if (props.fetch) {
      const data = await props.fetch(params)
      rows.value = (data.list || []).map(normalizeRow)
      total.value = Number(data.total || 0)
      loadError.value = ''
    } else {
      useMock()
    }
  } catch (e) {
    // 接口失败不再静默展示假数据：记录错误并提示，避免用户把 mock 当真实单据
    loadError.value = (e && e.message) ? e.message : '接口请求失败'
    console.error('[DataTable] 列表加载失败:', e)
    if (props.mockOnError) useMock()
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

// 对外暴露方法：reload 保持当前页刷新；reset 回到第一页刷新（筛选条件变更时避免停留在越界页）
defineExpose({ reload: load, reset: () => { page.value = 1; load() } })

load()
</script>
