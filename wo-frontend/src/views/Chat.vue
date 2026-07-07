<template>
  <div class="chat-page">
    <section class="chat-panel">
      <div class="chat-header">
        <div>
          <h3>AI 工单助手</h3>
          <p>会话 {{ shortSessionId }}，支持查询工单、检索知识库和辅助创建工单</p>
        </div>
        <div class="header-actions">
          <el-tag type="success">在线</el-tag>
          <el-button size="small" @click="loadHistory">加载历史</el-button>
          <el-button size="small" type="warning" plain @click="handleClearSession">清空会话</el-button>
        </div>
      </div>

      <div class="chat-messages" ref="messagesRef">
        <div v-for="(msg, i) in messages" :key="i" :class="['message', msg.role]">
          <div class="avatar">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
          <div class="bubble">{{ msg.content }}</div>
        </div>
        <div v-if="loading" class="message assistant">
          <div class="avatar">AI</div>
          <div class="bubble">正在分析上下文...</div>
        </div>
      </div>

      <div class="composer">
        <el-input v-model="input" placeholder="输入你的问题，例如：帮我查询待处理工单" @keyup.enter="sendMessage" :disabled="loading" />
        <el-button type="primary" @click="sendMessage" :loading="loading">发送</el-button>
      </div>
    </section>

    <aside class="quick-panel">
      <div class="panel-title">快捷指令</div>
      <div class="quick-actions">
        <el-button @click="quickAction('帮我查询所有待处理的工单')">查询待处理工单</el-button>
        <el-button @click="quickAction('搜索关于登录问题的知识库')">搜索登录问题</el-button>
        <el-button @click="quickAction('查看最近创建的工单')">查看最近工单</el-button>
        <el-button @click="quickAction('帮我创建一个高优先级的登录故障工单')">创建故障工单</el-button>
      </div>

      <div class="panel-title secondary">会话能力</div>
      <ul class="capability-list">
        <li>按 sessionId 保留最近 20 条上下文</li>
        <li>可重新加载后端会话历史</li>
        <li>可清空当前会话并开始新对话</li>
        <li>支持自然语言触发工单工具</li>
      </ul>
    </aside>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { chat, clearChatSession, getChatHistory } from '../api'

const SESSION_KEY = 'wo-chat-session-id'
const welcomeMessage = { role: 'assistant', content: '你好，我是 AI 工单助手。你可以让我查询工单、检索知识库，或帮你整理一条新工单。' }

const input = ref('')
const loading = ref(false)
const messages = ref([welcomeMessage])
const messagesRef = ref(null)
const sessionId = ref(loadOrCreateSessionId())
const shortSessionId = computed(() => sessionId.value.slice(-10))

function loadOrCreateSessionId() {
  const existing = localStorage.getItem(SESSION_KEY)
  if (existing) return existing
  const next = typeof crypto !== 'undefined' && crypto.randomUUID
    ? crypto.randomUUID()
    : `web-session-${Date.now()}-${Math.random().toString(16).slice(2)}`
  localStorage.setItem(SESSION_KEY, next)
  return next
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

const normalizeHistoryMessage = (item) => ({
  role: item.role === 'user' ? 'user' : 'assistant',
  content: item.content || ''
})

const loadHistory = async () => {
  try {
    const res = await getChatHistory(sessionId.value)
    if (res.code === 0 && Array.isArray(res.data) && res.data.length > 0) {
      messages.value = res.data.map(normalizeHistoryMessage)
    } else {
      messages.value = [welcomeMessage]
    }
    scrollToBottom()
  } catch (e) {
    messages.value = [welcomeMessage]
  }
}

const sendMessage = async () => {
  if (!input.value.trim() || loading.value) return

  const userMsg = input.value.trim()
  messages.value.push({ role: 'user', content: userMsg })
  input.value = ''
  scrollToBottom()

  loading.value = true
  try {
    const res = await chat({ message: userMsg, sessionId: sessionId.value })
    if (res.code === 0) {
      if (res.data.sessionId && res.data.sessionId !== sessionId.value) {
        sessionId.value = res.data.sessionId
        localStorage.setItem(SESSION_KEY, res.data.sessionId)
      }
      messages.value.push({ role: 'assistant', content: res.data.content })
    } else {
      messages.value.push({ role: 'assistant', content: `抱歉，处理失败：${res.message}` })
    }
  } catch (e) {
    messages.value.push({ role: 'assistant', content: '网络错误，请稍后重试' })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

const handleClearSession = async () => {
  await ElMessageBox.confirm('确认清空当前 AI 会话历史？', '清空会话', { type: 'warning' })
  const res = await clearChatSession(sessionId.value)
  if (res.code === 0) {
    messages.value = [welcomeMessage]
    ElMessage.success('会话已清空')
  } else {
    ElMessage.error(res.message || '清空失败')
  }
}

const quickAction = (msg) => {
  input.value = msg
  sendMessage()
}

onMounted(loadHistory)
</script>

<style scoped>
.chat-page {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 16px;
  min-height: calc(100vh - 120px);
}

.chat-panel,
.quick-panel {
  background: #fff;
  border: 1px solid #e7eaf0;
  border-radius: 8px;
}

.chat-panel {
  display: flex;
  flex-direction: column;
  min-height: 620px;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 20px;
  border-bottom: 1px solid #edf0f5;
}

.chat-header h3 {
  margin: 0;
  color: #1f2937;
}

.chat-header p {
  margin: 4px 0 0;
  color: #7b8794;
  font-size: 13px;
}

.header-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f8fafc;
}

.message {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 14px;
}

.message.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 50%;
  background: #dbeafe;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.message.user .avatar {
  background: #e0f2fe;
  color: #0369a1;
}

.bubble {
  max-width: 72%;
  padding: 11px 14px;
  border-radius: 8px;
  background: #fff;
  color: #303133;
  line-height: 1.7;
  word-break: break-word;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
}

.message.user .bubble {
  background: #409eff;
  color: #fff;
}

.composer {
  display: flex;
  gap: 10px;
  padding: 16px;
  border-top: 1px solid #edf0f5;
}

.quick-panel {
  padding: 18px;
}

.panel-title {
  color: #1f2937;
  font-weight: 700;
}

.secondary {
  margin-top: 24px;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 12px;
}

.quick-actions :deep(.el-button) {
  justify-content: flex-start;
  margin-left: 0;
}

.capability-list {
  margin: 12px 0 0;
  padding-left: 18px;
  color: #4b5563;
  line-height: 1.9;
}

@media (max-width: 980px) {
  .chat-page {
    grid-template-columns: 1fr;
  }

  .chat-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .header-actions {
    justify-content: flex-start;
  }
}
</style>
