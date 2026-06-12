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
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
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
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
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
</style>
