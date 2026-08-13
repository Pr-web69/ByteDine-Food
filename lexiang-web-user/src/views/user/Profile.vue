<template>
  <div class="profile-page">
    <div class="profile-header">
      <div class="header-bg"></div>
      <div class="avatar-wrap"><img :src="avatar" class="avatar" /></div>
      <h2 class="username">{{ userName }}</h2>
      <p class="uid">UID: {{ userId }}</p>
    </div>

    <div class="content-wrap">
      <div class="menu-card">
        <div class="menu-item" @click="$router.push('/address')"><div class="menu-left"><el-icon :size="18"><Location /></el-icon><span>收货地址</span></div><el-icon><ArrowRight /></el-icon></div>
        <div class="menu-item" @click="$router.push('/order')"><div class="menu-left"><el-icon :size="18"><Document /></el-icon><span>我的订单</span></div><el-icon><ArrowRight /></el-icon></div>
        <div class="menu-item" @click="toggleVoice">
          <div class="menu-left"><el-icon :size="18"><component :is="voiceEnabled ? 'Microphone' : 'MuteNotification'" /></el-icon><span>语音播报</span></div>
          <el-switch :model-value="voiceEnabled" size="small" @click.stop />
        </div>
      </div>
      <el-button class="logout-btn" @click="handleLogout">退出登录</el-button>
      <div class="version">字节餐饮 v1.0.0</div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ArrowRight, Location, Document, Microphone, MuteNotification } from '@element-plus/icons-vue'
import { useVoice } from '@/composables/useVoice'
import logo from '@/assets/logo-icon.png'

const router = useRouter()
const { enabled: voiceEnabled, toggle: toggleVoice } = useVoice()
const userName = ref(localStorage.getItem('userName') || localStorage.getItem('adminUserName') || '用户')
const userId = ref(localStorage.getItem('userId') || '')
const avatar = logo

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定退出登录？', '提示', { type: 'warning', confirmButtonText: '退出' })
    ;['token','userId','userName','userType'].forEach(k => localStorage.removeItem(k))
    router.push('/login')
  } catch (e) {
    if (e !== 'cancel') console.error('退出登录异常:', e)
  }
}
</script>

<style scoped>
.profile-page { --bg: #f8fafc; min-height: 100vh; background: var(--bg); padding-bottom: 60px; font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif; }
.profile-header { position: relative; padding: 40px 24px 32px; overflow: hidden; text-align: center; }
.header-bg { position: absolute; inset: 0; background: linear-gradient(135deg, var(--color-primary) 0%, #6b9dff 100%); border-radius: 0 0 32px 32px; }
.avatar-wrap { position: relative; z-index: 1; display: inline-block; }
.avatar { width: 80px; height: 80px; border-radius: 50%; object-fit: cover; border: 3px solid rgba(255,255,255,.4); }
.username { position: relative; z-index: 1; font-size: 20px; font-weight: 700; color: #fff; margin: 10px 0 4px; }
.uid { position: relative; z-index: 1; font-size: 13px; color: rgba(255,255,255,.7); margin: 0; }
.content-wrap { padding: 20px 16px; }
.menu-card { background: #fff; border-radius: 14px; overflow: hidden; box-shadow: 0 1px 4px rgba(0,0,0,.04); margin-bottom: 24px; }
.menu-item { display: flex; justify-content: space-between; align-items: center; padding: 16px; border-bottom: 1px solid #f0eeeb; cursor: pointer; transition: background .15s; }
.menu-item:last-child { border-bottom: none; }
.menu-item:hover { background: #f8fafc; }
.menu-left { display: flex; align-items: center; gap: 10px; font-size: 14px; font-weight: 500; }
.logout-btn { width: 100%; height: 46px; border-radius: 12px; font-weight: 500; }
.version { text-align: center; font-size: 12px; color: var(--text-placeholder); margin-top: 20px; }
</style>
