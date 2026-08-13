<template>
  <div class="user-app">
    <!-- 48px 毛玻璃顶栏 -->
    <header class="topbar">
      <div class="topbar-inner">
        <div class="tb-left" @click="$router.push('/home')">
          <img src="@/assets/logo-icon.png" class="tb-logo" />
          <span class="tb-brand">字节智能餐饮</span>
        </div>
        <div class="tb-right">
          <router-link to="/cart" class="tb-icon-btn">
            <el-badge :value="0" :hidden="true"><el-icon><ShoppingCart /></el-icon></el-badge>
          </router-link>
          <router-link to="/profile" class="tb-avatar" v-if="token">
            <el-icon><UserFilled /></el-icon>
          </router-link>
          <router-link to="/login" class="tb-login" v-else>登录</router-link>
        </div>
      </div>
    </header>

    <!-- 主内容 -->
    <main class="main-content">
      <Suspense>
        <template #default>
          <router-view v-slot="{ Component, route: viewRoute }">
            <transition name="page" appear>
              <component :is="Component" :key="viewRoute.fullPath" />
            </transition>
          </router-view>
        </template>
        <template #fallback>
          <div class="loading-skeleton">
            <div class="sk-card" v-for="i in 3" :key="i">
              <div class="sk-line sk-line--long"></div>
              <div class="sk-line sk-line--short"></div>
            </div>
          </div>
        </template>
      </Suspense>
    </main>

    <!-- 底部 TabBar（AI 按钮 FAB 凸起） -->
    <footer class="tabbar">
      <router-link to="/home" class="tab" active-class="active">
        <el-icon :size="20"><HomeFilled /></el-icon><span>首页</span>
      </router-link>
      <router-link to="/order" class="tab" active-class="active">
        <el-icon :size="20"><Document /></el-icon><span>订单</span>
      </router-link>
      <!-- AI FAB 凸起按钮 -->
      <div class="tab-fab" @click="$router.push('/ai')">
        <div class="fab-inner">
          <AiRobotIcon size="sm" />
        </div>
        <span>AI</span>
      </div>
      <router-link to="/cart" class="tab" active-class="active">
        <el-icon :size="20"><ShoppingCart /></el-icon><span>购物车</span>
      </router-link>
      <router-link to="/profile" class="tab" active-class="active">
        <el-icon :size="20"><User /></el-icon><span>我的</span>
      </router-link>
    </footer>

    <!-- 右下角悬浮 AI 按钮（可拖拽） -->
    <div
      class="float-ai"
      :style="floatStyle"
      @mousedown.prevent="startDrag"
      @touchstart.prevent="startDrag"
      @click="handleFloatClick"
    >
      <AiRobotIcon size="lg" show-label />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { HomeFilled, Document, ShoppingCart, User, UserFilled } from '@element-plus/icons-vue'
import AiRobotIcon from '@/components/AiRobotIcon.vue'

const router = useRouter()
const token = ref(localStorage.getItem('token'))

// ====== AI 悬浮按钮拖拽 ======
const floatPos = reactive({
  bottom: 70,
  right: 16,
})

const dragging = ref(false)
let dragStart = { x: 0, y: 0, bottom: 0, right: 0 }

// 恢复之前保存的位置
const saved = localStorage.getItem('aiFloatPos')
if (saved) {
  try {
    const p = JSON.parse(saved)
    floatPos.bottom = p.bottom || 70
    floatPos.right = p.right || 16
  } catch {}
}

const floatStyle = computed(() => ({
  bottom: floatPos.bottom + 'px',
  right: floatPos.right + 'px',
  transition: dragging.value ? 'none' : 'all 0.35s cubic-bezier(0.34, 1.56, 0.64, 1)',
}))

const clamp = (v, min, max) => Math.max(min, Math.min(max, v))

const startDrag = (e) => {
  dragging.value = true
  const pt = e.touches ? e.touches[0] : e
  dragStart = {
    x: pt.clientX,
    y: pt.clientY,
    bottom: floatPos.bottom,
    right: floatPos.right,
  }
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
  document.addEventListener('touchmove', onDrag, { passive: false })
  document.addEventListener('touchend', stopDrag)
}

const onDrag = (e) => {
  if (!dragging.value) return
  const pt = e.touches ? e.touches[0] : e
  const dx = dragStart.x - pt.clientX
  const dy = dragStart.y - pt.clientY
  const ww = window.innerWidth
  const wh = window.innerHeight
  floatPos.right = clamp(dragStart.right + dx, 8, ww - 64)
  floatPos.bottom = clamp(dragStart.bottom + dy, 60, wh - 100)
}

const stopDrag = () => {
  dragging.value = false
  localStorage.setItem('aiFloatPos', JSON.stringify({ bottom: floatPos.bottom, right: floatPos.right }))
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
  document.removeEventListener('touchmove', onDrag)
  document.removeEventListener('touchend', stopDrag)
}

const handleFloatClick = () => {
  // 如果拖拽距离很小，视为点击
  if (Math.abs(floatPos.bottom - dragStart.bottom) < 5 && Math.abs(floatPos.right - dragStart.right) < 5) {
    router.push('/ai')
  }
}

onUnmounted(() => {
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
  document.removeEventListener('touchmove', onDrag)
  document.removeEventListener('touchend', stopDrag)
})
</script>

<style>
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:'Microsoft YaHei','PingFang SC','Helvetica Neue',Helvetica,Arial,sans-serif;background:#eff3ff;color:#1a1a1a;-webkit-font-smoothing:antialiased}
.user-app{min-height:100vh;display:flex;flex-direction:column}

/* 顶栏 */
.topbar{position:sticky;top:0;z-index:100;height:48px;background:rgba(255,255,255,.88);backdrop-filter:blur(12px);-webkit-backdrop-filter:blur(12px);border-bottom:1px solid rgba(0,0,0,.05)}
.topbar-inner{max-width:1200px;margin:0 auto;height:100%;display:flex;align-items:center;justify-content:space-between;padding:0 16px}
.tb-left{display:flex;align-items:center;gap:6px;cursor:pointer}
.tb-logo{width:22px;height:22px;border-radius:4px}
.tb-brand{font-size:13px;font-weight:600;color:#1e293b}
.tb-right{display:flex;align-items:center;gap:12px}
.tb-icon-btn{color:#64748b;text-decoration:none;display:flex}
.tb-avatar{width:28px;height:28px;border-radius:50%;background:#eff6ff;color:var(--color-primary);display:flex;align-items:center;justify-content:center;text-decoration:none}
.tb-login{font-size:12px;color:var(--color-primary);text-decoration:none;font-weight:500}

/* 内容区 */
.main-content{flex:1;max-width:1200px;margin:0 auto;width:100%;padding-bottom:60px}

@media(min-width:768px){.tabbar{display:none}.main-content{padding-bottom:0}}
/* 底部 TabBar */
.tabbar{position:fixed;bottom:0;left:0;right:0;height:56px;background:rgba(255,255,255,.92);backdrop-filter:blur(12px);border-top:1px solid rgba(0,0,0,.05);display:flex;align-items:center;justify-content:space-around;padding:0 4px;z-index:100}
.tab{display:flex;flex-direction:column;align-items:center;gap:1px;font-size:10px;color:#94a3b8;text-decoration:none;flex:1}
.tab.active{color:var(--color-primary)}
/* FAB 凸起 */
.tab-fab{position:relative;top:-12px;display:flex;flex-direction:column;align-items:center;gap:2px;font-size:10px;color:#94a3b8;cursor:pointer;flex:1}
.fab-inner{width:48px;height:48px;border-radius:50%;background:rgba(255,255,255,.9);backdrop-filter:blur(12px);display:flex;align-items:center;justify-content:center;box-shadow:0 4px 16px rgba(0,0,0,.08);border:2px solid rgba(37,99,235,.12)}
.tab-fab:active .fab-inner{transform:scale(.92)}

/* 悬浮 AI */
.float-ai{
  position:fixed;
  z-index:99;
  cursor:grab;
  animation:floatPulse 3s ease-in-out infinite;
  user-select:none;
  -webkit-user-select:none;
}

.float-ai:active{cursor:grabbing}

@keyframes floatPulse{
  0%,100%{transform:translateY(0) scale(1)}
  50%{transform:translateY(-8px) scale(1.04)}
}

/* 过渡 */
.page-enter-active,.page-leave-active{transition:opacity .15s ease,transform .15s ease}
.page-enter-from{opacity:0;transform:translateY(4px)}
.page-leave-to{opacity:0;transform:translateY(-4px)}

/* 骨架屏 */
.loading-skeleton { padding: 16px; display: flex; flex-direction: column; gap: 12px; }
.sk-card { background: #fff; border-radius: 12px; padding: 16px; display: flex; flex-direction: column; gap: 10px; }
.sk-line { height: 14px; border-radius: 6px; background: linear-gradient(90deg, #e8ecf1 25%, #f0f3f7 50%, #e8ecf1 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; }
.sk-line--long { width: 80%; }
.sk-line--short { width: 40%; }
@keyframes shimmer { 0% { background-position: -200% 0; } 100% { background-position: 200% 0; } }

@media(min-width:768px){.tabbar{display:none}.main-content{padding-bottom:0}}
</style>
