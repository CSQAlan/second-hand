<template>
  <div class="auction-container">
    <div class="header-banner glass-card text-glow-auction">
      <div class="banner-content">
        <el-icon size="32" color="#8b5cf6"><TrendCharts /></el-icon>
        <h2 class="banner-title text-gradient">分布式拍卖大厅</h2>
        <p class="banner-desc">使用 Redisson 分布式排他锁保护竞价，绝对防并发冲突！</p>
      </div>
      <el-button type="primary" icon="Plus" @click="publishDialog = true" style="background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%) !important; box-shadow: 0 4px 14px rgba(139, 92, 246, 0.4);">
        发布拍卖商品
      </el-button>
    </div>

    <!-- 拍卖商品列表 -->
    <div v-loading="loading" class="auction-grid">
      <el-empty v-if="auctionList.length === 0" description="目前没有进行中的拍卖，去发布一个试试吧！" />
      
      <div v-for="item in auctionList" :key="item.id" class="auction-card glass-card" :class="{ 'ended-card': item.status === 1 }">
        <div class="card-image-box">
          <img :src="item.imageUrl || defaultImage" alt="商品主图" class="auction-img" />
          <div class="status-overlay" :class="item.status === 1 ? 'status-ended' : 'status-active'">
            {{ item.status === 1 ? '已结标' : '竞拍中' }}
          </div>
        </div>

        <div class="card-details">
          <h3 class="item-name">{{ item.name }}</h3>
          <p class="item-desc">{{ item.description }}</p>

          <div class="price-section">
            <div class="price-item">
              <span class="price-label">起拍价:</span>
              <span class="price-value">￥{{ item.startPrice }}</span>
            </div>
            <div class="price-item current-price-item">
              <span class="price-label">当前最高出价:</span>
              <span class="price-value highlight-price">￥{{ item.currentPrice || item.startPrice }}</span>
            </div>
            <div class="price-item">
              <span class="price-label">最小加价:</span>
              <span class="price-value">￥{{ item.minIncrement }}</span>
            </div>
          </div>

          <!-- 竞拍倒计时 -->
          <div class="timer-box" v-if="item.status === 0">
            <el-icon><Timer /></el-icon>
            <span class="timer-text">{{ countdownText(item) }}</span>
          </div>

          <!-- 竞拍出价交互 -->
          <div class="bid-action-box" v-if="item.status === 0">
            <el-input-number 
              v-model="bidAmounts[item.id]" 
              :min="getMinBidAmount(item)" 
              :step="item.minIncrement"
              size="small"
              class="bid-input"
            />
            <el-button 
              type="primary" 
              size="small" 
              class="bid-btn"
              :loading="biddingId === item.id"
              @click="handleBid(item)"
            >
              出 价
            </el-button>
          </div>
          <div class="bid-ended-box" v-else>
            <el-alert 
              :title="item.highestBidderId ? '竞拍成功成交！' : '流拍（无人出价）'" 
              :type="item.highestBidderId ? 'success' : 'info'" 
              show-icon 
              :closable="false"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 发布拍卖对话框 -->
    <el-dialog v-model="publishDialog" title="发起拍卖商品" width="500px">
      <el-form :model="publishForm" :rules="publishRules" ref="formRef" label-position="top">
        <el-form-item label="关联闲置商品ID" prop="goodsId">
          <el-input v-model="publishForm.goodsId" placeholder="请输入您已发布的常规商品ID" />
        </el-form-item>

        <el-form-item label="起拍价格 (元)" prop="startPrice">
          <el-input-number v-model="publishForm.startPrice" :precision="2" :step="10" :min="0.01" style="width: 100%" />
        </el-form-item>

        <el-form-item label="最小加价幅度 (元)" prop="minIncrement">
          <el-input-number v-model="publishForm.minIncrement" :precision="2" :step="5" :min="1" style="width: 100%" />
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
const auctionList = ref([])
const biddingId = ref(null)

// 存储每个商品的临时输入出价
const bidAmounts = reactive({})

const defaultImage = 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&auto=format&fit=crop&q=60'

let timer = null

const formRef = ref(null)
const publishForm = reactive({
  goodsId: '',
  startPrice: 100.00,
  minIncrement: 10.00,
  timeRange: []
})

const publishRules = {
  goodsId: [{ required: true, message: '请输入商品ID', trigger: 'blur' }],
  startPrice: [{ required: true, message: '请设定起拍价格', trigger: 'blur' }],
  minIncrement: [{ required: true, message: '请设定最小加价幅度', trigger: 'blur' }],
  timeRange: [{ required: true, message: '请设定拍卖起止时间', trigger: 'change' }]
}

const fetchAuctionList = async () => {
  loading.value = true
  try {
    const response = await axios.get('http://localhost:8080/api/auction/list')
    if (response.data.code === 200) {
      auctionList.value = response.data.data
      
      // 初始化每个商品的竞价输入默认值
      auctionList.value.forEach(item => {
        if (!bidAmounts[item.id]) {
          bidAmounts[item.id] = getMinBidAmount(item)
        }
      })
    }
  } catch (error) {
    ElMessage.error('无法获取拍卖列表，请检查后端运行状态')
  } finally {
    loading.value = false
  }
}

const getMinBidAmount = (item) => {
  const current = item.currentPrice && item.currentPrice > 0 ? item.currentPrice : item.startPrice
  return current + item.minIncrement
}

// 出价
const handleBid = async (item) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录再参与出价！')
    router.push('/login')
    return
  }

  const bidPrice = bidAmounts[item.id]
  const minRequired = getMinBidAmount(item)
  if (bidPrice < minRequired) {
    ElMessage.warning(`出价必须大于或等于最低限制出价: ￥${minRequired}`)
    return
  }

  biddingId.value = item.id
  try {
    const headers = { Authorization: `Bearer ${userStore.token}` }
    const response = await axios.post(
      'http://localhost:8080/api/auction/bid',
      { auctionGoodsId: item.id, bidPrice: bidPrice },
      { headers }
    )

    if (response.data.code === 200) {
      ElMessage.success('恭喜，您的出价已被系统接受，当前处于领先！')
      fetchAuctionList() // 刷新最新出价
    } else {
      ElMessage.error(response.data.message || '出价失败')
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '出价抢占失败，出价已被他人抢先更新')
  } finally {
    biddingId.value = null
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
        startPrice: publishForm.startPrice,
        minIncrement: publishForm.minIncrement,
        startTime: publishForm.timeRange[0],
        endTime: publishForm.timeRange[1]
      }
      
      const response = await axios.post('http://localhost:8080/api/auction/publish', payload, { headers })
      if (response.data.code === 200) {
        ElMessage.success('商品拍卖活动发布成功！')
        publishDialog.value = false
        // 重置
        publishForm.goodsId = ''
        publishForm.timeRange = []
        fetchAuctionList()
      } else {
        ElMessage.error(response.data.message || '发布失败')
      }
    } catch (err) {
      ElMessage.error(err.response?.data?.message || '发布拍卖失败，请检查商品ID是否属于您发布！')
    } finally {
      publishing.value = false
    }
  })
}

const countdownText = (item) => {
  const now = new Date()
  const start = new Date(item.startTime)
  const end = new Date(item.endTime)
  
  if (now < start) {
    const diff = start - now
    return `距开拍: ${formatDuration(diff)}`
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

onMounted(() => {
  fetchAuctionList()
  
  // 1秒轮询更新倒计时状态，3秒轮询同步最新出价
  timer = setInterval(() => {
    // 强制刷新时间文本状态
    auctionList.value.forEach(item => {
      const now = new Date()
      const start = new Date(item.startTime)
      const end = new Date(item.endTime)
      item.started = now > start
      item.ended = now > end
    })
  }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.auction-container {
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

.auction-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}

.auction-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 0;
  height: 460px;
}

.ended-card {
  opacity: 0.75;
}

.card-image-box {
  position: relative;
  height: 160px;
  overflow: hidden;
  border-bottom: 1px solid var(--border-color);
}

.auction-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s;
}

.auction-card:hover .auction-img {
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

.status-active { background: rgba(139, 92, 246, 0.85); color: #fff; }
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

.price-section {
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.02);
  padding: 10px 14px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  gap: 4px;
}

.price-item {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-muted);
}

.price-value {
  color: var(--text-main);
  font-weight: 500;
}

.current-price-item {
  margin: 2px 0;
  border-top: 1px dashed rgba(255, 255, 255, 0.05);
  border-bottom: 1px dashed rgba(255, 255, 255, 0.05);
  padding: 4px 0;
}

.highlight-price {
  color: #8b5cf6;
  font-weight: 700;
  font-size: 15px;
  font-family: var(--font-display);
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
  justify-content: center;
}

.timer-text {
  font-family: var(--font-display);
  font-weight: 500;
}

.bid-action-box {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-top: auto;
}

.bid-input {
  flex: 1;
}

.bid-btn {
  background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%) !important;
  border: none !important;
}

.bid-ended-box {
  margin-top: auto;
}
</style>
