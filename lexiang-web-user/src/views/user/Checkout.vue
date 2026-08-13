<template>
  <div class="checkout-page">
    <!-- Header -->
    <div class="page-header">
      <div class="header-bg"></div>
      <div class="header-top">
        <el-button text class="back-btn" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回</span>
        </el-button>
      </div>
      <h2 class="page-title">确认下单</h2>
      <p class="page-subtitle">请核对信息后提交</p>
    </div>

    <div class="content-wrap">
      <!-- 加载骨架屏 -->
      <template v-if="pageLoading">
        <div class="card sk-card" v-for="s in 3" :key="s">
          <div class="sk-line sk-w40"></div>
          <div class="sk-line sk-w80" v-for="i in 2" :key="i"></div>
        </div>
      </template>

      <template v-else>
        <!-- 商品清单 -->
        <section class="card items-card">
          <div class="card-label">
            <span class="label-icon">🛒</span>
            <span>商品清单</span>
            <span class="label-extra">{{ cartList.length }}件</span>
          </div>

          <div class="item-row" v-for="item in cartList" :key="item.cartId">
            <img
                :src="item.dishImage || placeholderImg"
                :alt="item.dishName"
                class="item-thumb"
                @error="(e) => e.target.src = placeholderImg"
            />
            <div class="item-info">
              <span class="item-name">{{ item.dishName }}</span>
              <span class="item-spec" v-if="item.spec">{{ item.spec }}</span>
            </div>
            <div class="item-right">
              <span class="item-qty">x{{ item.quantity }}</span>
              <span class="item-price">￥{{ Number(item.amount).toFixed(2) }}</span>
            </div>
          </div>

          <el-empty
              v-if="!cartList.length && !pageLoading"
              description="购物车为空"
              :image-size="60"
          >
            <el-button type="primary" plain size="small" @click="$router.push('/home')">
              去选购
            </el-button>
          </el-empty>
        </section>

        <!-- 费用明细 -->
        <section class="card bill-card" v-if="cartList.length">
          <div class="card-label">
            <span class="label-icon">📋</span>
            <span>费用明细</span>
          </div>
          <div class="bill-row">
            <span>商品小计</span>
            <span>￥{{ subtotal }}</span>
          </div>
          <div class="bill-row">
            <span>配送费</span>
            <span :class="{ free: deliveryFee === 0 }">
              {{ deliveryFee === 0 ? '免配送费' : '￥' + deliveryFee.toFixed(2) }}
            </span>
          </div>
          <div class="bill-row" v-if="packingFee > 0">
            <span>打包费</span>
            <span>￥{{ packingFee.toFixed(2) }}</span>
          </div>
          <div class="bill-divider"></div>
          <div class="bill-row bill-total">
            <span>合计</span>
            <span class="total-amount">￥{{ total }}</span>
          </div>
        </section>

        <!-- 收货地址 -->
        <section class="card addr-card">
          <div class="card-label">
            <span class="label-icon">📍</span>
            <span>收货地址</span>
            <el-button link type="primary" size="small" class="add-addr-btn" @click="goAddAddr">
              + 新增
            </el-button>
          </div>

          <el-radio-group v-model="selectedAddr" class="addr-group" v-if="addresses.length">
            <div
                class="addr-item"
                v-for="a in addresses"
                :key="a.id"
                :class="{ active: selectedAddr === a.id }"
                @click="selectedAddr = a.id"
            >
              <el-radio :value="a.id" class="addr-radio">
                <div class="addr-top">
                  <span class="addr-name">{{ a.contactName }}</span>
                  <span class="addr-phone">{{ a.contactPhone }}</span>
                  <el-tag
                      v-if="a.isDefault"
                      size="small"
                      type="primary"
                      class="addr-tag"
                  >
                    默认
                  </el-tag>
                  <el-tag
                      v-else-if="a.tag"
                      size="small"
                      :type="tagType(a.tag)"
                      class="addr-tag"
                  >
                    {{ a.tag }}
                  </el-tag>
                </div>
                <div class="addr-detail">{{ a.addressDetail }}</div>
              </el-radio>
            </div>
          </el-radio-group>

          <div v-else class="empty-addr">
            <div class="empty-addr-icon">🏠</div>
            <p>请先添加收货地址</p>
            <el-button type="primary" plain round size="small" @click="goAddAddr">
              添加地址
            </el-button>
          </div>
        </section>

        <!-- 支付方式（支付宝） -->
        <section class="card pay-card">
          <div class="card-label">
            <span class="label-icon">💳</span>
            <span>支付方式</span>
          </div>
          <div class="pay-alipay-only">
            <svg class="alipay-logo-sm" viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg">
              <path d="M1024 512c0 282.77-229.23 512-512 512S0 794.77 0 512 229.23 0 512 0s512 229.23 512 512z" fill="#1677FF"/>
              <path d="M245.3 384.2h167.3c55.2 0 99.8 16.7 133.4 49.9 33.5 33.3 50.4 76 50.4 128.1 0 37.8-10.5 69.7-31.5 95.7-21 26-50.4 42.2-88.2 48.4l135.5 177.8H509.3L382.9 713H291.5v170.9H245.3V384.2zm46.2 46.2V667h121.7c36.7 0 65-11 84.8-33 19.8-22 29.7-50.5 29.7-85.5 0-30.3-8.5-54.8-25.5-73.5-17-18.7-44.4-28.1-82.1-28.1H291.5v-16.5z" fill="#FFF"/>
            </svg>
            <span class="pay-method-name">支付宝</span>
          </div>
        </section>

        <!-- 备注 -->
        <section class="card remark-card">
          <div class="card-label">
            <span class="label-icon">✏️</span>
            <span>订单备注</span>
            <span class="label-optional">选填</span>
          </div>
          <el-input
              v-model="remark"
              type="textarea"
              :rows="2"
              :maxlength="100"
              show-word-limit
              placeholder="如有口味偏好、过敏信息等，请在此备注"
              class="remark-input"
          />
        </section>

        <!-- 预计送达 -->
        <section class="card time-card">
          <div class="card-label">
            <span class="label-icon">⏱️</span>
            <span>预计送达</span>
          </div>
          <span class="time-value">约 {{ estimatedMinutes }} 分钟送达</span>
        </section>
      </template>
    </div>

    <!-- 底部提交栏 -->
    <div class="submit-bar" ref="submitBarRef" v-if="!pageLoading">
      <div class="submit-left">
        <div class="submit-total">
          <span class="submit-total-label">合计</span>
          <span class="submit-total-price"><small>￥</small>{{ total }}</span>
        </div>
        <span class="submit-brief" v-if="cartList.length">
          共{{ cartList.length }}件商品
        </span>
      </div>
      <el-button
          type="primary"
          size="large"
          class="submit-btn"
          :loading="submitting"
          :disabled="!canSubmit"
          @click="handleSubmit"
      >
        {{ submitting ? '提交中...' : '提交订单' }}
      </el-button>
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
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCartList } from '@/api/cart'
import { getAddressList } from '@/api/address'
import { submitOrder, getOrderToken } from '@/api/order'
import { createPayment } from '@/api/pay'
import PayDialog from '@/components/PayDialog.vue'
import placeholderImg from '@/assets/logo-icon.png'

const router = useRouter()
const route = useRoute()

const goBack = () => {
  if (window.history.length > 1) { router.back() }
  else { router.push('/home') }
}

/* ── 状态 ── */
const cartList = ref([])
const addresses = ref([])
const selectedAddr = ref(null)
const remark = ref('')
const pageLoading = ref(true)
const submitting = ref(false)
const submitBarRef = ref(null)
const submitBarHeight = ref(72)
const estimatedMinutes = ref(30)

// Payment state
const payVisible = ref(false)
const payOrderId = ref(null)
const payOrderNo = ref('')
const payAmount = ref('')
const payQrCode = ref('')
const payStatus = ref('WAIT_PAY')
const payMessage = ref('')
const payWindowRef = ref(null)      // 支付窗口引用（在点击上下文中打开）

/* ── 费用计算 ── */
const subtotal = computed(() =>
    cartList.value.reduce((s, i) => s + Number(i.amount || 0), 0)
)

const deliveryFee = computed(() => (subtotal.value >= 20 ? 0 : 2))
const packingFee = ref(0)

const total = computed(() =>
    (subtotal.value + deliveryFee.value + packingFee.value).toFixed(2)
)

const canSubmit = computed(
    () => !!selectedAddr.value && cartList.value.length > 0 && !submitting.value
)

/* ── 底部栏高度自适应 ── */
const updateBarHeight = () => {
  if (submitBarRef.value) {
    submitBarHeight.value = submitBarRef.value.offsetHeight
  }
}

/* ── 数据加载 ── */
const loadData = async () => {
  pageLoading.value = true
  try {
    const [c, a] = await Promise.all([
      getCartList().catch(() => []),
      getAddressList().catch(() => []),
    ])
    cartList.value = (c || []).map(item => ({ ...item, spec: item.specInfo }))
    addresses.value = a || []

    // 优先选默认地址，其次选第一个
    if (a?.length) {
      const def = a.find(x => x.isDefault)
      selectedAddr.value = def ? def.id : a[0].id
    }
  } catch {
    ElMessage.error('数据加载失败，请刷新重试')
  } finally {
    pageLoading.value = false
    await nextTick()
    updateBarHeight()
  }
}

watch(
  () => route.path,
  (p, oldPath) => { if (p === '/checkout' && oldPath !== '/checkout') loadData() }
)

onMounted(() => {
  loadData()
  window.addEventListener('resize', updateBarHeight)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateBarHeight)
})

/* ── 工具 ── */
const tagType = (tag) => {
  const map = { '家': 'danger', '公司': '', '学校': 'success' }
  return map[tag] ?? 'warning'
}

const goAddAddr = () => {
  router.push({ path: '/address', query: { from: 'checkout' } })
}

/* ── 提交订单 ── */
const handleSubmit = async () => {
  if (!canSubmit.value) return
  submitting.value = true

  let createdOrderId = null  // 记录已创建的订单ID，区分「下单失败」和「支付失败」
  let paymentWindowReady = false

  // ★ 关键修复：在用户点击事件的【同步】上下文里先打开空白窗口，
  // 后续异步拿到支付宝 HTML 后再写入。若等到 await 之后才 window.open，
  // 浏览器会判定为非用户手势而拦截弹窗——这正是「弹不出支付」的根因。
  payWindowRef.value = window.open('', '_blank')
  if (payWindowRef.value) {
    payWindowRef.value.document.write(
      '<html><body style="margin:0;display:flex;align-items:center;justify-content:center;height:100vh;font-family:sans-serif"><p style="color:#999">正在跳转支付宝支付…</p></body></html>'
    )
  }

  try {
    // 下单前获取幂等 Token
    const orderToken = await getOrderToken()

    const res = await submitOrder(
      { addressId: selectedAddr.value, remark: remark.value },
      { 'X-Order-Token': orderToken }
    )
    createdOrderId = res?.id || res?.data?.id
    if (!createdOrderId) { ElMessage.error('订单创建失败'); return }

    // ★ 订单已创建，调用支付宝支付
    const payRes = await createPayment(createdOrderId)
    payOrderId.value = createdOrderId
    payOrderNo.value = payRes?.orderNo || ''
    payAmount.value = payRes?.amount || total.value
    payQrCode.value = payRes?.qrCode || ''
    payStatus.value = payRes?.status || 'WAIT_PAY'
    payMessage.value = payRes?.message || ''

    // ★ 把支付宝跳转 HTML 写入已预打开的窗口
    if (payRes?.qrCode) {
      if (payWindowRef.value && !payWindowRef.value.closed) {
        payWindowRef.value.document.write(payRes.qrCode)
        payWindowRef.value.document.close()
        paymentWindowReady = true
      } else {
        // 兜底：预打开被拦截或已被关闭 → 用 Blob URL 再试一次
        const blob = new Blob([payRes.qrCode], { type: 'text/html;charset=utf-8' })
        const url = URL.createObjectURL(blob)
        payWindowRef.value = window.open(url, '_blank')
        if (payWindowRef.value) {
          paymentWindowReady = true
        } else {
          ElMessage.warning('弹窗被浏览器拦截，请在弹窗中手动打开支付页面')
        }
      }
    }

    payVisible.value = true
  } catch (e) {
    const errMsg = e?.message || '下单失败，请重试'

    // ★ 未支付订单拦截：弹出温馨提示弹窗，引导用户处理已有待支付订单
    if (errMsg.includes('未支付订单')) {
      ElMessageBox.confirm(
        '<div style="text-align:center;padding:8px 0">' +
        '<p style="font-size:15px;color:#e6a23c;font-weight:600;margin:0 0 8px">⚠ 你尚有未支付订单</p>' +
        '<p style="font-size:13px;color:#666;margin:0;line-height:1.6">' +
        '每个用户同时只能有<strong>1笔待支付订单</strong>，<br/>' +
        '请先前往订单列表完成支付或取消后再下单。</p>' +
        '</div>',
        '温馨提示',
        {
          confirmButtonText: '去处理',
          cancelButtonText: '稍后再说',
          type: 'warning',
          dangerouslyUseHTMLString: true,
          distinguishCancelAndClose: true,
          closeOnClickModal: false,
        }
      ).then(() => {
        // 用户点击「去处理」→ 跳转到订单列表页
        router.push('/order')
      }).catch((action) => {
        // 用户点击「稍后再说」或关闭弹窗 → 留在当前页
        if (action === 'cancel') {
          // do nothing
        }
      })
      return
    }

    // ★ 区分：订单已创建但支付失败 vs 下单本身失败
    if (createdOrderId) {
      // 订单已创建但支付拉起失败 → 提示用户去订单列表完成支付
      ElMessage.warning('订单已生成，支付服务暂时异常，请在订单列表中完成支付')
    } else {
      // 下单本身失败 → 显示具体错误信息
      ElMessage.error(errMsg)
    }
  } finally {
    submitting.value = false
    // 支付窗口未派上用场（下单或支付失败）时，关闭残留的空白窗口
    if (!paymentWindowReady && payWindowRef.value && !payWindowRef.value.closed) {
      payWindowRef.value.close()
      payWindowRef.value = null
    }
  }
}

const onPaySuccess = () => {
  payVisible.value = false
  router.replace('/order')
}
</script>

<style scoped>
.checkout-page {
  --ck-primary: var(--color-primary, #0097ff);
  --ck-accent: #ff6b35;
  --ck-bg: #f7f8fa;
  --ck-card: #ffffff;
  --ck-text: #333333;
  --ck-text-sub: #999999;
  --ck-border: #eeeeee;
  --ck-radius: 14px;
  --ck-success: #34c759;

  min-height: 100vh;
  background: var(--ck-bg);
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Helvetica Neue', sans-serif;
  /* 底部避让：submitBar 实际高度 + Tab 栏（隐藏页无 Tab，留 0 即可） + safe-area */
  padding-bottom: calc(v-bind(submitBarHeight) * 1px + 20px + env(safe-area-inset-bottom, 0px));
}

/* ── Header ── */
.page-header {
  position: relative;
  padding: 18px 20px 26px;
  overflow: hidden;
}

.header-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, var(--ck-primary) 0%, #5bc1ff 100%);
  border-radius: 0 0 24px 24px;
}

.header-top {
  position: relative;
  z-index: 2;
  margin-bottom: 10px;
}

.back-btn {
  color: rgba(255, 255, 255, 0.9) !important;
  font-size: 13px;
}

.back-btn:hover {
  color: #fff !important;
}

.page-title {
  position: relative;
  z-index: 2;
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 2px;
}

.page-subtitle {
  position: relative;
  z-index: 2;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.75);
  margin: 0;
}

/* ── Content ── */
.content-wrap {
  padding: 0 16px;
  margin-top: 8px;
}

/* ── Card ── */
.card {
  background: var(--ck-card);
  border-radius: var(--ck-radius);
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.card-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 600;
  color: var(--ck-text);
  margin-bottom: 12px;
}

.label-icon {
  font-size: 15px;
}

.label-extra {
  margin-left: auto;
  font-size: 12px;
  font-weight: 400;
  color: var(--ck-text-sub);
}

.label-optional {
  font-size: 11px;
  font-weight: 400;
  color: var(--ck-text-sub);
  background: #f5f5f5;
  padding: 1px 6px;
  border-radius: 4px;
  margin-left: 4px;
}

/* ── Item Row ── */
.item-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid var(--ck-border);
}

.item-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.item-row:first-of-type {
  padding-top: 0;
}

.item-thumb {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
}

.item-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.item-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--ck-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-spec {
  font-size: 11px;
  color: var(--ck-text-sub);
}

.item-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 3px;
  flex-shrink: 0;
}

.item-qty {
  font-size: 11px;
  color: var(--ck-text-sub);
  background: #f5f5f5;
  padding: 1px 6px;
  border-radius: 8px;
}

.item-price {
  font-size: 14px;
  font-weight: 600;
  color: var(--ck-accent);
}

/* ── Bill ── */
.bill-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: var(--ck-text-sub);
  padding: 5px 0;
}

.bill-row .free {
  color: var(--ck-success);
  font-weight: 500;
}

.bill-divider {
  height: 1px;
  background: var(--ck-border);
  margin: 8px 0;
}

.bill-total {
  font-size: 14px;
  font-weight: 500;
  color: var(--ck-text);
  padding-top: 6px;
}

.total-amount {
  font-size: 22px;
  font-weight: 800;
  color: var(--ck-accent);
  letter-spacing: -0.5px;
}

/* ── Address ── */
.add-addr-btn {
  margin-left: auto;
  font-size: 12px;
}

.addr-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.addr-item {
  padding: 12px;
  border: 2px solid var(--ck-border);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s ease;
  background: #fff;
}

.addr-item:hover {
  border-color: #c7d9ff;
}

.addr-item.active {
  border-color: var(--ck-primary);
  background: #f0f7ff;
}

.addr-radio {
  width: 100%;
}

.addr-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 3px;
}

.addr-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--ck-text);
}

.addr-phone {
  font-size: 13px;
  color: var(--ck-text-sub);
}

.addr-tag {
  transform: scale(0.85);
  transform-origin: left center;
}

.addr-detail {
  font-size: 13px;
  color: var(--ck-text-sub);
  line-height: 1.4;
}

.empty-addr {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0 4px;
  gap: 8px;
}

.empty-addr-icon {
  font-size: 32px;
  opacity: 0.6;
}

.empty-addr p {
  margin: 0;
  font-size: 13px;
  color: var(--ck-text-sub);
}

/* ── Remark ── */
.remark-input :deep(.el-textarea__inner) {
  border-radius: 10px;
  border-color: var(--ck-border);
  font-size: 13px;
  resize: none;
  transition: border-color 0.25s ease;
}

.remark-input :deep(.el-textarea__inner:focus) {
  border-color: var(--ck-primary);
  box-shadow: 0 0 0 3px rgba(0, 151, 255, 0.08);
}

/* ── Delivery Time ── */
.time-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--ck-primary);
  letter-spacing: -0.3px;
}

/* ── Submit Bar ── */
.submit-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  max-width: 480px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  padding-bottom: calc(12px + env(safe-area-inset-bottom, 0px));
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  z-index: 100;
}

.submit-left {
  display: flex;
  flex-direction: column;
}

.submit-total {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.submit-total-label {
  font-size: 12px;
  color: var(--ck-text-sub);
}

.submit-total-price {
  font-size: 24px;
  font-weight: 800;
  color: var(--ck-accent);
  letter-spacing: -1px;
}

.submit-total-price small {
  font-size: 14px;
  font-weight: 600;
}

.submit-brief {
  font-size: 11px;
  color: var(--ck-text-sub);
  margin-top: 1px;
}

.submit-btn {
  height: 48px;
  padding: 0 36px;
  border-radius: 24px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.5px;
  box-shadow: 0 4px 14px rgba(0, 151, 255, 0.3);
  transition: all 0.25s ease;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(0, 151, 255, 0.4);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0);
}

.submit-btn:disabled {
  background: #c0c0c0;
  border-color: #c0c0c0;
  box-shadow: none;
}

/* ── Skeleton ── */
.sk-card {
  min-height: 60px;
}

.sk-line {
  height: 14px;
  border-radius: 4px;
  margin-bottom: 10px;
  background: linear-gradient(90deg, #eee 25%, #e0e0e0 50%, #eee 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.sk-line:last-child {
  margin-bottom: 0;
}

.sk-w40 { width: 40%; }
.sk-w80 { width: 80%; }

@keyframes shimmer {
  0%   { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* 支付方式 */
/* 支付方式（v3.0 仅支付宝） */
.pay-alipay-only { display: flex; align-items: center; gap: 10px; padding: 12px 16px; border: 2px solid var(--ck-primary); border-radius: 12px; background: #f0f7ff; }
.alipay-logo-sm { width: 24px; height: 24px; flex-shrink: 0; }
.pay-method-name { font-size: 15px; font-weight: 600; color: var(--ck-text); }

/* ── Responsive ── */
@media (min-width: 640px) {
  .submit-bar {
    border-radius: 16px 16px 0 0;
  }
}
</style>