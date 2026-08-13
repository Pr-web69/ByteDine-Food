<template>
  <div class="page-wrap">
    <div class="page-toolbar">
      <h3 class="page-title">订单管理</h3>
      <el-button size="small" :icon="Refresh" @click="load" :loading="loading">刷新</el-button>
    </div>

    <!-- 状态筛选 tabs -->
    <div class="status-tabs">
      <el-radio-group v-model="statusFilter" @change="load">
        <el-radio-button :value="undefined">全部</el-radio-button>
        <el-radio-button :value="0">待支付</el-radio-button>
        <el-radio-button :value="1">待接单</el-radio-button>
        <el-radio-button :value="2">待配送</el-radio-button>
        <el-radio-button :value="3">已完成</el-radio-button>
        <el-radio-button :value="4">已取消</el-radio-button>
      </el-radio-group>
      <el-input v-model="keyword" placeholder="搜索订单号" clearable style="width:200px" @change="load" class="keyword-input" />
    </div>

    <!-- 删掉了stripe，避免斑马纹覆盖高亮底色 -->
    <el-table :data="list" v-loading="loading" empty-text="暂无订单" :row-class-name="rowClassName">
      <el-table-column prop="orderNo" label="订单号" width="170" />
      <el-table-column label="菜品" min-width="200">
        <template #default="{ row }">{{ row.details ? row.details.map(d => d.dishName||'').join('、') : '-' }}</template>
      </el-table-column>
      <el-table-column prop="totalAmount" label="金额" width="100">
        <template #default="{ row }">￥{{ Number(row.totalAmount).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="stat(row.status)" :effect="row.status === 1 ? 'dark' : 'light'" size="small">{{ row.statusText }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="时间" width="120">
        <template #default="{ row }">{{ (row.createTime||'').slice(0,16).replace('T',' ') }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button size="small" link type="primary" @click="$router.push(`/admin/order/${row.id}`)">详情</el-button>
          <el-button v-if="row.status===1" size="small" type="primary" @click="accept(row.id)">接单</el-button>
          <el-button v-if="row.status===2" size="small" type="success" @click="complete(row.id)">完成</el-button>
          <el-button v-if="row.status===0||row.status===1" size="small" type="danger" @click="cancel(row.id)">拒单</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination" v-if="total > 10">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="10" @current-change="onPage" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request'

const route = useRoute()
const list = ref([]), total = ref(0), page = ref(1), loading = ref(false)
const statusFilter = ref(undefined), keyword = ref('')

let pollTimer = null

const stat = s => ({ 0:'warning',1:'danger',2:'primary',3:'success',4:'info' }[s]||'')

// 待接单（status=1）整行红色边框高亮，提醒商家及时处理
const rowClassName = ({ row }) => (row.status === 1 ? 'order-row-wait' : '')

const load = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/order/list', {
      params: { page: page.value, pageSize: 10, status: statusFilter.value, keyword: keyword.value || undefined }
    })
    list.value = res.records || []; total.value = res.total || 0
  } catch (e) { console.error('订单列表加载失败:', e) } finally { loading.value = false }
}

watch(() => route.query.keyword, (v) => {
  keyword.value = v || ''
  load()
}, { immediate: false })

onMounted(() => {
  load()
  // 每 30 秒自动轮询一次订单列表，确保新支付订单及时显示
  pollTimer = setInterval(load, 30000)
})

onBeforeUnmount(() => {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
})

const onPage = p => { page.value = p; load() }

const accept = async id => {
  try { await request.put(`/admin/order/${id}/accept`); ElMessage.success('已接单，订单更新为待配送'); load() } catch (e) { console.error('接单失败:', e) }
}
const complete = async id => {
  try { await request.put(`/admin/order/${id}/complete`); ElMessage.success('订单已完成'); load() } catch (e) { console.error('完成订单失败:', e) }
}
const cancel = async id => {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入拒单原因', '拒单', {
      inputValue: '商家拒单',
      confirmButtonText: '确认拒单',
      cancelButtonText: '返回',
      inputValidator: (v) => v?.trim() ? true : '取消原因不能为空',
      type: 'warning'
    })
    await request.put(`/admin/order/${id}/cancel`, { reason: reason.trim() })
    ElMessage.success('已拒单')
    load()
  } catch (e) {
    if (e !== 'cancel') console.error('取消订单失败:', e)
  }
}
</script>

<style scoped>
.page-wrap { max-width: 1200px; }
.page-toolbar { margin-bottom: 16px; }
.page-title { font-size: 18px; font-weight: 600; margin: 0; }
.status-tabs { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; flex-wrap: wrap; }
.keyword-input { margin-left: auto; }
.pagination { display: flex; justify-content: center; margin-top: 16px; }

/* 待接单高亮样式 */
:deep(.el-table__body > tbody > tr.order-row-wait > td) {
  background-color: #fef0f0 !important;
}
:deep(.el-table__body > tbody > tr.order-row-wait > td:first-child) {
  box-shadow: inset 3px 0 0 #f56c6c !important;
}
</style>
