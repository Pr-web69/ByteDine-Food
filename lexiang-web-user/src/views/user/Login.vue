<template>
  <div class="login-page">
    <!-- 左侧品牌视觉区 -->
    <div class="login-left">
      <div class="left-bg"></div>
      <div class="left-overlay"></div>
      <div class="left-decoration">
        <div class="deco-circle c1"></div>
        <div class="deco-circle c2"></div>
        <div class="deco-circle c3"></div>
      </div>
      <div class="left-content">
        <div class="brand-area">
          <img src="@/assets/logo-icon.png" class="brand-logo" alt="字节餐饮" />
          <h1 class="brand-name">字节智能餐饮</h1>
          <p class="brand-name-en">ByteBites · Lexiang Food</p>
        </div>
        <p class="brand-desc">校园智慧餐饮平台，AI 驱动的个性化点餐体验</p>
        <div class="feature-list">
          <div class="feature-item" v-for="f in features" :key="f.title">
            <span class="feature-icon" v-html="f.icon"></span>
            <div class="feature-text">
              <span class="feature-title">{{ f.title }}</span>
              <span class="feature-desc">{{ f.desc }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧登录区 -->
    <div class="login-right">
      <div class="right-content" :class="{ 'is-visible': cardVisible }">
        <div class="card-header">
          <h2 class="card-title">欢迎回来</h2>
          <p class="card-desc">手机号登录，享受校园美食</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" size="large" class="login-form" @submit.prevent="handleLogin">
          <input type="text" style="position:absolute;opacity:0;height:0;width:0" tabindex="-1" />
          <input type="password" style="position:absolute;opacity:0;height:0;width:0" tabindex="-1" />
          <el-form-item prop="phone">
            <el-input ref="phoneInputRef" v-model="form.phone" placeholder="请输入手机号" :prefix-icon="Phone"
              maxlength="11" clearable autocomplete="off" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" :prefix-icon="Lock"
              show-password autocomplete="off" @keyup.enter="handleLogin" />
          </el-form-item>

          <div class="form-extra">
            <el-checkbox v-model="rememberMe" label="记住账号" size="small" />
            <el-link type="primary" :underline="false" class="forgot-link" @click="handleForgot">忘记密码？</el-link>
          </div>

          <el-form-item>
            <el-button type="primary" class="login-btn" :class="{ 'btn-disabled': !canSubmit }"
              :loading="loading" :disabled="!canSubmit || loading" @click="handleLogin">
              {{ loading ? '登录中...' : '登 录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="card-footer">
          <p class="register-hint">
            还没有账号？<el-link type="primary" :underline="false" @click="goRegister">立即注册</el-link>
          </p>
        </div>
      </div>
    </div>

    <!-- 忘记密码重置弹窗 -->
    <el-dialog v-model="resetVisible" title="重置密码" width="340px" :close-on-click-modal="false" align-center>
      <div class="reset-dialog">
        <el-input v-model="resetForm.phone" placeholder="请输入手机号" maxlength="11" clearable />
        <div class="code-row">
          <el-input v-model="resetForm.code" placeholder="请输入6位验证码" maxlength="6" />
          <el-button class="code-btn" :disabled="countdown > 0 || !validPhone" :loading="sendingCode" @click="handleSendCode">
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
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Phone, Lock } from '@element-plus/icons-vue'
import { login, forgotPassword, sendResetCode } from '@/api/user'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)
const phoneInputRef = ref(null)
const loading = ref(false)
const cardVisible = ref(false)
const rememberMe = ref(false)

const form = reactive({ phone: '', password: '' })
const canSubmit = computed(() => form.phone.trim() && form.password.trim())

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const features = [
  { icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="8" width="16" height="12" rx="3"/><circle cx="9" cy="13" r="1" fill="currentColor" stroke="none"/><circle cx="15" cy="13" r="1" fill="currentColor" stroke="none"/><path d="M9 16c1 1 3 1 6 0"/><line x1="12" y1="8" x2="12" y2="4"/><circle cx="12" cy="3.5" r=".8" fill="currentColor" stroke="none"/></svg>', title: 'AI 智能推荐', desc: '基于口味偏好，精准推荐菜品' },
  { icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></svg>', title: '极速配送', desc: '校园内30分钟送达' },
  { icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 3v18h18"/><rect x="7" y="10" width="3" height="8" rx="1"/><rect x="13" y="6" width="3" height="12" rx="1"/><rect x="19" y="14" width="0.5" height="4" rx="0.25"/></svg>', title: '数据分析', desc: '商家经营数据一目了然' },
]

onMounted(() => {
  const savedPhone = localStorage.getItem('user_phone')
  if (savedPhone) { form.phone = savedPhone; rememberMe.value = true }
  requestAnimationFrame(() => { cardVisible.value = true })
  setTimeout(() => phoneInputRef.value?.focus(), 600)
})

const handleLogin = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const data = await login(form.phone, form.password)
    ;['token','userId','userName','userType'].forEach(k => localStorage.removeItem(k))
    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', data.userId)
    localStorage.setItem('userName', data.userName || '')
    localStorage.setItem('userType', '1')
    rememberMe.value
      ? localStorage.setItem('user_phone', form.phone)
      : localStorage.removeItem('user_phone')
    ElMessage.success('登录成功')
    router.replace(route.query.redirect || '/home')
  } catch (e) {
    ElMessage.error(e?.message || '登录失败')
  } finally { loading.value = false }
}

const goRegister = () => router.push('/register')

/* ── 忘记密码重置 ── */
const resetVisible = ref(false)
const resetForm = reactive({ phone: '', code: '' })
const sendingCode = ref(false)
const resetLoading = ref(false)
const countdown = ref(0)
let countdownTimer = null

const validPhone = computed(() => /^1[3-9]\d{9}$/.test(resetForm.phone))
const canReset = computed(() => validPhone.value && /^\d{6}$/.test(resetForm.code))

const handleForgot = () => {
  resetForm.phone = form.phone || ''
  resetForm.code = ''
  countdown.value = 0
  resetVisible.value = true
}

const handleSendCode = async () => {
  if (!validPhone.value) { ElMessage.warning('请输入正确的手机号'); return }
  sendingCode.value = true
  try {
    await sendResetCode(resetForm.phone.trim())
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
    await forgotPassword(resetForm.phone.trim(), resetForm.code.trim())
    resetVisible.value = false
    form.password = ''
    ElMessage.success('密码已重置为 123456，请用新密码登录')
  } catch (e) {
    ElMessage.error(e?.message || '重置失败')
  } finally { resetLoading.value = false }
}

onBeforeUnmount(() => { if (countdownTimer) clearInterval(countdownTimer) })
</script>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
  background: #ffffff;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  overflow: hidden;
}

/* ===== 左侧品牌区 ===== */
.login-left {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  min-height: 100vh;
}

.left-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #3b7ddd 0%, #4b8ff0 40%, #60a5fa 70%, #93c5fd 100%);
  z-index: 0;
}


.left-overlay {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 80%, rgba(6, 182, 212, 0.2) 0%, transparent 45%),
    radial-gradient(circle at 80% 20%, rgba(99, 102, 241, 0.25) 0%, transparent 45%);
  z-index: 1;
}

.left-decoration {
  position: absolute;
  inset: 0;
  z-index: 2;
  overflow: hidden;
  pointer-events: none;
}

.deco-circle {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.deco-circle.c1 {
  width: 500px;
  height: 500px;
  top: -120px;
  right: -120px;
}

.deco-circle.c2 {
  width: 360px;
  height: 360px;
  bottom: -80px;
  left: -80px;
}

.deco-circle.c3 {
  width: 200px;
  height: 200px;
  top: 40%;
  right: 15%;
  background: rgba(255, 255, 255, 0.03);
  border: none;
}

.left-content {
  position: relative;
  z-index: 3;
  padding: 48px;
  max-width: 480px;
  color: #fff;
}

.brand-area {
  margin-bottom: 24px;
}

.brand-logo {
  width: 96px;
  height: 96px;
  border-radius: 20px;
  margin-bottom: 20px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.2), 0 0 0 1px rgba(255, 255, 255, 0.12);
}



.brand-name {
  font-size: 36px;
  font-weight: 800;
  color: #fff;
  margin: 0 0 6px;
  letter-spacing: -0.5px;
}

.brand-name-en {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.65);
  margin: 0;
  letter-spacing: 1px;
}

.brand-desc {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.8);
  margin: 0 0 40px;
  line-height: 1.6;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 14px;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.12);
  transition: all 0.3s ease;
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.16);
  transform: translateX(4px);
}

.feature-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}

.feature-text {
  display: flex;
  flex-direction: column;
}

.feature-title {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
}

.feature-desc {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.65);
  margin-top: 2px;
}

/* ===== 右侧登录区 ===== */
.login-right {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 460px;
  min-height: 100vh;
  background: linear-gradient(180deg, #f7f9ff 0%, #eef4ff 100%);
  padding: 48px;
  flex-shrink: 0;
}


.right-content {
  width: 100%;
  max-width: 360px;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.5s cubic-bezier(0.22, 1, 0.36, 1);
}

.right-content.is-visible {
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

.login-form :deep(.el-form-item:last-child) {
  margin-bottom: 0;
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

.form-extra {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 22px;
}

.login-btn {
  width: 100%;
  height: 48px;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 1px;
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

.card-footer {
  margin-top: 28px;
  text-align: center;
}

.register-hint {
  font-size: 13px;
  color: #94a3b8;
  margin: 0;
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

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .login-page {
    flex-direction: column;
  }

  .login-left {
    min-height: auto;
    padding: 32px 24px;
  }

  .left-content {
    padding: 0;
    max-width: 100%;
    text-align: center;
  }

  .brand-logo {
    width: 56px;
    height: 56px;
  }

  .brand-name {
    font-size: 26px;
  }

  .feature-list {
    display: none;
  }

  .login-right {
    width: 100%;
    min-height: auto;
    padding: 32px 24px 48px;
    box-shadow: none;
  }

  .right-content {
    max-width: 400px;
    margin: 0 auto;
  }
}
</style>
