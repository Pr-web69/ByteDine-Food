<template>
  <div class="ml-page">
    <!-- 左侧品牌视觉区 -->
    <div class="ml-left">
      <div class="ml-bg"></div>
      <div class="ml-overlay"></div>
      <div class="ml-decoration">
        <div class="deco-grid"></div>
        <div class="deco-line l1"></div>
        <div class="deco-line l2"></div>
        <div class="deco-line l3"></div>
      </div>
      <div class="ml-left-content">
        <div class="ml-brand">
          <h1 class="ml-title">商家管理后台</h1>
          <p class="ml-subtitle">ByteBites · Merchant Console</p>
        </div>
        <p class="ml-desc">安全登录，管理您的店铺经营数据</p>
        <div class="ml-features">
          <div class="ml-feature" v-for="f in features" :key="f.title">
            <span class="ml-feature-icon" v-html="f.icon"></span>
            <span class="ml-feature-title">{{ f.title }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧登录表单 -->
    <div class="ml-right">
      <div class="ml-card" :class="{ 'is-visible': cardVisible }">
        <div class="card-header">
          <h2 class="card-title">商家登录</h2>
          <p class="card-desc">管理店铺，查看经营数据</p>
        </div>

        <el-form :model="form" size="large" class="login-form" @submit.prevent="handleLogin">
          <el-form-item>
            <el-input v-model="form.phone" placeholder="请输入用户名" prefix-icon="User" autocomplete="off" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock"
              show-password @keyup.enter="handleLogin" autocomplete="off" />
          </el-form-item>
          <div class="forgot-row">
            <el-link type="primary" :underline="false" @click="handleForgot">忘记密码？</el-link>
          </div>
          <el-button type="primary" class="login-btn" :loading="loading" :disabled="!form.phone || !form.password" native-type="submit">
            商家登录
          </el-button>
        </el-form>
      </div>
    </div>

    <!-- 忘记密码重置弹窗 -->
    <el-dialog v-model="resetVisible" title="重置密码" width="340px" :close-on-click-modal="false" align-center>
      <div class="reset-dialog">
        <el-input v-model="resetForm.username" placeholder="请输入用户名" clearable />
        <div class="code-row">
          <el-input v-model="resetForm.code" placeholder="请输入6位验证码" maxlength="6" />
          <el-button class="code-btn" :disabled="countdown > 0 || !validUsername" :loading="sendingCode" @click="handleSendCode">
            {{ countdown > 0 ? countdown + 's' : '获取验证码' }}
          </el-button>
        </div>
        <p class="reset-tip"> </p>
      </div>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetLoading" :disabled="!canReset" @click="handleReset">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const loading = ref(false)
const cardVisible = ref(false)
const form = reactive({ phone: '', password: '' })

const features = [
  { icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#F97316" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 3v18h18"/><rect x="7" y="10" width="3" height="8" rx="1"/><rect x="13" y="6" width="3" height="12" rx="1"/><rect x="19" y="14" width="0.5" height="4" rx="0.25"/></svg>', title: '经营数据' },
  { icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#2563EB" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>', title: '订单管理' },
  { icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#10B981" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>', title: '实时营业' },
]

onMounted(() => {
  requestAnimationFrame(() => { cardVisible.value = true })
})

const handleLogin = async () => {
  if (!form.phone || !form.password) return
  loading.value = true
  try {
    const data = await request.post('/merchant/login', form)
    ;['adminToken','adminUserId','adminUserName','adminUserType'].forEach(k => localStorage.removeItem(k))
    localStorage.setItem('adminToken', data.token)
    localStorage.setItem('adminUserId', data.userId)
    localStorage.setItem('adminUserName', data.userName || '管理员')
    localStorage.setItem('adminUserType', '2')
    ElMessage.success('登录成功')
    router.push('/admin/dashboard')
  } catch (e) {
    ElMessage.error(e?.message || '登录失败，请检查账号密码')
  } finally { loading.value = false }
}

/* ── 忘记密码重置 ── */
const resetVisible = ref(false)
const resetForm = reactive({ username: '', code: '' })
const sendingCode = ref(false)
const resetLoading = ref(false)
const countdown = ref(0)
let countdownTimer = null

const validUsername = computed(() => resetForm.username.trim() !== '')
const canReset = computed(() => validUsername.value && /^\d{6}$/.test(resetForm.code))

const handleForgot = () => {
  resetForm.username = form.phone || ''
  resetForm.code = ''
  countdown.value = 0
  resetVisible.value = true
}

const handleSendCode = async () => {
  if (!validUsername.value) { ElMessage.warning('请输入用户名'); return }
  sendingCode.value = true
  try {
    await request.post('/merchant/send-code', { username: resetForm.username.trim() })
    ElMessage.success('验证码已发送，请查看后端控制台')
    countdown.value = 60
    clearInterval(countdownTimer)
    countdownTimer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(countdownTimer)
    }, 1000)
  } catch (e) {
    ElMessage.error(e?.message || '发送失败')
  } finally { sendingCode.value = false }
}

const handleReset = async () => {
  if (!canReset.value) return
  resetLoading.value = true
  try {
    await request.post('/merchant/forgot-password', { username: resetForm.username.trim(), code: resetForm.code.trim() })
    resetVisible.value = false
    form.password = ''
    ElMessage.success('密码已重置为 888888，请用新密码登录')
  } catch (e) {
    ElMessage.error(e?.message || '重置失败')
  } finally { resetLoading.value = false }
}

onBeforeUnmount(() => { if (countdownTimer) clearInterval(countdownTimer) })
</script>

<style scoped>
.ml-page {
  display: flex;
  min-height: 100vh;
  background: #ffffff;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  overflow: hidden;
}

/* ===== 左侧品牌区 ===== */
.ml-left {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  overflow: hidden;
}

.ml-bg {
  position: absolute;
  inset: 0;
  background:
    url('/images/Regis.jpg') center/100% 120% no-repeat ,
    linear-gradient(160deg, rgba(30,64,175,0.55) 0%, rgba(37,99,235,0.45) 30%, rgba(59,130,246,0.40) 60%, rgba(96,165,250,0.35) 100%);
  z-index: 0;
}

.ml-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg,
    rgba(30, 64, 175, 0.20) 0%,
    rgba(37, 99, 235, 0.15) 50%,
    rgba(96, 165, 250, 0.10) 100%);
  z-index: 1;
}

.ml-decoration {
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
  overflow: hidden;
}

.deco-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.03) 1px, transparent 1px);
  background-size: 60px 60px;
}

.deco-line {
  position: absolute;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.15), transparent);
}

.deco-line.l1 { width: 60%; top: 35%; left: -10%; transform: rotate(-25deg); }
.deco-line.l2 { width: 50%; top: 55%; right: -10%; transform: rotate(15deg); }
.deco-line.l3 { width: 40%; bottom: 25%; left: 5%; transform: rotate(-10deg); }

.ml-left-content {
  position: relative;
  z-index: 3;
  padding: 48px;
  max-width: 460px;
  color: #fff;
}

.ml-brand {
  margin-bottom: 28px;
}

.ml-logo {
  width: 80px;
  height: 80px;
  border-radius: 20px;
  margin-bottom: 18px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.18), 0 0 0 1px rgba(255, 255, 255, 0.12);
}

.ml-title {
  font-size: 34px;
  font-weight: 800;
  margin: 0 0 6px;
  letter-spacing: -0.5px;
}

.ml-subtitle {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.55);
  margin: 0;
  letter-spacing: 1px;
}

.ml-desc {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.75);
  margin: 0 0 40px;
  line-height: 1.6;
}

.ml-features {
  display: flex;
  gap: 12px;
}

.ml-feature {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 18px 12px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  backdrop-filter: blur(8px);
  transition: all 0.25s;
}

.ml-feature:hover {
  background: rgba(255, 255, 255, 0.1);
  transform: translateY(-2px);
}

.ml-feature-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.ml-feature-title {
  font-size: 13px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.85);
}

/* ===== 右侧登录区 ===== */
.ml-right {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 460px;
  min-height: 100vh;
  background: linear-gradient(180deg, #f7f9ff 0%, #eef4ff 100%);
  padding: 48px;
  flex-shrink: 0;
}

.ml-card {
  width: 100%;
  max-width: 360px;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.5s cubic-bezier(0.22, 1, 0.36, 1);
}

.ml-card.is-visible {
  opacity: 1;
  transform: translateY(0);
}

.card-header {
  margin-bottom: 28px;
}

.card-title {
  font-size: 26px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 6px;
}

.card-desc {
  font-size: 14px;
  color: #94a3b8;
  margin: 0;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 12px;
  padding: 4px 14px;
  height: 48px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
  transition: all 0.25s ease;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #93c5fd inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #2563eb inset, 0 0 0 4px rgba(37, 99, 235, 0.08);
}

.forgot-row {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 4px;
}

/* ===== 忘记密码重置弹窗 ===== */
.reset-dialog {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.code-row {
  display: flex;
  gap: 8px;
}
.code-row .el-input {
  flex: 1;
}
.code-btn {
  flex-shrink: 0;
  width: 110px;
}
.reset-tip {
  margin: 0;
  font-size: 12px;
  color: #94a3b8;
}

.login-btn {
  width: 100%;
  height: 48px;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 1px;
  margin-top: 8px;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.35);
  transition: all 0.25s ease;
}

.login-btn:disabled {
  background: #cbd5e1;
  border-color: #cbd5e1;
  box-shadow: none;
  cursor: not-allowed;
}

.login-btn:not(:disabled):hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.4);
}

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .ml-page {
    flex-direction: column;
  }

  .ml-left {
    min-height: auto;
    padding: 32px 24px;
  }

  .ml-left-content {
    padding: 0;
    max-width: 100%;
    text-align: center;
  }

  .ml-logo {
    width: 52px;
    height: 52px;
  }

  .ml-title {
    font-size: 26px;
  }

  .ml-features {
    display: none;
  }

  .ml-right {
    width: 100%;
    min-height: auto;
    padding: 32px 24px 48px;
  }

  .ml-card {
    max-width: 400px;
    margin: 0 auto;
  }
}
</style>
