<template>
  <div>
    <PageCard title="采购入库" desc="验收入库生成批次 · 按 药品 + 生产批号 自动建立批次库存"></PageCard>

    <!-- 步骤条：1 选择待入库采购单 → 2 验收录入批次效期 → 3 入库完成生成批次 -->
    <div class="steps">
      <div class="step" :class="stepClass(1)"><span class="num">1</span>选择待入库采购单</div>
      <div class="step-line" :class="{ on: currentStep >= 2 }"></div>
      <div class="step" :class="stepClass(2)"><span class="num">2</span>验收录入批次效期</div>
      <div class="step-line" :class="{ on: currentStep >= 3 }"></div>
      <div class="step" :class="stepClass(3)"><span class="num">3</span>入库完成生成批次</div>
    </div>

    <!-- ========== 视图一：待入库采购单列表 ========== -->
    <div v-if="currentStep === 1">
      <div class="note warn">仅审核通过的采购单可入库 · 入库后按「药品 + 批号」自动生成批次库存</div>
      <DataTable ref="tableRef" :columns="columns" :fetch="fetchData" :mock="mockData" :query="query">
        <template #actions="{ row }">
          <!-- 待入库（状态 1/2）显示验收入库入口 -->
          <button v-if="[1, 2].includes(Number(row.inboundStatus))" class="btn btn-sm" @click="openInbound(row)">验收入库</button>
          <span v-else class="dim" style="font-size:12px">已入库</span>
        </template>
      </DataTable>
    </div>

    <!-- ========== 视图二：验收录入（批号 / 效期 / 库位） ========== -->
    <div v-if="currentStep === 2 && currentPo">
      <!-- 页头：当前采购单号 + 返回列表 -->
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px">
        <div>
          <h3 style="font-size:16px;color:var(--ink)">验收入库 · <span style="color:var(--primary)">{{ currentPo.orderNo }}</span></h3>
          <p style="font-size:12.5px;color:var(--muted);margin-top:4px">逐行录入 生产批号 / 有效期 / 库位 · 保存后自动生成批次库存</p>
        </div>
        <button class="btn btn-ghost" @click="backToList">← 返回列表</button>
      </div>

      <!-- 单据信息头 -->
      <div class="card">
        <div class="card-title">单据信息</div>
        <div class="form-grid" style="grid-template-columns:repeat(4,1fr)">
          <div class="f-item"><label>供应商</label><input :value="currentPo.supplierName" disabled></div>
          <div class="f-item"><label>采购日期</label><input :value="currentPo.createTime || '-'" disabled></div>
          <div class="f-item">
            <label>审核状态</label>
            <div style="height:40px;border:1.5px solid var(--line);border-radius:10px;padding:0 12px;display:flex;align-items:center;background:var(--surface-2)">
              <span class="tag ok">已审核</span>
            </div>
          </div>
          <div class="f-item"><label>经办人</label><input :value="currentPo.createBy || '张伟'" disabled></div>
          <div class="f-item">
            <label>入库仓库 *</label>
            <select v-model="inbound.warehouseId">
              <option v-for="w in warehouseOptions" :key="w.id" :value="w.id">{{ w.name }}</option>
            </select>
          </div>
          <div class="f-item"><label>入库日期</label><input type="date" v-model="inbound.inboundDate"></div>
          <div class="f-item"><label>验收人</label><input v-model.trim="inbound.inspector" placeholder="验收人"></div>
          <div class="f-item"><label>入库单号</label><input :value="inbound.inboundNo" disabled></div>
        </div>
      </div>

      <!-- 验收明细表 -->
      <div class="card">
        <div class="card-title">验收明细 · 录入批次效期<span class="more">共 {{ inbound.items.length }} 种药品</span></div>
        <div class="tbl-wrap">
          <table class="tbl">
            <thead>
              <tr><th>药品</th><th>规格</th><th>采购数量</th><th>本次入库</th><th>生产批号 *</th><th>有效期 *</th><th>存储条件</th><th>库位</th><th>金额</th></tr>
            </thead>
            <tbody>
              <tr v-for="(it, i) in inbound.items" :key="i">
                <td class="name">{{ it.drugName }}</td>
                <td>{{ it.spec }}</td>
                <td>{{ it.purchaseQty }} 盒</td>
                <td><input type="number" min="0" v-model.number="it.quantity" style="width:64px;height:30px;border:1.5px solid var(--line);border-radius:8px;padding:0 8px;text-align:center"></td>
                <td><input v-model.trim="it.batchNo" placeholder="如 A20261029" style="width:130px;height:30px;border:1.5px solid var(--line);border-radius:8px;padding:0 10px"></td>
                <td><input type="date" v-model="it.expiryDate" style="width:140px;height:30px;border:1.5px solid var(--line);border-radius:8px;padding:0 10px"></td>
                <td>{{ it.storageCondition }}</td>
                <td><input v-model.trim="it.location" placeholder="如 A-03" style="width:90px;height:30px;border:1.5px solid var(--line);border-radius:8px;padding:0 10px"></td>
                <td>{{ fmtMoney((Number(it.quantity) || 0) * (Number(it.price) || 0)) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <!-- 近效期 90 天内自动提醒 -->
        <div v-if="nearExpiryNotes.length" class="note warn" style="margin-top:16px">
          {{ nearExpiryNotes.map(n => `「${n.drugName}」有效期 ${n.expiryDate}（剩 ${n.days} 天）`).join('；') }} 属近效期 90 天内批次，入库后自动进入近效期预警。
        </div>
        <!-- 底部操作 + 合计 -->
        <div style="display:flex;align-items:center;gap:12px;margin-top:18px">
          <button class="btn btn-ghost" @click="backToList">取消</button>
          <button class="btn btn-primary" @click="confirmInbound">✓ 确认入库</button>
          <div style="margin-left:auto;display:flex;gap:28px;align-items:center">
            <div><span class="dim">入库品种</span><b style="display:block;font-size:15px;color:var(--ink)">{{ inbound.items.length }}</b></div>
            <div><span class="dim">入库数量</span><b style="display:block;font-size:15px;color:var(--ink)">{{ inboundSum.qty }} 盒</b></div>
            <div><span class="dim">入库金额</span><b style="display:block;font-size:22px;color:var(--primary-dark)">{{ fmtMoney(inboundSum.amount) }}</b></div>
          </div>
        </div>
      </div>
    </div>

    <!-- ========== 视图三：入库完成回执 ========== -->
    <div v-if="currentStep === 3">
      <div class="card">
        <!-- 成功回执：大圆 ✓ + 标题 + 描述 -->
        <div style="text-align:center;padding:30px 0 26px">
          <div style="width:64px;height:64px;border-radius:50%;background:var(--ok-bg);color:var(--ok);margin:0 auto 18px;display:grid;place-items:center;font-size:30px">✓</div>
          <h3 style="font-size:20px;color:var(--ink);margin-bottom:8px">入库完成 · 批次库存已生成</h3>
          <p style="font-size:13px;color:var(--muted);line-height:1.9">入库单 <b style="color:var(--primary)">{{ inbound.inboundNo }}</b> 已生效<br>按「药品 + 生产批号」自动生成 {{ doneBatches.length }} 个新批次，实时库存与库存流水已同步更新</p>
        </div>
      </div>
      <div class="card">
        <div class="card-title">本次生成的新批次<span class="more">可在「批次管理」中追溯</span></div>
        <div class="tbl-wrap">
          <table class="tbl">
            <thead>
              <tr><th>药品</th><th>生产批号</th><th>有效期至</th><th>效期状态</th><th>入库数量</th><th>仓库 · 库位</th><th>库存变动</th></tr>
            </thead>
            <tbody>
              <tr v-for="(b, i) in doneBatches" :key="i">
                <td class="name">{{ b.drugName }}</td>
                <td class="mono">{{ b.batchNo }}</td>
                <td class="mono">{{ b.expiryDate }}</td>
                <td><span class="tag" :class="expiryTag(b.expiryDate).type">{{ expiryTag(b.expiryDate).text }}</span></td>
                <td>{{ b.quantity }} 盒</td>
                <td>{{ warehouseName(inbound.warehouseId) }} {{ b.location }}</td>
                <td><span class="tag info">+{{ b.quantity }} 入库流水</span></td>
              </tr>
            </tbody>
          </table>
        </div>
        <!-- 底部操作 -->
        <div style="display:flex;align-items:center;gap:12px;margin-top:16px">
          <button class="btn btn-ghost" @click="backToList">返回待入库列表</button>
          <button class="btn btn-primary" @click="backToList">继续入库下一单</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// 采购入库页：三步流程（待入库列表 → 验收录入批次效期 → 完成回执）
import { computed, reactive, ref } from 'vue'
import PageCard from '../../components/PageCard.vue'
import DataTable from '../../components/DataTable.vue'
import { getPurchaseOrders, getPurchaseOrder, inboundPurchaseOrder, getWarehouseOptions } from '../../api/purchase'
import { notifySuccess, notifyError } from '../../utils/notify'

const tableRef = ref()
// 当前步骤：1 待入库列表 / 2 验收录入 / 3 完成回执
const currentStep = ref(1)
const query = reactive({})

// 金额千分位格式化
function fmtMoney(n) {
  return '¥ ' + (Number(n) || 0).toLocaleString('zh-CN')
}
// 今日日期 YYYY-MM-DD
function todayStr() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
// 距离指定日期的剩余天数（按自然日）
function daysUntil(dateStr) {
  const t = new Date(dateStr).getTime()
  const now = new Date().setHours(0, 0, 0, 0)
  return Math.round((t - now) / 86400000)
}
// 步骤条状态：已走过的置 done、当前置 on
function stepClass(n) {
  if (currentStep.value > n) return 'done'
  if (currentStep.value === n) return 'on'
  return ''
}

// 列配置：审核状态恒为「已审核」，入库状态 1/2 待入库 / 3 已入库
const columns = [
  { title: '采购单号', key: 'orderNo', className: 'mono name', width: '170px' },
  { title: '供应商', key: 'supplierName' },
  { title: '药品明细', key: 'drugNames', render: (r) => r.drugNames || (r.items || []).map(i => i.drugName).join('、') || '-' },
  { title: '采购数量', key: 'quantity', render: (r) => `${Number(r.quantity) || 0} 盒` },
  { title: '金额', key: 'totalAmount', render: (r) => fmtMoney(r.totalAmount) },
  { title: '审核状态', key: 'auditStatus', width: '100px', tag: () => ({ type: 'ok', text: '已审核' }) },
  { title: '入库状态', key: 'inboundStatus', width: '100px',
    tag: (r) => (Number(r.inboundStatus) === 3 ? { type: 'ok', text: '已入库' } : { type: 'warn', text: '待入库' }) }
]

// 拉取待入库采购单（status 1=待审核 / 2=待入库）
function fetchData(params) {
  return getPurchaseOrders({ status: '1,2', page: params.page, pageSize: params.pageSize })
}

// 兜底演示数据：3 条待入库 + 1 条已入库（复用原型数据）
const mockData = [
  { id: 1, orderNo: 'PO-20261028-001', supplierName: '华北制药', inboundStatus: 2, quantity: 600, totalAmount: 22600, createTime: '2026-10-28', createBy: '张伟',
    drugNames: '阿莫西林胶囊 等 2 种', items: [
      { itemId: 1, drugId: 1, drugName: '阿莫西林胶囊', spec: '0.25g×24粒', quantity: 500, purchasePrice: 37, storageCondition: '常温 ≤30℃' },
      { itemId: 2, drugId: 2, drugName: '阿司匹林肠溶片', spec: '100mg×30片', quantity: 100, purchasePrice: 41, storageCondition: '常温 ≤30℃' }
    ] },
  { id: 3, orderNo: 'PO-20261026-003', supplierName: '恒瑞医药', inboundStatus: 2, quantity: 400, totalAmount: 12400, createTime: '2026-10-26', createBy: '李敏',
    drugNames: '二甲双胍缓释片 等 1 种', items: [
      { itemId: 31, drugId: 5, drugName: '二甲双胍缓释片', spec: '0.5g×60片', quantity: 400, purchasePrice: 31, storageCondition: '常温 ≤30℃' }
    ] },
  { id: 4, orderNo: 'PO-20261024-004', supplierName: '石药集团', inboundStatus: 1, quantity: 600, totalAmount: 8400, createTime: '2026-10-24', createBy: '张伟',
    drugNames: '阿司匹林肠溶片 等 1 种', items: [
      { itemId: 41, drugId: 2, drugName: '阿司匹林肠溶片', spec: '100mg×30片', quantity: 600, purchasePrice: 14, storageCondition: '常温 ≤30℃' }
    ] },
  { id: 2, orderNo: 'PO-20261027-002', supplierName: '太极集团', inboundStatus: 3, quantity: 300, totalAmount: 9900, createTime: '2026-10-27', createBy: '张伟',
    drugNames: '藿香正气口服液 等 1 种', items: [] }
]

// ---------- 入库仓库下拉 ----------
const warehouseOptions = ref([])
async function loadWarehouses() {
  try {
    const data = await getWarehouseOptions()
    warehouseOptions.value = (data && data.list) || []
  } catch (e) {
    warehouseOptions.value = [{ id: 1, name: '常温库' }, { id: 2, name: '阴凉库' }, { id: 3, name: '冷藏库 2~8℃' }]
  }
}
loadWarehouses()
function warehouseName(id) {
  const w = warehouseOptions.value.find(x => x.id === id)
  return w ? w.name : '常温库'
}

// ---------- 验收录入表单 ----------
const currentPo = ref(null) // 当前入库的采购单
const inbound = reactive({ warehouseId: null, inboundDate: todayStr(), inspector: '王建国', inboundNo: '', items: [] })

// 点击「验收入库」：把该行单号 / 供应商 / 明细带入，进入视图二
async function openInbound(row) {
  let po = row
  let items = row.items
  // 行内无明细时回源拉详情接口
  if (!items || !items.length) {
    try {
      const d = await getPurchaseOrder(row.id)
      po = d || row
      items = (d && d.items) || []
    } catch (e) {
      notifyError('采购单明细加载失败')
      return
    }
  }
  currentPo.value = po
  inbound.warehouseId = po.warehouseId ?? 1
  inbound.inboundDate = todayStr()
  // 入库单号：IN-日期-序号
  inbound.inboundNo = `IN-${todayStr().replace(/-/g, '')}-001`
  // 明细行：本次入库默认等于采购数量，批号/效期/库位待录入
  inbound.items = items.map(it => ({
    itemId: it.itemId ?? it.id,
    drugId: it.drugId,
    drugName: it.drugName,
    spec: it.spec || '-',
    purchaseQty: Number(it.quantity) || 0,
    quantity: Number(it.quantity) || 0,
    batchNo: '',
    expiryDate: '',
    storageCondition: it.storageCondition || '常温 ≤30℃',
    location: '',
    price: Number(it.purchasePrice) || 0
  }))
  currentStep.value = 2
}

// 近效期提醒：有效期在 90 天内的行自动列出
const nearExpiryNotes = computed(() =>
  inbound.items.filter(it => it.expiryDate && daysUntil(it.expiryDate) >= 0 && daysUntil(it.expiryDate) <= 90)
    .map(it => ({ drugName: it.drugName, expiryDate: it.expiryDate, days: daysUntil(it.expiryDate) }))
)

// 合计：品种 / 数量 / 金额
const inboundSum = computed(() => ({
  qty: inbound.items.reduce((s, it) => s + (Number(it.quantity) || 0), 0),
  amount: inbound.items.reduce((s, it) => s + (Number(it.quantity) || 0) * (Number(it.price) || 0), 0)
}))

// 确认入库：调接口成功后进入回执视图
const doneBatches = ref([])
async function confirmInbound() {
  const bad = inbound.items.filter(it => !it.batchNo || !it.expiryDate || (Number(it.quantity) || 0) <= 0)
  if (bad.length) return notifyError('请完整填写 本次入库数量 / 生产批号 / 有效期')
  const payload = {
    items: inbound.items.map(it => ({
      itemId: it.itemId,
      quantity: Number(it.quantity),
      batchNo: it.batchNo,
      expiryDate: it.expiryDate
    }))
  }
  try {
    await inboundPurchaseOrder(currentPo.value.id, payload)
    // 快照本次生成的批次，供回执展示
    doneBatches.value = inbound.items.map(it => ({ ...it }))
    currentStep.value = 3
    notifySuccess('入库成功，批次库存已生成')
  } catch (e) {
    notifyError(e.message || '入库失败')
  }
}

// 效期状态：90 天内近效期 warn，否则正常 ok
function expiryTag(expiryDate) {
  return daysUntil(expiryDate) <= 90 ? { type: 'warn', text: '近效期 90天' } : { type: 'ok', text: '正常' }
}

// 返回待入库列表
function backToList() {
  currentStep.value = 1
  tableRef.value && tableRef.value.reload()
}
</script>
