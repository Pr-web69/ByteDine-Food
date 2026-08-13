<template>
  <el-container class="admin-layout">
    <!-- ====== 左侧侧边栏 ====== -->
    <el-aside
        :width="isCollapsed ? '65px' : '200px'"
        class="admin-aside"
    >
      <!-- 折叠按钮 -->
      <el-tooltip
          :content="isCollapsed ? '展开菜单' : '收起菜单'"
          placement="right"
          :show-after="400"
      >
        <div class="collapse-float" @click="toggleCollapse">
          <el-icon :size="14">
            <component :is="isCollapsed ? 'Expand' : 'Fold'" />
          </el-icon>
        </div>
      </el-tooltip>

      <!-- Logo -->
      <div class="aside-header" :class="{ collapsed: isCollapsed }">
        <div class="logo-area">
          <div class="logo-icon-wrap">
            <img
                v-if="logoSrc"
                :src="logoSrc"
                class="logo-img"
                alt="Logo"
                @error="logoSrc = ''"
            />
            <span v-else class="logo-text-fallback">字节</span>
          </div>
          <transition name="fade-text">
            <span v-show="!isCollapsed" class="logo-text">字节餐饮</span>
          </transition>
        </div>
      </div>

      <!-- 可滚动主菜单 -->
      <el-menu
          :default-active="activeMenu"
          :collapse="isCollapsed"
          :collapse-transition="false"
          class="aside-menu"
          @select="handleMenuSelect"
      >
        <el-menu-item
            v-for="item in menuList"
            :key="item.index"
            :index="item.index"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- ====== 右侧区域 ====== -->
    <el-container class="right-container">
      <el-header class="admin-header" height="56px">
        <div class="header-left">
          <el-tooltip content="点击切换营业状态" placement="bottom" :show-after="400">
            <span
                class="shop-status-tag"
                :class="{ closed: !isShopOpen }"
                @click="toggleShopStatus"
            >
              {{ isShopOpen ? '营业中' : '已打烊' }}
            </span>
          </el-tooltip>
        </div>

        <div class="header-right">
          <div class="header-search">
            <el-input
                v-model="searchKeyword"
                placeholder="搜索订单、菜品..."
                :prefix-icon="Search"
                :suffix-icon="voiceListening ? 'Microphone' : ''"
                clearable
                size="small"
                class="search-input"
                @keyup.enter="handleSearch"
                @clear="handleSearch"
            />
          </div>

          <el-tooltip :content="voiceTooltip" placement="bottom" :show-after="300">
            <el-icon :size="18" class="header-icon" :class="{ 'voice-on': voiceEnabled || voiceListening }" @click="toggleVoiceInput">
              <component :is="voiceIcon" />
            </el-icon>
          </el-tooltip>

          <el-tooltip content="全屏" placement="bottom" :show-after="300">
            <el-icon :size="18" class="header-icon" @click="toggleFullscreen">
              <FullScreen />
            </el-icon>
          </el-tooltip>

          <el-tooltip content="通知" placement="bottom" :show-after="300">
            <el-badge
                :value="notifyCount"
                :hidden="notifyCount === 0"
                :max="99"
                class="notify-badge"
            >
              <el-icon :size="18" class="header-icon">
                <Bell />
              </el-icon>
            </el-badge>
          </el-tooltip>

          <el-dropdown trigger="click" @command="handleUserCmd">
            <div class="user-trigger">
              <el-avatar :size="30" class="user-avatar-el">
                <el-icon :size="16"><UserFilled /></el-icon>
              </el-avatar>
              <span class="user-name">{{ userName }}</span>
              <el-icon :size="10" class="user-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人信息
                </el-dropdown-item>
                <el-dropdown-item command="setting">
                  <el-icon><Setting /></el-icon>系统设置
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="admin-main">
        <Suspense>
          <template #default>
            <router-view v-slot="{ Component, route: viewRoute }">
              <transition name="page-slide" appear>
                <keep-alive :include="cachedViews">
                  <component :is="Component" :key="viewRoute.fullPath" />
                </keep-alive>
              </transition>
            </router-view>
          </template>
          <template #fallback>
            <div class="admin-loading">
              <el-skeleton :rows="3" animated />
            </div>
          </template>
        </Suspense>
      </el-main>
    </el-container>

    <!-- AI 悬浮按钮（唯一的 AI 入口） -->
    <AiFloatBtn
        :left-offset="aiLeftOffset"
        @open="openAiChat"
    />

    <!-- AI 聊天面板 -->
    <AiChatPanel v-model:visible="aiChatVisible" />
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import {
  HomeFilled, DataAnalysis, Document,
  KnifeFork, Menu as MenuIcon, User, Picture,
  Bell, UserFilled, Operation,
  Expand, Fold, ArrowDown,
  Search, FullScreen, Setting, SwitchButton,
  Microphone, MuteNotification
} from '@element-plus/icons-vue'

import { useVoice } from '@/composables/useVoice'
import { getAdminBusinessStatus, toggleAdminBusinessStatus } from '@/api/merchant'
import { playCancelSound, playNewOrderSound } from '@/utils/sound'
import request from '@/utils/request'

import logoImg from '@/assets/logo-icon.png'
import AiFloatBtn from '@/components/AiFloatBtn.vue'
import AiChatPanel from '@/components/AiChatPanel.vue'

const route = useRoute()
const router = useRouter()
const { enabled: voiceEnabled, toggle: toggleVoice } = useVoice()

/* ============================
   顶部搜索 + 语音输入
   ============================ */
const searchKeyword = ref('')
const voiceListening = ref(false)
let recognition = null

const voiceIcon = computed(() => {
  if (voiceListening.value) return 'Microphone'
  return voiceEnabled.value ? 'Microphone' : 'MuteNotification'
})
const voiceTooltip = computed(() => {
  if (voiceListening.value) return '正在听你说...'
  return voiceEnabled.value ? '点击语音输入' : '语音播报已关闭，点击进行语音输入'
})

const handleSearch = () => {
  const kw = searchKeyword.value?.trim()
  if (!kw) {
    // 清空时回到当前页面（去掉查询参数）
    router.replace({ path: route.path })
    return
  }
  // 判断关键字是否像订单号：纯数字 或 LX 开头
  const isOrderNo = /^\d+$/.test(kw) || /^LX/i.test(kw)
  // 当前所在页面优先级最高，避免在订单页搜索时也被跳到菜品页
  const onOrderPage = route.path.startsWith('/admin/order')
  const onDishPage = route.path.startsWith('/admin/dish')
  if (onOrderPage || (isOrderNo && !onDishPage)) {
    router.push({ path: '/admin/order', query: { keyword: kw } })
  } else {
    router.push({ path: '/admin/dish', query: { name: kw } })
  }
}

const initVoiceRecognition = () => {
  if (recognition) return
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SpeechRecognition) return
  recognition = new SpeechRecognition()
  recognition.lang = 'zh-CN'
  recognition.continuous = false
  recognition.interimResults = false
  recognition.onstart = () => { voiceListening.value = true }
  recognition.onend = () => { voiceListening.value = false }
  recognition.onerror = (e) => {
    voiceListening.value = false
    if (e.error !== 'aborted') ElMessage.warning('语音识别失败，请重试')
  }
  recognition.onresult = (e) => {
    const text = e.results[0][0].transcript
    searchKeyword.value = text
    handleSearch()
  }
}

const toggleVoiceInput = () => {
  initVoiceRecognition()
  if (!recognition) {
    ElMessage.warning('当前浏览器不支持语音识别')
    return
  }
  if (voiceListening.value) {
    recognition.stop()
  } else {
    recognition.start()
  }
}


const isCollapsed = ref(false)
const toggleCollapse = () => { isCollapsed.value = !isCollapsed.value }

const logoSrc = ref(logoImg)

const menuList = [
  { index: 'dashboard',  icon: HomeFilled,   title: '工作台' },
  { index: 'statistics', icon: DataAnalysis, title: '数据统计' },
  { index: 'order',      icon: Document,     title: '订单管理' },
  { index: 'dish',       icon: KnifeFork,    title: '菜品管理' },
  { index: 'category',   icon: MenuIcon,     title: '分类管理' },
  { index: 'spec',       icon: Operation,    title: '规格管理' },
  { index: 'banner',     icon: Picture,      title: '轮播图管理' },
  { index: 'staff',      icon: User,         title: '员工管理' },
]

const activeMenu = computed(() => {
  const segments = route.path.split('/')
  const last = segments[segments.length - 1]?.toLowerCase()
  const matched = menuList.find(m => m.index === last)
  return matched ? matched.index : 'dashboard'
})

const cachedViews = ref([])
const notifyCount = ref(3)
const userName = computed(() => localStorage.getItem('adminUserName') || '管理员')

/* ============================
   顶部栏
   ============================ */
const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen().catch(() => {})
  } else {
    document.exitFullscreen().catch(() => {})
  }
}

const isShopOpen = ref(true)
const shopTogglePending = ref(false)

const fetchShopStatus = async () => {
  try {
    const status = await getAdminBusinessStatus()
    isShopOpen.value = status === 1
  } catch (e) {
    console.error('获取营业状态失败', e)
  }
}

const toggleShopStatus = async () => {
  if (shopTogglePending.value) return
  shopTogglePending.value = true
  try {
    if (isShopOpen.value) {
      const ok = await ElMessageBox.confirm(
          '确定要打烊吗？打烊后将暂停接收新订单。',
          '打烊确认',
          { confirmButtonText: '确认打烊', cancelButtonText: '取消', type: 'warning' }
      ).then(() => true).catch(() => false)
      if (!ok) return
    }
    const newStatus = await toggleAdminBusinessStatus()
    isShopOpen.value = newStatus === 1
    ElMessage.success(isShopOpen.value ? '已恢复营业，开始接收订单' : '已打烊，暂停接收新订单')
  } catch (e) {
    console.error('切换营业状态失败', e)
    ElMessage.error('操作失败，请稍后重试')
  } finally {
    shopTogglePending.value = false
  }
}

onMounted(() => {
  initVoiceRecognition()
  fetchShopStatus()
  startOrderPolling()
})

onUnmounted(() => {
  clearInterval(orderPollTimer)
})

// ===== 订单轮询通知 =====
let orderPollTimer = null
let lastCancelCount = 0
let lastPendingCount = 0

const startOrderPolling = async () => {
  // 初始加载一次
  try {
    const res = await request.get('/admin/order/list', { params: { page: 1, pageSize: 1, status: 4 } })
    lastCancelCount = res?.total || 0
  } catch {}
  try {
    const res = await request.get('/admin/order/list', { params: { page: 1, pageSize: 1, status: 1 } })
    lastPendingCount = res?.total || 0
  } catch {}

  // 每 10 秒轮询
  orderPollTimer = setInterval(async () => {
    try {
      // 检测取消订单
      const cancelRes = await request.get('/admin/order/list', { params: { page: 1, pageSize: 1, status: 4 } })
      const newCancel = cancelRes?.total || 0
      if (newCancel > lastCancelCount) {
        playCancelSound()
        notifyCount.value = Math.max(notifyCount.value, newCancel - lastCancelCount)
      }
      lastCancelCount = newCancel

      // 检测已支付新订单
      const pendingRes = await request.get('/admin/order/list', { params: { page: 1, pageSize: 1, status: 1 } })
      const newPending = pendingRes?.total || 0
      if (newPending > lastPendingCount) {
        playNewOrderSound()
        notifyCount.value += newPending - lastPendingCount
        // 顶部横幅播报
        ElNotification({
          title: '新订单提醒',
          message: '你有新的订单，请及时处理！',
          type: 'success',
          position: 'top-right',
          duration: 8000,
          offset: 70
        })
      }
      lastPendingCount = newPending
    } catch {}
  }, 10000)
}

/* ============================
   菜单 & 用户
   ============================ */
const handleMenuSelect = (index) => { router.push(`/admin/${index}`) }

const handleUserCmd = async (cmd) => {
  if (cmd === 'profile' || cmd === 'setting') {
    ElMessage.info('功能开发中')
  }
  if (cmd === 'logout') {
    try {
      await ElMessageBox.confirm('确定退出登录？', '退出确认', {
        confirmButtonText: '退出', cancelButtonText: '取消', type: 'warning',
      })
      ;['adminToken','adminUserId','adminUserName','adminUserType'].forEach(k => localStorage.removeItem(k))
      ElMessage.success('已退出登录')
      router.push('/admin/login')
    } catch { /* 取消 */ }
  }
}

/* ============================
   AI 客服
   ============================ */
const aiChatVisible = ref(false)
const openAiChat = () => { aiChatVisible.value = true }

const aiLeftOffset = computed(() => isCollapsed.value ? 3 : 65)
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  overflow: hidden;
}

/* ================================
   侧边栏 - B 端深石墨蓝
   ================================ */
.admin-aside {
  background: var(--bb-sidebar);
  display: flex;
  flex-direction: column;
  transition: width 0.28s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: visible;
  position: relative;
  box-shadow: 2px 0 12px rgba(0, 0, 0, 0.12);
  will-change: width;
}

/* ===== 顶部 Logo 区域 ===== */
.aside-header {
  height: 56px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  padding: 0 8px 0 20px;
  background: rgba(0, 0, 0, 0.2);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}
.aside-header.collapsed {
  padding: 0;
  justify-content: center;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 10px;
  overflow: hidden;
}
.logo-icon-wrap {
  width: 28px;
  height: 28px;
  border-radius: 7px;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.12);
}
.logo-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.logo-text-fallback {
  font-size: 12px;
  font-weight: 800;
  color: #fff;
}
.logo-text {
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  white-space: nowrap;
  letter-spacing: 0.02em;
}

/* ===== 折叠按钮 ===== */
.collapse-float {
  position: absolute;
  top: 16px;
  right: -30px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #fff;
  border: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  cursor: pointer;
  z-index: 99;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.collapse-float:hover {
  background: var(--bb-primary);
  color: #fff;
  border-color: var(--bb-primary);
  transform: scale(1.12);
}

/* ===== 菜单 ===== */
.aside-menu {
  flex: 1;
  border-right: none;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 8px 0;
  --el-menu-bg-color: transparent;
  --el-menu-text-color: #cbd5e1;
  --el-menu-active-color: #ffffff;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.05);
}
.aside-menu::-webkit-scrollbar { width: 0; }
.aside-menu :deep(.el-menu-item) {
  height: 46px;
  line-height: 46px;
  margin: 4px 10px;
  border-radius: 8px;
  font-size: 13.5px;
  transition: all 0.2s ease;
  color: #cbd5e1;
}
.aside-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}
.aside-menu :deep(.el-menu-item.is-active) {
  background: var(--bb-sidebar-active);
  color: #fff;
  font-weight: 600;
  box-shadow: 0 4px 16px rgba(23, 114, 255, 0.4);
}
.aside-menu :deep(.el-menu-item .el-icon) {
  color: inherit;
}

/* 文字过渡 */
.fade-text-enter-active,
.fade-text-leave-active { transition: opacity 0.2s ease; }
.fade-text-enter-from,
.fade-text-leave-to { opacity: 0; }

/* ================================
   顶部导航栏 - 白底
   ================================ */
.admin-header {
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
  box-shadow: var(--bb-shadow-topbar);
  border-bottom: 1px solid var(--bb-border-light);
}
.header-left { display: flex; align-items: center; margin-left: 16px; 
}
.header-right { display: flex; align-items: center; gap: 18px; }

.header-search .search-input {
  width: 220px;
  transition: width 0.3s;
}
.header-search .search-input :deep(.el-input__wrapper) {
  border-radius: var(--bb-radius-sm);
  background: #f8fafc;
  box-shadow: 0 0 0 1px var(--bb-border) inset;
}
.header-search .search-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--bb-primary-light) inset;
  background: #fff;
}
.header-search .search-input:focus-within { width: 300px; }

.header-icon {
  color: #64748b;
  cursor: pointer; padding: 5px; border-radius: 6px; transition: all .2s;
}
.header-icon:hover { color: var(--bb-primary); background: #eff6ff; }

.notify-badge :deep(.el-badge__content) { font-size: 10px; background: #ef4444; }

/* 营业状态标签 */
.shop-status-tag {
  padding: 4px 14px;
  border-radius: 14px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  user-select: none;
  transition: all 0.2s;
  background: var(--bb-success-bg);
  color: var(--bb-success);
  border: 1px solid var(--bb-success-border);
}
.shop-status-tag:hover { background: #d1fae5; }
.shop-status-tag.closed {
  background: #f3f4f6;
  color: #6b7280;
  border-color: #e5e7eb;
}
.shop-status-tag.closed:hover { background: #e5e7eb; }

/* 用户信息 */
.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px 4px 4px;
  border-radius: 8px;
  transition: background 0.2s;
}
.user-trigger:hover { background: #f1f5f9; }
.user-avatar-el { background: var(--bb-primary-bg); color: var(--bb-primary); }
.user-name { font-size: 13px; color: #374151; font-weight: 500; }
.user-arrow { color: #94a3b8; }

/* ================================
   主内容区
   ================================ */
.admin-main {
  background: #f1f5f9;
  overflow-y: auto;
  padding: 20px;
  flex: 1;
}
.admin-loading {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
  background: #fff;
  border-radius: 12px;
}

.page-slide-enter-active { transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
.page-slide-leave-active { transition: all 0.15s ease; }
.page-slide-enter-from { opacity: 0; transform: translateY(12px); }
.page-slide-leave-to   { opacity: 0; transform: translateY(-6px); }

/* ================================
   响应式
   ================================ */
@media (max-width: 768px) {
  .header-search { display: none; }
}
</style>
