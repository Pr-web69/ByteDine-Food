<template>
  <div class="ai-robot-icon" :class="sizeClass">
    <div class="glass-body">
      <div class="glass-highlight"></div>
      <svg class="robot-svg" viewBox="0 0 32 32" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
        <!-- 天线 -->
        <line x1="16" y1="3" x2="16" y2="6.5" />
        <circle cx="16" cy="2.5" r="1.2" fill="currentColor" stroke="none" />
        <!-- 头部圆角方形 -->
        <rect x="6" y="6.5" width="20" height="16" rx="4.5" />
        <!-- 眼睛 -->
        <circle cx="11.5" cy="13" r="1.8" fill="currentColor" stroke="none" />
        <circle cx="20.5" cy="13" r="1.8" fill="currentColor" stroke="none" />
        <!-- 嘴巴 -->
        <path d="M12 17.5c1.5 1.2 6.5 1.2 8 0" />
        <!-- 小短手 -->
        <line x1="6" y1="13" x2="3.5" y2="13" />
        <line x1="26" y1="13" x2="28.5" y2="13" />
        <circle cx="3" cy="13" r="1" fill="currentColor" stroke="none" />
        <circle cx="29" cy="13" r="1" fill="currentColor" stroke="none" />
        <!-- 底部脖子 -->
        <rect x="13" y="22.5" width="6" height="2.5" rx="1" />
        <!-- 身体 -->
        <rect x="8" y="25" width="16" height="5" rx="2.5" />
      </svg>
    </div>
    <span class="robot-label" v-if="showLabel">AI</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  size: { type: String, default: 'md' }, // xs | sm | md | lg
  showLabel: { type: Boolean, default: false }
})

const sizeClass = computed(() => `size-${props.size}`)
</script>

<style scoped>
.ai-robot-icon {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

/* 玻璃拟态主体 */
.glass-body {
  position: relative;
  border-radius: 28%;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.85) 0%, rgba(6, 182, 212, 0.75) 100%);
  backdrop-filter: blur(12px) saturate(1.4);
  -webkit-backdrop-filter: blur(12px) saturate(1.4);
  border: 1px solid rgba(255, 255, 255, 0.25);
  box-shadow:
    0 4px 16px rgba(37, 99, 235, 0.3),
    inset 0 1px 1px rgba(255, 255, 255, 0.4),
    inset 0 -1px 1px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  transition: transform 0.2s ease;
}

/* 高光反射 */
.glass-highlight {
  position: absolute;
  top: 8%;
  left: 12%;
  width: 35%;
  height: 30%;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.5) 0%, transparent 70%);
  pointer-events: none;
  filter: blur(1px);
}

.robot-svg {
  position: relative;
  z-index: 1;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.15));
}

.robot-label {
  font-size: 9px;
  font-weight: 700;
  color: #2563eb;
  letter-spacing: 0.5px;
  line-height: 1;
}

/* 尺寸 */
.size-xs .glass-body { width: 20px; height: 20px; border-radius: 6px; }
.size-xs .robot-svg { width: 14px; height: 14px; }

.size-sm .glass-body { width: 28px; height: 28px; border-radius: 8px; }
.size-sm .robot-svg { width: 20px; height: 20px; }

.size-md .glass-body { width: 36px; height: 36px; border-radius: 10px; }
.size-md .robot-svg { width: 26px; height: 26px; }

.size-lg .glass-body { width: 48px; height: 48px; border-radius: 14px; }
.size-lg .robot-svg { width: 34px; height: 34px; }

/* hover 微动 */
.ai-robot-icon:hover .glass-body {
  transform: translateY(-2px) scale(1.03);
  box-shadow:
    0 8px 24px rgba(37, 99, 235, 0.4),
    inset 0 1px 1px rgba(255, 255, 255, 0.5);
}
</style>
