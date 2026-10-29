<template>
  <!-- 审计日志：P1 预留，无接口，静态演示表 + 类型筛选 -->
  <div>
    <PageCard title="审计日志" desc="系统管理 · 操作 / 登录行为留痕（P1 预留）" />

    <!-- 日志类型筛选 chips -->
    <div class="filters">
      <button class="chip" :class="{ active: filter === '' }" @click="filter = ''">全部</button>
      <button class="chip" :class="{ active: filter === '操作' }" @click="filter = '操作'">操作日志</button>
      <button class="chip" :class="{ active: filter === '登录' }" @click="filter = '登录'">登录日志</button>
    </div>

    <div class="card">
      <div class="tbl-wrap">
        <table class="tbl">
          <thead>
            <tr>
              <th style="width:170px">时间</th><th style="width:110px">操作人</th>
              <th style="width:120px">类型</th><th>内容</th><th style="width:160px">IP 地址</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="l in filteredLogs" :key="l.id">
              <td class="dim">{{ l.time }}</td>
              <td>{{ l.operator }}</td>
              <td><span class="tag" :class="l.type === '登录' ? 'info' : 'gray'">{{ l.type === '登录' ? '登录日志' : '操作日志' }}</span></td>
              <td>{{ l.content }}</td>
              <td class="mono">{{ l.address }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
// 审计日志页：静态演示数据（P1 预留，不调接口）
import { computed, ref } from 'vue'
import PageCard from '../../components/PageCard.vue'

// 日志类型筛选：'' 全部 / '操作' 操作日志 / '登录' 登录日志
const filter = ref('')

// 静态日志数据
const logs = [
  { id: 1, time: '2026-10-29 10:12', operator: '王建国', type: '登录', content: '登录系统', address: '192.168.1.101' },
  { id: 2, time: '2026-10-29 10:15', operator: '李敏', type: '操作', content: '新增药品：阿莫西林胶囊', address: '192.168.1.105' },
  { id: 3, time: '2026-10-29 09:48', operator: '张伟', type: '操作', content: '新增采购订单：PO20261029-003', address: '192.168.1.103' },
  { id: 4, time: '2026-10-29 09:20', operator: '周瑞', type: '操作', content: '库存盘点：一号常温库', address: '192.168.1.108' },
  { id: 5, time: '2026-10-29 08:55', operator: '陈浩', type: '登录', content: '登录系统', address: '192.168.1.102' },
  { id: 6, time: '2026-10-28 17:30', operator: '王建国', type: '操作', content: '修改角色权限：库管', address: '192.168.1.101' }
]

// 按类型筛选后的列表
const filteredLogs = computed(() => (filter.value ? logs.filter((l) => l.type === filter.value) : logs))
</script>
