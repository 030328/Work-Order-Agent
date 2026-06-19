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
        <el-button type="primary" @click="handleSearch">搜索</el-button>
      </div>

      <el-table v-if="results.length" :data="results" stripe class="result-table">
        <el-table-column label="内容">
          <template #default="{ row }">
            <div class="content-cell">{{ row.content }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="sourceType" label="来源" width="130" />
        <el-table-column prop="score" label="相似度" width="110">
          <template #default="{ row }">
            <strong>{{ formatScore(row.score) }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="verified" label="验证状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.verified === 1 ? 'success' : 'info'" size="small">
              {{ row.verified === 1 ? '已验证' : 'AI生成' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="输入关键词后开始检索" />
    </section>

    <aside class="info-panel">
      <div class="panel-title">RAG 数据质量</div>
      <div class="quality-list">
        <div>
          <strong>已验证优先</strong>
          <span>人工确认过的内容会在召回后优先展示。</span>
        </div>
        <div>
          <strong>相似度排序</strong>
          <span>向量检索结果按相似度和点赞数综合排序。</span>
        </div>
        <div>
          <strong>来源可追溯</strong>
          <span>支持 FAQ、文档和历史工单三类来源。</span>
        </div>
      </div>
    </aside>

    <el-dialog v-model="showAdd" title="添加知识库文档" width="560px">
      <el-form :model="addForm" label-width="88px">
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
        <el-form-item label="已验证">
          <el-switch v-model="addForm.verified" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" @click="handleAdd">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { indexKnowledge, searchKnowledge } from '../api'

const searchQuery = ref('')
const results = ref([])
const showAdd = ref(false)
const addForm = ref({ content: '', sourceType: 'FAQ', verified: 0 })

const handleSearch = async () => {
  if (!searchQuery.value.trim()) return
  const res = await searchKnowledge({ query: searchQuery.value, topK: 5 })
  if (res.code === 0) {
    results.value = res.data || []
  } else {
    ElMessage.error(res.message)
  }
}

const handleAdd = async () => {
  if (!addForm.value.content.trim()) {
    ElMessage.warning('请输入文档内容')
    return
  }
  const res = await indexKnowledge(addForm.value)
  if (res.code === 0) {
    ElMessage.success('添加成功')
    showAdd.value = false
    addForm.value = { content: '', sourceType: 'FAQ', verified: 0 }
  } else {
    ElMessage.error(res.message)
  }
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
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.result-table {
  width: 100%;
}

.content-cell {
  max-width: 760px;
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

.full-control {
  width: 100%;
}

@media (max-width: 980px) {
  .knowledge-page {
    grid-template-columns: 1fr;
  }
}
</style>
