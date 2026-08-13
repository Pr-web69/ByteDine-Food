<template>
  <div class="dashboard">
    <!-- 今日运营数据 -->
    <section class="panel">
      <div class="panel-header">
        <span class="panel-title">今日运营数据</span>
        <span class="panel-date">{{ today }}</span>
      </div>
      <div class="stat-grid" v-loading="loading.stats">
        <div
            class="stat-card"
            v-for="(card, i) in statCards"
            :key="i"
        >
          <div class="stat-icon-wrap" :style="{ background: card.iconBg }">
            <el-icon :size="20" :color="card.color">
              <component :is="card.icon" />
            </el-icon>
          </div>
          <div class="stat-body">
            <div class="stat-label">{{ card.label }}</div>
            <div class="stat-value" :style="{ color: card.color }">
              {{ card.prefix }}{{ card.value }}{{ card.suffix }}
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 订单状态 -->
    <section class="panel">
      <div class="panel-header">
        <span class="panel-title">订单状态</span>
        <el-button text type="primary" size="small" @click="$router.push('/admin/order')">
          订单明细 <el-icon class="ml-4"><ArrowRight /></el-icon>
        </el-button>
      </div>
      <div class="status-grid">
        <div
            class="status-card"
            v-for="s in statusCards"
            :key="s.type"
            @click="$router.push(`/admin/order?status=${s.type}`)"
        >
          <div class="status-icon-box" :style="{ background: s.iconBg }">
            <el-icon :size="22" :color="s.color">
              <component :is="s.icon" />
            </el-icon>
          </div>
          <div class="status-text">
            <span class="status-name">{{ s.name }}</span>
            <span class="status-count" :style="{ color: s.color }">{{ s.count }}</span>
          </div>
        </div>
      </div>
      <!-- 状态占比条 -->
      <div class="status-bar" v-if="statusTotal > 0">
        <div
            v-for="s in statusCards"
            :key="s.type + '-bar'"
            class="status-bar-seg"
            :style="{ flex: s.count || 0.2, background: s.color }"
            :title="`${s.name}: ${s.count}`"
        ></div>
      </div>
    </section>

    <!-- 菜品 + 套餐总览 -->
    <div class="row-2col">
      <section class="panel">
        <div class="panel-header">
          <span class="panel-title">菜品总览</span>
          <el-button text type="primary" size="small" @click="$router.push('/admin/dish')">
            管理 <el-icon class="ml-4"><ArrowRight /></el-icon>
          </el-button>
        </div>
        <div class="ov-body">
          <div class="ov-item">
            <div class="ov-dot on"></div>
            <span>已启售</span>
            <strong>{{ dishStats.onSale }}</strong>
          </div>
          <div class="ov-item">
            <div class="ov-dot off"></div>
            <span>已停售</span>
            <strong>{{ dishStats.offSale }}</strong>
          </div>
        </div>
        <el-button type="primary" plain size="small" class="ov-add" @click="$router.push('/admin/dish')">
          <el-icon><Plus /></el-icon>新增菜品
        </el-button>
      </section>

      <section class="panel">
        <div class="panel-header">
          <span class="panel-title">套餐总览</span>
          <el-button text type="primary" size="small" @click="$router.push('/admin/setmeal')">
            管理 <el-icon class="ml-4"><ArrowRight /></el-icon>
          </el-button>
        </div>
        <div class="ov-body">
          <div class="ov-item">
            <div class="ov-dot on"></div>
            <span>已启售</span>
            <strong>{{ setmealStats.onSale }}</strong>
          </div>
          <div class="ov-item">
            <div class="ov-dot off"></div>
            <span>已停售</span>
            <strong>{{ setmealStats.offSale }}</strong>
          </div>
        </div>
        <el-button type="primary" plain size="small" class="ov-add" @click="$router.push('/admin/setmeal')">
          <el-icon><Plus /></el-icon>新增套餐
        </el-button>
      </section>
    </div>

    <!-- 最近订单 -->
    <section class="panel">
      <div class="panel-header">
        <span class="panel-title">最近订单</span>
        <el-button text type="primary" size="small" @click="$router.push('/admin/order')">
          查看全部 <el-icon class="ml-4"><ArrowRight /></el-icon>
        </el-button>
      </div>
      <el-table
          :data="recentOrders"
          stripe
          empty-text="暂无订单"
          class="order-table"
          v-loading="loading.orders"
      >
        <el-table-column prop="orderNo" label="订单号" width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="mono-text">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="菜品" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.dishSummary || '-' }}</template>
        </el-table-column>
        <el-table-column label="金额" width="100" align="right">
          <template #default="{ row }">
            <span class="amount">￥{{ Number(row.totalAmount || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="tagType(row.status)" size="small" :effect="row.status === 1 ? 'dark' : 'light'">
              {{ row.statusText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="155">
          <template #default="{ row }">
            <span class="time-text">{{ fmtTime(row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
                v-if="row.status === 1"
                size="small"
                type="primary"
                link
                @click="handleAccept(row)"
            >
              接单
            </el-button>
            <el-button
                size="small"
                link
                type="primary"
                @click="$router.push(`/admin/order/${row.id || row.orderNo}`)"
            >
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useVoice } from '@/composables/useVoice'
import { ElMessage } from 'element-plus'
import {
  Money, ShoppingCart, Trophy, User, TrendCharts,
  Bell, Clock, CircleCheck, CloseBold, Plus, ArrowRight,
} from '@element-plus/icons-vue'
import { getDashboard } from '@/api/statistics'
import { acceptOrder } from '@/api/order'
import request from '@/utils/request'

/* ── 日期 ── */
const today = new Date().toLocaleDateString('zh-CN').replace(/\//g, '.')

/* ── Loading 拆分 ── */
const loading = ref({ stats: true, orders: true })

/* ── 运营指标 ── */
const statCards = ref([
  { label: '营业额 (元)', value: '0.00', prefix: '¥', suffix: '', icon: Money,        color: '#F97316', bg: '#FFF7ED', iconBg: '#FFF7ED' },
  { label: '有效订单',     value: '0',    prefix: '',  suffix: '', icon: ShoppingCart,  color: '#2563EB', bg: '#EFF6FF', iconBg: '#EFF6FF' },
  { label: '订单完成率',   value: '0',    prefix: '',  suffix: '%', icon: Trophy,       color: '#10B981', bg: '#ECFDF5', iconBg: '#ECFDF5' },
  { label: '平均客单价',   value: '0.00', prefix: '¥', suffix: '', icon: TrendCharts,  color: '#F59E0B', bg: '#FFFBEB', iconBg: '#FFFBEB' },
  { label: '新增用户',     value: '0',    prefix: '',  suffix: '', icon: User,          color: '#6366F1', bg: '#EEF2FF', iconBg: '#EEF2FF' },
])

/* ── 订单状态 ── */
const statusCards = ref([
  { type: 0, name: '待支付', icon: Bell,      color: '#F59E0B', iconBg: '#FFFBEB', count: 0 },
  { type: 1, name: '已支付', icon: Clock,     color: '#2563EB', iconBg: '#EFF6FF', count: 0 },
  { type: 2, name: '配送中', icon: CircleCheck, color: '#06B6D4', iconBg: '#ECFEFF', count: 0 },
  { type: 3, name: '已完成', icon: CircleCheck, color: '#10B981', iconBg: '#ECFDF5', count: 0 },
  { type: 4, name: '已取消', icon: CloseBold,  color: '#6B7280', iconBg: '#F3F4F6', count: 0 },
])

const statusTotal = computed(() => statusCards.value.reduce((s, c) => s + c.count, 0))

/* ── 菜品 / 套餐 ── */
const dishStats = ref({ onSale: 0, offSale: 0 })
const setmealStats = ref({ onSale: 0, offSale: 0 })

/* ── 最近订单 ── */
const recentOrders = ref([])

/* ── 工具函数 ── */
const tagType = (s) => ({ 0: 'warning', 1: 'danger', 2: 'primary', 3: 'success', 4: 'info' }[s] || 'info')

const fmtTime = (t) => {
  if (!t) return '-'
  return t.replace('T', ' ').slice(0, 16)
}

/* ── 接单操作 ── */
const handleAccept = async (row) => {
  try {
    await acceptOrder(row.id || row.orderNo)
    ElMessage.success('接单成功')
    await loadOrders()
  } catch (e) {
    ElMessage.error(e?.message || '接单失败')
  }
}

/* ── 数据加载 ── */
const voice = useVoice()
let pollTimer = null
let lastOrderCount = 0

onMounted(() => {
  loadStats()
  loadOrders()
  pollTimer = setInterval(() => { loadStats(); loadOrders() }, 30000)
})

onBeforeUnmount(() => {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
})

const loadStats = async () => {
  loading.value.stats = true
  try {
    // 刷新缓存获取最新数据
    await request.post('/admin/statistics/refresh').catch(() => {})
    const stats = await getDashboard()
    if (!stats || !stats.orderStatus) { loading.value.stats = false; return }

    // 订单状态匹配 — 优先用 id，兼容 name
    const statusMap = {}
    ;(stats.orderStatus || []).forEach(s => {
      const key = s.id ?? s.name
      statusMap[key] = Number(s.value) || 0
    })

    statusCards.value.forEach(card => {
      card.count = statusMap[card.type] ?? statusMap[card.name] ?? 0
    })

    const total = statusTotal.value
    const completed = statusCards.value[3].count

    // 营业额：从 API 读取，不造假数据
    const revenue = stats.todayRevenue ?? stats.revenue ?? 0
    const avgPrice = total > 0 ? (revenue / total) : 0

    statCards.value[0].value = Number(revenue).toFixed(2)
    statCards.value[1].value = String(total)
    statCards.value[2].value = String(total > 0 ? Math.round(completed / total * 100) : 0)
    statCards.value[3].value = avgPrice.toFixed(2)
    statCards.value[4].value = String(stats.newUsers ?? stats.newCustomers ?? 0)

    // 菜品
    dishStats.value.onSale = stats.dishOnSale ?? 0
    dishStats.value.offSale = stats.dishOffSale ?? 0

    // 套餐
    setmealStats.value.onSale = stats.setmealOnSale ?? 0
    setmealStats.value.offSale = stats.setmealOffSale ?? 0
  } catch (e) {
    console.error('统计数据加载失败:', e)
    ElMessage.error('统计数据加载失败')
  } finally {
    loading.value.stats = false
  }
}

const loadOrders = async () => {
  loading.value.orders = true
  try {
    const res = await request.get('/admin/order/list', {
      params: { page: 1, pageSize: 8 }
    })
    if (res?.records) {
      recentOrders.value = res.records.map(o => ({
        ...o,
        dishSummary: o.details?.map(d => d.dishName).filter(Boolean).join('、') || '',
      }))
    }
    // 检测新订单并语音播报
    const newCount = recentOrders.value.filter(o => o.status === 0).length
    if (newCount > lastOrderCount && lastOrderCount > 0) {
      voice.speakNewOrder()
    }
    lastOrderCount = newCount
  } catch (e) {
    console.error('订单数据加载失败:', e)
    ElMessage.error('订单数据加载失败')
  } finally {
    loading.value.orders = false
  }
}
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ── Panel ── */
.panel {
  background: #ffffff;
  border-radius: var(--bb-radius-md);
  padding: 24px;
  box-shadow: var(--bb-shadow-card);
  border: 1px solid var(--bb-border-light);
}

.panel-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.panel-title {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}

.panel-date {
  font-size: 12px;
  color: #94a3b8;
  margin-left: 10px;
  padding: 2px 10px;
  background: #f8fafc;
  border-radius: 12px;
}

.panel-header .el-button {
  margin-left: auto;
  font-size: 13px;
}

.ml-4 {
  margin-left: 4px;
}

/* ── Stat Grid ── */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  border-radius: var(--bb-radius-md);
  padding: 20px;
  background: #ffffff;
  border: 1px solid var(--bb-border-light);
  transition: all 0.25s ease;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--bb-shadow-md);
  border-color: var(--bb-border);
}

.stat-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: var(--bb-radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-body {
  min-width: 0;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 6px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
  color: #1e293b;
}

/* ── Status Grid ── */
.status-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}

.status-card {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #ffffff;
  border-radius: var(--bb-radius-md);
  padding: 18px;
  cursor: pointer;
  transition: all 0.25s;
  border: 1px solid var(--bb-border-light);
}

.status-card:hover {
  box-shadow: var(--bb-shadow-md);
  transform: translateY(-2px);
  border-color: var(--bb-border);
}

.status-icon-box {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.status-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.status-name {
  font-size: 13px;
  color: #64748b;
}

.status-count {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
}

/* Status bar */
.status-bar {
  display: flex;
  height: 8px;
  border-radius: 4px;
  overflow: hidden;
  gap: 3px;
  background: var(--bb-border-light);
}

.status-bar-seg {
  min-width: 6px;
  border-radius: 4px;
  transition: flex 0.5s ease;
}

/* ── 2-Column Row ── */
.row-2col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

/* ── Overview ── */
.ov-body {
  display: flex;
  justify-content: space-around;
  margin-bottom: 20px;
}

.ov-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #475569;
}

.ov-item strong {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  margin-left: 4px;
}

.ov-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.ov-dot.on  { background: var(--bb-success); }
.ov-dot.off { background: #cbd5e1; }

.ov-add {
  align-self: center;
  border-radius: 8px;
}

/* ── Order Table ── */
.order-table {
  border-radius: var(--bb-radius-sm);
  overflow: hidden;
}

.order-table :deep(.el-table__header th) {
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
  font-size: 13px;
  height: 44px;
}

.order-table :deep(.el-table__row) {
  transition: background 0.2s;
}

.order-table :deep(.el-table__row:hover) {
  background: #f8fafc;
}

.mono-text {
  font-family: 'DM Mono', 'SF Mono', monospace;
  font-size: 12px;
  color: #64748b;
}

.amount {
  font-weight: 700;
  color: #1e293b;
}

.time-text {
  font-size: 12px;
  color: #94a3b8;
}

/* ── Responsive ── */
@media (max-width: 1200px) {
  .stat-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .row-2col {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .status-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>