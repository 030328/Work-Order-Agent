<template>
  <div class="chat-page">
    <section class="chat-panel">
      <div class="chat-header">
        <div>
          <h3>AI 工单助手</h3>
          <p>可查询工单、检索知识库，也可以协助创建工单</p>
        </div>
        <el-tag type="success">在线</el-tag>
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

      <div class="panel-title secondary">能力范围</div>
      <ul class="capability-list">
        <li>自然语言查询工单</li>
        <li>调用知识库 RAG 检索</li>
        <li>辅助生成工单内容</li>
        <li>触发工单状态操作</li>
      </ul>
    </aside>
  </div>
</template>

<script setup>
import { nextTick, ref } from 'vue'
import { chat } from '../api'

const input = ref('')
const loading = ref(false)
const messages = ref([
  { role: 'assistant', content: '你好，我是 AI 工单助手。你可以让我查询工单、检索知识库，或帮你整理一条新工单。' }
])
const messagesRef = ref(null)

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

const sendMessage = async () => {
  if (!input.value.trim() || loading.value) return

  const userMsg = input.value.trim()
  messages.value.push({ role: 'user', content: userMsg })
  input.value = ''
  scrollToBottom()

  loading.value = true
  try {
    const res = await chat({ message: userMsg, sessionId: 'web-session-1' })
    if (res.code === 0) {
      messages.value.push({ role: 'assistant', content: res.data.content })
    } else {
      messages.value.push({ role: 'assistant', content: '抱歉，处理失败：' + res.message })
    }
  } catch (e) {
    messages.value.push({ role: 'assistant', content: '网络错误，请稍后重试' })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

const quickAction = (msg) => {
  input.value = msg
  sendMessage()
}
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
}
</style>
