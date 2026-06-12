<template>
  <div class="chatbot-wrapper">
    <!-- 悬浮聊天球 -->
    <div v-show="!isOpen" class="chat-ball glass-card" @click="toggleChat">
      <el-badge is-dot class="chat-badge">
        <el-icon size="26" color="#6366f1"><Service /></el-icon>
      </el-badge>
    </div>

    <!-- 聊天窗口面板 -->
    <transition name="slide">
      <div v-show="isOpen" class="chat-panel glass-card">
        <div class="chat-header">
          <div class="header-info">
            <el-avatar :size="28" src="https://cube.elemecdn.com/3/7c/3ea6beec983693f4585d56d5672a8png.png" />
            <span class="bot-name text-gradient">智能客服 (RAG)</span>
          </div>
          <el-button type="text" icon="Close" @click="toggleChat" class="close-btn" />
        </div>

        <!-- 聊天记录区域 -->
        <div class="chat-messages" ref="msgContainer">
          <div 
            v-for="(msg, index) in messages" 
            :key="index" 
            class="message-row"
            :class="msg.role === 'user' ? 'row-user' : 'row-bot'"
          >
            <div class="message-bubble" :class="msg.role === 'user' ? 'bubble-user' : 'bubble-bot'">
              {{ msg.content }}
            </div>
          </div>
          <div v-if="loading" class="message-row row-bot">
            <div class="message-bubble bubble-bot typing-indicator">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>

        <!-- 输入发送区域 -->
        <div class="chat-input-area">
          <el-input
            v-model="inputMsg"
            placeholder="提问系统规则/帮助..."
            @keyup.enter="handleSend"
            class="chat-input"
          >
            <template #append>
              <el-button icon="Promotion" @click="handleSend" :disabled="!inputMsg.trim()" />
            </template>
          </el-input>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const isOpen = ref(false)
const loading = ref(false)
const inputMsg = ref('')
const msgContainer = ref(null)

const messages = reactive([
  {
    role: 'bot',
    content: '您好！我是闲置交易系统的 AI 客服小助手。我可以帮您解答有关“系统抢购公平性、秒杀未支付订单时效、拍卖出价与结标交易发货”等问题。请随时提问！'
  }
])

const toggleChat = () => {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    scrollToBottom()
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (msgContainer.value) {
      msgContainer.value.scrollTop = msgContainer.value.scrollHeight
    }
  })
}

const handleSend = async () => {
  const text = inputMsg.value.trim()
  if (!text) return

  messages.push({ role: 'user', content: text })
  inputMsg.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const response = await axios.post('http://localhost:8080/api/chatbot/ask', { question: text })
    if (response.data.code === 200) {
      messages.push({ role: 'bot', content: response.data.data })
    } else {
      messages.push({ role: 'bot', content: '抱歉，小助手暂时开小差了，请稍后重试。' })
    }
  } catch (err) {
    messages.push({ role: 'bot', content: '连接客服失败，请检查后端服务是否开启。' })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}
</script>

<style scoped>
.chatbot-wrapper {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 2000;
}

.chat-ball {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  padding: 0;
  box-shadow: 0 4px 20px rgba(99, 102, 241, 0.4);
}

.chat-ball:hover {
  transform: scale(1.1) rotate(5deg);
}

.chat-panel {
  width: 340px;
  height: 460px;
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.6);
}

.chat-header {
  height: 50px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 16px;
  border-bottom: 1px solid var(--border-color);
}

.header-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.bot-name {
  font-size: 14px;
  font-weight: 600;
}

.close-btn {
  color: var(--text-muted) !important;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: rgba(0, 0, 0, 0.1);
}

.message-row {
  display: flex;
  width: 100%;
}

.row-user {
  justify-content: flex-end;
}

.row-bot {
  justify-content: flex-start;
}

.message-bubble {
  max-width: 80%;
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 13px;
  line-height: 1.4;
  word-break: break-all;
}

.bubble-user {
  background-color: var(--color-primary);
  color: #fff;
  border-bottom-right-radius: 2px;
}

.bubble-bot {
  background-color: rgba(255, 255, 255, 0.05);
  color: var(--text-main);
  border-bottom-left-radius: 2px;
  border: 1px solid var(--border-color);
}

.chat-input-area {
  padding: 12px;
  border-top: 1px solid var(--border-color);
}

.chat-input :deep(.el-input-group__append) {
  background-color: transparent !important;
  border-color: var(--border-color) !important;
}

/* 正在输入指示器 */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 12px 16px;
  align-items: center;
}

.typing-indicator span {
  width: 6px;
  height: 6px;
  background-color: var(--text-muted);
  border-radius: 50%;
  display: inline-block;
  animation: bounce 1.4s infinite ease-in-out both;
}

.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1.0); }
}

/* 划入过渡动画 */
.slide-enter-active, .slide-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.slide-enter-from, .slide-leave-to {
  transform: translateY(30px) scale(0.9);
  opacity: 0;
}
</style>
