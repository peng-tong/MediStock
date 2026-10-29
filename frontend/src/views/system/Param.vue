<template>
  <!-- 参数配置：P1 预留，无接口，静态示例表展示 -->
  <div>
    <PageCard title="参数配置" desc="系统管理 · 全局配置项管理" />

    <!-- 预留提示条 -->
    <div class="note">参数配置属于 P1 预留，表 sys_config（预警天数、单号规则等）后续落地</div>

    <div class="card">
      <div class="card-title">参数示例</div>
      <div class="tbl-wrap">
        <table class="tbl">
          <thead>
            <tr><th style="width:220px">参数键</th><th style="width:120px">参数值</th><th>说明</th></tr>
          </thead>
          <tbody>
            <tr v-for="p in params" :key="p.paramKey">
              <td class="mono">{{ p.paramKey }}</td>
              <td>{{ p.paramValue }}</td>
              <td>{{ p.remark }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
// 参数配置页：静态演示数据（P1 预留，不调接口）
import PageCard from '../../components/PageCard.vue'

// 静态参数列表：键 / 值 / 说明
const params = [
  { paramKey: 'expiry.warn.days', paramValue: '90', remark: '近效期预警天数（到期前 N 天提醒）' },
  { paramKey: 'expiry.warn.days30', paramValue: '30', remark: '近效期预警天数（临期加速提醒）' },
  { paramKey: 'order.no.prefix', paramValue: 'PO', remark: '采购单号前缀' },
  { paramKey: 'stock.warn.rate', paramValue: '1.2', remark: '库存预警系数' }
]
</script>
