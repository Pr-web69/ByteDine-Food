<template>
  <div class="login-page">
    <div class="page-header">
      <div class="header-bg"></div>
      <div class="header-nav"><el-button text class="back-btn" @click="$router.push('/login')"><el-icon><ArrowLeft /></el-icon><span>返回登录</span></el-button></div>
      <h1 class="brand">📝 注册账号</h1>
      <p class="slogan">加入字节餐饮，开启美食之旅</p>
    </div>

    <div class="content-wrap">
      <el-form ref="formRef" :model="form" :rules="rules" size="large">
        <el-form-item prop="phone"><el-input v-model="form.phone" placeholder="请输入手机号" prefix-icon="Phone" maxlength="11" clearable /></el-form-item>
        <el-form-item prop="nickname"><el-input v-model="form.nickname" placeholder="请输入昵称" prefix-icon="User" maxlength="20" /></el-form-item>
        <el-form-item prop="password"><el-input v-model="form.password" type="password" placeholder="请输入密码（至少6位）" prefix-icon="Lock" show-password /></el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-btn" :loading="loading" @click="handleRegister">注 册</el-button>
        </el-form-item>
      </el-form>
      <div class="footer">已有账号？<el-link type="primary" @click="$router.push('/login')">去登录</el-link></div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/user'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const form = reactive({ phone: '', nickname: '', password: '' })
const rules = {
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }, { pattern: /^1[3-9]\d{9}$/, message: '格式不正确', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '至少6位', trigger: 'blur' }]
}

const handleRegister = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try { await register(form); ElMessage.success('注册成功，请登录'); router.push('/login') } catch (e) { console.error('注册失败:', e) } finally { loading.value = false }
}
</script>

<style scoped>
.login-page { min-height: 100vh; background: var(--bg-card); font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif; }
.page-header { position: relative; padding: 24px 20px 36px; overflow: hidden; }
.header-bg { position: absolute; inset: 0; background: linear-gradient(135deg, var(--color-primary) 0%, #6b9dff 100%); border-radius: 0 0 32px 32px; }
.header-nav { position: relative; z-index: 2; margin-bottom: 12px; }
.back-btn { color: rgba(255,255,255,.9) !important; font-size: 13px; }
.brand { position: relative; z-index: 1; text-align: center; font-size: 28px; font-weight: 700; color: #fff; margin-bottom: 6px; }
.slogan { position: relative; z-index: 1; text-align: center; font-size: 14px; color: rgba(255,255,255,.75); }
.content-wrap { padding: 24px 24px 0; max-width: 380px; margin: 0 auto; }
.login-btn { width: 100%; height: 48px; border-radius: 12px; font-size: 16px; font-weight: 600; box-shadow: 0 4px 14px rgba(37,99,235,.3); transition: all .2s; }
.login-btn:hover { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(37,99,235,.4); }
:deep(.el-input__wrapper) { border-radius: 10px; box-shadow: 0 0 0 1px var(--border-color) inset; transition: all .2s; }
:deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px var(--color-primary-light) inset; }
:deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 2px var(--color-primary) inset; }
.footer { text-align: center; font-size: 13px; color: var(--text-secondary); margin-top: 16px; }
</style>
