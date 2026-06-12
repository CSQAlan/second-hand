<template>
  <div class="register-wrapper">
    <div class="aura-1"></div>
    <div class="aura-2"></div>
    <div class="register-container glass-card">
      <h2 class="register-title text-gradient text-glow-primary">用户注册</h2>
      <p class="register-subtitle">创建您的二手系统账户</p>
      
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入注册用户名" prefix-icon="User" />
        </el-form-item>

        <label class="el-form-item__label" style="padding: 0 0 4px; display: inline-block;">昵称</label>
        <el-form-item prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入您的系统昵称" prefix-icon="Edit" />
        </el-form-item>
        
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" prefix-icon="Lock" show-password />
        </el-form-item>

        <div class="form-actions">
          <el-button type="primary" @click="handleRegister" :loading="loading" class="register-btn">
            注 册 账 户
          </el-button>
        </div>
      </el-form>

      <div class="login-link">
        已有账号？<router-link to="/login">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: ''
})

const validatePass2 = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const rules = {
  username: [{ required: true, message: '请设定用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请设定密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少为 6 位', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validatePass2, trigger: 'blur' }]
}

const handleRegister = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    
    loading.value = true
    try {
      const response = await axios.post('http://localhost:8080/api/auth/register', {
        username: form.username,
        password: form.password,
        nickname: form.nickname
      })
      if (response.data.code === 200) {
        ElMessage.success('注册成功，请登录')
        router.push('/login')
      } else {
        ElMessage.error(response.data.message || '注册失败')
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
.register-wrapper {
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

.register-container {
  width: 400px;
  z-index: 10;
  padding: 30px 40px;
}

.register-title {
  font-size: 26px;
  text-align: center;
  margin-bottom: 8px;
  font-weight: 700;
}

.register-subtitle {
  text-align: center;
  color: var(--text-muted);
  font-size: 14px;
  margin-bottom: 24px;
}

.register-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  margin-top: 16px;
  border-radius: 8px;
}

.login-link {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: var(--text-muted);
}

.login-link a {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s;
}

.login-link a:hover {
  color: #818cf8;
  text-decoration: underline;
}
</style>
