<template>
  <div class="pay-success-page">
    <div class="header-wrap">
      <div class="header-bg"></div>
      <div class="header-inner">
        <h2 class="title">支付结果</h2>
        <p class="subtitle">正在确认支付状态...</p>
      </div>
    </div>

    <div class="content">
      <div class="result-card" v-if="loading">
        <el-icon class="loading-icon" :size="48"><Loading /></el-icon>
        <p class="loading-text">正在确认支付结果...</p>
      </div>

      <div class="result-card success" v-else-if="status === 'SUCCESS'">
        <div class="icon-wrap">
          <el-icon :size="56"><CircleCheckFilled /></el-icon>
        </div>
        <h3 class="result-title">支付成功</h3>
        <p class="result-desc" v-if="payOrderNo">订单号: {{ payOrderNo }}</p>
        <p class="result-desc" v-if="amount">金额: ￥{{ Number(amount).toFixed(2) }}</p>
        <div class="actions">
          <el-button type="primary" round @click="goOrder">查看订单</el-button>
          <el-button round @click="goHome">返回首页</el-button>
        </div>
      </div>

      <div class="result-card fail" v-else>
        <div class="icon-wrap">
          <el-icon :size="56"><WarningFilled /></el-icon>
        </div>
        <h3 class="result-title">支付未完成</h3>
        <p class="result-desc">请在订单列表中查看并完成支付</p>
        <div class="actions">
          <el-button type="primary" round @click="goOrder">查看订单</el-button>
          <el-button round @click="goHome">返回首页</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { queryPaymentByOrderNo } from '@/api/pay'
import { Loading, CircleCheckFilled, WarningFilled } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const loading = ref(true)
const status = ref('')
const payOrderNo = ref('')
const amount = ref('')

const goOrder = () => router.push('/order')
const goHome = () => router.push('/home')

onMounted(async () => {
  const orderNo = route.query.out_trade_no
  if (orderNo) {
    try {
      // 轮询最多 10 次，每次间隔 2 秒
      for (let i = 0; i < 10; i++) {
        const res = await queryPaymentByOrderNo(orderNo)
        if (res?.status === 'SUCCESS') {
          status.value = 'SUCCESS'
          payOrderNo.value = res.orderNo
          amount.value = res.amount
          loading.value = false
          return
        }
        await new Promise(r => setTimeout(r, 2000))
      }
    } catch (e) {
      console.error('Query payment failed:', e)
    }
  }
  loading.value = false
})
</script>

<style scoped>
.pay-success-page {
  min-height: 100vh;
  background: #f7f8fa;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif;
}

.header-wrap {
  position: relative;
  padding: 30px 20px 24px;
  overflow: hidden;
}

.header-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #0097ff 0%, #5bc1ff 100%);
  border-radius: 0 0 24px 24px;
}

.header-inner {
  position: relative;
  z-index: 1;
}

.title {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 4px;
}

.subtitle {
  font-size: 13px;
  color: rgba(255,255,255,0.78);
  margin: 0;
}

.content {
  padding: 20px 16px;
}

.result-card {
  background: #fff;
  border-radius: 16px;
  padding: 40px 24px;
  text-align: center;
  box-shadow: 0 1px 6px rgba(0,0,0,0.04);
}

.icon-wrap {
  color: #07c160;
  margin-bottom: 12px;
}

.result-card.fail .icon-wrap {
  color: #ff976a;
}

.result-title {
  font-size: 20px;
  font-weight: 700;
  color: #333;
  margin: 0 0 8px;
}

.result-desc {
  font-size: 14px;
  color: #999;
  margin: 0 0 4px;
}

.actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 24px;
}

.loading-icon {
  color: #0097ff;
  animation: spin 1s linear infinite;
  margin-bottom: 12px;
}

.loading-text {
  font-size: 15px;
  color: #999;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
