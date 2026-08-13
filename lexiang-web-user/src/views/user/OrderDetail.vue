<template>
  <div class="orderdetail-page">
    <div class="page-header">
      <div class="header-bg"></div>
      <div class="header-nav"><el-button text class="back-btn" @click="$router.back()"><el-icon><ArrowLeft /></el-icon><span>返回</span></el-button></div>
      <h2 class="page-title">订单详情</h2>
    </div>

    <div class="content-wrap" v-if="order">
      <div class="status-card">
        <div class="status-icon">{{ statusIcon }}</div>
        <div class="status-text">{{ order.statusText }}</div>
        <div class="status-no">#{{ order.orderNo }}</div>
      </div>

      <div class="card" v-if="order.consignee">
        <div class="card-title">📍 收货信息</div>
        <div class="info-row"><span>收货人</span><span>{{ order.consignee }}</span></div>
        <div class="info-row"><span>联系电话</span><span>{{ maskedPhone }}</span></div>
        <div class="info-row"><span>收货地址</span><span>{{ order.address }}</span></div>
      </div>

      <div class="card">
        <div class="card-title">📦 菜品明细</div>
        <div class="detail-item" v-for="d in order.details" :key="d.id">
          <img :src="d.dishImage || placeholderImg" class="di-img" loading="lazy" />
          <div class="di-info">
            <span>{{ d.dishName }}</span>
            <span class="di-spec" v-if="d.specInfo">{{ d.specInfo }}</span>
            <span class="di-price">￥{{ Number(d.price).toFixed(2) }} x {{ d.quantity }}</span>
          </div>
          <span class="di-amount">￥{{ Number(d.amount).toFixed(2) }}</span>
        </div>
      </div>

      <div class="card">
        <div class="info-row"><span>订单金额</span><span class="val-amount">￥{{ Number(order.totalAmount).toFixed(2) }}</span></div>
        <div class="info-row"><span>下单时间</span><span>{{ formatTime(order.createTime) }}</span></div>
        <div class="info-row" v-if="order.payTime"><span>支付时间</span><span>{{ formatTime(order.payTime) }}</span></div>
        <div class="info-row" v-if="order.finishTime"><span>完成时间</span><span>{{ formatTime(order.finishTime) }}</span></div>
        <div class="info-row" v-if="order.remark"><span>备注</span><span>{{ order.remark }}</span></div>
        <div class="info-row" v-if="order.cancelReason"><span>取消原因</span><span class="val-danger">{{ order.cancelReason }}</span></div>
      </div>

      <!-- 用户端操作 -->
      <div class="actions" v-if="order.status === 0">
        <el-button type="primary" size="large" class="action-btn" @click="handlePay">去支付</el-button>
        <el-button type="danger" size="large" class="action-btn" plain @click="handleCancel">取消订单</el-button>
      </div>
      <!-- 待接单：用户等待商家确认 -->
      <div class="actions" v-if="order.status === 1">
        <div class="waiting-hint">商家正在确认订单，请耐心等待...</div>
      </div>
      <!-- 待配送：用户确认收货 -->
      <div class="actions" v-if="order.status === 2">
        <el-button type="primary" size="large" class="action-btn" @click="handleConfirm">确认收货</el-button>
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
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderDetail, cancelOrder, confirmOrder } from '@/api/order'
import { createPayment } from '@/api/pay'
import PayDialog from '@/components/PayDialog.vue'
import placeholderImg from '@/assets/logo-icon.png'

const route = useRoute()
const order = ref(null)

const statusIcon = computed(() => ({ 0: '⏳', 1: '🛎️', 2: '🚀', 3: '🎉', 4: '❌' }[order.value?.status] || '📋'))

// 手机号脱敏：138****1234
const maskedPhone = computed(() => {
  const p = order.value?.phone
  if (!p) return ''
  return p.slice(0, 3) + '****' + p.slice(-4)
})

// 支付弹窗状态
const payVisible = ref(false)
const payOrderId = ref(null)
const payOrderNo = ref('')
const payAmount = ref('')
const payQrCode = ref('')
const payStatus = ref('WAIT_PAY')
const payMessage = ref('')
const payWindowRef = ref(null)

const reload = async () => {
  try {
    order.value = await getOrderDetail(route.params.id)
  } catch (e) {
    console.error('加载订单详情失败:', e)
  }
}

onMounted(reload)

const handlePay = async () => {
  if (!order.value) return
  try {
    const res = await createPayment(order.value.id)
    payOrderId.value = order.value.id
    payOrderNo.value = res?.orderNo || order.value.orderNo
    payAmount.value = res?.amount || order.value.totalAmount
    payQrCode.value = res?.qrCode || ''
    payStatus.value = res?.status || 'WAIT_PAY'
    payMessage.value = res?.message || ''

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
  reload()
}

const handleCancel = async () => {
  try {
    await ElMessageBox.confirm('确定取消该订单？', '取消订单', { type: 'warning' })
    await cancelOrder(order.value.id, '用户取消')
    ElMessage.success('已取消')
    await reload()
  } catch (e) {
    if (e !== 'cancel') console.error('取消订单失败:', e)
  }
}

const handleConfirm = async () => {
  try {
    await confirmOrder(order.value.id)
    ElMessage.success('已确认收货')
    await reload()
  } catch (e) {
    console.error('确认收货失败:', e)
  }
}

const formatTime = (t) => t ? t.slice(0, 16).replace('T', ' ') : ''
</script>

<style scoped>
.orderdetail-page { --bg: #f8fafc; min-height: 100vh; background: var(--bg); padding-bottom: 40px; font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif; }
.page-header { position: relative; padding: 18px 20px 26px; overflow: hidden; }
.header-bg { position: absolute; inset: 0; background: linear-gradient(135deg, var(--color-primary) 0%, #6b9dff 100%); border-radius: 0 0 24px 24px; }
.header-nav { position: relative; z-index: 2; margin-bottom: 8px; }
.back-btn { color: rgba(255,255,255,.9) !important; }
.page-title { position: relative; z-index: 2; font-size: 22px; font-weight: 700; color: #fff; margin: 0; }
.content-wrap { padding: 0 16px; margin-top: 8px; }
.status-card { background: #fff; border-radius: 14px; padding: 24px; text-align: center; margin-bottom: 12px; box-shadow: 0 1px 4px rgba(0,0,0,.04); }
.status-icon { font-size: 40px; margin-bottom: 8px; }
.status-text { font-size: 18px; font-weight: 700; color: var(--color-primary); margin-bottom: 4px; }
.status-no { font-size: 12px; color: var(--text-placeholder); font-family: monospace; }
.card { background: #fff; border-radius: 14px; padding: 16px; margin-bottom: 12px; box-shadow: 0 1px 4px rgba(0,0,0,.04); }
.card-title { font-size: 15px; font-weight: 600; margin-bottom: 12px; }
.detail-item { display: flex; align-items: center; gap: 10px; padding: 8px 0; border-bottom: 1px solid #f0eeeb; }
.detail-item:last-child { border-bottom: none; }
.di-img { width: 48px; height: 48px; border-radius: 8px; object-fit: cover; flex-shrink: 0; }
.di-info { flex: 1; font-size: 13px; display: flex; flex-direction: column; }
.di-price { font-size: 12px; color: var(--text-secondary); }
.di-spec { font-size: 11px; color: var(--color-primary); background: rgba(58,122,254,0.08); padding: 1px 8px; border-radius: 4px; width: fit-content; }
.di-amount { font-size: 15px; font-weight: 700; color: var(--color-accent); }
.info-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #f0eeeb; font-size: 14px; color: var(--text-secondary); }
.info-row:last-child { border-bottom: none; }
.info-row span:last-child { color: var(--text-main); font-weight: 500; }
.val-amount { font-size: 18px; font-weight: 700; color: var(--color-accent) !important; }
.val-danger { color: var(--color-danger) !important; }
.actions { margin-top: 16px; }
.action-btn { width: 100%; height: 48px; border-radius: 14px; font-size: 16px; font-weight: 600; }
.waiting-hint { text-align: center; padding: 16px; background: #f0f4ff; border-radius: 12px; color: var(--color-primary); font-size: 14px; }
</style>
