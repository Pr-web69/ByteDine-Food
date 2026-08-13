<template>
  <div class="stats-page">
    <div class="page-header-bar">
      <h3 class="page-title">数据统计</h3>
      <el-button type="primary" plain size="small" @click="load" :loading="loading">刷新数据</el-button>
    </div>

    <!-- KPI Cards -->
    <el-row :gutter="16" class="kpi-row">
      <el-col :xs="12" :sm="6"><div class="kpi-card"><div class="kpi-label">今日订单</div><div class="kpi-value blue">{{ data.todayOrders || 0 }}</div></div></el-col>
      <el-col :xs="12" :sm="6"><div class="kpi-card"><div class="kpi-label">今日营业额</div><div class="kpi-value orange">¥{{ (data.todayRevenue || 0).toFixed(2) }}</div></div></el-col>
      <el-col :xs="12" :sm="6"><div class="kpi-card"><div class="kpi-label">总订单</div><div class="kpi-value green">{{ data.totalOrders || 0 }}</div></div></el-col>
      <el-col :xs="12" :sm="6"><div class="kpi-card"><div class="kpi-label">总营收</div><div class="kpi-value purple">¥{{ (data.totalRevenue || 0).toFixed(2) }}</div></div></el-col>
    </el-row>

    <!-- Charts -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="14"><div class="card"><h4 class="card-title">订单趋势（近7天）</h4><div ref="trendChart" class="chart-box"></div></div></el-col>
      <el-col :xs="24" :lg="10"><div class="card"><h4 class="card-title">订单状态分布</h4><div ref="pieChart" class="chart-box"></div></div></el-col>
    </el-row>

    <!-- Top Dishes Table -->
    <div class="card">
      <div class="card-title-row"><h4 class="card-title">菜品销量排行 TOP10</h4></div>
      <el-table :data="data.topDishes || []" v-loading="loading" border stripe size="small">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="name" label="菜品名称" />
        <el-table-column prop="sales" label="销量" width="100" />
        <el-table-column label="单价" width="100"><template #default="{ row }">¥{{ Number(row.price || 0).toFixed(2) }}</template></el-table-column>
        <el-table-column label="销售额" width="120"><template #default="{ row }">¥{{ (Number(row.sales || 0) * Number(row.price || 0)).toFixed(2) }}</template></el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import request from '@/utils/request'
import * as echarts from 'echarts'

const data = reactive({ todayOrders: 0, todayRevenue: 0, totalOrders: 0, totalRevenue: 0, orderTrend: [], topDishes: [], orderStatus: [] })
const loading = ref(false)
const trendChart = ref(null)
const pieChart = ref(null)

const load = async () => {
  loading.value = true
  try {
    await request.post('/admin/statistics/refresh')
    const res = await request.get('/admin/statistics/dashboard')
    Object.assign(data, res)
    await nextTick()
    renderCharts()
  } catch (e) { console.error('统计数据加载失败:', e) } finally { loading.value = false }
}

const renderCharts = () => {
  if (trendChart.value) {
    const chart = echarts.init(trendChart.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: (data.orderTrend || []).map(d => d.date?.substring(5)) },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{ data: (data.orderTrend || []).map(d => d.count), type: 'line', smooth: true, areaStyle: { color: 'rgba(37,99,235,0.1)' }, itemStyle: { color: '#2563EB' } }],
      grid: { top: 20, right: 20, bottom: 30, left: 40 }
    })
  }
  if (pieChart.value) {
    const chart = echarts.init(pieChart.value)
    chart.setOption({
      tooltip: { trigger: 'item' },
      series: [{ type: 'pie', radius: ['40%', '70%'], data: (data.orderStatus || []).map(d => ({ name: d.name, value: d.value })), label: { show: true, formatter: '{b}: {c}' } }],
      color: ['#F59E0B', '#10B981', '#3B82F6', '#8B5CF6', '#EF4444']
    })
  }
}

onMounted(load)
</script>

<style scoped>
.stats-page { padding: 16px; }
.page-header-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-title { margin: 0; font-size: 20px; font-weight: 700; }
.kpi-row { margin-bottom: 16px; }
.kpi-card { background: #fff; border-radius: 12px; padding: 18px 16px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); text-align: center; }
.kpi-label { font-size: 13px; color: #64748b; margin-bottom: 6px; }
.kpi-value { font-size: 28px; font-weight: 800; }
.kpi-value.blue { color: #2563EB; } .kpi-value.orange { color: #F97316; } .kpi-value.green { color: #10B981; } .kpi-value.purple { color: #8B5CF6; }
.chart-row { margin-bottom: 16px; }
.card { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); margin-bottom: 16px; }
.card-title { margin: 0 0 12px; font-size: 15px; font-weight: 700; color: #1e293b; }
.card-title-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.chart-box { width: 100%; height: 300px; }
</style>
