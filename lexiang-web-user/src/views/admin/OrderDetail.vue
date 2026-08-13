<template>
  <div class="admin-order-detail" v-loading="loading">
    <div class="detail-header">
      <el-button :icon="ArrowLeft" @click="$router.back()">返回</el-button>
      <h3 class="detail-title">订单详情</h3>
    </div>

    <template v-if="order">
      <!-- 订单状态 -->
      <section class="info-card">
        <div class="status-bar">
          <el-tag :type="statusTagType(order.status)" size="large" effect="dark">
            {{ order.statusText }}
          </el-tag>
          <span class="order-no">订单号：{{ order.orderNo }}</span>
        </div>
      </section>

      <!-- 订单信息 -->
      <section class="info-card">
        <h4 class="card-title">订单信息</h4>
        <div class="info-row"><span>下单时间</span><span>{{ formatTime(order.createTime) }}</span></div>
        <div class="info-row" v-if="order.payTime"><span>支付时间</span><span>{{ formatTime(order.payTime) }}</span></div>
        <div class="info-row" v-if="order.cancelTime"><span>取消时间</span><span>{{ formatTime(order.cancelTime) }}</span></div>
        <div class="info-row" v-if="order.finishTime"><span>完成时间</span><span>{{ formatTime(order.finishTime) }}</span></div>
        <div class="info-row" v-if="order.remark"><span>备注</span><span>{{ order.remark }}</span></div>
        <div class="info-row" v-if="order.cancelReason"><span>取消原因</span><span class="cancel-reason">{{ order.cancelReason }}</span></div>
      </section>

      <!-- 收货信息 -->
      <section class="info-card" v-if="order.consignee">
        <h4 class="card-title">收货信息</h4>
        <div class="info-row"><span>收货人</span><span>{{ order.consignee }}</span></div>
        <div class="info-row"><span>联系电话</span><span>{{ order.phone }}</span></div>
        <div class="info-row"><span>配送地址</span><span>{{ order.address }}</span></div>
      </section>

      <!-- 菜品明细 -->
      <section class="info-card">
        <h4 class="card-title">菜品明细</h4>
        <el-table :data="order.details || []" stripe>
          <el-table-column label="菜品" min-width="200">
            <template #default="{ row }">
              <div class="dish-cell">
                <img v-if="row.dishImage" :src="row.dishImage" class="dish-thumb" />
                <span>{{ row.dishName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="specInfo" label="规格" width="150" />
          <el-table-column label="单价" width="100" align="right">
            <template #default="{ row }">￥{{ Number(row.price).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" align="center" />
          <el-table-column label="小计" width="100" align="right">
            <template #default="{ row }">
              <span class="amount">￥{{ Number(row.amount).toFixed(2) }}</span>
            </template>
          </el-table-column>
        </el-table>
        <div class="total-row">
          <span>合计</span>
          <span class="total-amount">￥{{ Number(order.totalAmount).toFixed(2) }}</span>
        </div>
      </section>

      <!-- 商家操作区 -->
      <section class="info-card actions-card" v-if="canOperate">
        <h4 class="card-title">商家操作</h4>
        <div class="actions">
          <!-- 待接单：接单 + 拒单 -->
          <el-button v-if="order.status === 1" type="primary" @click="handleAccept">接单</el-button>
          <el-button v-if="order.status === 1" type="danger" plain @click="handleCancel">拒单</el-button>
          <!-- 待支付：拒单 -->
          <el-button v-if="order.status === 0" type="danger" plain @click="handleCancel">拒单</el-button>
          <!-- 待配送：完成订单 -->
          <el-button v-if="order.status === 2" type="success" @click="handleComplete">完成订单</el-button>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getAdminOrderDetail, acceptOrder, adminCancelOrder, completeOrder } from '@/api/order'

const route = useRoute()
const order = ref(null)
const loading = ref(true)

const canOperate = computed(() => {
  const s = order.value?.status
  return s === 0 || s === 1 || s === 2
})

const statusTagType = (s) => ({ 0: 'warning', 1: 'danger', 2: 'primary', 3: 'success', 4: 'info' }[s] || 'info')
const formatTime = (t) => t ? t.replace('T', ' ').slice(0, 19) : '-'

const reload = async () => {
  loading.value = true
  try {
    order.value = await getAdminOrderDetail(route.params.id)
  } catch (e) {
    ElMessage.error('加载订单详情失败')
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(reload)

const handleAccept = async () => {
  try {
    await acceptOrder(order.value.id)
    ElMessage.success('已接单，订单状态更新为待配送')
    await reload()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

const handleCancel = async () => {
  const label = order.value.status === 1 ? '拒单' : '拒单'
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入' + label + '原因', label, {
      inputValue: order.value.status === 1 ? '商家拒单' : '商家拒单',
      confirmButtonText: '确认' + label,
      cancelButtonText: '返回',
      inputValidator: (v) => v?.trim() ? true : '原因不能为空',
      type: 'warning'
    })
    await adminCancelOrder(order.value.id, reason.trim())
    ElMessage.success('已' + label)
    await reload()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.message || '操作失败')
  }
}

const handleComplete = async () => {
  try {
    await ElMessageBox.confirm('确认完成该订单？', '完成订单', { type: 'success' })
    await completeOrder(order.value.id)
    ElMessage.success('订单已完成')
    await reload()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.message || '操作失败')
  }
}
</script>

<style scoped>
.admin-order-detail { max-width: 800px; }
.detail-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.detail-title { margin: 0; font-size: 18px; font-weight: 600; }
.info-card { background: #fff; border-radius: 12px; padding: 20px; margin-bottom: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.card-title { font-size: 15px; font-weight: 600; margin: 0 0 12px; color: #1e293b; }
.status-bar { display: flex; align-items: center; gap: 12px; }
.order-no { font-size: 13px; color: #64748b; font-family: monospace; }
.info-row { display: flex; justify-content: space-between; padding: 6px 0; font-size: 14px; color: #475569; }
.cancel-reason { color: #ef4444; }
.dish-cell { display: flex; align-items: center; gap: 8px; }
.dish-thumb { width: 36px; height: 36px; border-radius: 6px; object-fit: cover; }
.amount { font-weight: 600; color: #f97316; }
.total-row { display: flex; justify-content: flex-end; align-items: center; gap: 12px; margin-top: 12px; font-size: 16px; }
.total-amount { font-size: 22px; font-weight: 800; color: #f97316; }
.actions { display: flex; gap: 12px; }
</style>
