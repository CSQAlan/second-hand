<template>
  <el-container class="app-layout">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside">
      <div class="logo-area">
        <el-icon size="24" color="#6366f1"><Shop /></el-icon>
        <span v-show="!isCollapse" class="logo-text text-gradient text-glow-primary">闲鱼极速版</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        class="layout-menu"
        :collapse="isCollapse"
        router
      >
        <el-menu-item index="/goods">
          <el-icon><Goods /></el-icon>
          <template #title>二手集市</template>
        </el-menu-item>
        
        <el-menu-item index="/seckill">
          <el-icon><Lightning /></el-icon>
          <template #title>限定闪购</template>
        </el-menu-item>
        
        <el-menu-item index="/auction">
          <el-icon><TrendCharts /></el-icon>
          <template #title>拍卖大厅</template>
        </el-menu-item>

        <el-menu-item v-if="userStore.role === 'ROLE_ADMIN'" index="/dashboard">
          <el-icon><DataLine /></el-icon>
          <template #title>控制台面板</template>
        </el-menu-item>

        <el-menu-item v-if="userStore.isLoggedIn" index="/user-center">
          <el-icon><User /></el-icon>
          <template #title>个人中心</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶部导航栏 -->
      <el-header class="layout-header">
        <div class="header-left">
          <el-button type="text" @click="isCollapse = !isCollapse" class="toggle-btn">
            <el-icon size="20">
              <Fold v-if="!isCollapse" />
              <Expand v-else />
            </el-icon>
          </el-button>
        </div>

        <div class="header-right">
          <!-- 消息信箱入口 -->
          <el-button 
            v-if="userStore.isLoggedIn" 
            type="text" 
            icon="ChatDotRound" 
            class="mailbox-btn"
            @click="openMailbox"
          >
            私信信箱
          </el-button>

          <!-- 未登录状态 -->
          <div v-if="!userStore.isLoggedIn" class="user-action">
            <el-button type="primary" size="small" @click="goToLogin">去登录</el-button>
          </div>
          <!-- 已登录状态 -->
          <el-dropdown v-else trigger="click" @command="handleCommand">
            <div class="user-profile">
              <el-avatar :size="32" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
              <span class="user-name">{{ userStore.nickname || userStore.username }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主视图区域 -->
      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
    
    <!-- 全局聊天弹框 -->
    <ChatDialog />
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { useChatStore } from '../store/chat'
import { ElMessage } from 'element-plus'
import ChatDialog from '../components/ChatDialog.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const chatStore = useChatStore()

const isCollapse = ref(false)

const activeMenu = computed(() => {
  return route.path
})

const goToLogin = () => {
  router.push('/login')
}

const openMailbox = () => {
  chatStore.openChat(null, '')
}

const handleCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
    ElMessage.success('已安全退出登录')
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/user-center')
  }
}
</script>

<style scoped>
.app-layout {
  min-height: 100vh;
}

.layout-aside {
  background-color: rgba(14, 17, 23, 0.95);
  border-right: 1px solid var(--border-color);
  transition: width 0.3s ease;
  display: flex;
  flex-direction: column;
}

.logo-area {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 12px;
  border-bottom: 1px solid var(--border-color);
}

.logo-text {
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 18px;
  letter-spacing: 0.05em;
  white-space: nowrap;
}

.layout-menu {
  flex: 1;
  margin-top: 16px;
}

.layout-header {
  height: 60px;
  background-color: rgba(10, 13, 19, 0.8);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.toggle-btn {
  color: var(--text-main) !important;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.mailbox-btn {
  color: #a5b4fc !important;
  font-weight: 600;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.mailbox-btn:hover {
  color: #fff !important;
  text-shadow: 0 0 8px rgba(165, 180, 252, 0.4);
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s;
}

.user-profile:hover {
  background: rgba(255, 255, 255, 0.03);
}

.user-name {
  color: var(--text-main);
  font-size: 14px;
  font-weight: 500;
}

.layout-main {
  background-color: var(--bg-main);
  padding: 24px;
}

/* 渐变过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
