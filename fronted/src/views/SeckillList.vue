<template>
  <div class="seckill-container">
    <div class="header-banner glass-card text-glow-seckill">
      <div class="banner-content">
        <el-icon size="32" color="#10b981"><Lightning /></el-icon>
        <h2 class="banner-title text-gradient">限时秒杀抢购区</h2>
        <p class="banner-desc">高并发高可靠保障，每位用户限购 1 件！</p>
      </div>
      <el-button type="success" icon="Plus" @click="publishDialog = true">
        发布秒杀抢购
      </el-button>
    </div>

    <!-- 秒杀商品列表 -->
    <div v-loading="loading" class="seckill-grid">
      <el-empty v-if="seckillList.length === 0" description="目前没有秒杀活动，去发布一个试试吧！" />
      
      <div v-for="item in seckillList" :key="item.id" class="seckill-card glass-card" :class="{ 'ended-card': item.ended }">
        <div class="card-image-box">
          <img :src="item.imageUrl || defaultImage" alt="秒杀图" class="seckill-img" />
          <div class="status-overlay" :class="statusClass(item)">
            {{ statusText(item) }}
          </div>
        </div>
        
        <div class="card-details">
          <h3 class="item-name">{{ item.name }}</h3>
          <p class="item-desc">{{ item.description }}</p>
          
          <div class="price-row">
            <span class="seckill-price">￥{{ item.seckillPrice }}</span>
            <span class="original-price">￥{{ item.originalPrice }}</span>
          </div>

          <!-- 库存进度条 -->
          <div class="stock-info">
            <div class="stock-label">
              <span>库存剩余: {{ item.stock }} 件</span>
            </div>
            <el-progress 
              :percentage="calculateStockPercent(item.stock)" 
              :status="item.stock === 0 ? 'exception' : 'success'"
              :show-text="false"
            />
          </div>

          <!-- 倒计时面板 -->
          <div class="timer-box" v-if="!item.ended">
            <el-icon><Timer /></el-icon>
            <span class="timer-text">{{ countdownText(item) }}</span>
          </div>

          <div class="action-row">
            <el-button 
              type="danger" 
              class="seckill-btn"
              :disabled="!item.started || item.ended || item.stock === 0"
              @click="openVerifyDialog(item)"
              :loading="buyingId === item.goodsId"
            >
              {{ btnText(item) }}
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 滑块验证码弹窗（高并发防刷） -->
    <el-dialog v-model="verifyDialog" title="安全验证" width="360px" :close-on-click-modal="false" align-center>
      <div class="slider-verify-box">
        <p class="verify-tip">请向右滑动滑块以完成安全抢购校验</p>
        <div class="slider-track" ref="sliderTrack">
          <div class="slider-bg" :style="{ width: sliderLeft + 'px' }"></div>
          <div 
            class="slider-handle" 
            :style="{ left: sliderLeft + 'px' }"
            @mousedown="onDragStart"
            @touchstart="onDragStart"
          >
            <el-icon><ArrowRightBold v-if="!isVerified" /><Check v-else /></el-icon>
          </div>
          <div class="slider-text" v-show="sliderLeft < 100">向右拖动完成验证</div>
        </div>
      </div>
    </el-dialog>

    <!-- 发布秒杀对话框 -->
    <el-dialog v-model="publishDialog" title="发起秒杀活动" width="500px">
      <el-form :model="publishForm" :rules="publishRules" ref="formRef" label-position="top">
        <el-form-item label="关联闲置商品ID" prop="goodsId">
          <el-input v-model="publishForm.goodsId" placeholder="请输入您已发布的常规商品ID" />
        </el-form-item>

        <el-form-item label="秒杀价格 (元)" prop="seckillPrice">
          <el-input-number v-model="publishForm.seckillPrice" :precision="2" :step="5" :min="0.01" style="width: 100%" />
        </el-form-item>

        <el-form-item label="抢购库存数" prop="stock">
          <el-input-number v-model="publishForm.stock" :min="1" :step="1" style="width: 100%" />
        </el-form-item>

        <el-form-item label="活动时间范围" prop="timeRange">
          <el-date-picker
            v-model="publishForm.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 100%"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="publishDialog = false">取 消</el-button>
          <el-button type="primary" @click="submitPublish" :loading="publishing">确 认 发 布</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '../store/user'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import axios from 'axios'

const userStore = useUserStore()
const router = useRouter()

const loading = ref(false)
const publishing = ref(false)
const publishDialog = ref(false)
const seckillList = ref([])

const defaultImage = 'https://images.unsplash.com/photo-1542751371-adc38448a05e?w=500&auto=format&fit=crop&q=60'

// 定时器更新倒计时
let timer = null

// 抢购状态与滑块验证
const verifyDialog = ref(false)
const selectedItem = ref(null)
const buyingId = ref(null)
const sliderLeft = ref(0)
const maxSliderLeft = 250 // track-width (300) - handle-width (50)
const isDragging = ref(false)
const isVerified = ref(false)
const startX = ref(0)

const formRef = ref(null)
const publishForm = reactive({
  goodsId: '',
  seckillPrice: 9.90,
  stock: 10,
  timeRange: []
})

const publishRules = {
  goodsId: [{ required: true, message: '请输入商品ID', trigger: 'blur' }],
  seckillPrice: [{ required: true, message: '请输入抢购价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请设定抢购库存', trigger: 'blur' }],
  timeRange: [{ required: true, message: '请设定起止时间范围', trigger: 'change' }]
}

const fetchSeckillList = async () => {
  loading.value = true
  try {
    const response = await axios.get('http://localhost:8080/api/seckill/list')
    if (response.data.code === 200) {
      seckillList.value = response.data.data
    }
  } catch (error) {
    ElMessage.error('无法获取秒杀列表，请检查后端运行状态')
  } finally {
    loading.value = false
  }
}

// 轮询更新各商品的时间状态与剩余秒数
const updateTimes = () => {
  const now = new Date()
  seckillList.value.forEach(item => {
    const start = new Date(item.startTime)
    const end = new Date(item.endTime)
    
    item.started = now > start
    item.ended = now > end
  })
}

// 状态覆盖层样式与文本
const statusText = (item) => {
  if (item.ended) return '活动已结束'
  if (!item.started) return '即将开始'
  if (item.stock === 0) return '已抢光'
  return '抢购中'
}

const statusClass = (item) => {
  if (item.ended) return 'status-ended'
  if (!item.started) return 'status-upcoming'
  if (item.stock === 0) return 'status-soldout'
  return 'status-active'
}

const btnText = (item) => {
  if (item.ended) return '已结束'
  if (!item.started) return '即将开抢'
  if (item.stock === 0) return '已抢光'
  return '立 即 抢 购'
}

const calculateStockPercent = (stock) => {
  return Math.min(100, Math.max(0, stock * 10))
}

const countdownText = (item) => {
  const now = new Date()
  const start = new Date(item.startTime)
  const end = new Date(item.endTime)
  
  if (now < start) {
    const diff = start - now
    return `距开始: ${formatDuration(diff)}`
  } else {
    const diff = end - now
    return `距结束: ${formatDuration(diff)}`
  }
}

const formatDuration = (ms) => {
  const s = Math.floor(ms / 1000)
  const sec = s % 60
  const m = Math.floor(s / 60)
  const min = m % 60
  const h = Math.floor(m / 60)
  return `${String(h).padStart(2, '0')}:${String(min).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
}

// 滑块拖拽防刷验证
const openVerifyDialog = (item) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录再进行抢购！')
    router.push('/login')
    return
  }
  selectedItem.value = item
  sliderLeft.value = 0
  isVerified.value = false
  verifyDialog.value = true
}

const onDragStart = (e) => {
  if (isVerified.value) return
  isDragging.value = true
  startX.value = e.type === 'mousedown' ? e.clientX : e.touches[0].clientX
  
  document.addEventListener('mousemove', onDragMove)
  document.addEventListener('mouseup', onDragEnd)
  document.addEventListener('touchmove', onDragMove)
  document.addEventListener('touchend', onDragEnd)
}

const onDragMove = (e) => {
  if (!isDragging.value) return
  const currentX = e.type === 'mousemove' ? e.clientX : e.touches[0].clientX
  let offset = currentX - startX.value
  
  if (offset < 0) offset = 0
  if (offset > maxSliderLeft) offset = maxSliderLeft
  
  sliderLeft.value = offset
  
  if (offset >= maxSliderLeft) {
    isVerified.value = true
    isDragging.value = false
    onDragEnd()
    // 验证通过，触发抢购
    handleOrder()
  }
}

const onDragEnd = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('mouseup', onDragEnd)
  document.removeEventListener('touchmove', onDragMove)
  document.removeEventListener('touchend', onDragEnd)
  
  if (!isVerified.value) {
    sliderLeft.value = 0
  }
}

// 发起高并发抢购请求
const handleOrder = async () => {
  verifyDialog.value = false
  const item = selectedItem.value
  if (!item) return

  buyingId.value = item.goodsId
  ElMessage.info('订单处理中，正在排队抢购...')

  try {
    const headers = { Authorization: `Bearer ${userStore.token}` }
    const response = await axios.post(
      'http://localhost:8080/api/seckill/order', 
      { goodsId: item.goodsId },
      { headers }
    )
    if (response.data.code === 200) {
      ElMessage.success(response.data.data || '抢购成功！订单生成中...')
      fetchSeckillList()
    } else {
      ElMessage.error(response.data.message || '抢购失败')
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '抢购过于拥挤，请稍后重试')
  } finally {
    buyingId.value = null
  }
}

const submitPublish = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录！')
    router.push('/login')
    return
  }

  formRef.value.validate(async (valid) => {
    if (!valid) return
    publishing.value = true
    try {
      const headers = { Authorization: `Bearer ${userStore.token}` }
      const payload = {
        goodsId: publishForm.goodsId,
        seckillPrice: publishForm.seckillPrice,
        stock: publishForm.stock,
        startTime: publishForm.timeRange[0],
        endTime: publishForm.timeRange[1]
      }
      
      const response = await axios.post('http://localhost:8080/api/seckill/publish', payload, { headers })
      if (response.data.code === 200) {
        ElMessage.success('秒杀活动发起成功！已开启库存预热！')
        publishDialog.value = false
        // 重置
        publishForm.goodsId = ''
        publishForm.stock = 10
        publishForm.timeRange = []
        fetchSeckillList()
      } else {
        ElMessage.error(response.data.message || '发布失败')
      }
    } catch (err) {
      ElMessage.error(err.response?.data?.message || '发布秒杀失败，请检查关联商品ID是否为您所发布！')
    } finally {
      publishing.value = false
    }
  })
}

onMounted(() => {
  fetchSeckillList()
  timer = setInterval(() => {
    updateTimes()
  }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.seckill-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.header-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 32px;
}

.banner-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.banner-title {
  font-size: 22px;
  font-weight: 700;
}

.banner-desc {
  font-size: 13px;
  color: var(--text-muted);
}

.seckill-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}

.seckill-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 0;
  height: 440px;
}

.ended-card {
  opacity: 0.6;
}

.card-image-box {
  position: relative;
  height: 180px;
  overflow: hidden;
  border-bottom: 1px solid var(--border-color);
}

.seckill-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s;
}

.seckill-card:hover .seckill-img {
  transform: scale(1.05);
}

.status-overlay {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
}

.status-active { background: rgba(16, 185, 129, 0.85); color: #fff; }
.status-upcoming { background: rgba(245, 158, 11, 0.85); color: #fff; }
.status-soldout { background: rgba(239, 68, 68, 0.85); color: #fff; }
.status-ended { background: rgba(107, 114, 128, 0.85); color: #fff; }

.card-details {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
}

.item-name {
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-desc {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.4;
  height: 34px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.seckill-price {
  font-size: 20px;
  font-weight: 700;
  color: #ef4444;
  font-family: var(--font-display);
}

.original-price {
  font-size: 12px;
  color: var(--text-dim);
  text-decoration: line-through;
}

.stock-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stock-label {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: var(--text-muted);
}

.timer-box {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(255, 255, 255, 0.03);
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 12px;
  color: var(--text-muted);
  border: 1px solid var(--border-color);
}

.timer-text {
  font-family: var(--font-display);
  font-weight: 500;
}

.action-row {
  margin-top: auto;
}

.seckill-btn {
  width: 100%;
  height: 38px;
  font-size: 14px;
  font-weight: 600;
}

/* 滑块验证器样式 */
.slider-verify-box {
  padding: 10px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.verify-tip {
  font-size: 12px;
  color: var(--text-muted);
}

.slider-track {
  position: relative;
  width: 300px;
  height: 50px;
  background-color: rgba(255, 255, 255, 0.05);
  border: 1px solid var(--border-color);
  border-radius: 25px;
  overflow: hidden;
  user-select: none;
}

.slider-bg {
  height: 100%;
  background-color: rgba(16, 185, 129, 0.2);
  border-radius: 25px 0 0 25px;
}

.slider-handle {
  position: absolute;
  top: 0;
  left: 0;
  width: 50px;
  height: 100%;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #fff;
  cursor: grab;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.4);
  transition: transform 0.1s;
}

.slider-handle:active {
  cursor: grabbing;
  transform: scale(0.95);
}

.slider-text {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 12px;
  color: var(--text-muted);
  pointer-events: none;
}
</style>
