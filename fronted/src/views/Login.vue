<template>
  <div class="login-wrapper">
    <div class="aura-1"></div>
    <div class="aura-2"></div>
    <div class="login-container glass-card">
      <h2 class="login-title text-gradient text-glow-primary">二手交易系统</h2>
      <p class="login-subtitle">请登录您的账户</p>
      
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" />
        </el-form-item>
        
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password />
        </el-form-item>

        <div class="form-actions">
          <el-button type="primary" @click="handleLogin" :loading="loading" class="login-btn">
            立 即 登 录
          </el-button>
        </div>
      </el-form>

      <div class="register-link">
        还没有账号？<router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    
    loading.value = true
    try {
      const response = await axios.post('/api/auth/login', form)
      if (response.data.code === 200) {
        userStore.login(response.data.data)
        ElMessage.success('登录成功')
        router.push('/')
      } else {
        ElMessage.error(response.data.message || '登录失败')
      }
    } catch (error) {
      ElMessage.error(error.response?.data?.message || '网络连接异常，请检查后端运行状态')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-wrapper {
  position: relative;
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: var(--bg-main);
  overflow: hidden;
}

.aura-1 {
  position: absolute;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(99,102,241,0.2) 0%, rgba(99,102,241,0) 70%);
  top: 15%;
  left: 20%;
  filter: blur(40px);
  animation: pulse 6s infinite alternate;
}

.aura-2 {
  position: absolute;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(139,92,246,0.15) 0%, rgba(139,92,246,0) 70%);
  bottom: 15%;
  right: 20%;
  filter: blur(50px);
  animation: pulse 8s infinite alternate-reverse;
}

.login-container {
  width: 400px;
  z-index: 10;
  padding: 40px;
}

.login-title {
  font-size: 28px;
  text-align: center;
  margin-bottom: 8px;
  font-weight: 700;
}

.login-subtitle {
  text-align: center;
  color: var(--text-muted);
  font-size: 14px;
  margin-bottom: 32px;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  margin-top: 16px;
  border-radius: 8px;
}

.register-link {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: var(--text-muted);
}

.register-link a {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s;
}

.register-link a:hover {
  color: #818cf8;
  text-decoration: underline;
}
</style>
