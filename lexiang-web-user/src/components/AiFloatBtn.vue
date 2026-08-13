<template>
  <div
      class="ai-float"
      :style="{ left: leftOffset + 'px' }"
      @mouseenter="hovered = true"
      @mouseleave="hovered = false"
  >
    <!-- ====== 轨道环 1（顺时针） ====== -->
    <div class="orbit-ring ring-1" :class="{ fast: hovered }">
      <span class="odot od-main"></span>
      <span class="odot od-trail"></span>
    </div>

    <!-- ====== 轨道环 2（逆时针） ====== -->
    <div class="orbit-ring ring-2" :class="{ fast: hovered }">
      <span class="odot od-main"></span>
      <span class="odot od-trail"></span>
    </div>

    <!-- ====== 主按钮 ====== -->
    <button
        class="ai-btn"
        :aria-label="ariaLabel"
        @click="handleOpen"
    >
      <span class="scan-line"></span>
      <span class="inner-glow"></span>

      <!-- SVG 芯片 AI 图标 -->
      <svg
          class="ai-icon"
          viewBox="0 0 32 32"
          width="28"
          height="28"
          fill="none"
          stroke="currentColor"
          stroke-width="1.5"
          stroke-linecap="round"
          stroke-linejoin="round"
      >
        <path d="M11 2v4"/>
        <path d="M16 2v4"/>
        <path d="M21 2v4"/>
        <path d="M11 26v4"/>
        <path d="M16 26v4"/>
        <path d="M21 26v4"/>
        <path d="M2 11h4"/>
        <path d="M2 16h4"/>
        <path d="M2 21h4"/>
        <path d="M26 11h4"/>
        <path d="M26 16h4"/>
        <path d="M26 21h4"/>
        <rect x="6" y="6" width="20" height="20" rx="3"/>
        <path d="M16 6v-3"/>
        <circle cx="16" cy="2.2" r="1" fill="currentColor" stroke="none">
          <animate attributeName="r" values="1;1.6;1" dur="2.5s" repeatCount="indefinite"/>
          <animate attributeName="opacity" values="1;0.4;1" dur="2.5s" repeatCount="indefinite"/>
        </circle>
        <circle cx="12" cy="13.5" r="1.4" fill="currentColor" stroke="none">
          <animate attributeName="opacity" values="1;0.25;1" dur="2s" repeatCount="indefinite"/>
        </circle>
        <circle cx="20" cy="13.5" r="1.4" fill="currentColor" stroke="none">
          <animate attributeName="opacity" values="1;0.25;1" dur="2s" begin="0.35s" repeatCount="indefinite"/>
        </circle>
        <circle cx="16" cy="20" r="1.4" fill="currentColor" stroke="none">
          <animate attributeName="opacity" values="1;0.25;1" dur="2s" begin="0.7s" repeatCount="indefinite"/>
        </circle>
        <line x1="12" y1="13.5" x2="20" y2="13.5" stroke="currentColor" stroke-width="0.7" opacity="0.3">
          <animate attributeName="opacity" values="0.3;0.7;0.3" dur="2s" repeatCount="indefinite"/>
        </line>
        <line x1="12" y1="13.5" x2="16" y2="20" stroke="currentColor" stroke-width="0.7" opacity="0.3">
          <animate attributeName="opacity" values="0.3;0.7;0.3" dur="2s" begin="0.35s" repeatCount="indefinite"/>
        </line>
        <line x1="20" y1="13.5" x2="16" y2="20" stroke="currentColor" stroke-width="0.7" opacity="0.3">
          <animate attributeName="opacity" values="0.3;0.7;0.3" dur="2s" begin="0.7s" repeatCount="indefinite"/>
        </line>
      </svg>
    </button>

    <!-- ====== 注释标签（已经调整至图标上方） ====== -->
    <div class="ai-label" :class="{ visible: labelVisible }">
      <span class="label-dot"></span>
      <span>AI 智能客服</span>
    </div>

    <!-- ====== 气泡提示 ====== -->
    <transition name="bubble-pop">
      <div
          v-if="showBubble"
          class="ai-bubble"
          role="button"
          tabindex="0"
          @click="handleOpen"
          @keydown.enter="handleOpen"
      >
        <span class="bubble-text">{{ bubbleText }}</span>
        <button class="bubble-close" aria-label="关闭" @click.stop="dismissBubble">✕</button>
        <div class="bubble-arrow"></div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  leftOffset:      { type: Number,  default: 82 },
  bubbleText:      { type: String,  default: '有经营问题？随时问我' },
  bubbleDelay:     { type: Number,  default: 4000 },
  bubbleDuration:  { type: Number,  default: 6000 },
  ariaLabel:       { type: String,  default: '打开 AI 智能客服' },
  annotationDelay: { type: Number,  default: 2000 },
})

const emit = defineEmits(['open', 'bubble-dismiss'])

const hovered = ref(false)
const showBubble = ref(false)
const labelVisible = ref(false)

let t1 = null, t2 = null, t3 = null

const handleOpen = () => {
  showBubble.value = false
  emit('open')
}

const dismissBubble = () => {
  showBubble.value = false
  emit('bubble-dismiss')
}

onMounted(() => {
  t1 = setTimeout(() => { labelVisible.value = true }, props.annotationDelay)
  t2 = setTimeout(() => {
    showBubble.value = true
    t3 = setTimeout(() => { showBubble.value = false }, props.bubbleDuration)
  }, props.bubbleDelay)
})

onUnmounted(() => {
  clearTimeout(t1)
  clearTimeout(t2)
  clearTimeout(t3)
})
</script>

<style scoped>
/* ==============================
   容器：固定在侧边栏底部区域
   ============================== */
.ai-float {
  position: fixed;
  bottom: 56px;        /* 距底部 56px，避开侧边栏最底部菜单 */
  z-index: 100;
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: left 0.28s cubic-bezier(0.4, 0, 0.2, 1);
}

/* ==============================
   轨道环
   ============================== */
.orbit-ring {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  pointer-events: none;
}

.ring-1 {
  width: 80px;
  height: 80px;
  border: 1px dashed rgba(99, 102, 241, 0.18);
  animation: orbitSpin 8s linear infinite;
}
.ring-1.fast { animation-duration: 3s; }

.ring-2 {
  width: 96px;
  height: 96px;
  border: 1px dotted rgba(6, 182, 212, 0.14);
  animation: orbitSpin 14s linear infinite reverse;
}
.ring-2.fast { animation-duration: 5s; }

@keyframes orbitSpin {
  to { transform: translate(-50%, -50%) rotate(360deg); }
}

.odot {
  position: absolute;
  border-radius: 50%;
  background: #818cf8;
  box-shadow: 0 0 8px rgba(129, 140, 248, 0.6);
}
.od-main {
  width: 5px;
  height: 5px;
  top: -2px;
  left: 50%;
  transform: translateX(-50%);
}
.od-trail {
  width: 3px;
  height: 3px;
  bottom: -1px;
  left: 20%;
  opacity: 0.4;
}

.ring-2 .od-main {
  background: #22d3ee;
  box-shadow: 0 0 8px rgba(34, 211, 238, 0.6);
  top: auto;
  bottom: -2px;
  left: 60%;
  transform: none;
}
.ring-2 .od-trail {
  background: #22d3ee;
  top: 20%;
  left: auto;
  right: -1px;
  bottom: auto;
}

/* ==============================
   主按钮
   ============================== */
.ai-btn {
  position: relative;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: linear-gradient(135deg, #6366f1 0%, #06b6d4 100%);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  overflow: hidden;
  z-index: 2;
  box-shadow:
      0 0 0 0 rgba(99, 102, 241, 0.4),
      0 0 0 0 rgba(6, 182, 212, 0.2),
      0 4px 16px rgba(99, 102, 241, 0.35);
  transition:
      transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1),
      box-shadow 0.3s ease;
  /* 浮动 + 呼吸 */
  animation: btnFloat 4.5s ease-in-out infinite,
  glowBreath 3s ease-in-out infinite;
}

@keyframes btnFloat {
  0%, 100% { transform: translateY(0); }
  50%      { transform: translateY(-5px); }
}

@keyframes glowBreath {
  0%, 100% {
    box-shadow:
        0 0 0 0 rgba(99, 102, 241, 0.4),
        0 0 0 0 rgba(6, 182, 212, 0.2),
        0 4px 16px rgba(99, 102, 241, 0.35);
  }
  50% {
    box-shadow:
        0 0 0 14px rgba(99, 102, 241, 0),
        0 0 0 28px rgba(6, 182, 212, 0),
        0 8px 32px rgba(99, 102, 241, 0.45);
  }
}

.ai-btn:hover {
  transform: translateY(-5px) scale(1.12);
  box-shadow:
      0 0 0 8px rgba(99, 102, 241, 0.15),
      0 0 0 20px rgba(6, 182, 212, 0.08),
      0 8px 36px rgba(99, 102, 241, 0.5);
  animation-play-state: paused;
}
.ai-btn:active {
  transform: translateY(-2px) scale(1.05);
  transition-duration: 0.1s;
}
.ai-btn:focus-visible {
  outline: 2px solid #a5b4fc;
  outline-offset: 4px;
}

/* 扫描线 */
.scan-line {
  position: absolute;
  left: 0;
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg, transparent 0%, rgba(255,255,255,0.45) 50%, transparent 100%);
  animation: scanDown 3s ease-in-out infinite;
  pointer-events: none;
}
@keyframes scanDown {
  0%   { top: -2px; opacity: 0; }
  8%   { opacity: 1; }
  92%  { opacity: 1; }
  100% { top: calc(100% + 2px); opacity: 0; }
}

/* 内部旋转光效 */
.inner-glow {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: conic-gradient(
      from 0deg,
      transparent 0deg,
      rgba(255, 255, 255, 0.08) 60deg,
      transparent 120deg
  );
  animation: innerSpin 6s linear infinite;
  pointer-events: none;
}
@keyframes innerSpin { to { transform: rotate(360deg); } }

/* SVG 图标 */
.ai-icon {
  position: relative;
  z-index: 3;
  filter: drop-shadow(0 1px 3px rgba(0, 0, 0, 0.25));
}

/* ==============================
   注释标签（调整到图标上方居中）
   ============================== */
.ai-label {
  position: absolute;
  bottom: calc(100% + 10px);
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(15, 23, 42, 0.88);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(99, 102, 241, 0.25);
  color: #e2e8f0;
  font-size: 12px;
  font-weight: 500;
  padding: 4px 14px;
  border-radius: 20px;
  white-space: nowrap;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.25);
  letter-spacing: 0.3px;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.6s ease;
  z-index: 10;
}
.ai-label.visible { opacity: 1; }

.label-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22d3ee;
  box-shadow: 0 0 6px rgba(34, 211, 238, 0.6);
  animation: dotPulse 2s ease-in-out infinite;
  flex-shrink: 0;
}
@keyframes dotPulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50%      { opacity: 0.4; transform: scale(0.6); }
}

/* ==============================
   气泡提示（上方弹出）
   ============================== */
.ai-bubble {
  position: absolute;
  bottom: calc(100% + 12px);
  left: 50%;
  transform: translateX(-50%);
  background: #fff;
  color: #1e293b;
  font-size: 13px;
  font-weight: 500;
  padding: 10px 16px;
  border-radius: 12px;
  white-space: nowrap;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1), 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid #e2e8f0;
  z-index: 10;
  transition: box-shadow 0.2s;
}
.ai-bubble:hover {
  box-shadow: 0 8px 30px rgba(99, 102, 241, 0.12), 0 2px 8px rgba(0, 0, 0, 0.05);
}
.bubble-text { flex: 1; }
.bubble-close {
  font-size: 11px;
  color: #94a3b8;
  cursor: pointer;
  background: none;
  border: none;
  padding: 2px;
  line-height: 1;
  transition: color 0.2s;
}
.bubble-close:hover { color: #ef4444; }

.bubble-arrow {
  position: absolute;
  bottom: -6px;
  left: 50%;
  transform: translateX(-50%) rotate(45deg);
  width: 12px;
  height: 12px;
  background: #fff;
  border-right: 1px solid #e2e8f0;
  border-bottom: 1px solid #e2e8f0;
}

/* 气泡动画 */
.bubble-pop-enter-active { transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1); }
.bubble-pop-leave-active { transition: all 0.25s ease; }
.bubble-pop-enter-from { opacity: 0; transform: translateX(-50%) translateY(8px) scale(0.9); }
.bubble-pop-leave-to   { opacity: 0; transform: translateX(-50%) translateY(4px) scale(0.95); }

/* ==============================
   响应式
   ============================== */
@media (max-width: 768px) {
  .ai-float {
    bottom: 24px;
    width: 48px;
    height: 48px;
  }
  .ai-btn { width: 48px; height: 48px; }
  .ai-icon { width: 22px; height: 22px; }
  .orbit-ring.ring-1 { width: 66px; height: 66px; }
  .orbit-ring.ring-2 { width: 80px; height: 80px; }
  .ai-label { font-size: 11px; padding: 3px 10px; }
}
</style>