<template>
  <!-- 库存盘点：盘点单列表 → 实盘录入（差异实时计算）→ 差异调整完成回执 -->
  <div>
    <!-- 步骤条：1 选择盘点单 / 2 实盘录入 / 3 差异调整 -->
    <div class="steps">
      <div class="step" :class="{ done: step >= 1, on: step === 1 }"><span class="num">1</span>选择盘点单</div>
      <div class="step-line" :class="{ on: step >= 2 }"></div>
      <div class="step" :class="{ done: step >= 2, on: step === 2 }"><span class="num">2</span>实盘录入 · 差异计算</div>
      <div class="step-line" :class="{ on: step >= 3 }"></div>
      <div class="step" :class="{ done: step >= 3, on: step === 3 }"><span class="num">3</span>差异调整 · 生成流水</div>
    </div>

    <!-- ============ 视图1：盘点单列表 ============ -->
    <div v-if="view === 'list'">
      <PageCard title="盘点单列表" desc="盘点流程：新建盘点单 → 实盘录入 → 盘盈/盘亏差异调整 → 自动更新库存并生成流水">
        <template #actions>
          <button class="btn btn-primary" @click="openCreate">+ 新建盘点单</button>
        </template>
      </PageCard>

      <DataTable ref="tableRef" :columns="listColumns" :fetch="fetchList" :mock="listMock">
        <template #actions="{ row }">
          <template v-if="Number(row.status) === 0">
            <button class="btn btn-sm" @click="openCount(row)">继续盘点</button>
          </template>
          <template v-else-if="Number(row.status) === 1">
            <button class="btn btn-sm" @click="openCount(row)">查看</button>
          </template>
        </template>
      </DataTable>
    </div>

    <!-- ============ 视图2：实盘录入 + 差异实时计算 ============ -->
    <div v-else-if="view === 'count'">
      <div class="page-head">
        <div>
          <h2>盘点单 {{ current.takeNo }}</h2>
          <p>录入实盘数，系统自动计算 盘盈 / 盘亏 · 金额按最新批次成本价</p>
        </div>
        <div class="page-actions">
          <button class="btn btn-ghost" @click="backToList">← 返回列表</button>
        </div>
      </div>

      <!-- 差异汇总卡（实盘输入变化时实时重算） -->
      <div class="stat-grid">
        <div class="stat-card">
          <div class="stat-ico teal">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 9l8-5 8 5v11H4z"/><path d="M4 9h16"/><path d="M12 9v11"/></svg>
          </div>
          <div>
            <div class="lbl">账面库存金额</div>
            <div class="val">¥ {{ fmtMoney(summary.bookMoney) }}</div>
            <div class="trend" style="color:var(--primary)">按批次成本价汇总</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-ico teal">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 9l8-5 8 5v11H4z"/><path d="M4 9h16"/><path d="M12 9v11"/></svg>
          </div>
          <div>
            <div class="lbl">实盘库存金额</div>
            <div class="val">¥ {{ fmtMoney(summary.realMoney) }}</div>
            <div class="trend" style="color:var(--primary)">按实盘数量重算</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-ico green">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 3v18M12 3l4 4M12 3L8 7"/></svg>
          </div>
          <div>
            <div class="lbl">盘盈</div>
            <div class="val">{{ summary.gainQty }} 盒 / ¥ {{ summary.gainMoney }}</div>
            <div class="trend up">实盘多于账面</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-ico red">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 3v18M12 21l-4-4M12 21l4-4"/></svg>
          </div>
          <div>
            <div class="lbl">盘亏</div>
            <div class="val">{{ summary.lossQty }} 盒 / ¥ {{ summary.lossMoney }}</div>
            <div class="trend down">实盘少于账面</div>
          </div>
        </div>
      </div>

      <!-- 单据信息 -->
      <div class="card">
        <div class="card-title">单据信息</div>
        <div class="form-grid">
          <div class="f-item"><label>盘点仓库</label><input :value="current.warehouseName" disabled></div>
          <div class="f-item"><label>盘点范围</label><input :value="current.range || '全部药品'" disabled></div>
          <div class="f-item"><label>盘点方式</label><input value="现场实物盘点" disabled></div>
          <div class="f-item"><label>盘点人</label><input :value="(current.createByName || '') + (current.createTime ? ' · ' + String(current.createTime).slice(0, 10) : '')" disabled></div>
        </div>
      </div>

      <!-- 盘点明细表 -->
      <div class="card">
        <div class="card-title">盘点明细 · 实盘录入<span class="more">试试点「实盘数」：差异实时更新</span></div>
        <div class="tbl-wrap">
          <table class="tbl">
            <thead>
              <tr>
                <th>药品</th><th>规格</th><th>批次 / 效期</th><th>账面数</th><th>实盘数</th><th>差异</th><th>差异类型</th><th>差异金额</th><th>备注</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in rows" :key="r.id">
                <td class="name">{{ r.drugName }}</td>
                <td class="dim">{{ r.spec }}</td>
                <td class="dim mono">{{ r.batchNo }} · {{ r.expiryDate }}</td>
                <td>{{ r.bookQuantity }} 盒</td>
                <td><input class="num-input" type="number" min="0" v-model.number="r.actualQuantity"></td>
                <td :class="diffOf(r).diff > 0 ? 'text-ok' : diffOf(r).diff < 0 ? 'text-danger' : ''" style="font-weight:600">{{ fmtDiff(diffOf(r).diff) }}</td>
                <td><span class="tag" :class="diffOf(r).type">{{ diffOf(r).text }}</span></td>
                <td>{{ fmtMoney(diffOf(r).diffMoney) }}</td>
                <td class="dim">{{ r.remark }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="note" style="margin-top:16px">
          差异调整规则：盘盈按最近批次成本价回补库存；盘亏冲减对应批次并生成「盘点差异」库存流水，全程可追溯。
        </div>
        <!-- 底部操作条 + 实时合计 -->
        <div class="footbar">
          <button class="btn btn-ghost" @click="saveItems">保存盘点</button>
          <button class="btn btn-primary" @click="doConfirm">提交差异调整</button>
          <div class="sum">
            <div><span class="dim">盘点品种</span><b>{{ rows.length }} 种</b></div>
            <div><span class="dim">存在差异</span><b>{{ summary.diffCount }} 种</b></div>
            <div><span class="dim">差异金额</span><b class="big" :style="{ color: summary.diffMoney < 0 ? 'var(--danger)' : 'var(--ok)' }">¥ {{ summary.diffMoney }}</b></div>
          </div>
        </div>
      </div>
    </div>

    <!-- ============ 视图3：差异调整完成回执 ============ -->
    <div v-else>
      <div class="card">
        <div class="receipt">
          <div class="ok-ico">✓</div>
          <h3>差异调整完成 · 库存已同步</h3>
          <p>盘点单 <b style="color:var(--primary)">{{ current.takeNo }}</b> 差异已调整<br>
            盘盈 {{ summary.gainQty }} 盒回补库存 · 盘亏 {{ summary.lossQty }} 盒冲减批次 · {{ flows.length }} 条「盘点差异」流水已生成</p>
        </div>
      </div>
      <div class="card">
        <div class="card-title">本次调整的库存流水<span class="more">可在「库存流水」中追溯</span></div>
        <div class="tbl-wrap">
          <table class="tbl">
            <thead>
              <tr><th>流水号</th><th>类型</th><th>药品</th><th>批号</th><th>变动数量</th><th>变动金额</th><th>关联单号</th><th>操作人</th></tr>
            </thead>
            <tbody>
              <tr v-for="f in flows" :key="f.flowNo">
                <td class="mono name">{{ f.flowNo }}</td>
                <td><span class="tag" :class="f.type === '盘亏' ? 'danger' : 'ok'">盘点差异 · {{ f.type }}</span></td>
                <td>{{ f.drugName }}</td>
                <td class="mono">{{ f.batchNo }}</td>
                <td :class="f.qty < 0 ? 'text-danger' : 'text-ok'" style="font-weight:600">{{ f.qty > 0 ? '+' + f.qty : f.qty }} 盒</td>
                <td>{{ fmtMoney(f.money) }}</td>
                <td class="mono">{{ f.takeNo }}</td>
                <td class="dim">{{ f.operator }}</td>
              </tr>
              <tr v-if="!flows.length">
                <td colspan="8" class="empty">本次盘点无差异，无需生成流水</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="footbar" style="margin-top:16px">
          <button class="btn btn-ghost" @click="backToList">返回盘点单列表</button>
        </div>
      </div>
    </div>

    <!-- 新建盘点单弹窗（选择仓库） -->
    <Modal :visible="createVisible" title="新建盘点单" @close="createVisible = false">
      <div class="form-grid">
        <div class="f-item"><label>盘点仓库 *</label>
          <select v-model="createForm.warehouseId">
            <option :value="1">常温库</option>
            <option :value="2">阴凉库</option>
            <option :value="3">冷藏库</option>
          </select>
        </div>
        <div class="f-item"><label>盘点范围</label>
          <input :value="'全部药品'" disabled>
        </div>
      </div>
      <template #footer>
        <button class="btn btn-ghost" @click="createVisible = false">取消</button>
        <button class="btn btn-primary" @click="doCreate">创建</button>
      </template>
    </Modal>
  </div>
</template>

<script setup>
// 库存盘点页：三视图流程（列表 / 实盘录入 / 完成回执）
// 差异实时计算：实盘数 v-model 到 rows[i].actualQuantity，computed 统一重算差异与汇总
import { computed, reactive, ref } from 'vue'
import PageCard from '../../components/PageCard.vue'
import DataTable from '../../components/DataTable.vue'
import Modal from '../../components/Modal.vue'
import { getStocktakes, getStocktake, createStocktake, updateStocktakeItems, confirmStocktake } from '../../api/inventory'
import { notifySuccess, notifyError } from '../../utils/notify'

const tableRef = ref()
// 视图切换：list 盘点单列表 / count 实盘录入 / done 完成回执；step 对应步骤条
const view = ref('list')
const step = ref(1)
// 当前操作的盘点单
const current = ref({ id: null, takeNo: '', warehouseName: '', range: '', createBy: '', createTime: '' })

// ---------- 视图1：盘点单列表 ----------
const listColumns = [
  { title: '盘点单号', key: 'takeNo', className: 'mono name', width: '180px' },
  { title: '盘点仓库', key: 'warehouseName' },
  { title: '药品品种', key: 'speciesCount', align: 'center', render: (r) => `${r.speciesCount || 0} 种` },
  { title: '状态', key: 'status', width: '100px', tag: (r) => ({ 0: { type: 'info', text: '盘点中' }, 1: { type: 'ok', text: '已完成' }, 2: { type: 'gray', text: '已取消' } })[Number(r.status)] || { type: 'gray', text: '未知' } },
  { title: '盘点日期', key: 'createTime', className: 'dim', width: '130px', render: (r) => String(r.createTime || '').slice(0, 10) },
  { title: '盘点人', key: 'createByName', className: 'dim', width: '100px' }
]

// 兜底演示列表（1 条盘点中 + 2 条已完成）
const listMock = [
  { id: 1, takeNo: 'PC-20261029-001', warehouseName: '常温库', range: '全部药品', speciesCount: 186, status: 0, createTime: '2026-10-29', createBy: '王建国' },
  { id: 2, takeNo: 'PC-20261027-002', warehouseName: '阴凉库', range: '指定药品', speciesCount: 42, status: 1, createTime: '2026-10-27', createBy: '陈浩' },
  { id: 3, takeNo: 'PC-20261020-003', warehouseName: '冷藏库', range: '全部药品', speciesCount: 36, status: 1, createTime: '2026-10-20', createBy: '陈浩' }
]

function fetchList(params) {
  return getStocktakes(params)
}

// ---------- 视图2：盘点明细行（实盘数 v-model 到 actualQuantity） ----------
const rows = reactive([
  { id: 1, drugName: '阿莫西林胶囊', spec: '0.25g×24粒', batchNo: 'A20250812', expiryDate: '2027-08', bookQuantity: 850, actualQuantity: 850, price: 37, remark: '—' },
  { id: 2, drugName: '布洛芬缓释胶囊', spec: '0.3g×20粒', batchNo: 'B20250903', expiryDate: '2027-09', bookQuantity: 640, actualQuantity: 620, price: 14, remark: '上架时跌落破损 20 盒' },
  { id: 3, drugName: '藿香正气口服液', spec: '10ml×10支', batchNo: 'H20251012', expiryDate: '2027-10', bookQuantity: 260, actualQuantity: 263, price: 33, remark: '赠品入库未登记' },
  { id: 4, drugName: '维生素C片', spec: '0.1g×100片', batchNo: 'V20250905', expiryDate: '2027-09', bookQuantity: 32, actualQuantity: 32, price: 9, remark: '—' }
])

// 单行差异计算：实盘 - 账面，返回差异数量 / 金额 / 类型标签
function diffOf(r) {
  const diff = (Number(r.actualQuantity) || 0) - r.bookQuantity
  return {
    diff,
    diffMoney: diff * r.price,
    type: diff > 0 ? 'ok' : diff < 0 ? 'danger' : 'gray',
    text: diff > 0 ? '盘盈' : diff < 0 ? '盘亏' : '持平'
  }
}

// 差异汇总（实时联动）：账面金额 / 实盘金额 / 盘盈 / 盘亏 / 差异品种数 / 差异金额
const summary = computed(() => {
  let bookMoney = 0, realMoney = 0, gainQty = 0, gainMoney = 0, lossQty = 0, lossMoney = 0, diffCount = 0, diffMoney = 0
  rows.forEach(r => {
    const real = Number(r.actualQuantity) || 0
    const diff = real - r.bookQuantity
    bookMoney += r.bookQuantity * r.price
    realMoney += real * r.price
    if (diff > 0) { gainQty += diff; gainMoney += diff * r.price; diffCount++ }
    if (diff < 0) { lossQty -= diff; lossMoney -= diff * r.price; diffCount++ }
    diffMoney += diff * r.price
  })
  return { bookMoney, realMoney, gainQty, gainMoney, lossQty, lossMoney, diffCount, diffMoney }
})

// ---------- 视图3：差异调整生成的库存流水 ----------
const flows = computed(() => {
  const list = []
  rows.forEach((r, i) => {
    const d = diffOf(r)
    if (d.diff === 0) return
    list.push({
      flowNo: `FL-20261029-${String(22 + i).padStart(3, '0')}`,
      type: d.diff > 0 ? '盘盈' : '盘亏',
      drugName: r.drugName,
      batchNo: r.batchNo,
      qty: d.diff,
      money: d.diffMoney,
      takeNo: current.value.takeNo,
      operator: current.value.createBy || '王建国'
    })
  })
  return list
})

// 金额 / 差异格式化
const fmtMoney = (n) => Number(n || 0).toLocaleString('zh-CN')
const fmtDiff = (n) => (n > 0 ? '+' : '') + n

// ---------- 视图切换 ----------
// 进入实盘录入：尝试拉取盘点明细，失败（后端未就绪）沿用内置演示行
async function openCount(row) {
  current.value = row
  try {
    const d = await getStocktake(row.id)
    if (d) {
      // 详情含仓库名/盘点人/明细，合并回当前单据信息
      current.value = { ...row, ...d }
      if (Array.isArray(d.items) && d.items.length) {
        rows.splice(0, rows.length, ...d.items.map(it => ({ price: 0, remark: '—', ...it })))
      }
    }
  } catch (e) { /* 接口异常，沿用列表行与内置演示明细 */ }
  view.value = 'count'
  step.value = 2
}

function backToList() {
  view.value = 'list'
  step.value = 1
  tableRef.value && tableRef.value.reload()
}

// 保存盘点：提交实盘数量
async function saveItems() {
  try {
    await updateStocktakeItems(current.value.id, { items: rows.map(r => ({ id: r.id, actualQuantity: Number(r.actualQuantity) || 0 })) })
    notifySuccess('盘点已保存')
  } catch (e) {
    notifyError(e.message || '保存失败')
  }
}

// 提交差异调整：成功切到完成回执视图
async function doConfirm() {
  try {
    await confirmStocktake(current.value.id)
    notifySuccess('差异调整完成 · 库存已同步')
    view.value = 'done'
    step.value = 3
  } catch (e) {
    notifyError(e.message || '提交失败')
  }
}

// ---------- 新建盘点单 ----------
const createVisible = ref(false)
const createForm = reactive({ warehouseId: 1 })

function openCreate() { createVisible.value = true }

async function doCreate() {
  try {
    await createStocktake({ warehouseId: createForm.warehouseId })
    notifySuccess('盘点单已创建')
    createVisible.value = false
    tableRef.value.reload()
  } catch (e) {
    notifyError(e.message || '创建失败')
  }
}
</script>

<style scoped>
/* 盘盈统计卡颜色（全局 .stat-ico 仅有 teal/amber/red/blue，此处补充 green） */
.stat-ico.green { background: var(--ok-bg); color: var(--ok); }
/* 实盘数输入框 */
.num-input { width: 74px; height: 30px; border: 1.5px solid var(--line); border-radius: 8px; padding: 0 8px;
  text-align: center; font-size: 13px; color: var(--ink); background: #fff; outline: none; transition: .2s; }
.num-input:focus { border-color: var(--primary); }
/* 底部操作条与实时合计 */
.footbar { display: flex; align-items: center; gap: 12px; margin-top: 18px; }
.footbar .sum { margin-left: auto; display: flex; gap: 28px; align-items: center; }
.footbar .sum b { display: block; font-size: 15px; color: var(--ink); }
.footbar .sum .big { font-size: 22px; color: var(--primary-dark); }
/* 完成回执 */
.receipt { text-align: center; padding: 30px 0 26px; }
.receipt .ok-ico { width: 64px; height: 64px; border-radius: 50%; background: var(--ok-bg); color: var(--ok);
  margin: 0 auto 18px; display: grid; place-items: center; font-size: 30px; }
.receipt h3 { font-size: 20px; color: var(--ink); margin-bottom: 8px; }
.receipt p { font-size: 13px; color: var(--muted); line-height: 1.9; }
</style>
