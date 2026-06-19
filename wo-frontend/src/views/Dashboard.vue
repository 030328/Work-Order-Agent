<template>
  <div class="dashboard-page">
    <div class="metric-grid">
      <div class="metric-panel">
        <span>当前结果</span>
        <strong>{{ total }}</strong>
        <p>符合筛选条件的工单</p>
      </div>
      <div class="metric-panel">
        <span>待处理</span>
        <strong>{{ statusCount.OPEN }}</strong>
        <p>等待 AI 或人工介入</p>
      </div>
      <div class="metric-panel">
        <span>AI已处理</span>
        <strong>{{ statusCount.AI_SOLVED }}</strong>
        <p>等待创建人确认</p>
      </div>
      <div class="metric-panel">
        <span>处理中</span>
        <strong>{{ statusCount.IN_PROGRESS }}</strong>
        <p>已被处理人认领</p>
      </div>
      <div class="metric-panel">
        <span>已转人工</span>
        <strong>{{ statusCount.ESCALATED }}</strong>
        <p>AI 或 SLA 触发升级</p>
      </div>
    </div>

    <section class="toolbar">
      <div class="filters">
        <el-segmented v-model="scope" :options="scopeOptions" @change="handleFilterChange" />
        <el-input v-model="keyword" placeholder="搜索标题或关键词" class="keyword-input" @keyup.enter="handleFilterChange" />
        <el-select v-model="status" placeholder="状态" clearable class="filter-select" @change="handleFilterChange">
          <el-option label="待处理" value="OPEN" />
          <el-option label="AI已处理" value="AI_SOLVED" />
          <el-option label="已转人工" value="ESCALATED" />
          <el-option label="处理中" value="IN_PROGRESS" />
          <el-option label="已解决" value="RESOLVED" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
        <el-button type="primary" @click="handleFilterChange">查询</el-button>
      </div>
      <el-button type="success" @click="$router.push('/workorder/create')">创建工单</el-button>
    </section>

    <section class="table-panel">
      <el-table v-if="workOrders.length" :data="workOrders" stripe class="workorder-table">
        <el-table-column prop="orderNo" label="工单编号" width="180" />
        <el-table-column label="标题">
          <template #default="{ row }">
            <div class="title-cell">
              <strong>{{ row.title }}</strong>
              <span>{{ row.category || '未分类' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="110">
          <template #default="{ row }">
            <el-tag :type="getPriorityType(row.priority)" size="small">
              {{ getPriorityLabel(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assigneeName" label="处理人" width="120">
          <template #default="{ row }">{{ row.assigneeName || '未分配' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/workorder/${row.id}`)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-else :description="emptyText" />

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadData"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getWorkOrders, getWorkOrderStatusStats } from '../api'

const workOrders = ref([])
const keyword = ref('')
const status = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const stats = ref({})

const currentUserId = localStorage.getItem('userId')
const currentRole = localStorage.getItem('role') || 'USER'
const defaultScope = currentRole === 'AGENT' ? 'assigned' : ['ADMIN', 'MANAGER'].includes(currentRole) ? 'all' : 'created'
const scope = ref(defaultScope)

const scopeOptions = [
  { label: '全部工单', value: 'all' },
  { label: '我创建的', value: 'created' },
  { label: '我处理的', value: 'assigned' }
]

const buildQuery = () => {
  return {
    ...buildStatsQuery(),
    page: page.value,
    size: size.value,
    status: status.value
  }
}

const buildStatsQuery = () => {
  const query = {
    keyword: keyword.value
  }

  if (scope.value === 'created' && currentUserId) {
    query.creatorId = currentUserId
  }
  if (scope.value === 'assigned' && currentUserId) {
    query.assigneeId = currentUserId
  }

  return query
}

const loadData = async () => {
  const [listRes, statsRes] = await Promise.all([
    getWorkOrders(buildQuery()),
    getWorkOrderStatusStats(buildStatsQuery())
  ])

  if (listRes.code === 0) {
    workOrders.value = listRes.data.records || []
    total.value = listRes.data.total || 0
  }
  if (statsRes.code === 0) {
    stats.value = statsRes.data || {}
  }
}

const handleFilterChange = () => {
  page.value = 1
  loadData()
}

const emptyText = computed(() => {
  const map = {
    all: '暂无匹配工单',
    created: '暂无我创建的工单',
    assigned: '暂无我处理的工单'
  }
  return map[scope.value] || '暂无匹配工单'
})

const statusCount = computed(() => {
  return {
    OPEN: stats.value.OPEN || 0,
    AI_SOLVED: stats.value.AI_SOLVED || 0,
    IN_PROGRESS: stats.value.IN_PROGRESS || 0,
    ESCALATED: stats.value.ESCALATED || 0
  }
})

const getStatusType = (status) => {
  const map = { OPEN: 'info', AI_SOLVED: 'success', ESCALATED: 'warning', IN_PROGRESS: 'primary', RESOLVED: 'success', CLOSED: '' }
  return map[status] || 'info'
}

const getStatusLabel = (status) => {
  const map = { OPEN: '待处理', AI_ANALYZING: 'AI分析中', AI_SOLVED: 'AI已处理', ESCALATED: '已转人工', IN_PROGRESS: '处理中', RESOLVED: '已解决', CLOSED: '已关闭' }
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

onMounted(loadData)
</script>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
}

.metric-panel,
.toolbar,
.table-panel {
  background: #fff;
  border: 1px solid #e7eaf0;
  border-radius: 8px;
}

.metric-panel {
  padding: 18px;
}

.metric-panel span {
  color: #6b7280;
  font-size: 13px;
}

.metric-panel strong {
  display: block;
  margin-top: 8px;
  color: #111827;
  font-size: 28px;
  line-height: 1;
}

.metric-panel p {
  margin: 8px 0 0;
  color: #8a94a6;
  font-size: 12px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  gap: 12px;
}

.filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.keyword-input {
  width: 260px;
}

.filter-select {
  width: 150px;
}

.table-panel {
  padding: 8px 0 14px;
}

.workorder-table {
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

@media (max-width: 960px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .keyword-input {
    width: 100%;
  }
}
</style>
