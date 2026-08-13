<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    :close-on-click-modal="false"
    width="420px"
    class="pay-dialog"
    destroy-on-close
  >
    <template #header>
      <div class="pay-header">
        <svg class="alipay-logo" viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg">
          <path d="M1024 512c0 282.77-229.23 512-512 512S0 794.77 0 512 229.23 0 512 0s512 229.23 512 512z" fill="#1677FF"/>
          <path d="M245.3 384.2h167.3c55.2 0 99.8 16.7 133.4 49.9 33.5 33.3 50.4 76 50.4 128.1 0 37.8-10.5 69.7-31.5 95.7-21 26-50.4 42.2-88.2 48.4l135.5 177.8H509.3L382.9 713H291.5v170.9H245.3V384.2zm46.2 46.2V667h121.7c36.7 0 65-11 84.8-33 19.8-22 29.7-50.5 29.7-85.5 0-30.3-8.5-54.8-25.5-73.5-17-18.7-44.4-28.1-82.1-28.1H291.5v-16.5z" fill="#FFF"/>
        </svg>
        <div class="pay-title">支付宝支付</div>
        <div class="pay-amount">¥{{ amount }}</div>
      </div>
    </template>

    <!-- 等待支付 -->
    <div class="pay-body" v-if="internalStatus === 'WAIT_PAY'">
      <!-- 弹窗已打开 -->
      <div v-if="popupOpened" class="popup-status">
        <svg viewBox="0 0 48 48" width="40" height="40"><circle cx="24" cy="24" r="23" fill="#1677FF" opacity="0.15"/><path d="M16 24l6 6 10-12" stroke="#1677FF" stroke-width="3" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>
        <h4>支付窗口已打开</h4>
        <p class="hint">请在支付宝新窗口中完成支付<br/>支付完成后将自动跳转到订单页面</p>
        <el-button v-if="payWindow?.closed" type="primary" round size="small" @click="openPayWindow">
          重新打开支付页面
        </el-button>
        <div class="polling-bar">
          <el-icon class="polling-dot"><Loading /></el-icon>
          <span>正在等待支付结果...</span>
        </div>
      </div>

      <!-- 弹窗被拦截 -->
      <div v-else-if="popupBlocked" class="popup-blocked">
        <div class="blocked-icon">
          <svg viewBox="0 0 48 48" width="48" height="48"><circle cx="24" cy="24" r="23" fill="#f97316" opacity="0.15"/><path d="M24 16v12M24 32h.02" stroke="#f97316" stroke-width="3" fill="none" stroke-linecap="round"/></svg>
        </div>
        <h4>弹窗被浏览器拦截</h4>
        <p class="hint">请点击下方按钮手动打开支付宝支付页面</p>
        <el-button type="primary" size="large" round @click="openPayWindow">
          打开支付宝支付页面
        </el-button>
      </div>

      <!-- 加载中 -->
      <div v-else class="loading-wrap">
        <el-icon class="loading-icon" :size="36"><Loading /></el-icon>
        <p>正在生成支付订单...</p>
      </div>

      <div class="pay-actions">
        <el-button text type="primary" size="small" @click="handleCancel">取消支付</el-button>
      </div>
    </div>

    <!-- 支付成功 -->
    <div class="pay-body pay-success" v-else-if="internalStatus === 'SUCCESS'">
      <div class="success-icon">
        <svg viewBox="0 0 48 48" width="48" height="48"><circle cx="24" cy="24" r="23" fill="#10b981" stroke="#059669" stroke-width="2"/><path d="M14 24l7 7 13-13" stroke="#fff" stroke-width="3" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>
      </div>
      <h3>支付成功</h3>
      <p>订单 {{ orderNo }} 已成功支付</p>
    </div>

    <!-- 支付失败 -->
    <div class="pay-body pay-fail" v-else-if="internalStatus === 'FAIL'">
      <div class="fail-icon">
        <svg viewBox="0 0 48 48" width="48" height="48"><circle cx="24" cy="24" r="23" fill="#ef4444" stroke="#dc2626" stroke-width="2"/><path d="M16 16l16 16M32 16L16 32" stroke="#fff" stroke-width="3" fill="none" stroke-linecap="round"/></svg>
      </div>
      <h3>支付失败</h3>
      <p>{{ failedMsg }}</p>
      <el-button type="primary" round @click="$emit('retry')">重新支付</el-button>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, watch, nextTick, onBeforeUnmount } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { queryPayment } from '@/api/pay'

const props = defineProps({
  visible: Boolean,
  orderId: [Number, String],
  orderNo: String,
  amount: [Number, String],
  qrCode: String,
  status: { type: String, default: 'WAIT_PAY' },
  message: String,
  payWindow: { type: Object, default: null },   // 父组件预打开的窗口引用
})

const emit = defineEmits(['update:visible', 'success', 'retry', 'cancel'])

const internalStatus = ref(props.status)
const failedMsg = ref('')
const payWindow = ref(null)
const popupOpened = ref(false)
const popupBlocked = ref(false)
let pollTimer = null
let pollCount = 0
const MAX_POLL = 100

/**
 * 弹窗打开/关闭时：
 * - 打开且有 HTML → 新窗口跳转支付宝 + 开始轮询
 * - 关闭 → 停轮询 + 关支付宝窗口
 */
watch(() => props.visible, async (v) => {
  if (v && props.qrCode) {
    await nextTick()
    // 父组件已打开窗口 → 直接使用，不再重复打开
    if (props.payWindow) {
      payWindow.value = props.payWindow
      popupOpened.value = true
      // 监控父组件窗口关闭
      const checkClosed = setInterval(() => {
        if (!payWindow.value || payWindow.value.closed) {
          clearInterval(checkClosed)
        }
      }, 2000)
    } else if (!payWindow.value) {
      openPayWindow()
    }
    startPolling()
  }
  if (!v) {
    closeAll()
  }
})

watch(() => props.status, v => { internalStatus.value = v })

/**
 * 新建窗口打开支付宝支付页面
 * 用 Blob URL 承载 Alipay SDK 返回的 HTML，避免 iframe 被 X-Frame-Options 拦截
 * 弹窗被浏览器拦截时显示手动打开按钮
 */
const openPayWindow = () => {
  const html = props.qrCode
  if (!html) {
    popupOpened.value = false
    return
  }

  // 关闭旧窗口
  if (payWindow.value && !payWindow.value.closed) {
    payWindow.value.close()
  }

  const blob = new Blob([html], { type: 'text/html;charset=utf-8' })
  const url = URL.createObjectURL(blob)

  payWindow.value = window.open(url, '_blank')

  if (payWindow.value) {
    popupOpened.value = true
    popupBlocked.value = false
    // 定期检测窗口是否被用户关闭 + 清理 Blob URL
    const checkClosed = setInterval(() => {
      if (!payWindow.value || payWindow.value.closed) {
        clearInterval(checkClosed)
        URL.revokeObjectURL(url)
      }
    }, 2000)
  } else {
    popupBlocked.value = true
    popupOpened.value = false
    URL.revokeObjectURL(url)
  }
}

/**
 * 轮询支付状态：每3秒查一次，最多100次（5分钟）
 * 支付成功后延迟600ms触发回调，让用户看到成功动画
 */
const startPolling = () => {
  if (pollTimer || !props.orderId) return
  pollCount = 0
  pollTimer = setInterval(async () => {
    pollCount++
    if (pollCount > MAX_POLL) {
      stopPolling()
      internalStatus.value = 'FAIL'
      failedMsg.value = '支付超时，请重新下单'
      return
    }
    try {
      const res = await queryPayment(props.orderId)
      if (res?.status === 'SUCCESS') {
        stopPolling()
        internalStatus.value = 'SUCCESS'
        if (payWindow.value && !payWindow.value.closed) payWindow.value.close()
        setTimeout(() => emit('success'), 600)
      } else if (res?.status === 'FAIL') {
        stopPolling()
        internalStatus.value = 'FAIL'
        failedMsg.value = res?.message || '支付失败'
      }
    } catch { /* 网络波动忽略，继续轮询 */ }
  }, 3000)
}

const stopPolling = () => {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}

const handleCancel = () => {
  closeAll()
  emit('update:visible', false)
  emit('cancel')
}

const closeAll = () => {
  stopPolling()
  if (payWindow.value && !payWindow.value.closed) {
    payWindow.value.close()
  }
  popupOpened.value = false
  popupBlocked.value = false
}

onBeforeUnmount(closeAll)
</script>

<style scoped>
.pay-dialog :deep(.el-dialog__body) { padding: 0; }
.pay-header { text-align: center; padding: 24px 20px 16px; }
.alipay-logo { width: 40px; height: 40px; margin-bottom: 6px; }
.pay-title { font-size: 18px; font-weight: 700; color: #1e293b; margin-bottom: 8px; }
.pay-amount { font-size: 32px; font-weight: 800; color: #f97316; letter-spacing: -1px; }
.pay-body { text-align: center; padding: 0 20px 24px; }

/* ── 已打开弹窗 ── */
.popup-status { padding: 8px 0 12px; }
.popup-status h4 { color: #1677FF; margin: 8px 0 4px; font-size: 16px; }
.popup-status .hint { color: #64748b; font-size: 13px; margin-bottom: 12px; line-height: 1.6; }
.polling-bar { display: flex; align-items: center; justify-content: center; gap: 6px; color: #94a3b8; font-size: 13px; margin-top: 8px; }
.polling-dot { animation: spin 1.2s linear infinite; }

/* ── 弹窗被拦截 ── */
.popup-blocked { padding: 12px 0 16px; }
.blocked-icon { margin-bottom: 8px; }
.popup-blocked h4 { color: #f97316; margin: 4px 0; font-size: 16px; }
.popup-blocked .hint { color: #64748b; font-size: 13px; margin-bottom: 16px; }

/* ── 加载中 ── */
.loading-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 0;
  color: #94a3b8;
  gap: 8px;
}
.loading-icon { animation: spin 1.2s linear infinite; color: #1677FF; }
@keyframes spin { to { transform: rotate(360deg); } }

/* ── 底部操作 ── */
.pay-actions { display: flex; justify-content: center; margin-top: 8px; }

/* ── 成功/失败 ── */
.pay-success, .pay-fail { padding: 16px 20px 24px; }
.success-icon, .fail-icon { margin-bottom: 8px; }
.pay-success h3 { color: #10b981; margin: 0 0 4px; }
.pay-fail h3 { color: #ef4444; margin: 0 0 4px; }
.pay-fail p { color: #64748b; margin-bottom: 16px; }
</style>
