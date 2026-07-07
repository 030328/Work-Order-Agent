<template>
  <div class="users-page">
    <section class="toolbar">
      <div class="filters">
        <el-input v-model="filters.username" clearable placeholder="用户名精确查询" class="username-input" @keyup.enter="handleFilter" />
        <el-select v-model="filters.role" clearable placeholder="角色" class="filter-select" @change="handleFilter">
          <el-option label="管理员" value="ADMIN" />
          <el-option label="部门经理" value="MANAGER" />
          <el-option label="处理人" value="AGENT" />
          <el-option label="提交人" value="USER" />
        </el-select>
        <el-select v-model="filters.department" clearable filterable allow-create placeholder="部门" class="filter-select" @change="handleFilter">
          <el-option v-for="item in departments" :key="item" :label="item" :value="item" />
        </el-select>
        <el-button type="primary" @click="loadUsers">查询</el-button>
      </div>
      <el-button @click="resetFilters">重置</el-button>
    </section>

    <section class="table-panel">
      <el-table :data="users" stripe class="user-table" v-loading="loading">
        <el-table-column prop="username" label="用户名" min-width="130" />
        <el-table-column prop="realName" label="姓名" min-width="120" />
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="getRoleType(row.role)" size="small">{{ getRoleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="department" label="部门" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="String(row.status) === '1' ? 'success' : 'info'" size="small">
              {{ String(row.status) === '1' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="!canEditUsers" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" :disabled="!canEditUsers || isSelf(row)" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadUsers"
        />
      </div>
    </section>

    <el-dialog v-model="editVisible" title="编辑用户" width="560px">
      <el-form :model="editForm" label-width="86px">
        <el-form-item label="用户名">
          <el-input v-model="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="editForm.realName" maxlength="50" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.role" class="full-control">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="部门经理" value="MANAGER" />
            <el-option label="处理人" value="AGENT" />
            <el-option label="提交人" value="USER" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="editForm.department" filterable allow-create class="full-control">
            <el-option v-for="item in departments" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" maxlength="100" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" maxlength="11" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="editForm.password" type="password" show-password placeholder="留空表示不修改" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteUser, getUserByUsername, getUsers, updateUser } from '../api'

const loading = ref(false)
const saving = ref(false)
const users = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const filters = ref({ username: '', role: '', department: '' })
const editVisible = ref(false)
const editForm = ref({})
const currentUserId = localStorage.getItem('userId')
const currentRole = localStorage.getItem('role') || 'USER'
const canEditUsers = computed(() => currentRole === 'ADMIN')

const departments = computed(() => {
  const set = new Set(['技术部', '客服部', '运维部', '产品部'])
  users.value.forEach(user => {
    if (user.department) set.add(user.department)
  })
  return Array.from(set)
})

const loadUsers = async () => {
  loading.value = true
  try {
    if (filters.value.username.trim()) {
      const res = await getUserByUsername(filters.value.username.trim())
      if (res.code === 0 && res.data) {
        const matched = [res.data].filter(user => {
          const roleMatched = !filters.value.role || user.role === filters.value.role
          const departmentMatched = !filters.value.department || user.department === filters.value.department
          return roleMatched && departmentMatched
        })
        users.value = matched
        total.value = matched.length
      } else {
        users.value = []
        total.value = 0
        ElMessage.error(res.message || '用户不存在')
      }
      return
    }

    const res = await getUsers({
      page: page.value,
      size: size.value,
      role: filters.value.role || undefined,
      department: filters.value.department || undefined
    })
    if (res.code === 0) {
      users.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      ElMessage.error(res.message || '用户加载失败')
    }
  } finally {
    loading.value = false
  }
}

const handleFilter = () => {
  page.value = 1
  loadUsers()
}

const resetFilters = () => {
  filters.value = { username: '', role: '', department: '' }
  handleFilter()
}

const openEdit = (row) => {
  editForm.value = {
    id: row.id,
    username: row.username,
    password: '',
    realName: row.realName,
    role: row.role,
    department: row.department,
    email: row.email,
    phone: row.phone
  }
  editVisible.value = true
}

const handleSave = async () => {
  saving.value = true
  try {
    const payload = { ...editForm.value }
    if (!payload.password) {
      delete payload.password
    }
    const res = await updateUser(payload.id, payload)
    if (res.code === 0) {
      ElMessage.success('用户已更新')
      editVisible.value = false
      loadUsers()
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } finally {
    saving.value = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除用户 ${row.realName || row.username}？`, '删除确认', { type: 'warning' })
  const res = await deleteUser(row.id)
  if (res.code === 0) {
    ElMessage.success('用户已删除')
    loadUsers()
  } else {
    ElMessage.error(res.message || '删除失败')
  }
}

const isSelf = (row) => String(row.id) === String(currentUserId)

const formatTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

const getRoleLabel = (role) => {
  const map = { ADMIN: '管理员', MANAGER: '部门经理', AGENT: '处理人', USER: '提交人', SYSTEM: '系统' }
  return map[role] || role
}

const getRoleType = (role) => {
  const map = { ADMIN: 'danger', MANAGER: 'warning', AGENT: 'primary', USER: 'info', SYSTEM: 'success' }
  return map[role] || 'info'
}

onMounted(loadUsers)
</script>

<style scoped>
.users-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar,
.table-panel {
  background: #fff;
  border: 1px solid #e7eaf0;
  border-radius: 8px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
}

.filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-select {
  width: 160px;
}

.username-input {
  width: 190px;
}

.table-panel {
  padding: 8px 0 14px;
}

.user-table {
  width: 100%;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  padding: 14px 16px 0;
}

.full-control {
  width: 100%;
}

@media (max-width: 860px) {
  .toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .filter-select {
    width: 100%;
  }

  .username-input {
    width: 100%;
  }
}
</style>
