<template>
  <div class="cart-page">
    <!-- Header -->
    <div class="page-header">
      <div class="header-bg"></div>
      <div class="header-top">
        <el-button text class="back-btn" @click="$router.push('/home')">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回</span>
        </el-button>
        <el-button
            v-if="list.length"
            text
            class="clear-btn"
            @click="handleClearAll"
        >
          清空
        </el-button>
      </div>
      <h2 class="page-title">购物车</h2>
      <p class="page-count" v-if="list.length">{{ list.length }} 件商品</p>
    </div>

    <div class="content-wrap">
      <!-- 骨架屏 -->
      <template v-if="loading">
        <div class="sk-item" v-for="i in 3" :key="i">
          <div class="sk-check"></div>
          <div class="sk-img"></div>
          <div class="sk-body">
            <div class="sk-line sk-w60"></div>
            <div class="sk-line sk-w40"></div>
          </div>
        </div>
      </template>

      <template v-else-if="list.length">
        <!-- 全选栏 -->
        <div class="select-bar">
          <el-checkbox
              v-model="allSelected"
              :indeterminate="isIndeterminate"
              @change="handleSelectAll"
          >
            <span class="select-label">全选</span>
          </el-checkbox>
          <span class="selected-count" v-if="selectedCount > 0">
            已选 {{ selectedCount }} 件
          </span>
        </div>

        <!-- 商品列表 -->
        <TransitionGroup name="cart" tag="div" class="cart-list">
          <div
              class="cart-item"
              v-for="item in list"
              :key="item.cartId"
              :class="{ 'is-selected': selectedIds.has(item.cartId) }"
          >
            <!-- 单选 -->
            <el-checkbox
                v-model="item._selected"
                class="item-check"
                @change="onItemSelectChange"
            />

            <!-- 图片 -->
            <div class="item-img-wrap" @click="goDetail(item)">
              <img
                  :src="item.dishImage || placeholderImg"
                  :alt="item.dishName"
                  class="item-img"
                  loading="lazy"
                  @error="(e) => e.target.src = placeholderImg"
              />
            </div>

            <!-- 信息 -->
            <div class="item-info" @click="goDetail(item)">
              <div class="item-name">{{ item.dishName }}</div>
              <div class="item-spec" v-if="item.spec">{{ item.spec }}</div>
              <div class="item-meta">
                <span class="item-unit-price">
                  ￥{{ Number(item.price).toFixed(2) }}
                </span>
                <span class="item-line-total" v-if="item.quantity > 1">
                  小计 ￥{{ Number(item.amount).toFixed(2) }}
                </span>
              </div>
            </div>

            <!-- 数量控件 -->
            <div class="item-stepper">
              <el-button
                  circle
                  size="small"
                  class="step-btn step-minus"
                  :icon="Minus"
                  :loading="item._operating === 'minus'"
                  @click="changeQty(item, -1)"
              />
              <span class="qty-num">{{ item.quantity }}</span>
              <el-button
                  circle
                  size="small"
                  type="primary"
                  class="step-btn step-plus"
                  :icon="Plus"
                  :loading="item._operating === 'plus'"
                  @click="changeQty(item, 1)"
              />
            </div>
          </div>
        </TransitionGroup>

        <!-- 滑动删除提示 -->
        <p class="swipe-hint">左滑商品可快速删除</p>
      </template>

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <div class="empty-illustration">
          <div class="empty-cart-icon">🛒</div>
          <div class="empty-dots">
            <span></span><span></span><span></span>
          </div>
        </div>
        <p class="empty-title">购物车空空如也</p>
        <p class="empty-desc">快去选几道喜欢的菜品吧</p>
        <el-button type="primary" round class="empty-btn" @click="$router.push('/home')">
          去逛逛
        </el-button>
      </div>
    </div>

    <!-- 底部结算栏 -->
    <Transition name="bar">
      <div class="cart-footer" v-if="!loading && list.length" ref="footerRef">
        <div class="footer-left">
          <el-checkbox
              v-model="allSelected"
              :indeterminate="isIndeterminate"
              @change="handleSelectAll"
              class="footer-check"
          >
            全选
          </el-checkbox>
          <div class="footer-total">
            <span class="total-label">合计</span>
            <span class="total-amount">
              <small>￥</small>{{ selectedTotal }}
            </span>
          </div>
        </div>
        <el-button
            type="primary"
            size="large"
            class="checkout-btn"
            :disabled="selectedCount === 0"
            @click="handleCheckout"
        >
          去结算{{ selectedCount > 0 ? `(${selectedCount})` : '' }}
        </el-button>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Plus, Minus } from '@element-plus/icons-vue'
import { getCartList, updateCartQty, deleteCartItem, clearCart } from '@/api/cart'
import { getOrderList } from '@/api/order'
import placeholderImg from '@/assets/logo-icon.png'

const router = useRouter()

const list = ref([])
const loading = ref(true)
const footerRef = ref(null)
const footerHeight = ref(72)

/* ── 数据加载 ── */
const load = async () => {
  loading.value = true
  try {
    const data = (await getCartList()) || []
    // 给每条数据注入选中状态和操作锁
    list.value = data.map(item => ({
      ...item,
      spec: item.specInfo,  // 映射后端字段
      _selected: true,
      _operating: null,
    }))
  } catch (e) {
    console.error('购物车加载失败:', e)
    ElMessage.error('购物车加载失败，请刷新重试')
  } finally {
    loading.value = false
    await nextTick()
    updateFooterHeight()
  }
}

onMounted(() => {
  load()
  window.addEventListener('resize', updateFooterHeight)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateFooterHeight)
})

const updateFooterHeight = () => {
  if (footerRef.value) {
    footerHeight.value = footerRef.value.offsetHeight
  }
}

/* ── 选中状态计算 ── */
const selectedIds = computed(() =>
    new Set(list.value.filter(i => i._selected).map(i => i.cartId))
)

const selectedCount = computed(() => selectedIds.value.size)

const allSelected = computed({
  get: () => list.value.length > 0 && list.value.every(i => i._selected),
  set: () => {}, // 由 handleSelectAll 处理
})

const isIndeterminate = computed(() => {
  const cnt = selectedCount.value
  return cnt > 0 && cnt < list.value.length
})

const selectedTotal = computed(() =>
    list.value
        .filter(i => i._selected)
        .reduce((s, i) => s + Number(i.amount || 0), 0)
        .toFixed(2)
)

const handleSelectAll = (val) => {
  list.value.forEach(item => { item._selected = val })
}

const onItemSelectChange = () => {
  // el-checkbox 已通过 v-model 直接修改 item._selected，无需额外处理
}

/* ── 数量变更 ── */
const changeQty = async (item, delta) => {
  const newQty = item.quantity + delta

  // 减到 0 以下时确认删除
  if (newQty < 1) {
    try {
      await ElMessageBox.confirm('确定从购物车中移除该商品？', '移除确认', {
        confirmButtonText: '移除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger',
      })
      item._operating = 'minus'
      await deleteCartItem(item.cartId)
      ElMessage.success('已移除')
      await load()
    } catch (e) {
      if (e !== 'cancel' && e?.message) {
        ElMessage.error(e.message || '操作失败')
      }
    } finally {
      item._operating = null
    }
    return
  }

  // 正常加减
  const opKey = delta > 0 ? 'plus' : 'minus'
  item._operating = opKey
  try {
    await updateCartQty(item.cartId, newQty)
    await load()
  } catch (e) {
    ElMessage.error(e?.message || '数量更新失败')
  } finally {
    item._operating = null
  }
}

/* ── 清空购物车 ── */
const handleClearAll = async () => {
  try {
    await ElMessageBox.confirm('确定清空购物车？此操作不可撤销。', '清空确认', {
      confirmButtonText: '清空',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger',
    })
    await clearCart()
    ElMessage.success('购物车已清空')
    await load()
  } catch (e) {
    if (e !== 'cancel' && e?.message) {
      ElMessage.error('清空失败，请重试')
    }
  }
}

/* ── 跳转 ── */
const goDetail = (item) => {
  router.push(`/dish/${item.dishId || item.id}`)
}

const handleCheckout = async () => {
  if (!localStorage.getItem('token')) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (selectedCount.value === 0) {
    ElMessage.warning('请至少选择一件商品')
    return
  }

  // 检测是否有未支付订单
  try {
    const res = await getOrderList({ page: 1, pageSize: 1, status: 0 })
    if (res?.total > 0) {
      await ElMessageBox.confirm(
        '您有一笔未支付订单，是否前往支付？',
        '未支付提醒',
        {
          confirmButtonText: '去支付',
          cancelButtonText: '继续选购',
          type: 'warning',
        }
      )
      router.push('/order')
      return
    }
  } catch (e) {
    // 查询失败不阻塞结算流程（如网络异常）
  }

  router.push('/checkout')
}
</script>

<style scoped>
.cart-page {
  --cp-primary: var(--color-primary, #0097ff);
  --cp-accent: #ff6b35;
  --cp-bg: #f7f8fa;
  --cp-card: #ffffff;
  --cp-text: #333333;
  --cp-text-sub: #999999;
  --cp-border: #eeeeee;
  --cp-radius: 14px;
  --cp-danger: #f56c6c;

  min-height: 100vh;
  background: var(--cp-bg);
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Helvetica Neue', sans-serif;
  /* 底部避让：TabBar + 结算栏 */
  padding-bottom: calc(v-bind(footerHeight) * 1px + 60px + env(safe-area-inset-bottom, 0px) + 16px);
}

/* ── Header ── */
.page-header {
  position: relative;
  padding: 18px 20px 24px;
  overflow: hidden;
}

.header-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, var(--cp-primary) 0%, #5bc1ff 100%);
  border-radius: 0 0 24px 24px;
}

.header-top {
  position: relative;
  z-index: 2;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.back-btn,
.clear-btn {
  color: rgba(255, 255, 255, 0.9) !important;
  font-size: 13px;
}

.back-btn:hover,
.clear-btn:hover {
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

.page-count {
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

/* ── Select Bar ── */
.select-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 4px 12px;
}

.select-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--cp-text);
}

.selected-count {
  font-size: 12px;
  color: var(--cp-primary);
  font-weight: 500;
}

/* ── Cart List ── */
.cart-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.cart-item {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--cp-card);
  border-radius: var(--cp-radius);
  padding: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  border: 1.5px solid transparent;
  transition: all 0.25s ease;
}

.cart-item.is-selected {
  border-color: rgba(0, 151, 255, 0.15);
  background: #fbfdff;
}

/* ── Checkbox ── */
.item-check {
  flex-shrink: 0;
}

.item-check :deep(.el-checkbox__inner) {
  width: 18px;
  height: 18px;
  border-radius: 50%;
}

.item-check :deep(.el-checkbox__inner::after) {
  width: 5px;
  height: 9px;
  left: 5px;
  top: 1px;
}

/* ── Image ── */
.item-img-wrap {
  flex-shrink: 0;
  cursor: pointer;
}

.item-img {
  width: 68px;
  height: 68px;
  border-radius: 10px;
  object-fit: cover;
  transition: transform 0.2s;
}

.item-img:hover {
  transform: scale(1.04);
}

/* ── Info ── */
.item-info {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.item-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--cp-text);
  margin-bottom: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-spec {
  font-size: 11px;
  color: var(--cp-text-sub);
  margin-bottom: 6px;
}

.item-meta {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.item-unit-price {
  font-size: 15px;
  font-weight: 700;
  color: var(--cp-accent);
  letter-spacing: -0.3px;
}

.item-line-total {
  font-size: 11px;
  color: var(--cp-text-sub);
}

/* ── Stepper ── */
.item-stepper {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.step-btn {
  width: 28px !important;
  height: 28px !important;
}

.step-minus {
  background: var(--cp-border) !important;
  border: none !important;
  color: var(--cp-text-sub) !important;
}

.step-minus:hover {
  background: #e0e0e0 !important;
}

.step-plus {
  box-shadow: 0 2px 8px rgba(0, 151, 255, 0.3);
}

.qty-num {
  font-size: 15px;
  font-weight: 700;
  color: var(--cp-text);
  min-width: 22px;
  text-align: center;
}

/* ── Swipe Hint ── */
.swipe-hint {
  text-align: center;
  font-size: 11px;
  color: var(--cp-text-sub);
  padding: 12px 0 4px;
  opacity: 0.7;
}

/* ── Footer ── */
.cart-footer {
  position: fixed;
  bottom: 60px;
  left: 0;
  right: 0;
  max-width: 480px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  padding-bottom: calc(10px + env(safe-area-inset-bottom, 0px));
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  z-index: 90;
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.footer-check {
  flex-shrink: 0;
}

.footer-total {
  display: flex;
  flex-direction: column;
}

.total-label {
  font-size: 11px;
  color: var(--cp-text-sub);
}

.total-amount {
  font-size: 20px;
  font-weight: 800;
  color: var(--cp-accent);
  letter-spacing: -0.5px;
  line-height: 1.1;
}

.total-amount small {
  font-size: 13px;
  font-weight: 600;
}

.checkout-btn {
  height: 44px;
  padding: 0 28px;
  border-radius: 22px;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.3px;
  box-shadow: 0 4px 14px rgba(0, 151, 255, 0.3);
  transition: all 0.25s ease;
}

.checkout-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(0, 151, 255, 0.4);
}

.checkout-btn:disabled {
  background: #c0c0c0;
  border-color: #c0c0c0;
  box-shadow: none;
}

/* ── Empty State ── */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 20px;
  animation: fadeUp 0.5s ease both;
}

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(20px); }
  to   { opacity: 1; transform: translateY(0); }
}

.empty-illustration {
  position: relative;
  margin-bottom: 20px;
}

.empty-cart-icon {
  font-size: 56px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50%      { transform: translateY(-8px); }
}

.empty-dots {
  display: flex;
  justify-content: center;
  gap: 6px;
  margin-top: 10px;
}

.empty-dots span {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--cp-primary);
  opacity: 0.3;
  animation: dotPulse 1.4s ease-in-out infinite;
}

.empty-dots span:nth-child(2) { animation-delay: 0.2s; }
.empty-dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes dotPulse {
  0%, 80%, 100% { opacity: 0.2; transform: scale(0.8); }
  40%           { opacity: 0.6; transform: scale(1.2); }
}

.empty-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--cp-text);
  margin: 0 0 4px;
}

.empty-desc {
  font-size: 13px;
  color: var(--cp-text-sub);
  margin: 0 0 20px;
}

.empty-btn {
  padding: 10px 36px !important;
}

/* ── Skeleton ── */
.sk-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: var(--cp-card);
  border-radius: var(--cp-radius);
  margin-bottom: 10px;
}

.sk-check {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: linear-gradient(90deg, #eee 25%, #e0e0e0 50%, #eee 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  flex-shrink: 0;
}

.sk-img {
  width: 68px;
  height: 68px;
  border-radius: 10px;
  background: linear-gradient(90deg, #eee 25%, #e0e0e0 50%, #eee 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  flex-shrink: 0;
}

.sk-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sk-line {
  height: 14px;
  border-radius: 4px;
  background: linear-gradient(90deg, #eee 25%, #e0e0e0 50%, #eee 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.sk-w40 { width: 40%; }
.sk-w60 { width: 60%; }

@keyframes shimmer {
  0%   { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ── TransitionGroup ── */
.cart-enter-active {
  transition: all 0.35s ease;
}

.cart-leave-active {
  transition: all 0.3s ease;
  position: absolute;
}

.cart-enter-from {
  opacity: 0;
  transform: translateX(24px);
}

.cart-leave-to {
  opacity: 0;
  transform: translateX(-24px);
}

/* ── Footer bar transition ── */
.bar-enter-active,
.bar-leave-active {
  transition: all 0.3s ease;
}

.bar-enter-from,
.bar-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

/* ── Responsive ── */
@media (min-width: 640px) {
  .cart-footer {
    border-radius: 16px 16px 0 0;
  }
}
</style>