<template>
  <!-- 库存调拨：P1 预留占位页（后续迭代实现调拨单流转与调拨流水） -->
  <div>
    <PageCard title="库存调拨" desc="调拨单管理 · 仓库间药品调配"></PageCard>

    <div class="card">
      <div class="placeholder">
        <div class="ph-ico">
          <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <path d="M7 17L17 7M17 7h-6M17 7v6"/>
            <path d="M12 3v3a9 9 0 1 0 9 9h-3"/>
          </svg>
        </div>
        <h3>库存调拨 P1 预留</h3>
        <p>调拨流水类型 6 已预留，接口位 /api/inventory/transfers，后续实现</p>
      </div>
    </div>
  </div>
</template>

<script setup>
// 库存调拨：占位页，不调用任何接口
import PageCard from '../../components/PageCard.vue'
</script>
