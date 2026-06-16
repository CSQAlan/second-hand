<!-- [Skill: SKILL.md] -->
<template>
  <div class="dashboard-container">
    <!-- 头部信息 -->
    <div class="dashboard-header glass-card">
      <div class="header-info">
        <h1 class="title text-gradient text-glow-primary">数据控制中心</h1>
        <p class="subtitle">实时监控平台核心业务指标、用户增长态势与订单交易分布</p>
      </div>
      <el-button type="primary" icon="Refresh" @click="fetchStats" :loading="loading">
        同步最新数据
      </el-button>
    </div>

    <!-- 数据指标网格 -->
    <div v-loading="loading" class="stats-grid">
      <div class="stats-card glass-card hover-glow-primary">
        <div class="card-icon bg-primary-glow">
          <el-icon size="28" color="#6366f1"><User /></el-icon>
        </div>
        <div class="card-content">
          <span class="card-label">总注册用户</span>
          <span class="card-value">{{ stats.totalUsers || 0 }}</span>
        </div>
        <div class="card-trend text-gradient">用户总规模</div>
      </div>

      <div class="stats-card glass-card hover-glow-seckill">
        <div class="card-icon bg-seckill-glow">
          <el-icon size="28" color="#10b981"><Money /></el-icon>
        </div>
        <div class="card-content">
          <span class="card-label">平台成交额 (元)</span>
          <span class="card-value">￥{{ stats.totalSales || '0.00' }}</span>
        </div>
        <div class="card-trend text-glow-seckill">累计成交额</div>
      </div>

      <div class="stats-card glass-card hover-glow-auction">
        <div class="card-icon bg-auction-glow">
          <el-icon size="28" color="#8b5cf6"><Goods /></el-icon>
        </div>
        <div class="card-content">
          <span class="card-label">在售闲置商品</span>
          <span class="card-value">{{ stats.activeGoods || 0 }}</span>
        </div>
        <div class="card-trend text-gradient">集市当前流通</div>
      </div>

      <div class="stats-card glass-card hover-glow-primary">
        <div class="card-icon bg-primary-glow">
          <el-icon size="28" color="#3b82f6"><Lightning /></el-icon>
        </div>
        <div class="card-content">
          <span class="card-label">秒杀预扣库存</span>
          <span class="card-value">{{ stats.seckillStock || 0 }}</span>
        </div>
        <div class="card-trend text-glow-primary">抢购池剩余量</div>
      </div>
    </div>

    <!-- 图表排版 -->
    <div class="charts-layout">
      <!-- 第一排：折线图与饼图 -->
      <div class="charts-row-half">
        <div class="chart-wrapper glass-card">
          <div class="chart-header">
            <h3 class="chart-title">交易趋势 (最近7天)</h3>
            <span class="chart-subtitle">每日成交额与付款订单量</span>
          </div>
          <div ref="lineChartRef" class="chart-body"></div>
        </div>

        <div class="chart-wrapper glass-card">
          <div class="chart-header">
            <h3 class="chart-title">商品分类占比</h3>
            <span class="chart-subtitle">闲置商品种类关键词分布</span>
          </div>
          <div ref="pieChartRef" class="chart-body"></div>
        </div>
      </div>

      <!-- 第二排：柱状图（全宽） -->
      <div class="charts-row-full">
        <div class="chart-wrapper glass-card">
          <div class="chart-header">
            <h3 class="chart-title">业务订单类型统计</h3>
            <span class="chart-subtitle">普通交易、高并发秒杀与拍卖订单数量分布</span>
          </div>
          <div ref="barChartRef" class="chart-body"></div>
        </div>
      </div>
    </div>

    <!-- 平台交易操作日志监控 -->
    <div class="log-console-card glass-card">
      <div class="chart-header-custom">
        <div class="header-left-side">
          <h3 class="chart-title text-gradient">平台交易操作日志监控 (Platform Transaction Log)</h3>
          <span class="chart-subtitle">实时监控全平台二手交易、星闪秒杀和臻品拍卖的订单流水</span>
        </div>
        <div class="log-controls">
          <el-input 
            v-model="logKeyword" 
            placeholder="搜索商品/买家/卖家/订单号..." 
            size="small" 
            clearable 
            style="width: 240px; margin-right: 16px;" 
            prefix-icon="Search"
          />
          <el-checkbox v-model="autoRefresh" label="自动轮询 (5s)" size="small" style="margin-right: 16px;" />
          <el-button type="primary" size="small" icon="Refresh" @click="fetchOperations" :loading="loadingOperations" class="log-refresh-btn">刷新日志</el-button>
        </div>
      </div>
      <div class="operation-table-wrapper" ref="terminalRef">
        <div v-if="filteredOperations.length === 0" class="log-empty-tip">
          没有匹配的交易操作记录
        </div>
        <table v-else class="operation-table">
          <thead>
            <tr>
              <th>订单编号</th>
              <th>操作时间</th>
              <th>交易商品</th>
              <th>金额</th>
              <th>买方</th>
              <th>卖方</th>
              <th>交易类型</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="op in filteredOperations" :key="op.orderNo" class="op-row">
              <td class="order-no-cell">{{ op.orderNo }}</td>
              <td class="time-cell">{{ formatDateTime(op.createTime) }}</td>
              <td class="goods-cell">{{ op.goodsName }}</td>
              <td class="price-cell">￥{{ op.price }}</td>
              <td class="user-cell">{{ op.buyerName }}</td>
              <td class="user-cell">{{ op.sellerName }}</td>
              <td class="type-cell">
                <span :class="['type-badge', getTypeClass(op.type)]">
                  {{ getTypeName(op.type) }}
                </span>
              </td>
              <td class="status-cell">
                <span :class="['status-badge', getStatusClass(op.status)]">
                  {{ getStatusName(op.status) }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useUserStore } from '../store/user'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import * as echarts from 'echarts'

const userStore = useUserStore()
const loading = ref(false)
const stats = ref({
  totalUsers: 0,
  totalSales: '0.00',
  activeGoods: 0,
  seckillStock: 0
})

// 图表 DOM 引用
const lineChartRef = ref(null)
const pieChartRef = ref(null)
const barChartRef = ref(null)

// ECharts 实例引用
let lineChart = null
let pieChart = null
let barChart = null

// 交易操作日志监控相关
const logKeyword = ref('')
const operationsList = ref([])
const autoRefresh = ref(false)
const loadingOperations = ref(false)
const terminalRef = ref(null)
let logTimer = null

const fetchOperations = async () => {
  loadingOperations.value = true
  try {
    const headers = { Authorization: `Bearer ${userStore.token}` }
    const response = await axios.get('http://localhost:8080/api/admin/operations', { headers })
    if (response.data.code === 200) {
      operationsList.value = response.data.data || []
    } else {
      ElMessage.error(response.data.message || '获取操作日志失败')
    }
  } catch (error) {
    console.error('获取操作日志异常:', error)
  } finally {
    loadingOperations.value = false
  }
}

const filteredOperations = computed(() => {
  if (!logKeyword.value) return operationsList.value
  const keyword = logKeyword.value.toLowerCase()
  return operationsList.value.filter(op => 
    String(op.orderNo).toLowerCase().includes(keyword) ||
    String(op.goodsName).toLowerCase().includes(keyword) ||
    String(op.buyerName).toLowerCase().includes(keyword) ||
    String(op.sellerName).toLowerCase().includes(keyword) ||
    getTypeName(op.type).toLowerCase().includes(keyword) ||
    getStatusName(op.status).toLowerCase().includes(keyword)
  )
})

const getTypeName = (type) => {
  switch (type) {
    case 0: return '普通交易'
    case 1: return '星闪秒杀'
    case 2: return '臻品拍卖'
    default: return '未知'
  }
}

const getTypeClass = (type) => {
  switch (type) {
    case 0: return 'type-normal'
    case 1: return 'type-seckill'
    case 2: return 'type-auction'
    default: return ''
  }
}

const getStatusName = (status) => {
  switch (status) {
    case 0: return '待付款'
    case 1: return '已付款/待发货'
    case 2: return '已发货'
    case 3: return '已收货'
    case 4: return '交易成功'
    case 5: return '已取消'
    default: return '未知'
  }
}

const getStatusClass = (status) => {
  switch (status) {
    case 0: return 'status-warning'
    case 1: return 'status-info'
    case 2: return 'status-primary'
    case 3:
    case 4: return 'status-success'
    case 5: return 'status-cancel'
    default: return ''
  }
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

watch(autoRefresh, (newVal) => {
  if (newVal) {
    logTimer = setInterval(fetchOperations, 5000)
  } else {
    if (logTimer) {
      clearInterval(logTimer)
      logTimer = null
    }
  }
})

const fetchStats = async () => {
  loading.value = true
  try {
    const headers = { Authorization: `Bearer ${userStore.token}` }
    const response = await axios.get('http://localhost:8080/api/admin/dashboard/stats', { headers })
    if (response.data.code === 200) {
      stats.value = response.data.data
      await nextTick()
      initCharts(response.data.data)
    } else {
      ElMessage.error(response.data.message || '获取数据失败')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '您没有权限访问该页面或无法连接后台')
  } finally {
    loading.value = false
  }
}

const initCharts = (data) => {
  // --- 1. 折线图 ---
  if (lineChartRef.value) {
    if (!lineChart) {
      lineChart = echarts.init(lineChartRef.value)
    }
    const lineData = data.lineChart || { categories: [], amounts: [], orderCounts: [] }
    const lineOption = {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross', label: { backgroundColor: '#1f2937' } }
      },
      legend: {
        data: ['日交易额 (元)', '日订单量 (笔)'],
        textStyle: { color: '#9ca3af' }
      },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: [
        {
          type: 'category',
          data: lineData.categories,
          axisLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } },
          axisLabel: { color: '#9ca3af' }
        }
      ],
      yAxis: [
        {
          type: 'value',
          name: '金额 (元)',
          axisLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } },
          axisLabel: { color: '#9ca3af', formatter: '￥{value}' },
          splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } }
        },
        {
          type: 'value',
          name: '笔数 (笔)',
          axisLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } },
          axisLabel: { color: '#9ca3af' },
          splitLine: { show: false }
        }
      ],
      series: [
        {
          name: '日交易额 (元)',
          type: 'line',
          smooth: true,
          yAxisIndex: 0,
          lineStyle: { width: 3, color: '#6366f1' },
          itemStyle: { color: '#6366f1' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(99, 102, 241, 0.4)' },
              { offset: 1, color: 'rgba(99, 102, 241, 0)' }
            ])
          },
          data: lineData.amounts
        },
        {
          name: '日订单量 (笔)',
          type: 'line',
          smooth: true,
          yAxisIndex: 1,
          lineStyle: { width: 3, color: '#10b981' },
          itemStyle: { color: '#10b981' },
          data: lineData.orderCounts
        }
      ]
    }
    lineChart.setOption(lineOption)
  }

  // --- 2. 饼图 ---
  if (pieChartRef.value) {
    if (!pieChart) {
      pieChart = echarts.init(pieChartRef.value)
    }
    const pieData = data.pieChart || []
    const pieOption = {
      backgroundColor: 'transparent',
      tooltip: { trigger: 'item', formatter: '{b} : {c} ({d}%)' },
      legend: {
        orient: 'vertical',
        left: 'left',
        textStyle: { color: '#9ca3af' }
      },
      series: [
        {
          name: '分类占比',
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 8,
            borderColor: '#0f172a',
            borderWidth: 2
          },
          label: { show: false, position: 'center' },
          emphasis: {
            label: { show: true, fontSize: 16, fontWeight: 'bold', color: '#fff' }
          },
          labelLine: { show: false },
          data: pieData,
          color: ['#6366f1', '#10b981', '#8b5cf6', '#06b6d4', '#f59e0b']
        }
      ]
    }
    pieChart.setOption(pieOption)
  }

  // --- 3. 柱状图 ---
  if (barChartRef.value) {
    if (!barChart) {
      barChart = echarts.init(barChartRef.value)
    }
    const barData = data.barChart || { types: [], counts: [] }
    const barOption = {
      backgroundColor: 'transparent',
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: [
        {
          type: 'category',
          data: barData.types,
          axisLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } },
          axisLabel: { color: '#9ca3af' }
        }
      ],
      yAxis: [
        {
          type: 'value',
          name: '订单量 (笔)',
          axisLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } },
          axisLabel: { color: '#9ca3af' },
          splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } }
        }
      ],
      series: [
        {
          name: '数量',
          type: 'bar',
          barWidth: '35%',
          itemStyle: {
            borderRadius: [8, 8, 0, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#8b5cf6' },
              { offset: 1, color: '#6366f1' }
            ])
          },
          data: barData.counts
        }
      ]
    }
    barChart.setOption(barOption)
  }
}

const handleResize = () => {
  if (lineChart) lineChart.resize()
  if (pieChart) pieChart.resize()
  if (barChart) barChart.resize()
}

onMounted(() => {
  fetchStats()
  fetchOperations()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (logTimer) {
    clearInterval(logTimer)
  }
})
</script>

<style scoped>
.dashboard-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 32px;
}

.header-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.title {
  font-size: 28px;
  font-weight: 700;
  margin: 0;
}

.subtitle {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 24px;
}

.stats-card {
  display: flex;
  align-items: center;
  gap: 16px;
  position: relative;
  overflow: hidden;
  padding: 24px;
}

.card-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bg-primary-glow {
  background: var(--color-primary-glow);
  border: 1px solid rgba(99, 102, 241, 0.25);
}

.bg-seckill-glow {
  background: var(--color-seckill-glow);
  border: 1px solid rgba(16, 185, 129, 0.25);
}

.bg-auction-glow {
  background: var(--color-auction-glow);
  border: 1px solid rgba(139, 92, 246, 0.25);
}

.card-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.card-label {
  font-size: 13px;
  color: var(--text-muted);
}

.card-value {
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 700;
  color: var(--text-main);
}

.card-trend {
  position: absolute;
  bottom: 12px;
  right: 16px;
  font-size: 11px;
  color: var(--text-dim);
}

/* 霓虹悬停发光 */
.hover-glow-primary:hover {
  border-color: rgba(99, 102, 241, 0.4);
  box-shadow: 0 0 20px rgba(99, 102, 241, 0.15);
}

.hover-glow-seckill:hover {
  border-color: rgba(16, 185, 129, 0.4);
  box-shadow: 0 0 20px rgba(16, 185, 129, 0.15);
}

.hover-glow-auction:hover {
  border-color: rgba(139, 92, 246, 0.4);
  box-shadow: 0 0 20px rgba(139, 92, 246, 0.15);
}

/* 图表布局 */
.charts-layout {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.charts-row-half {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

@media (max-width: 1024px) {
  .charts-row-half {
    grid-template-columns: 1fr;
  }
}

.charts-row-full {
  width: 100%;
}

.chart-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 400px;
}

.chart-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 12px;
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-main);
}

.chart-subtitle {
  font-size: 12px;
  color: var(--text-dim);
}

.chart-body {
  flex: 1;
  width: 100%;
  height: 100%;
}

/* 日志控制台样式 */
.log-console-card {
  margin-top: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: linear-gradient(135deg, rgba(20, 24, 33, 0.8) 0%, rgba(10, 13, 19, 0.9) 100%);
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.chart-header-custom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 16px;
  flex-wrap: wrap;
  gap: 16px;
}

.header-left-side {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.log-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.log-refresh-btn {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%) !important;
  border: none !important;
}

.operation-table-wrapper {
  background-color: rgba(3, 7, 18, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.04);
  border-radius: 12px;
  padding: 8px;
  height: 400px;
  overflow-y: auto;
  box-shadow: inset 0 2px 10px rgba(0, 0, 0, 0.6);
}

.operation-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.operation-table th {
  padding: 14px 16px;
  color: var(--text-muted);
  font-weight: 600;
  font-size: 13px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(15, 23, 42, 0.6);
  position: sticky;
  top: 0;
  z-index: 10;
  backdrop-filter: blur(10px);
}

.operation-table td {
  padding: 12px 16px;
  color: var(--text-main);
  font-size: 13px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.03);
}

.op-row {
  transition: background-color 0.2s ease;
}

.op-row:hover {
  background-color: rgba(255, 255, 255, 0.02);
}

.order-no-cell {
  font-family: monospace;
  color: var(--text-dim);
}

.time-cell {
  color: var(--text-muted);
  white-space: nowrap;
}

.goods-cell {
  font-weight: 500;
  color: #e2e8f0;
}

.price-cell {
  color: #fbbf24;
  font-weight: 600;
  white-space: nowrap;
}

.user-cell {
  color: #cbd5e1;
}

.log-empty-tip {
  color: var(--text-dim);
  text-align: center;
  padding-top: 150px;
  font-size: 14px;
}

/* 标签徽章样式 */
.type-badge, .status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
}

/* 交易类型 */
.type-normal {
  background: rgba(6, 182, 212, 0.15);
  color: #22d3ee;
  border: 1px solid rgba(6, 182, 212, 0.25);
}

.type-seckill {
  background: rgba(16, 185, 129, 0.15);
  color: #34d399;
  border: 1px solid rgba(16, 185, 129, 0.25);
  text-shadow: 0 0 6px rgba(52, 211, 153, 0.2);
}

.type-auction {
  background: rgba(139, 92, 246, 0.15);
  color: #a78bfa;
  border: 1px solid rgba(139, 92, 246, 0.25);
}

/* 交易状态 */
.status-warning {
  background: rgba(245, 158, 11, 0.15);
  color: #fbbf24;
  border: 1px solid rgba(245, 158, 11, 0.25);
}

.status-info {
  background: rgba(59, 130, 246, 0.15);
  color: #60a5fa;
  border: 1px solid rgba(59, 130, 246, 0.25);
}

.status-primary {
  background: rgba(99, 102, 241, 0.15);
  color: #818cf8;
  border: 1px solid rgba(99, 102, 241, 0.25);
}

.status-success {
  background: rgba(16, 185, 129, 0.15);
  color: #34d399;
  border: 1px solid rgba(16, 185, 129, 0.25);
}

.status-cancel {
  background: rgba(156, 163, 175, 0.15);
  color: #9ca3af;
  border: 1px solid rgba(156, 163, 175, 0.25);
}
</style>
