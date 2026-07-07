<template>
  <el-container class="app-shell">
    <el-aside width="232px" class="sidebar">
      <div class="brand">
        <div class="brand-mark">WO</div>
        <div>
          <h3>AI 工单系统</h3>
          <p>Agent Workbench</p>
        </div>
      </div>

      <el-menu
        :default-active="$route.path"
        router
        class="side-menu"
        background-color="#243044"
        text-color="#c8d3e2"
        active-text-color="#ffffff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><List /></el-icon>
          <span>工单列表</span>
        </el-menu-item>
        <el-menu-item index="/workorder/create">
          <el-icon><Plus /></el-icon>
          <span>创建工单</span>
        </el-menu-item>
        <el-menu-item v-if="canHandleDepartmentPool" index="/department-pool">
          <el-icon><List /></el-icon>
          <span>部门工单池</span>
        </el-menu-item>
        <el-menu-item index="/chat">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI 助手</span>
        </el-menu-item>
        <el-menu-item v-if="canManageUsers" index="/ai/analyze">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI 分析</span>
        </el-menu-item>
        <el-menu-item index="/knowledge">
          <el-icon><Document /></el-icon>
          <span>知识库</span>
        </el-menu-item>
        <el-menu-item v-if="canManageUsers" index="/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div>
          <h2>{{ pageTitle }}</h2>
          <p>{{ pageSubtitle }}</p>
        </div>
        <div class="user-area">
          <div class="user-meta">
            <strong>{{ displayName }}</strong>
            <span>{{ roleLabel }}</span>
          </div>
          <el-button text @click="logout">退出</el-button>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChatDotRound, Document, List, Plus, User } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const displayName = computed(() => localStorage.getItem('realName') || localStorage.getItem('username') || '用户')
const roleLabel = computed(() => {
  const map = { ADMIN: '管理员', MANAGER: '部门经理', AGENT: '处理人员', USER: '普通用户' }
  return map[localStorage.getItem('role')] || '普通用户'
})
const canManageUsers = computed(() => ['ADMIN', 'MANAGER', 'SYSTEM'].includes(localStorage.getItem('role')))
const canHandleDepartmentPool = computed(() => ['ADMIN', 'MANAGER', 'AGENT', 'SYSTEM'].includes(localStorage.getItem('role')))

const pageMeta = {
  Dashboard: ['工单工作台', '集中查看、筛选和跟进工单'],
  CreateWorkOrder: ['创建工单', '录入问题并触发 AI 分析和 SLA 分配'],
  DepartmentPool: ['部门工单池', '查看本部门待认领或待分配的人工工单'],
  WorkOrderDetail: ['工单详情', '查看上下文、AI 建议和处理动作'],
  Chat: ['AI 助手', '通过自然语言查询工单和知识库'],
  AiAnalyze: ['AI 分析', '独立验证 AI 工单分析能力和建议结果'],
  Knowledge: ['知识库', '维护 RAG 检索使用的业务知识'],
  UserManagement: ['用户管理', '维护账号、角色、部门和处理人池']
}

const currentMeta = computed(() => pageMeta[route.name] || ['工单系统', ''])
const pageTitle = computed(() => currentMeta.value[0])
const pageSubtitle = computed(() => currentMeta.value[1])

const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userId')
  localStorage.removeItem('username')
  localStorage.removeItem('realName')
  localStorage.removeItem('role')
  router.push('/login')
}
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: #f4f6f9;
}

.sidebar {
  background: #243044;
  box-shadow: 1px 0 0 rgba(15, 23, 42, 0.08);
}

.brand {
  height: 72px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-mark {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #409eff;
  font-weight: 700;
}

.brand h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}

.brand p {
  margin: 4px 0 0;
  color: #9fb0c6;
  font-size: 12px;
}

.side-menu {
  border-right: 0;
  padding: 10px 8px;
}

.side-menu :deep(.el-menu-item) {
  height: 44px;
  margin: 4px 0;
  border-radius: 6px;
}

.side-menu :deep(.el-menu-item.is-active) {
  background: #409eff;
}

.topbar {
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e7eaf0;
  padding: 0 28px;
}

.topbar h2 {
  margin: 0;
  color: #1f2937;
  font-size: 20px;
}

.topbar p {
  margin: 4px 0 0;
  color: #7b8794;
  font-size: 13px;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #4b5563;
}

.user-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.user-meta strong {
  color: #1f2937;
  font-size: 14px;
}

.user-meta span {
  color: #8a94a6;
  font-size: 12px;
}

.main-content {
  padding: 24px;
}
</style>
