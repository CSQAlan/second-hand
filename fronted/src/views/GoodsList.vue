<template>
  <div class="goods-container">
    <!-- 顶部操作栏 -->
    <div class="action-bar glass-card">
      <div class="search-box">
        <el-input
          v-model="searchQuery"
          placeholder="搜索您心仪的二手商品..."
          prefix-icon="Search"
          clearable
          class="custom-search"
        />
      </div>
      <el-button type="primary" icon="Plus" @click="handleOpenPublish">
        发布闲置商品
      </el-button>
    </div>

    <!-- 商品列表 -->
    <div v-loading="loading" class="goods-grid">
      <el-empty v-if="filteredGoods.length === 0" description="暂无在售的二手商品，快去发布一个吧！" />
      
      <div v-for="item in filteredGoods" :key="item.id" class="goods-card glass-card clickable-card" @click="showDetail(item)">
        <div class="goods-img-wrapper">
          <img :src="item.imageUrl || defaultImage" alt="商品主图" class="goods-img" />
          <div class="price-tag">￥{{ item.price }}</div>
        </div>
        <div class="goods-info">
          <h3 class="goods-name">{{ item.name }}</h3>
          <p class="goods-desc">{{ item.description }}</p>
          <div class="goods-footer">
            <span class="publish-time">发布时间: {{ formatDate(item.createTime) }}</span>
            <el-button type="success" size="small" @click.stop="handleBuy(item)">
              立即购买
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 发布商品对话框 -->
    <el-dialog v-model="publishDialog" title="发布您的二手闲置" width="500px">
      <el-form :model="publishForm" :rules="publishRules" ref="formRef" label-position="top">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="publishForm.name" placeholder="请输入商品名称，如：iPhone 13 95新" />
        </el-form-item>

        <el-form-item label="转让价格 (元)" prop="price">
          <el-input-number v-model="publishForm.price" :precision="2" :step="10" :min="0.01" style="width: 100%" />
        </el-form-item>

        <el-form-item label="商品分类" prop="category">
          <el-select v-model="publishForm.category" placeholder="请选择商品所属分类" style="width: 100%">
            <el-option label="手机数码" value="手机数码" />
            <el-option label="图书教材" value="图书教材" />
            <el-option label="服饰鞋帽" value="服饰鞋帽" />
            <el-option label="美妆个护" value="美妆个护" />
            <el-option label="其它闲置" value="其它闲置" />
          </el-select>
        </el-form-item>

        <el-form-item label="新旧成色" prop="condition">
          <el-select v-model="publishForm.condition" placeholder="请选择商品成色" style="width: 100%">
            <el-option label="全新" value="全新" />
            <el-option label="九五新" value="九五新" />
            <el-option label="九成新" value="九成新" />
            <el-option label="八成新" value="八成新" />
            <el-option label="七成新及以下" value="七成新及以下" />
          </el-select>
        </el-form-item>

        <el-form-item label="交易形式" prop="tradingMethod">
          <el-select v-model="publishForm.tradingMethod" placeholder="请选择交易形式" style="width: 100%">
            <el-option label="邮寄" value="邮寄" />
            <el-option label="面交" value="面交" />
            <el-option label="面交/邮寄" value="面交/邮寄" />
          </el-select>
        </el-form-item>

        <el-form-item label="交易所在地" prop="location">
          <el-input v-model="publishForm.location" placeholder="请输入交易地点，如：北京大学、海淀区等" />
        </el-form-item>

        <el-form-item label="商品图片" prop="imageUrl">
          <el-upload
            class="image-uploader"
            action="http://localhost:8080/api/upload"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleUploadSuccess"
            :before-upload="beforeUpload"
          >
            <img v-if="publishForm.imageUrl" :src="publishForm.imageUrl" class="uploaded-image" />
            <el-icon v-else class="image-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div style="font-size: 11px; color: var(--text-dim); margin-top: 6px;">
            支持上传本地图片 (存储在本地磁盘并记录到数据库)，或在下方直接粘贴外链 URL
          </div>
          <el-input v-model="publishForm.imageUrl" placeholder="或者直接输入/修改图片 URL 外链" style="margin-top: 8px;" />
        </el-form-item>

        <el-form-item label="商品描述" prop="description">
          <el-input v-model="publishForm.description" type="textarea" :rows="4" placeholder="请详细描述商品的成色、配件等信息..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="publishDialog = false">取 消</el-button>
          <el-button type="primary" @click="submitPublish" :loading="publishing">确 认 发 布</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 商品详情抽屉 -->
    <el-drawer
      v-model="detailDrawer"
      title="商品详细档案"
      size="480px"
      class="goods-detail-drawer"
      destroy-on-close
    >
      <div v-if="selectedGoods" class="detail-content" v-loading="loadingDetail">
        <div class="detail-img-box">
          <img :src="selectedGoods.imageUrl || defaultImage" alt="商品图片" class="detail-large-img" />
          <span class="detail-category-badge">{{ selectedGoods.category || '未分类' }}</span>
        </div>

        <div class="detail-body">
          <div class="detail-title-row">
            <h2 class="detail-name">{{ selectedGoods.name }}</h2>
            <span class="detail-price">￥{{ selectedGoods.price }}</span>
          </div>

          <div class="detail-stats-bar">
            <span class="stat-item"><el-icon><View /></el-icon> 浏览量: {{ selectedGoods.viewCount || 0 }}</span>
            <span class="stat-item"><el-icon><Star /></el-icon> 收藏数: {{ detailData.favoriteCount || 0 }}</span>
          </div>

          <el-divider />

          <div class="detail-section">
            <h4 class="section-title">宝贝属性</h4>
            <div class="attributes-grid">
              <div class="attr-item"><span class="attr-label">新旧成色:</span> <span class="attr-val">{{ selectedGoods.condition || '未知' }}</span></div>
              <div class="attr-item"><span class="attr-label">交易形式:</span> <span class="attr-val">{{ selectedGoods.tradingMethod || '未知' }}</span></div>
              <div class="attr-item"><span class="attr-label">所在地:</span> <span class="attr-val">{{ selectedGoods.location || '未知' }}</span></div>
            </div>
          </div>

          <el-divider />

          <div class="detail-section">
            <h4 class="section-title">宝贝介绍</h4>
            <p class="detail-desc-text">{{ selectedGoods.description }}</p>
          </div>

          <el-divider />

          <!-- 卖家信息 -->
          <div class="detail-section" v-if="detailData.seller">
            <h4 class="section-title">发布者信息</h4>
            <div class="seller-card-inner">
              <el-avatar :size="40" :src="detailData.seller.avatar || defaultAvatar" />
              <div class="seller-info-content">
                <span class="seller-nickname">{{ detailData.seller.nickname || detailData.seller.username }}</span>
                <span class="seller-role">{{ detailData.seller.role === 'ROLE_ADMIN' ? '官方管理员' : '普通卖家' }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部操作 -->
        <div class="detail-drawer-footer">
          <el-button 
            :type="detailData.isFavorite ? 'warning' : 'info'" 
            :icon="detailData.isFavorite ? 'StarFilled' : 'Star'" 
            circle
            @click="toggleFavorite"
            :loading="togglingFav"
          />
          <el-button type="primary" class="chat-seller-btn" icon="ChatDotRound" @click="chatWithSeller">
            联系卖家
          </el-button>
          <el-button type="success" class="buy-now-btn" @click="handleBuy(selectedGoods)">
            立即购买
          </el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useUserStore } from '../store/user'
import { useChatStore } from '../store/chat'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter, useRoute } from 'vue-router'
import axios from 'axios'

const userStore = useUserStore()
const chatStore = useChatStore()
const router = useRouter()
const route = useRoute()

const loading = ref(false)
const publishing = ref(false)
const publishDialog = ref(false)
const searchQuery = ref('')
const goodsList = ref([])

const defaultImage = 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500&auto=format&fit=crop&q=60'
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const formRef = ref(null)
const publishForm = reactive({
  name: '',
  price: 99.00,
  category: '其它闲置',
  condition: '九成新',
  tradingMethod: '面交/邮寄',
  location: '',
  imageUrl: '',
  description: ''
})

const publishRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  description: [{ required: true, message: '请添加商品描述', trigger: 'blur' }]
}

const uploadHeaders = computed(() => {
  return {
    Authorization: `Bearer ${userStore.token}`
  }
})

const beforeUpload = (file) => {
  const isJPGorPNG = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/gif' || file.type === 'image/webp'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJPGorPNG) {
    ElMessage.error('图片只能是 JPG/PNG/GIF/WEBP 格式!')
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!')
  }
  return isJPGorPNG && isLt2M
}

const handleUploadSuccess = (response) => {
  if (response.code === 200) {
    publishForm.imageUrl = response.data
    ElMessage.success('图片上传成功！')
  } else {
    ElMessage.error(response.message || '图片上传失败')
  }
}

const filteredGoods = computed(() => {
  return goodsList.value
})

// 监听搜索框输入，通过防抖调用后端搜索接口实现真正的向量/全文检索
let searchTimeout = null
watch(searchQuery, (newVal) => {
  if (searchTimeout) clearTimeout(searchTimeout)
  searchTimeout = setTimeout(async () => {
    loading.value = true
    try {
      let url = 'http://localhost:8080/api/goods/list'
      if (newVal.trim()) {
        url = `http://localhost:8080/api/goods/search?keyword=${encodeURIComponent(newVal.trim())}`
      }
      const response = await axios.get(url)
      if (response.data.code === 200) {
        goodsList.value = response.data.data
      }
    } catch (error) {
      console.error('搜索商品失败:', error)
      ElMessage.error('搜索请求失败，请检查网络')
    } finally {
      loading.value = false
    }
  }, 300) // 300ms 防抖频率
})

const fetchGoods = async () => {
  loading.value = true
  try {
    const response = await axios.get('http://localhost:8080/api/goods/list')
    if (response.data.code === 200) {
      goodsList.value = response.data.data
    }
  } catch (error) {
    logError(error)
  } finally {
    loading.value = false
  }
}

const handleBuy = (item) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再进行购买！')
    router.push('/login')
    return
  }

  ElMessageBox.confirm(
    `您确认要以 ￥${item.price} 的价格购买商品“${item.name}”吗？`,
    '购买确认',
    {
      confirmButtonText: '确认付款',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      const headers = { Authorization: `Bearer ${userStore.token}` }
      const response = await axios.post(`http://localhost:8080/api/goods/buy/${item.id}`, {}, { headers })
      if (response.data.code === 200) {
        ElMessage.success('购买成功！已自动扣款并生成订单。')
        detailDrawer.value = false // 购买成功后关闭抽屉
        fetchGoods()
      } else {
        ElMessage.error(response.data.message || '购买失败')
      }
    } catch (err) {
      ElMessage.error(err.response?.data?.message || '购买过程出错，请重试')
    }
  }).catch(() => {})
}

// 商品详情与收藏私聊控制
const detailDrawer = ref(false)
const selectedGoods = ref(null)
const detailData = ref({
  favoriteCount: 0,
  isFavorite: false,
  seller: null
})
const loadingDetail = ref(false)
const togglingFav = ref(false)

const showDetail = async (item) => {
  selectedGoods.value = item
  detailDrawer.value = true
  loadingDetail.value = true
  try {
    const headers = userStore.isLoggedIn ? { Authorization: `Bearer ${userStore.token}` } : {}
    const response = await axios.get(`http://localhost:8080/api/goods/detail/${item.id}`, { headers })
    if (response.data.code === 200) {
      detailData.value = response.data.data
      if (response.data.data.goods) {
        selectedGoods.value = response.data.data.goods
      }
    }
  } catch (error) {
    console.error('获取商品详情失败:', error)
  } finally {
    loadingDetail.value = false
  }
}

const toggleFavorite = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录再进行收藏！')
    router.push('/login')
    return
  }
  togglingFav.value = true
  try {
    const headers = { Authorization: `Bearer ${userStore.token}` }
    const response = await axios.post(`http://localhost:8080/api/favorite/toggle/${selectedGoods.value.id}`, {}, { headers })
    if (response.data.code === 200) {
      detailData.value.isFavorite = !detailData.value.isFavorite
      detailData.value.favoriteCount += detailData.value.isFavorite ? 1 : -1
      ElMessage.success(detailData.value.isFavorite ? '收藏成功！' : '已取消收藏')
    }
  } catch (error) {
    ElMessage.error('操作收藏失败')
  } finally {
    togglingFav.value = false
  }
}

const chatWithSeller = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录再联系卖家！')
    router.push('/login')
    return
  }
  if (selectedGoods.value.sellerId === Number(userStore.userId)) {
    ElMessage.warning('这是您自己发布的商品哦！')
    return
  }
  detailDrawer.value = false
  chatStore.openChat(
    selectedGoods.value.sellerId,
    detailData.value.seller?.nickname || detailData.value.seller?.username || '卖家',
    selectedGoods.value.id
  )
}

const handleOpenPublish = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录再发布闲置商品！')
    router.push('/login')
    return
  }
  publishDialog.value = true
}

const submitPublish = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('登录失效，请先登录！')
    router.push('/login')
    return
  }

  formRef.value.validate(async (valid) => {
    if (!valid) return
    publishing.value = true
    try {
      const headers = { Authorization: `Bearer ${userStore.token}` }
      const response = await axios.post('http://localhost:8080/api/goods/publish', publishForm, { headers })
      if (response.data.code === 200) {
        ElMessage.success('商品发布成功！')
        publishDialog.value = false
        // 重置表单
        publishForm.name = ''
        publishForm.price = 99.00
        publishForm.category = '其它闲置'
        publishForm.condition = '九成新'
        publishForm.tradingMethod = '面交/邮寄'
        publishForm.location = ''
        publishForm.imageUrl = ''
        publishForm.description = ''
        fetchGoods()
      } else {
        ElMessage.error(response.data.message || '发布失败')
      }
    } catch (err) {
      ElMessage.error(err.response?.data?.message || '系统繁忙，请重试')
    } finally {
      publishing.value = false
    }
  })
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const logError = (error) => {
  console.error(error)
  ElMessage.error('无法连接到后端服务器')
}

// 监听 URL 路由中的 detailId，实现点击客服推荐链接自动弹出商品详情抽屉
watch(() => route.query.detailId, (newId) => {
  if (newId) {
    showDetail({ id: Number(newId) })
  }
})

onMounted(() => {
  fetchGoods()
  if (route.query.detailId) {
    showDetail({ id: Number(route.query.detailId) })
  }
})
</script>

<style scoped>
.goods-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
}

.search-box {
  width: 400px;
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
  min-height: 200px;
}

.goods-card {
  display: flex;
  flex-direction: column;
  height: 380px;
  overflow: hidden;
  padding: 0;
}

.goods-img-wrapper {
  position: relative;
  height: 200px;
  width: 100%;
  overflow: hidden;
  border-bottom: 1px solid var(--border-color);
}

.goods-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s;
}

.goods-card:hover .goods-img {
  transform: scale(1.1);
}

.price-tag {
  position: absolute;
  bottom: 12px;
  right: 12px;
  background: rgba(99, 102, 241, 0.85);
  backdrop-filter: blur(4px);
  color: #fff;
  padding: 4px 10px;
  border-radius: 20px;
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.goods-info {
  display: flex;
  flex-direction: column;
  padding: 16px;
  flex: 1;
}

.goods-name {
  font-size: 16px;
  margin-bottom: 8px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.goods-desc {
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.5;
  margin-bottom: 16px;
  height: 38px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.goods-footer {
  margin-top: auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.publish-time {
  font-size: 11px;
  color: var(--text-dim);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.image-uploader :deep(.el-upload) {
  border: 1px dashed var(--border-color);
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 120px;
  height: 120px;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: rgba(255, 255, 255, 0.02);
  transition: border-color 0.2s;
}

.image-uploader :deep(.el-upload:hover) {
  border-color: var(--color-primary);
}

.image-uploader-icon {
  font-size: 28px;
  color: var(--text-dim);
}

.uploaded-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.clickable-card {
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.clickable-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
  border-color: rgba(99, 102, 241, 0.2);
}

/* 商品详情抽屉样式 */
.goods-detail-drawer :deep(.el-drawer) {
  background-color: #0d1117 !important;
  color: #fff;
  border-left: 1px solid rgba(255, 255, 255, 0.08) !important;
}

.goods-detail-drawer :deep(.el-drawer__title) {
  color: #a5b4fc !important;
  font-weight: 700;
  font-family: var(--font-display);
}

.detail-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding-bottom: 60px;
}

.detail-img-box {
  position: relative;
  width: 100%;
  height: 240px;
  overflow: hidden;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.detail-large-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-category-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  background: rgba(99, 102, 241, 0.85);
  color: #fff;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
}

.detail-body {
  padding: 16px 0;
  flex: 1;
  overflow-y: auto;
  margin-bottom: 20px;
}

.detail-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.detail-name {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
}

.detail-price {
  font-size: 24px;
  font-weight: 800;
  color: #10b981;
  font-family: var(--font-display);
}

.detail-stats-bar {
  display: flex;
  gap: 16px;
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-dim);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.section-title {
  font-size: 13px;
  font-weight: 700;
  color: #a5b4fc;
  letter-spacing: 0.05em;
  margin-bottom: 4px;
}

.attributes-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  background-color: rgba(255, 255, 255, 0.02);
  padding: 12px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.04);
}

.attr-item {
  font-size: 12px;
}

.attr-label {
  color: var(--text-muted);
  margin-right: 6px;
}

.attr-val {
  color: #fff;
  font-weight: 600;
}

.detail-desc-text {
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.6;
  white-space: pre-wrap;
}

.seller-card-inner {
  display: flex;
  align-items: center;
  gap: 12px;
  background-color: rgba(255, 255, 255, 0.02);
  padding: 12px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.04);
}

.seller-info-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.seller-nickname {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
}

.seller-role {
  font-size: 11px;
  color: var(--text-dim);
}

.detail-drawer-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  background-color: #0b0d13;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  padding: 12px 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  z-index: 10;
}

.chat-seller-btn {
  background: linear-gradient(135deg, #4f46e5 0%, #3730a3 100%) !important;
  border: none !important;
  flex: 1;
}

.buy-now-btn {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%) !important;
  border: none !important;
  flex: 1.2;
}
</style>
