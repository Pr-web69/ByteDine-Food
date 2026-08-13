<template>
  <div class="order-list-page">
    <div class="page-header">
      <div class="header-bg"></div>
      <h2 class="page-title">我的订单</h2>
      <p class="page-subtitle">查看全部订单与支付状态</p>
    </div>

    <div class="content-wrap">
      <!-- 状态筛选 -->
      <div class="filter-bar">
        <el-radio-group v-model="statusFilter" size="small" @change="load">
          <el-radio-button :value="undefined">全部</el-radio-button>
          <el-radio-button :value="0">待支付</el-radio-button>
          <el-radio-button :value="1">待接单</el-radio-button>
          <el-radio-button :value="2">待配送</el-radio-button>
          <el-radio-button :value="3">已完成</el-radio-button>
          <el-radio-button :value="4">已取消</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="order-skeleton">
        <div v-for="i in 3" :key="i" class="sk-card">
          <div class="sk-line sk-w30"></div>
          <div class="sk-line sk-w60"></div>
          <div class="sk-line sk-w40"></div>
        </div>
      </div>

      <!-- 订单列表 -->
      <template v-else>
        <div v-if="orders.length" class="order-list">
          <div
            v-for="order in orders"
            :key="order.id"
            class="order-card"
            @click="goDetail(order.id)"
          >
            <div class="order-top">
              <span class="order-no">#{{ order.orderNo }}</span>
              <el-tag :type="statusType(order.status)" size="small" effect="plain">
                {{ order.statusText }}
              </el-tag>
            </div>
            <div class="order-info">
              <div class="info-row">
                <span>下单时间</span>
                <span>{{ formatTime(order.createTime) }}</span>
              </div>
              <div class="info-row">
                <span>订单金额</span>
                <span class="order-amount">￥{{ Number(order.totalAmount || 0).toFixed(2) }}</span>
              </div>
            </div>
            <div class="order-actions" @click.stop>
              <el-button
                v-if="order.status === 0"
                type="primary"
                size="small"
                round
                @click="handlePay(order)"
              >
                去支付
              </el-button>
              <el-button
                v-if="order.status === 0"
                size="small"
                round
                @click="handleCancel(order.id)"
              >
                取消订单
              </el-button>
              <el-button
                v-if="order.status === 2"
                type="primary"
                size="small"
                round
                @click="handleConfirm(order.id)"
              >
                确认收货
              </el-button>
              <el-button
                v-if="order.status === 3 || order.status === 4"
                size="small"
                round
                disabled
              >
                {{ order.status === 3 ? '已完成' : '已取消' }}
              </el-button>
            </div>
          </div>
        </div>

        <el-empty v-else description="暂无订单" :image-size="80">
          <el-button type="primary" plain round size="small" @click="$router.push('/home')">
            去选购
          </el-button>
        </el-empty>
      </template>

      <div v-if="total > pageSize" class="pagination">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          v-model:current-page="page"
          @current-change="load"
        />
      </div>
    </div>

    <!-- 支付弹窗 -->
    <PayDialog
      v-model:visible="payVisible"
      :order-id="payOrderId"
      :order-no="payOrderNo"
      :amount="payAmount"
      :qr-code="payQrCode"
      :status="payStatus"
      :message="payMessage"
      :pay-window="payWindowRef"
      @success="onPaySuccess"
      @cancel="payVisible = false"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderList, cancelOrder, confirmOrder } from '@/api/order'
import { createPayment } from '@/api/pay'
import PayDialog from '@/components/PayDialog.vue'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const orders = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const statusFilter = ref(undefined)

// 支付弹窗状态
const payVisible = ref(false)
const payOrderId = ref(null)
const payOrderNo = ref('')
const payAmount = ref('')
const payQrCode = ref('')
const payStatus = ref('WAIT_PAY')
const payMessage = ref('')
const payWindowRef = ref(null)

const statusType = (s) => ({ 0: 'warning', 1: '', 2: 'primary', 3: 'success', 4: 'info' }[s] || '')
const formatTime = (t) => t ? t.slice(0, 16).replace('T', ' ') : ''

const load = async () => {
  loading.value = true
  try {
    const res = await getOrderList({
      page: page.value,
      pageSize: pageSize.value,
      status: statusFilter.value
    })
    orders.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    console.error('加载订单列表失败:', e)
    ElMessage.error('加载订单失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  load()
})

watch(() => route.path, (p) => {
  if (p === '/order') {
    page.value = 1
    load()
  }
})

const goDetail = (id) => {
  router.push(`/order/${id}`)
}

const handlePay = async (order) => {
  try {
    const res = await createPayment(order.id)
    payOrderId.value = order.id
    payOrderNo.value = res?.orderNo || order.orderNo
    payAmount.value = res?.amount || order.totalAmount
    payQrCode.value = res?.qrCode || ''
    payStatus.value = res?.status || 'WAIT_PAY'
    payMessage.value = res?.message || ''

    // 在用户点击上下文中直接打开支付窗口
    if (res?.qrCode) {
      const blob = new Blob([res.qrCode], { type: 'text/html;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      payWindowRef.value = window.open(url, '_blank')
    }

    payVisible.value = true
  } catch (e) {
    console.error('创建支付失败:', e)
    ElMessage.error(e?.message || '发起支付失败')
  }
}

const onPaySuccess = () => {
  payVisible.value = false
  ElMessage.success('支付成功')
  // 【业务规则】支付成功后跳转到订单详情页
  if (payOrderId.value) {
    router.push(`/order/${payOrderId.value}`)
  } else {
    load()
  }
}

const handleCancel = async (id) => {
  try {
    await ElMessageBox.confirm('确定取消该订单吗？', '取消订单', { type: 'warning' })
    await cancelOrder(id, '用户取消')
    ElMessage.success('已取消订单')
    load()
  } catch (e) {
    if (e !== 'cancel') console.error('取消订单失败:', e)
  }
}

const handleConfirm = async (id) => {
  try {
    await confirmOrder(id)
    ElMessage.success('已确认收货')
    load()
  } catch (e) {
    console.error('确认收货失败:', e)
    ElMessage.error(e?.message || '确认收货失败')
  }
}
</script>

<style scoped>
.order-list-page {
  --ol-primary: var(--color-primary, #3a7afe);
  --ol-accent: #ff6b35;
  --ol-bg: #f5f5f7;
  --ol-card: #ffffff;
  --ol-text: #1d1d1f;
  --ol-text-sub: #8e8e93;
  --ol-border: #f0eeeb;
  --ol-radius: 16px;

  min-height: 100vh;
  background: var(--ol-bg);
  padding-bottom: 80px;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Helvetica Neue', sans-serif;
}

.page-header {
  position: relative;
  padding: 32px 20px 28px;
  overflow: hidden;
}

.header-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, var(--ol-primary) 0%, #6b9dff 100%);
}

.page-header::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 24px;
  background: var(--ol-bg);
  border-radius: 24px 24px 0 0;
  z-index: 1;
}

.page-title {
  position: relative;
  z-index: 1;
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 4px;
}

.page-subtitle {
  position: relative;
  z-index: 1;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.78);
  margin: 0;
}

.content-wrap {
  padding: 0 16px;
  margin-top: 8px;
}

.filter-bar {
  margin-bottom: 12px;
  overflow-x: auto;
  white-space: nowrap;
}

.filter-bar :deep(.el-radio-group) {
  display: inline-flex;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-card {
  background: var(--ol-card);
  border-radius: var(--ol-radius);
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.2s ease;
}

.order-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.order-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.order-no {
  font-size: 13px;
  color: var(--ol-text-sub);
  font-family: monospace;
}

.order-info {
  margin-bottom: 14px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: var(--ol-text-sub);
  padding: 4px 0;
}

.info-row span:last-child {
  color: var(--ol-text);
  font-weight: 500;
}

.order-amount {
  font-size: 16px;
  font-weight: 700;
  color: var(--ol-accent) !important;
}

.order-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  border-top: 1px solid var(--ol-border);
  padding-top: 12px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.order-skeleton {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sk-card {
  background: var(--ol-card);
  border-radius: var(--ol-radius);
  padding: 16px;
}

.sk-line {
  height: 14px;
  border-radius: 4px;
  margin-bottom: 10px;
  background: linear-gradient(90deg, #eee 25%, #e0e0e0 50%, #eee 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.sk-line:last-child { margin-bottom: 0; }
.sk-w30 { width: 30%; }
.sk-w40 { width: 40%; }
.sk-w60 { width: 60%; }

@keyframes shimmer {
  0%   { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
