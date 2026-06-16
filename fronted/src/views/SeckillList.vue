<template>
  <div class="seckill-container">
    <!-- 极光氛围顶部 Banner -->
    <div class="header-banner glass-card aurora-banner">
      <div class="aurora-glow-effect"></div>
      <div class="banner-content">
        <div class="title-with-icon">
          <el-icon size="36" class="lightning-glow"><Lightning /></el-icon>
          <h2 class="banner-title text-gradient-aurora">星闪臻选 · Aurora Drop</h2>
        </div>
        <p class="banner-desc">以先锋姿态，瞬息问鼎极值。极速通道限额派送，每位阁下仅限结缘一件。</p>
      </div>
      <el-button class="publish-aurora-btn" @click="publishDialog = true">
        <el-icon><Plus /></el-icon> 开启闪购企划
      </el-button>
    </div>

    <!-- 闪购商品列表 -->
    <div v-loading="loading" class="seckill-grid">
      <el-empty v-if="seckillList.length === 0" description="当前暂无臻选放送，期待您的非凡创意。" />
      
      <div 
        v-for="item in seckillList" 
        :key="item.id" 
        class="seckill-card aurora-card glass-card" 
        :class="{ 'ended-card': item.ended, 'soldout-card': item.stock === 0 }"
      >
        <div class="card-image-box">
          <img :src="item.imageUrl || defaultImage" alt="臻选图" class="seckill-img" />
          <div class="status-overlay" :class="statusClass(item)">
            <span class="status-dot"></span>
            {{ statusText(item) }}
          </div>
        </div>
        
        <div class="card-details">
          <h3 class="item-name">{{ item.name }}</h3>
          <p class="item-desc">{{ item.description }}</p>
          
          <div class="price-row">
            <span class="seckill-price-symbol">￥</span>
            <span class="seckill-price">{{ item.seckillPrice }}</span>
            <span class="original-price">原价 ￥{{ item.originalPrice }}</span>
          </div>

          <!-- 库存状态与进度条 -->
          <div class="stock-info">
            <div class="stock-label">
              <span>余量余存: {{ item.stock }} 件</span>
              <span class="stock-percentage-text" v-if="item.stock > 0">极速去化中</span>
              <span class="stock-percentage-text text-danger" v-else>名花有主</span>
            </div>
            <div class="custom-progress-track">
              <div 
                class="custom-progress-bar" 
                :style="{ width: calculateStockPercent(item.stock) + '%' }"
                :class="{ 'pulse-progress': item.stock > 0 && item.stock <= 3, 'empty-progress': item.stock === 0 }"
              ></div>
            </div>
          </div>

          <!-- 倒计时面板 -->
          <div class="timer-box" v-if="!item.ended" :class="{ 'upcoming-timer': !item.started }">
            <el-icon class="timer-icon"><Timer /></el-icon>
            <span class="timer-text">{{ countdownText(item) }}</span>
          </div>

          <div class="action-row">
            <el-button 
              class="seckill-btn aurora-btn"
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

    <!-- 密钥锁合校验弹窗 -->
    <el-dialog 
      v-model="verifyDialog" 
      title="密钥锁合安全校验" 
      width="380px" 
      :close-on-click-modal="false" 
      align-center
      class="verify-aurora-dialog"
    >
      <div class="slider-verify-box">
        <p class="verify-tip">请拖拽核心极光解锁滑块，完成本次结缘抢占</p>
        <div class="slider-track" ref="sliderTrack">
          <div class="slider-bg" :style="{ width: sliderLeft + 'px' }"></div>
          <div 
            class="slider-handle" 
            :style="{ left: sliderLeft + 'px' }"
            @mousedown="onDragStart"
            @touchstart="onDragStart"
          >
            <el-icon class="handle-icon"><ArrowRightBold v-if="!isVerified" /><Check v-else /></el-icon>
          </div>
          <div class="slider-text" v-show="sliderLeft < 100">拖动极光核以校验</div>
        </div>
      </div>
    </el-dialog>

    <!-- 发起闪购活动对话框 -->
    <el-dialog v-model="publishDialog" title="开启全新闪购企划" width="500px" class="publish-aurora-dialog">
      <el-form :model="publishForm" :rules="publishRules" ref="formRef" label-position="top">
        <el-form-item label="关联二手闲置商品编号" prop="goodsId">
          <el-input v-model="publishForm.goodsId" placeholder="请输入您已发布常规闲置的商品ID" />
        </el-form-item>

        <el-form-item label="星闪出货价格 (元)" prop="seckillPrice">
          <el-input-number v-model="publishForm.seckillPrice" :precision="2" :step="10" :min="0.01" style="width: 100%" />
        </el-form-item>

        <el-form-item label="限额出货数量 (件)" prop="stock">
          <el-input-number v-model="publishForm.stock" :min="1" :step="1" style="width: 100%" />
        </el-form-item>

        <el-form-item label="闪售投放时间周期" prop="timeRange">
          <el-date-picker
            v-model="publishForm.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="投放起始时间"
            end-placeholder="投放截止时间"
            style="width: 100%"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="publishDialog = false" class="cancel-aurora-btn">暂 缓</el-button>
          <el-button type="primary" @click="submitPublish" :loading="publishing" class="confirm-aurora-btn">启 动 企 划</el-button>
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
  if (item.ended) return '尘埃落定'
  if (!item.started) return '静候破晓'
  if (item.stock === 0) return '已被臻藏'
  return '星闪跃动中'
}

const statusClass = (item) => {
  if (item.ended) return 'status-ended'
  if (!item.started) return 'status-upcoming'
  if (item.stock === 0) return 'status-soldout'
  return 'status-active'
}

const btnText = (item) => {
  if (item.ended) return '已 结 束'
  if (!item.started) return '静 候 破 晓'
  if (item.stock === 0) return '已被臻藏'
  return '即 刻 结 缘'
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
  gap: 32px;
  animation: fadeIn 0.8s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 极光风格 Banner */
.aurora-banner {
  position: relative;
  overflow: hidden;
  padding: 36px 40px;
  background: linear-gradient(135deg, rgba(20, 24, 33, 0.85) 0%, rgba(10, 13, 19, 0.95) 100%);
  border: 1px solid rgba(255, 255, 255, 0.06);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5), inset 0 1px 0 rgba(255, 255, 255, 0.05);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.aurora-glow-effect {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(16, 185, 129, 0.08) 0%, rgba(99, 102, 241, 0.05) 50%, transparent 80%);
  pointer-events: none;
  animation: rotateGlow 20s linear infinite;
}

@keyframes rotateGlow {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.title-with-icon {
  display: flex;
  align-items: center;
  gap: 16px;
}

.lightning-glow {
  color: #10b981;
  filter: drop-shadow(0 0 8px rgba(16, 185, 129, 0.6));
  animation: pulseIcon 2.5s infinite;
}

@keyframes pulseIcon {
  0%, 100% { transform: scale(1); filter: drop-shadow(0 0 6px rgba(16, 185, 129, 0.5)); }
  50% { transform: scale(1.08); filter: drop-shadow(0 0 16px rgba(16, 185, 129, 0.8)); }
}

.text-gradient-aurora {
  background: linear-gradient(135deg, #a7f3d0 0%, #10b981 50%, #3b82f6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  font-size: 26px;
  font-weight: 800;
  letter-spacing: -0.01em;
}

.banner-desc {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 6px;
  letter-spacing: 0.03em;
}

/* 开启闪购企划按钮 */
.publish-aurora-btn {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.15) 0%, rgba(5, 150, 105, 0.25) 100%) !important;
  border: 1px solid rgba(16, 185, 129, 0.4) !important;
  color: #a7f3d0 !important;
  border-radius: 30px;
  padding: 12px 24px;
  font-size: 14px;
  font-weight: 600;
  height: auto;
  box-shadow: 0 4px 20px rgba(16, 185, 129, 0.15);
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.publish-aurora-btn:hover {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.25) 0%, rgba(5, 150, 105, 0.35) 100%) !important;
  border-color: rgba(16, 185, 129, 0.7) !important;
  color: #fff !important;
  box-shadow: 0 0 20px rgba(16, 185, 129, 0.35);
  transform: translateY(-2px);
}

/* 商品网格 */
.seckill-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 32px;
}

/* 商品卡片 */
.aurora-card {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 0;
  height: 460px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(20, 24, 33, 0.7) 0%, rgba(10, 13, 19, 0.8) 100%);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 20px;
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.aurora-card:hover {
  border-color: rgba(16, 185, 129, 0.25);
  transform: translateY(-6px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.6), 0 0 15px rgba(16, 185, 129, 0.1);
}

.ended-card {
  opacity: 0.55;
  filter: grayscale(0.3);
}

.soldout-card {
  opacity: 0.85;
}

.card-image-box {
  position: relative;
  height: 200px;
  overflow: hidden;
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
}

.seckill-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.8s cubic-bezier(0.16, 1, 0.3, 1);
}

.aurora-card:hover .seckill-img {
  transform: scale(1.06);
}

/* 浮动状态标签 */
.status-overlay {
  position: absolute;
  top: 14px;
  left: 14px;
  padding: 5px 12px;
  border-radius: 30px;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 6px;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25);
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.status-active {
  background: rgba(16, 185, 129, 0.15);
  color: #10b981;
  border-color: rgba(16, 185, 129, 0.3);
}
.status-active .status-dot {
  background: #10b981;
  box-shadow: 0 0 8px #10b981;
  animation: blink 1.5s infinite;
}

.status-upcoming {
  background: rgba(99, 102, 241, 0.15);
  color: #a5b4fc;
  border-color: rgba(99, 102, 241, 0.3);
}
.status-upcoming .status-dot {
  background: #a5b4fc;
  box-shadow: 0 0 8px #a5b4fc;
}

.status-soldout {
  background: rgba(107, 114, 128, 0.15);
  color: #9ca3af;
  border-color: rgba(107, 114, 128, 0.2);
}
.status-soldout .status-dot {
  background: #9ca3af;
}

.status-ended {
  background: rgba(239, 68, 68, 0.1);
  color: #f43f5e;
  border-color: rgba(239, 68, 68, 0.2);
}
.status-ended .status-dot {
  background: #f43f5e;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.card-details {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  flex: 1;
}

.item-name {
  font-size: 17px;
  font-weight: 700;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-desc {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.5;
  height: 36px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* 价格区域 */
.price-row {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.seckill-price-symbol {
  font-size: 14px;
  font-weight: 700;
  color: #10b981;
}

.seckill-price {
  font-size: 24px;
  font-weight: 800;
  color: #10b981;
  font-family: var(--font-display);
  letter-spacing: -0.02em;
  text-shadow: 0 0 10px rgba(16, 185, 129, 0.2);
}

.original-price {
  font-size: 11px;
  color: var(--text-dim);
  text-decoration: line-through;
  margin-left: 8px;
}

/* 库存指示条 */
.stock-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stock-label {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: var(--text-muted);
}

.stock-percentage-text {
  font-weight: 600;
  color: rgba(16, 185, 129, 0.8);
}

.stock-percentage-text.text-danger {
  color: #ef4444;
}

.custom-progress-track {
  width: 100%;
  height: 6px;
  background-color: rgba(255, 255, 255, 0.04);
  border-radius: 3px;
  overflow: hidden;
}

.custom-progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #10b981 0%, #3b82f6 100%);
  border-radius: 3px;
  transition: width 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

.pulse-progress {
  background: linear-gradient(90deg, #ef4444 0%, #f59e0b 100%);
  animation: progressPulse 1.5s infinite alternate;
}

.empty-progress {
  background: rgba(255, 255, 255, 0.15);
}

@keyframes progressPulse {
  from { filter: brightness(1); }
  to { filter: brightness(1.3); }
}

/* 计时器盒 */
.timer-box {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(16, 185, 129, 0.04);
  border: 1px solid rgba(16, 185, 129, 0.15);
  padding: 8px 14px;
  border-radius: 10px;
  font-size: 12px;
  color: #a7f3d0;
  transition: all 0.3s;
}

.timer-box.upcoming-timer {
  background: rgba(99, 102, 241, 0.04);
  border-color: rgba(99, 102, 241, 0.15);
  color: #c7d2fe;
}

.timer-icon {
  font-size: 14px;
}

.timer-text {
  font-family: var(--font-display);
  font-weight: 600;
  letter-spacing: 0.05em;
}

.action-row {
  margin-top: auto;
}

/* 结缘抢购按钮 */
.aurora-btn {
  width: 100%;
  height: 42px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.05em;
  border: none !important;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%) !important;
  color: #fff !important;
  box-shadow: 0 4px 15px rgba(16, 185, 129, 0.3);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1) !important;
}

.aurora-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(16, 185, 129, 0.5);
  background: linear-gradient(135deg, #12c289 0%, #06a675 100%) !important;
}

.aurora-btn:disabled {
  background: rgba(255, 255, 255, 0.05) !important;
  color: rgba(255, 255, 255, 0.25) !important;
  box-shadow: none !important;
  cursor: not-allowed;
}

/* 安全滑块校验对话框 */
.verify-aurora-dialog :deep(.el-dialog) {
  background: #0d1117 !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  border-radius: 20px !important;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.8) !important;
}

.slider-verify-box {
  padding: 10px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.verify-tip {
  font-size: 13px;
  color: var(--text-muted);
  letter-spacing: 0.02em;
}

.slider-track {
  position: relative;
  width: 320px;
  height: 54px;
  background-color: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 27px;
  overflow: hidden;
  user-select: none;
}

.slider-bg {
  height: 100%;
  background: linear-gradient(90deg, rgba(16, 185, 129, 0.05) 0%, rgba(16, 185, 129, 0.25) 100%);
  border-radius: 27px 0 0 27px;
}

.slider-handle {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #10b981 0%, #047857 100%);
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #fff;
  cursor: grab;
  box-shadow: 0 2px 10px rgba(16, 185, 129, 0.4);
  transition: transform 0.1s, box-shadow 0.3s;
}

.slider-handle:active {
  cursor: grabbing;
  transform: scale(0.95);
  box-shadow: 0 0 15px rgba(16, 185, 129, 0.6);
}

.handle-icon {
  font-size: 16px;
  font-weight: bold;
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
  font-weight: 500;
  letter-spacing: 0.05em;
}

/* 企划发布对话框 */
.publish-aurora-dialog :deep(.el-dialog) {
  background: #0d1117 !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  border-radius: 20px !important;
}

.publish-aurora-dialog :deep(.el-form-item__label) {
  color: #a5b4fc !important;
  font-weight: 600;
  font-size: 13px;
}

.publish-aurora-dialog :deep(.el-input__wrapper), 
.publish-aurora-dialog :deep(.el-input-number) {
  background-color: rgba(255, 255, 255, 0.02) !important;
  border: 1px solid rgba(255, 255, 255, 0.06) !important;
  border-radius: 8px !important;
}

.publish-aurora-dialog :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(99, 102, 241, 0.6) !important;
}

.cancel-aurora-btn {
  background: transparent !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  color: var(--text-muted) !important;
  border-radius: 8px;
}

.cancel-aurora-btn:hover {
  background: rgba(255, 255, 255, 0.05) !important;
  color: #fff !important;
}

.confirm-aurora-btn {
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%) !important;
  border: none !important;
  border-radius: 8px;
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.4);
}

.confirm-aurora-btn:hover {
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.6) !important;
  transform: translateY(-1px);
}
</style>
