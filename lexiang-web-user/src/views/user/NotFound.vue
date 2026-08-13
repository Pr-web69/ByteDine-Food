<template>
  <div class="not-found">
    <div class="nf-content">
      <div class="nf-code">404</div>
      <p class="nf-text">页面不存在</p>
      <el-button type="primary" round @click="goHome">返回首页</el-button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
const goHome = () => {
  const adminToken = localStorage.getItem('adminToken')
  router.push(adminToken ? '/admin/dashboard' : '/home')
}
</script>

<style scoped>
.not-found {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: #f5f5f7;
}
.nf-content { text-align: center; }
.nf-code { font-size: 80px; font-weight: 800; color: #3a7afe; line-height: 1; }
.nf-text { font-size: 18px; color: #64748b; margin: 12px 0 24px; }
</style>
