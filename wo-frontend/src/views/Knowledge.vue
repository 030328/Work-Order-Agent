<template>
  <div class="knowledge-page">
    <section class="search-panel">
      <div class="panel-header">
        <div>
          <h3>知识库检索</h3>
          <span>用于 AI Agent 分析工单时的 RAG 上下文</span>
        </div>
        <el-button type="primary" @click="showAdd = true">添加文档</el-button>
      </div>

      <div class="search-row">
        <el-input v-model="searchQuery" placeholder="输入问题、错误码或业务关键词" @keyup.enter="handleSearch" />
        <el-input-number v-model="topK" :min="1" :max="20" />
        <el-button type="primary" :loading="searching" @click="handleSearch">搜索</el-button>
      </div>

      <el-table v-if="results.length" :data="results" stripe class="result-table">
        <el-table-column label="文档" min-width="280">
          <template #default="{ row }">
            <div class="doc-cell">
              <strong>{{ row.title || buildFallbackTitle(row) }}</strong>
              <span>{{ row.content }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="sourceType" label="来源" width="130" />
        <el-table-column prop="sourceId" label="Source ID" width="130" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">{{ row.category || '-' }}</template>
        </el-table-column>
        <el-table-column prop="score" label="相似度" width="110">
          <template #default="{ row }">
            <strong>{{ formatScore(row.score) }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="verified" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.verified === 1 ? 'success' : 'info'" size="small">
              {{ row.verified === 1 ? '已审核' : '待审核' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="likeCount" label="点赞" width="90">
          <template #default="{ row }">{{ row.likeCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="!row.id || row.verified === 1" @click="handleVerify(row)">审核</el-button>
            <el-button link type="success" :disabled="!row.id" @click="handleLike(row)">点赞</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="输入关键词后开始检索" />
    </section>

    <aside class="info-panel">
      <div class="panel-title">RAG 数据质量</div>
      <div class="quality-list">
        <div>
          <strong>已审核优先</strong>
          <span>人工确认过的内容会在召回后优先展示。</span>
        </div>
        <div>
          <strong>相似度排序</strong>
          <span>向量检索结果按审核状态、相似度和点赞数综合排序。</span>
        </div>
        <div>
          <strong>来源可追溯</strong>
          <span>支持 FAQ、文档和历史工单三类来源。</span>
        </div>
      </div>
    </aside>

    <el-dialog v-model="showAdd" title="添加知识库文档" width="620px">
      <el-form :model="addForm" label-width="88px">
        <el-form-item label="标题">
          <el-input v-model="addForm.title" maxlength="120" placeholder="例如：登录失败排查手册" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="addForm.content" type="textarea" :rows="6" placeholder="输入可被 AI 检索引用的解决方案或 FAQ" />
        </el-form-item>
        <el-form-item label="来源类型">
          <el-select v-model="addForm.sourceType" class="full-control">
            <el-option label="FAQ" value="FAQ" />
            <el-option label="文档" value="DOCUMENTATION" />
            <el-option label="历史工单" value="HISTORICAL_WO" />
          </el-select>
        </el-form-item>
        <div class="form-row">
          <el-form-item label="Source ID">
            <el-input v-model="addForm.sourceId" maxlength="100" placeholder="可留空，系统自动生成" />
          </el-form-item>
          <el-form-item label="分类">
            <el-input v-model="addForm.category" maxlength="50" placeholder="例如：登录、支付、权限" />
          </el-form-item>
        </div>
        <el-form-item label="已审核">
          <el-switch v-model="addForm.verified" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" :loading="adding" @click="handleAdd">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { indexKnowledge, likeKnowledge, searchKnowledge, verifyKnowledge } from '../api'

const searchQuery = ref('')
const topK = ref(5)
const results = ref([])
const showAdd = ref(false)
const searching = ref(false)
const adding = ref(false)
const addForm = ref({
  title: '',
  content: '',
  sourceType: 'FAQ',
  sourceId: '',
  category: '',
  verified: 0,
  likeCount: 0
})

const handleSearch = async () => {
  if (!searchQuery.value.trim()) return
  searching.value = true
  try {
    const res = await searchKnowledge({ query: searchQuery.value, topK: topK.value })
    if (res.code === 0) {
      results.value = res.data || []
    } else {
      ElMessage.error(res.message)
    }
  } finally {
    searching.value = false
  }
}

const handleAdd = async () => {
  if (!addForm.value.content.trim()) {
    ElMessage.warning('请输入文档内容')
    return
  }
  adding.value = true
  try {
    const payload = { ...addForm.value }
    if (!payload.title) delete payload.title
    if (!payload.sourceId) delete payload.sourceId
    if (!payload.category) delete payload.category
    const res = await indexKnowledge(payload)
    if (res.code === 0) {
      ElMessage.success('添加成功')
      showAdd.value = false
      addForm.value = { title: '', content: '', sourceType: 'FAQ', sourceId: '', category: '', verified: 0, likeCount: 0 }
      if (searchQuery.value.trim()) {
        handleSearch()
      }
    } else {
      ElMessage.error(res.message)
    }
  } finally {
    adding.value = false
  }
}

const handleVerify = async (row) => {
  const res = await verifyKnowledge(row.id)
  if (res.code === 0) {
    row.verified = 1
    ElMessage.success('已标记为审核通过')
  } else {
    ElMessage.error(res.message || '审核失败')
  }
}

const handleLike = async (row) => {
  const res = await likeKnowledge(row.id)
  if (res.code === 0) {
    row.likeCount = (row.likeCount || 0) + 1
    ElMessage.success('点赞成功')
  } else {
    ElMessage.error(res.message || '点赞失败')
  }
}

const buildFallbackTitle = (row) => {
  return `${row.sourceType || 'UNKNOWN'}-${row.sourceId || row.id || 'N/A'}`
}

const formatScore = (score) => {
  if (score === undefined || score === null) return '-'
  return `${(score * 100).toFixed(1)}%`
}
</script>

<style scoped>
.knowledge-page {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 16px;
}

.search-panel,
.info-panel {
  background: #fff;
  border: 1px solid #e7eaf0;
  border-radius: 8px;
}

.search-panel {
  padding: 20px;
}

.info-panel {
  padding: 18px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.panel-header h3,
.panel-title {
  margin: 0;
  color: #1f2937;
  font-weight: 700;
}

.panel-header span {
  display: block;
  margin-top: 4px;
  color: #7b8794;
  font-size: 13px;
}

.search-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 120px 88px;
  gap: 10px;
  margin-bottom: 16px;
}

.result-table {
  width: 100%;
}

.doc-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.doc-cell strong {
  color: #1f2937;
}

.doc-cell span {
  color: #374151;
  line-height: 1.7;
}

.quality-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 14px;
}

.quality-list div {
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #edf0f5;
  border-radius: 8px;
}

.quality-list strong {
  display: block;
  color: #1f2937;
}

.quality-list span {
  display: block;
  margin-top: 6px;
  color: #6b7280;
  line-height: 1.6;
  font-size: 13px;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.full-control {
  width: 100%;
}

@media (max-width: 980px) {
  .knowledge-page,
  .search-row,
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
