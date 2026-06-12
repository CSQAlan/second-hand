<!-- [Skill: SKILL.md] -->
<template>
  <div class="user-center-container">
    <el-row :gutter="24">
      <!-- 个人信息卡片 -->
      <el-col :xs="24" :sm="24" :md="8" :lg="6">
        <div class="profile-card glass-card">
          <div class="avatar-area">
            <el-avatar :size="80" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
            <h2 class="nickname">{{ userStore.nickname || userStore.username }}</h2>
            <div style="display: flex; gap: 8px; justify-content: center; margin-top: 8px;">
              <el-tag v-if="userStore.role === 'ROLE_ADMIN'" type="danger" class="role-tag">超级管理员</el-tag>
              <template v-else-if="userStore.role === 'ROLE_USER'">
                <el-tag type="primary" class="role-tag">二手买家</el-tag>
                <el-tag type="success" class="role-tag">二手卖家</el-tag>
              </template>
              <el-tag v-else type="info" class="role-tag">普通用户</el-tag>
            </div>
          </div>
          
          <div class="profile-details">
            <div class="detail-item">
              <span class="label">用户名:</span>
              <span class="value">{{ userStore.username }}</span>
            </div>
            <div class="detail-item">
              <span class="label">用户ID:</span>
              <span class="value">#{{ userStore.userId }}</span>
            </div>
            <div class="detail-item">
              <span class="label">当前身份:</span>
              <span class="value">{{ roleDesc }}</span>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 核心业务订单列表 -->
      <el-col :xs="24" :sm="24" :md="16" :lg="18">
        <div class="orders-card glass-card">
          <el-tabs v-model="activeTab" class="custom-tabs" @tab-change="handleTabChange">
            <!-- 我买到的商品 -->
            <el-tab-pane label="我买到的商品" name="bought">
              <div v-loading="loading" class="order-list">
                <el-empty v-if="boughtOrders.length === 0" description="您还没有买过商品，快去集市逛逛吧！" />
                
                <div v-for="order in boughtOrders" :key="order.id" class="order-item glass-card-nested">
                  <div class="order-goods-img">
                    <img :src="order.goodsImageUrl || defaultImage" alt="商品图" />
                  </div>
                  <div class="order-goods-info">
                    <div class="order-header-row">
                      <span class="order-no">订单号: {{ order.orderNo }}</span>
                      <el-tag :type="getStatusTagType(order.status)">{{ getStatusName(order.status) }}</el-tag>
                    </div>
                    <h4 class="goods-name">{{ order.goodsName }}</h4>
                    <div class="order-details">
                      <span class="price">实付款: ￥{{ order.price }}</span>
                      <span class="time">下单时间: {{ formatDateTime(order.createTime) }}</span>
                    </div>
                    <div v-if="order.deliveryNo" class="delivery-info">
                      <el-icon><Postcard /></el-icon>
                      <span>物流单号: {{ order.deliveryNo }}</span>
                    </div>
                  </div>
                  <div class="order-actions">
                    <el-button
                      v-if="order.status === 2"
                      type="success"
                      size="small"
                      @click="handleReceive(order.orderNo)"
                    >
                      确认收货
                    </el-button>
                  </div>
                </div>
              </div>
            </el-tab-pane>

            <!-- 我卖出的商品 -->
            <el-tab-pane label="我卖出的商品" name="sold">
              <div v-loading="loading" class="order-list">
                <el-empty v-if="soldOrders.length === 0" description="您还没有卖出过商品，努力发布宝贝吧！" />
                
                <div v-for="order in soldOrders" :key="order.id" class="order-item glass-card-nested">
                  <div class="order-goods-img">
                    <img :src="order.goodsImageUrl || defaultImage" alt="商品图" />
                  </div>
                  <div class="order-goods-info">
                    <div class="order-header-row">
                      <span class="order-no">订单号: {{ order.orderNo }}</span>
                      <el-tag :type="getStatusTagType(order.status)">{{ getStatusName(order.status) }}</el-tag>
                    </div>
                    <h4 class="goods-name">{{ order.goodsName }}</h4>
                    <div class="order-details">
                      <span class="price">货款: ￥{{ order.price }}</span>
                      <span class="time">创建时间: {{ formatDateTime(order.createTime) }}</span>
                    </div>
                    <div v-if="order.deliveryNo" class="delivery-info">
                      <el-icon><Postcard /></el-icon>
                      <span>物流单号: {{ order.deliveryNo }}</span>
                    </div>
                  </div>
                  <div class="order-actions">
                    <el-button
                      v-if="order.status === 1"
                      type="primary"
                      size="small"
                      @click="openShipDialog(order)"
                    >
                      立即发货
                    </el-button>
                  </div>
                </div>
              </div>
            </el-tab-pane>

            <!-- 我发布的商品 -->
            <el-tab-pane label="我发布的闲置" name="listings">
              <div v-loading="loading" class="order-list">
                <el-empty v-if="myListings.length === 0" description="您还没有发布过任何闲置商品！" />
                
                <div v-for="item in myListings" :key="item.id" class="order-item glass-card-nested">
                  <div class="order-goods-img">
                    <img :src="item.imageUrl || defaultImage" alt="商品图" />
                  </div>
                  <div class="order-goods-info">
                    <div class="order-header-row">
                      <span class="order-no">商品ID: #{{ item.id }}</span>
                      <el-tag :type="item.status === 0 ? 'success' : 'info'">
                        {{ item.status === 0 ? '在售中' : '已售出/下架' }}
                      </el-tag>
                    </div>
                    <h4 class="goods-name">{{ item.name }}</h4>
                    <p class="goods-desc">{{ item.description }}</p>
                    <div class="order-details">
                      <span class="price">价格: ￥{{ item.price }}</span>
                      <span class="time">发布时间: {{ formatDateTime(item.createTime) }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-col>
    </el-row>

    <!-- 发货弹框 -->
    <el-dialog v-model="shipDialogVisible" title="填写物流信息" width="450px" class="ship-dialog">
      <el-form :model="shipForm" :rules="shipRules" ref="shipFormRef" label-position="top">
        <el-form-item label="物流快递单号" prop="deliveryNo">
          <el-input v-model="shipForm.deliveryNo" placeholder="请输入快递运单号，如：SF123456789" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="shipDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitShip" :loading="shipping">确认发货</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '../store/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const userStore = useUserStore()
const activeTab = ref('bought')
const loading = ref(false)
const shipping = ref(false)

const boughtOrders = ref([])
const soldOrders = ref([])
const myListings = ref([])

const defaultImage = 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500&auto=format&fit=crop&q=60'

// 角色相关属性
const roleType = computed(() => {
  if (userStore.role === 'ROLE_ADMIN') return 'danger'
  return 'primary'
})

const roleName = computed(() => {
  if (userStore.role === 'ROLE_ADMIN') return '超级管理员'
  return '二手卖家/买家'
})

const roleDesc = computed(() => {
  if (userStore.role === 'ROLE_ADMIN') return '平台数据监控与环境预热管理员'
  return '支持浏览宝贝、拍付商品、商家发货与买家确认收货'
})

// 发货弹窗
const shipDialogVisible = ref(false)
const shipFormRef = ref(null)
const currentOrder = ref(null)
const shipForm = ref({
  deliveryNo: ''
})

const shipRules = {
  deliveryNo: [
    { required: true, message: '请填写快递单号', trigger: 'blur' },
    { min: 5, message: '单号格式不正确', trigger: 'blur' }
  ]
}

// 标签分类加载数据
const handleTabChange = (name) => {
  if (name === 'bought') {
    fetchBought()
  } else if (name === 'sold') {
    fetchSold()
  } else if (name === 'listings') {
    fetchListings()
  }
}

// 买到的订单
const fetchBought = async () => {
  loading.value = true
  try {
    const headers = { Authorization: `Bearer ${userStore.token}` }
    const response = await axios.get('http://localhost:8080/api/orders/buyer', { headers })
    if (response.data.code === 200) {
      boughtOrders.value = response.data.data
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '获取买到的订单失败')
  } finally {
    loading.value = false
  }
}

// 卖出的订单
const fetchSold = async () => {
  loading.value = true
  try {
    const headers = { Authorization: `Bearer ${userStore.token}` }
    const response = await axios.get('http://localhost:8080/api/orders/seller', { headers })
    if (response.data.code === 200) {
      soldOrders.value = response.data.data
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '获取卖出的订单失败')
  } finally {
    loading.value = false
  }
}

// 发布的闲置
const fetchListings = async () => {
  loading.value = true
  try {
    // 后端接口暂无直接的用户发布列表筛选，采用全表获取后前端过滤（ sellerId 等于当前用户）
    const response = await axios.get('http://localhost:8080/api/goods/list')
    if (response.data.code === 200) {
      myListings.value = response.data.data.filter(
        (item) => String(item.sellerId) === String(userStore.userId)
      )
    }
  } catch (err) {
    ElMessage.error('获取发布的商品列表失败')
  } finally {
    loading.value = false
  }
}

// 确认收货
const handleReceive = (orderNo) => {
  ElMessageBox.confirm('您收到货物了吗？请务必在确认收到且商品完好后点击确认收货！', '收货确认', {
    confirmButtonText: '确认收货',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const headers = { Authorization: `Bearer ${userStore.token}` }
      const response = await axios.post(`http://localhost:8080/api/orders/receive/${orderNo}`, {}, { headers })
      if (response.data.code === 200) {
        ElMessage.success('确认收货成功！交易完成。')
        fetchBought()
      } else {
        ElMessage.error(response.data.message || '收货失败')
      }
    } catch (err) {
      ElMessage.error(err.response?.data?.message || '收货请求出错')
    }
  }).catch(() => {})
}

// 打开发货弹框
const openShipDialog = (order) => {
  currentOrder.value = order
  shipForm.value.deliveryNo = ''
  shipDialogVisible.value = true
}

// 确认发货提交
const submitShip = () => {
  shipFormRef.value.validate(async (valid) => {
    if (!valid) return
    shipping.value = true
    try {
      const headers = { Authorization: `Bearer ${userStore.token}` }
      const response = await axios.post(
        `http://localhost:8080/api/orders/ship/${currentOrder.value.orderNo}`,
        { deliveryNo: shipForm.value.deliveryNo },
        { headers }
      )
      if (response.data.code === 200) {
        ElMessage.success('商品发货成功！已录入单号。')
        shipDialogVisible.value = false
        fetchSold()
      } else {
        ElMessage.error(response.data.message || '发货失败')
      }
    } catch (err) {
      ElMessage.error(err.response?.data?.message || '请求发货出错')
    } finally {
      shipping.value = false
    }
  })
}

// 订单状态转化器
const getStatusName = (status) => {
  switch (status) {
    case 0: return '待付款'
    case 1: return '等待卖家发货'
    case 2: return '卖家已发货'
    case 3: return '交易成功'
    default: return '未知'
  }
}

const getStatusTagType = (status) => {
  switch (status) {
    case 0: return 'warning'
    case 1: return 'danger'
    case 2: return 'primary'
    case 3: return 'success'
    default: return 'info'
  }
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

onMounted(() => {
  fetchBought()
})
</script>

<style scoped>
.user-center-container {
  display: flex;
  flex-direction: column;
}

.profile-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px 24px;
  text-align: center;
  margin-bottom: 24px;
}

.avatar-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid var(--border-color);
  width: 100%;
  padding-bottom: 24px;
  margin-bottom: 24px;
}

.nickname {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-main);
  margin: 0;
}

.role-tag {
  border: none;
}

.profile-details {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.detail-item .label {
  color: var(--text-muted);
}

.detail-item .value {
  color: var(--text-main);
  font-weight: 500;
}

.orders-card {
  padding: 24px;
  min-height: 480px;
}

.custom-tabs :deep(.el-tabs__item) {
  color: var(--text-muted);
  font-size: 14px;
  font-family: var(--font-display);
}

.custom-tabs :deep(.el-tabs__item.is-active) {
  color: var(--color-primary);
  font-weight: 600;
}

.custom-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--color-primary);
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 16px;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 16px;
  border-radius: 12px;
}

.glass-card-nested {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.05);
  box-shadow: none;
  transition: all 0.3s;
}

.order-item:hover {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.1);
}

.order-goods-img {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  border: 1px solid var(--border-color);
}

.order-goods-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.order-goods-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.order-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-no {
  font-size: 12px;
  color: var(--text-dim);
}

.goods-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-main);
  margin: 0;
}

.goods-desc {
  font-size: 12px;
  color: var(--text-muted);
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.order-details {
  display: flex;
  gap: 24px;
  font-size: 13px;
}

.order-details .price {
  color: #f59e0b;
  font-weight: 600;
}

.order-details .time {
  color: var(--text-dim);
}

.delivery-info {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--color-primary);
  background: var(--color-primary-glow);
  padding: 4px 8px;
  border-radius: 4px;
  width: fit-content;
}

.order-actions {
  display: flex;
  align-items: center;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
