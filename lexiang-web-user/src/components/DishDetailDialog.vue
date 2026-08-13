<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    :close-on-click-modal="false"
    :lock-scroll="true"
    width="400px"
    class="dish-detail-dialog"
    destroy-on-close
  >
    <template #header="{ close }">
      <div class="dialog-header">
        <button class="close-btn" @click="close">
          <el-icon :size="22"><Close /></el-icon>
        </button>
      </div>
    </template>

    <div class="dialog-body" v-if="dish">
      <!-- 菜品大图 -->
      <div class="img-section">
        <img
          :src="dish.image || fallbackImg"
          :alt="dish.name"
          class="dish-hero-img"
          @error="e => e.target.src = fallbackImg"
        />
        <div class="img-gradient"></div>
      </div>

      <!-- 基本信息 -->
      <div class="info-section">
        <div class="name-row">
          <h2 class="dish-name">{{ dish.name }}</h2>
          <span class="dish-price">¥{{ computedPrice }}</span>
        </div>
        <p class="dish-desc" v-if="dish.description">{{ dish.description }}</p>
        <div class="meta-row">
          <span class="meta-item">月售 {{ dish.sales || 0 }}</span>
          <span class="meta-divider">·</span>
          <span class="meta-item">好评率 98%</span>
        </div>
      </div>

      <!-- 规格选择 -->
      <div class="spec-section" v-if="specGroups.length">
        <div class="spec-group" v-for="group in specGroups" :key="group.groupId">
          <div class="spec-label">{{ group.name }}</div>
          <div class="spec-options">
            <span
              class="spec-option"
              v-for="opt in group.items"
              :key="opt.itemId"
              :class="{ active: selectedSpecs[group.groupId] === opt.itemId }"
              @click="selectedSpecs[group.groupId] = opt.itemId"
            >
              {{ opt.name }}
              <template v-if="opt.priceExtra > 0">+¥{{ opt.priceExtra }}</template>
            </span>
          </div>
        </div>
      </div>

      <!-- 数量选择 -->
      <div class="qty-section">
        <span class="qty-label">数量</span>
        <div class="qty-stepper">
          <button class="qty-btn" :disabled="quantity <= 1" @click="quantity--">
            <el-icon :size="16"><Minus /></el-icon>
          </button>
          <span class="qty-num">{{ quantity }}</span>
          <button class="qty-btn qty-plus" @click="quantity++">
            <el-icon :size="16"><Plus /></el-icon>
          </button>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer" v-if="dish">
        <el-button
          type="primary"
          class="add-cart-btn"
          :loading="adding"
          @click="handleAddCart"
        >
          加入购物车 &nbsp; ¥{{ totalPrice }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Close, Minus, Plus } from '@element-plus/icons-vue'
import { addToCart } from '@/api/cart'
import fallbackImg from '@/assets/logo-icon.png'

const props = defineProps({
  visible: Boolean,
  dish: { type: Object, default: null },
})

const emit = defineEmits(['update:visible', 'added'])

const quantity = ref(1)
const adding = ref(false)
const selectedSpecs = reactive({})

// 直接使用 API 返回的 specGroups，不再走 name 匹配
const specGroups = computed(() => {
  if (!props.dish) return []
  return props.dish.specGroups || []
})

// 默认选中每个规格的第一个选项
watch(() => [props.dish, specGroups.value], () => {
  Object.keys(selectedSpecs).forEach(k => delete selectedSpecs[k])
  if (!specGroups.value?.length) return
  specGroups.value.forEach(g => {
    const def = g.items?.find(i => i.isDefault) || g.items?.[0]
    if (def) selectedSpecs[g.groupId] = def.itemId
  })
}, { immediate: true, deep: true })

// 计算规格加价
const specPriceAdjust = computed(() => {
  if (!specGroups.value?.length) return 0
  let adjust = 0
  specGroups.value.forEach(g => {
    const selected = g.items?.find(i => i.itemId === selectedSpecs[g.groupId])
    if (selected?.priceExtra) adjust += Number(selected.priceExtra)
  })
  return adjust
})

const basePrice = computed(() => Number(props.dish?.price || 0))
const computedPrice = computed(() => (basePrice.value + specPriceAdjust.value).toFixed(2))
const totalPrice = computed(() => (Number(computedPrice.value) * quantity.value).toFixed(2))

const selectedSpecLabel = computed(() => {
  if (!specGroups.value?.length) return undefined
  return specGroups.value
    .map(g => {
      const item = g.items?.find(i => i.itemId === selectedSpecs[g.groupId])
      return item?.name
    })
    .filter(Boolean)
    .join('、') || undefined
})

const handleAddCart = async () => {
  if (!localStorage.getItem('token')) {
    ElMessage.warning('请先登录')
    emit('update:visible', false)
    return
  }
  adding.value = true
  try {
    await addToCart(props.dish.id, quantity.value, Number(computedPrice.value), selectedSpecLabel.value)
    const specLabel = selectedSpecLabel.value
    const msg = specLabel
      ? `已添加「${props.dish.name}」(${specLabel}) x${quantity.value}`
      : `已添加「${props.dish.name}」x${quantity.value}`
    ElMessage.success(msg)
    emit('added')
    emit('update:visible', false)
  } catch (e) {
    ElMessage.error(e?.message || '添加失败')
  } finally {
    adding.value = false
    quantity.value = 1
  }
}
</script>

<style scoped>
.dish-detail-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.dish-detail-dialog :deep(.el-dialog__header) {
  padding: 0;
  margin: 0;
  position: relative;
}

.dialog-header {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 10;
}

.close-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(8px);
  border: none;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.close-btn:hover {
  background: rgba(0, 0, 0, 0.55);
  transform: scale(1.08);
}

/* ── 图片区 ── */
.img-section {
  position: relative;
  height: 200px;
  overflow: hidden;
}

.dish-hero-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.img-gradient {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: linear-gradient(transparent, rgba(255,255,255,1));
}

/* ── 信息区 ── */
.info-section {
  padding: 0 20px 16px;
}

.name-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 8px;
}

.dish-name {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
  line-height: 1.3;
}

.dish-price {
  font-size: 20px;
  font-weight: 800;
  color: #f97316;
  white-space: nowrap;
  flex-shrink: 0;
}

.dish-desc {
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
  margin: 0 0 8px;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #94a3b8;
}

.meta-divider {
  color: #cbd5e1;
}

/* ── 规格选择 ── */
.spec-section {
  padding: 0 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.spec-label {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 8px;
}

.spec-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.spec-option {
  padding: 7px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  color: #475569;
  background: #f1f5f9;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
  border: 1.5px solid transparent;
}

.spec-option:hover {
  background: #e2e8f0;
}

.spec-option.active {
  background: #eff6ff;
  color: #2563eb;
  border-color: #2563eb;
  font-weight: 600;
}

/* ── 数量 ── */
.qty-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px 8px;
}

.qty-label {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.qty-stepper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.qty-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1.5px solid #e2e8f0;
  background: #fff;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.qty-btn:hover:not(:disabled) {
  border-color: #2563eb;
  color: #2563eb;
}

.qty-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.qty-plus {
  background: #eff6ff;
  border-color: #2563eb;
  color: #2563eb;
}

.qty-plus:hover:not(:disabled) {
  background: #2563eb;
  color: #fff;
}

.qty-num {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  min-width: 24px;
  text-align: center;
}

/* ── 底部按钮 ── */
.dialog-footer {
  padding: 4px 20px 0;
}

.add-cart-btn {
  width: 100%;
  height: 48px;
  border-radius: 24px;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.5px;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.35);
}

.add-cart-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(37, 99, 235, 0.45);
}

/* ── 响应式 ── */
@media (max-width: 440px) {
  .dish-detail-dialog :deep(.el-dialog) {
    width: 100% !important;
    margin: 0 !important;
    border-radius: 16px 16px 0 0 !important;
    align-self: flex-end;
  }
}
</style>
