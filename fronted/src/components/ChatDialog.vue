<template>
  <el-dialog
    v-model="chatStore.isChatOpen"
    title="即时私信对话"
    width="800px"
    class="global-chat-dialog"
    :before-close="handleClose"
    align-center
  >
    <div class="chat-layout">
      <!-- 左栏：最近联系人列表 -->
      <div class="contacts-sidebar">
        <div class="sidebar-header">最近对话</div>
        <div class="contacts-list" v-loading="loadingContacts">
          <div v-if="contacts.length === 0" class="empty-contacts">暂无联系人</div>
          <div
            v-for="c in contacts"
            :key="c.userId"
            class="contact-item"
            :class="{ active: chatStore.activeContactId === c.userId }"
            @click="selectContact(c)"
          >
            <el-avatar :size="40" :src="c.avatar || defaultAvatar" />
            <div class="contact-info">
              <div class="contact-name-row">
                <span class="contact-name">{{ c.nickname || c.username }}</span>
                <span class="msg-time" v-if="c.lastMessageTime">{{ formatTime(c.lastMessageTime) }}</span>
              </div>
              <div class="contact-msg-row">
                <span class="latest-msg">{{ c.lastMessage }}</span>
                <el-badge v-if="c.unreadCount > 0" :value="c.unreadCount" class="msg-badge" />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右栏：对话窗口 -->
      <div class="chat-main">
        <template v-if="chatStore.activeContactId">
          <!-- 头部展示对方名称 -->
          <div class="chat-main-header">
            <span class="active-contact-title">与 {{ chatStore.activeContactName }} 对话中</span>
          </div>

          <!-- 消息流列表 -->
          <div class="messages-container" ref="messagesRef" v-loading="loadingHistory">
            <div v-if="messages.length === 0" class="empty-messages">
              你们还没有聊天记录，打个招呼吧！
            </div>
            
            <div
              v-for="msg in messages"
              :key="msg.id"
              class="message-wrapper"
              :class="{ 'message-mine': msg.senderId === Number(userStore.userId) }"
            >
              <div class="message-bubble">
                <div class="message-content">{{ msg.content }}</div>
                <div class="message-time">{{ formatTime(msg.createTime) }}</div>
              </div>
            </div>
          </div>

          <!-- 输入区 -->
          <div class="message-input-area">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="3"
              placeholder="请输入您要发送的信息（按下 Ctrl + Enter 发送）..."
              @keydown.ctrl.enter="sendMsg"
              resize="none"
            />
            <div class="input-footer">
              <span class="input-tips">Ctrl + Enter 快速发送</span>
              <el-button type="primary" size="small" @click="sendMsg" :loading="sending">
                发 送
              </el-button>
            </div>
          </div>
        </template>
        <template v-else>
          <div class="chat-empty-state">
            <el-icon size="64" color="var(--text-dim)"><ChatLineSquare /></el-icon>
            <p>选择左侧联系人，或在商品详情页点击“联系卖家”发起会话</p>
          </div>
        </template>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useUserStore } from '../store/user'
import { useChatStore } from '../store/chat'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const userStore = useUserStore()
const chatStore = useChatStore()

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const contacts = ref([])
const messages = ref([])
const inputMessage = ref('')

const loadingContacts = ref(false)
const loadingHistory = ref(false)
const sending = ref(false)

const messagesRef = ref(null)

let pollTimer = null

const handleClose = () => {
  chatStore.closeChat()
}

// 获取联系人列表
const fetchContacts = async () => {
  if (!userStore.isLoggedIn) return
  try {
    const headers = { Authorization: `Bearer ${userStore.token}` }
    const response = await axios.get('/api/chat/contacts', { headers })
    if (response.data.code === 200) {
      contacts.value = response.data.data
    }
  } catch (error) {
    console.error('获取联系人失败:', error)
  }
}

// 获取聊天历史记录
const fetchHistory = async (contactId) => {
  if (!userStore.isLoggedIn || !contactId) return
  try {
    const headers = { Authorization: `Bearer ${userStore.token}` }
    const response = await axios.get(`/api/chat/history?receiverId=${contactId}`, { headers })
    if (response.data.code === 200) {
      const newMessages = response.data.data
      
      // 判断是否需要滚动置底（如果消息数增多，自动滚到底部）
      const shouldScroll = messages.value.length !== newMessages.length
      messages.value = newMessages
      
      if (shouldScroll) {
        scrollToBottom()
      }
    }
  } catch (error) {
    console.error('获取聊天历史失败:', error)
  }
}

// 标记消息为已读
const markAsRead = async (contactId) => {
  if (!userStore.isLoggedIn || !contactId) return
  try {
    const headers = { Authorization: `Bearer ${userStore.token}` }
    await axios.post(`/api/chat/read?senderId=${contactId}`, {}, { headers })
    fetchContacts() // 刷新联系人列表以重置未读红点
  } catch (error) {
    console.error('标记已读失败:', error)
  }
}

// 切换对话联系人
const selectContact = (contact) => {
  chatStore.activeContactId = contact.userId
  chatStore.activeContactName = contact.nickname || contact.username
  messages.value = []
  fetchHistory(contact.userId)
  markAsRead(contact.userId)
}

// 发送聊天消息
const sendMsg = async () => {
  const content = inputMessage.value.trim()
  if (!content) return
  if (!chatStore.activeContactId) return

  sending.value = true
  try {
    const headers = { Authorization: `Bearer ${userStore.token}` }
    const payload = {
      receiverId: chatStore.activeContactId,
      goodsId: chatStore.activeGoodsId,
      content: content
    }
    const response = await axios.post('/api/chat/send', payload, { headers })
    if (response.data.code === 200) {
      inputMessage.value = ''
      // 插入到当前消息流中，提升响应速度
      messages.value.push(response.data.data)
      scrollToBottom()
      fetchContacts() // 刷新侧边栏
    } else {
      ElMessage.error(response.data.message || '发送失败')
    }
  } catch (error) {
    ElMessage.error('发送消息异常')
  } finally {
    sending.value = false
  }
}

// 自动滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

// 时间格式化
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const pad = (num) => String(num).padStart(2, '0')
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`
}

// 监视对话窗口打开状态
watch(() => chatStore.isChatOpen, (isOpen) => {
  if (isOpen) {
    fetchContacts()
    if (chatStore.activeContactId) {
      fetchHistory(chatStore.activeContactId)
      markAsRead(chatStore.activeContactId)
    }
    // 开启轮询拉取历史消息（每 3 秒更新一次）
    pollTimer = setInterval(() => {
      if (chatStore.activeContactId) {
        fetchHistory(chatStore.activeContactId)
      }
      fetchContacts()
    }, 3000)
  } else {
    // 关闭轮询
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }
})

// 监视正在会话的联系人变化
watch(() => chatStore.activeContactId, (newId) => {
  if (newId) {
    fetchHistory(newId)
    markAsRead(newId)
  }
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.global-chat-dialog :deep(.el-dialog) {
  background: #0d1117 !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  border-radius: 20px !important;
  overflow: hidden;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.7);
}

.global-chat-dialog :deep(.el-dialog__body) {
  padding: 0 !important;
}

.chat-layout {
  display: flex;
  height: 560px;
  background-color: #0b0d13;
}

/* 左侧联系人栏 */
.contacts-sidebar {
  width: 260px;
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  flex-direction: column;
  background-color: rgba(20, 24, 33, 0.5);
}

.sidebar-header {
  padding: 16px;
  font-size: 14px;
  font-weight: 700;
  color: #a5b4fc;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  letter-spacing: 0.05em;
}

.contacts-list {
  flex: 1;
  overflow-y: auto;
}

.empty-contacts {
  text-align: center;
  color: var(--text-dim);
  font-size: 12px;
  margin-top: 40px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid rgba(255, 255, 255, 0.02);
  transition: all 0.2s ease;
}

.contact-item:hover {
  background-color: rgba(255, 255, 255, 0.03);
}

.contact-item.active {
  background-color: rgba(99, 102, 241, 0.12);
  border-left: 3px solid #6366f1;
}

.contact-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.contact-name-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.contact-name {
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.msg-time {
  font-size: 10px;
  color: var(--text-dim);
}

.contact-msg-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.latest-msg {
  font-size: 11px;
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.msg-badge :deep(.el-badge__content) {
  background-color: #ef4444 !important;
  border: none;
  height: 16px;
  padding: 0 4px;
  line-height: 16px;
}

/* 右侧会话主窗口 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #080a0f;
}

.chat-main-header {
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  background-color: rgba(10, 13, 19, 0.3);
}

.active-contact-title {
  font-size: 14px;
  font-weight: 700;
  color: #fff;
}

.messages-container {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.empty-messages {
  text-align: center;
  color: var(--text-dim);
  font-size: 12px;
  margin-top: 100px;
}

.message-wrapper {
  display: flex;
  width: 100%;
}

.message-bubble {
  max-width: 70%;
  background-color: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 12px 12px 12px 2px;
  padding: 10px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message-content {
  font-size: 13px;
  color: #f3f4f6;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
}

.message-time {
  font-size: 9px;
  color: var(--text-dim);
  align-self: flex-end;
}

/* 我的消息气泡 */
.message-mine {
  justify-content: flex-end;
}

.message-mine .message-bubble {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.2) 0%, rgba(79, 70, 229, 0.2) 100%);
  border-color: rgba(99, 102, 241, 0.3);
  border-radius: 12px 12px 2px 12px;
}

/* 输入区域 */
.message-input-area {
  padding: 16px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  background-color: rgba(10, 13, 19, 0.4);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-input-area :deep(.el-textarea__inner) {
  background-color: rgba(255, 255, 255, 0.01) !important;
  border: 1px solid rgba(255, 255, 255, 0.05) !important;
  border-radius: 8px !important;
  color: #fff !important;
}

.message-input-area :deep(.el-textarea__inner:focus) {
  border-color: rgba(99, 102, 241, 0.6) !important;
  box-shadow: 0 0 10px rgba(99, 102, 241, 0.15);
}

.input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.input-tips {
  font-size: 11px;
  color: var(--text-dim);
}

/* 空白欢迎态 */
.chat-empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 16px;
  color: var(--text-muted);
  text-align: center;
  padding: 40px;
}

.chat-empty-state p {
  font-size: 13px;
  max-width: 300px;
  line-height: 1.6;
}
</style>
