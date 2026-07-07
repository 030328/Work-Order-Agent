<template>
  <div class="pool-page">
    <section class="toolbar">
      <div class="filters">
        <el-select
          v-model="department"
          filterable
          allow-create
          placeholder="部门"
          class="department-select"
          :disabled="!canSwitchDepartment"
          @change="handleDepartmentChange"
        >
          <el-option v-for="item in departments" :key="item" :label="item" :value="item" />
        </el-select>
        <el-button type="primary" :loading="loading" @click="loadPool">刷新</el-button>
      </div>
      <el-tag type="warning">待人工认领 {{ total }}</el-tag>
    </section>

    <section class="table-panel">
      <el-table :data="workOrders" stripe class="pool-table" v-loading="loading">
        <el-table-column prop="orderNo" label="工单编号" width="180" />
        <el-table-column label="标题" min-width="260">
          <template #default="{ row }">
            <div class="title-cell">
              <strong>{{ row.title }}</strong>
              <span>{{ row.category || '未分类' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="110">
          <template #default="{ row }">
            <el-tag :type="getPriorityType(row.priority)" size="small">{{ getPriorityLabel(row.priority) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag type="warning" size="small">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="creatorName" label="提交人" width="120">
          <template #default="{ row }">{{ row.creatorName || row.creatorId || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/workorder/${row.id}`)">详情</el-button>
            <el-button link type="success" @click="handleClaim(row)">认领</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && workOrders.length === 0" description="当前部门暂无待人工认领工单" />

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadPool"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { claimWorkOrder, getDepartmentWorkOrders, getUser, getUsers } from '../api'

const loading = ref(false)
const workOrders = ref([])
const departments = ref(['技术部', '客服部', '运维部', '产品部'])
const department = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const currentRole = localStorage.getItem('role') || 'USER'
const currentUserId = localStorage.getItem('userId')
const canSwitchDepartment = computed(() => ['ADMIN', 'MANAGER', 'SYSTEM'].includes(currentRole))

const initDepartment = async () => {
  if (currentUserId) {
    const profileRes = await getUser(currentUserId)
    if (profileRes.code === 0 && profileRes.data?.department) {
      department.value = profileRes.data.department
      addDepartment(profileRes.data.department)
    }
  }

  if (canSwitchDepartment.value) {
    try {
      const usersRes = await getUsers({ page: 1, size: 100 })
      if (usersRes.code === 0) {
        ;(usersRes.data.records || []).forEach(user => addDepartment(user.department))
      }
    } catch (e) {
      // Department switching still works with the manually typed value.
    }
  }

  if (!department.value) {
    department.value = departments.value[0]
  }
}

const addDepartment = (value) => {
  if (value && !departments.value.includes(value)) {
    departments.value.push(value)
  }
}

const loadPool = async () => {
  if (!department.value) return
  loading.value = true
  try {
    const res = await getDepartmentWorkOrders(department.value, {
      page: page.value,
      size: size.value
    })
    if (res.code === 0) {
      workOrders.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      workOrders.value = []
      total.value = 0
      ElMessage.error(res.message || '部门工单池加载失败')
    }
  } finally {
    loading.value = false
  }
}

const handleDepartmentChange = () => {
  addDepartment(department.value)
  page.value = 1
  loadPool()
}

const handleClaim = async (row) => {
  await ElMessageBox.confirm(`确认认领工单 ${row.orderNo}？`, '认领确认')
  const res = await claimWorkOrder(row.id)
  if (res.code === 0) {
    ElMessage.success('认领成功')
    loadPool()
  } else {
    ElMessage.error(res.message || '认领失败')
  }
}

const formatTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

const getStatusLabel = (status) => {
  const map = { ESCALATED: '待人工认领', IN_PROGRESS: '处理中', RESOLVED: '已解决', CLOSED: '已关闭' }
  return map[status] || status
}

const getPriorityType = (priority) => {
  const map = { URGENT: 'danger', HIGH: 'warning', MEDIUM: 'primary', LOW: 'info' }
  return map[priority] || 'info'
}

const getPriorityLabel = (priority) => {
  const map = { URGENT: '紧急', HIGH: '高', MEDIUM: '中', LOW: '低' }
  return map[priority] || priority
}

onMounted(async () => {
  await initDepartment()
  loadPool()
})
</script>

<style scoped>
.pool-page {
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
  gap: 10px;
}

.department-select {
  width: 220px;
}

.table-panel {
  padding: 8px 0 14px;
}

.pool-table {
  width: 100%;
}

.title-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.title-cell strong {
  color: #1f2937;
  font-weight: 600;
}

.title-cell span {
  color: #8a94a6;
  font-size: 12px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  padding: 14px 16px 0;
}

@media (max-width: 760px) {
  .toolbar,
  .filters {
    align-items: stretch;
    flex-direction: column;
  }

  .department-select {
    width: 100%;
  }
}
</style>
