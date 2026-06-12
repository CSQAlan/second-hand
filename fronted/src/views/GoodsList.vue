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
      <el-button type="primary" icon="Plus" @click="publishDialog = true">
        发布闲置商品
      </el-button>
    </div>

    <!-- 商品列表 -->
    <div v-loading="loading" class="goods-grid">
      <el-empty v-if="filteredGoods.length === 0" description="暂无在售的二手商品，快去发布一个吧！" />
      
      <div v-for="item in filteredGoods" :key="item.id" class="goods-card glass-card">
        <div class="goods-img-wrapper">
          <img :src="item.imageUrl || defaultImage" alt="商品主图" class="goods-img" />
          <div class="price-tag">￥{{ item.price }}</div>
        </div>
        <div class="goods-info">
          <h3 class="goods-name">{{ item.name }}</h3>
          <p class="goods-desc">{{ item.description }}</p>
          <div class="goods-footer">
            <span class="publish-time">发布时间: {{ formatDate(item.createTime) }}</span>
            <el-button type="success" size="small" @click="handleBuy(item)">
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
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useUserStore } from '../store/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import axios from 'axios'

const userStore = useUserStore()
const router = useRouter()

const loading = ref(false)
const publishing = ref(false)
const publishDialog = ref(false)
const searchQuery = ref('')
const goodsList = ref([])

const defaultImage = 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500&auto=format&fit=crop&q=60'

const formRef = ref(null)
const publishForm = reactive({
  name: '',
  price: 99.00,
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
  if (!searchQuery.value) return goodsList.value
  return goodsList.value.filter(item => 
    item.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
    item.description.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
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
        fetchGoods()
      } else {
        ElMessage.error(response.data.message || '购买失败')
      }
    } catch (err) {
      ElMessage.error(err.response?.data?.message || '购买过程出错，请重试')
    }
  }).catch(() => {})
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

onMounted(() => {
  fetchGoods()
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
</style>
