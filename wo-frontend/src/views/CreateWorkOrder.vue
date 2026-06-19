<template>
  <div class="create-page">
    <section class="form-panel">
      <div class="panel-header">
        <h3>提交新工单</h3>
        <span>AI 会自动生成摘要、分类建议和处理方案</span>
      </div>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="88px" class="workorder-form">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="例如：登录页面白屏，控制台报错" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="7"
            placeholder="请描述现象、影响范围、复现步骤和已经尝试过的处理方式"
          />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="分类" prop="category">
            <el-select v-model="form.category" placeholder="选择分类" class="full-control">
              <el-option label="BUG" value="BUG" />
              <el-option label="需求" value="FEATURE" />
              <el-option label="咨询" value="QUESTION" />
              <el-option label="维护" value="MAINTENANCE" />
              <el-option label="故障" value="INCIDENT" />
            </el-select>
          </el-form-item>
          <el-form-item label="优先级" prop="priority">
            <el-select v-model="form.priority" placeholder="选择优先级" class="full-control">
              <el-option label="低" value="LOW" />
              <el-option label="中" value="MEDIUM" />
              <el-option label="高" value="HIGH" />
              <el-option label="紧急" value="URGENT" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">提交并分析</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <aside class="side-panel">
      <div class="assist-block">
        <h4>处理链路</h4>
        <ol>
          <li>创建工单并写入 SLA 截止时间</li>
          <li>检索相似历史工单和知识库</li>
          <li>AI 生成摘要、情绪和解决建议</li>
          <li>必要时转入人工处理队列</li>
        </ol>
      </div>

      <div class="assist-block">
        <h4>优先级建议</h4>
        <div class="priority-guide">
          <span><b class="dot urgent"></b>紧急：核心流程不可用</span>
          <span><b class="dot high"></b>高：影响多个用户</span>
          <span><b class="dot medium"></b>中：影响可控</span>
          <span><b class="dot low"></b>低：咨询或轻微问题</span>
        </div>
      </div>
    </aside>

    <section v-if="result" class="result-panel">
      <div class="panel-header">
        <h3>创建结果</h3>
        <el-tag :type="getStatusType(result.status)">{{ getStatusLabel(result.status) }}</el-tag>
      </div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="工单编号">{{ result.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="SLA 截止">{{ result.slaDeadline || '待分配' }}</el-descriptions-item>
        <el-descriptions-item label="AI 摘要" :span="2">{{ result.aiSummary || '等待 AI 分析结果' }}</el-descriptions-item>
        <el-descriptions-item label="AI 情绪">
          <el-tag :type="result.aiSentiment === 'NEGATIVE' ? 'danger' : result.aiSentiment === 'POSITIVE' ? 'success' : 'info'">
            {{ result.aiSentiment || 'NEUTRAL' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="建议分类">{{ result.aiCategorySuggestion || '-' }}</el-descriptions-item>
        <el-descriptions-item label="建议方案" :span="2">{{ result.aiSuggestedSolution || '暂无建议' }}</el-descriptions-item>
      </el-descriptions>
      <div class="result-actions">
        <el-button type="success" @click="$router.push(`/workorder/${result.id}`)">查看详情</el-button>
        <el-button @click="resetForm">继续创建</el-button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createWorkOrder } from '../api'

const initialForm = () => ({ title: '', description: '', category: 'QUESTION', priority: 'MEDIUM' })

const formRef = ref()
const loading = ref(false)
const result = ref(null)
const form = ref(initialForm())
const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  description: [{ required: true, message: '请输入描述', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }]
}

const handleSubmit = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await createWorkOrder(form.value)
    if (res.code === 0) {
      ElMessage.success('工单创建成功')
      result.value = res.data
    } else {
      ElMessage.error(res.message)
    }
  } catch (e) {
    ElMessage.error('创建失败')
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  form.value = initialForm()
  result.value = null
  formRef.value?.clearValidate()
}

const getStatusType = (status) => {
  const map = { OPEN: 'info', AI_SOLVED: 'success', ESCALATED: 'warning', IN_PROGRESS: 'primary', RESOLVED: 'success', CLOSED: '' }
  return map[status] || 'info'
}

const getStatusLabel = (status) => {
  const map = { OPEN: '待处理', AI_ANALYZING: 'AI分析中', AI_SOLVED: 'AI已解决', ESCALATED: '已转人工', IN_PROGRESS: '处理中', RESOLVED: '已解决', CLOSED: '已关闭' }
  return map[status] || status
}
</script>

<style scoped>
.create-page {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
}

.form-panel,
.side-panel,
.result-panel {
  background: #fff;
  border: 1px solid #e7eaf0;
  border-radius: 8px;
}

.form-panel,
.result-panel {
  padding: 20px;
}

.side-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 18px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.panel-header h3,
.assist-block h4 {
  margin: 0;
  color: #1f2937;
}

.panel-header span {
  color: #7b8794;
  font-size: 13px;
}

.workorder-form {
  max-width: 820px;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.full-control {
  width: 100%;
}

.assist-block {
  padding-bottom: 16px;
  border-bottom: 1px solid #edf0f5;
}

.assist-block:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.assist-block ol {
  margin: 12px 0 0;
  padding-left: 20px;
  color: #4b5563;
  line-height: 1.8;
}

.priority-guide {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 12px;
  color: #4b5563;
  font-size: 13px;
}

.dot {
  width: 8px;
  height: 8px;
  display: inline-block;
  margin-right: 8px;
  border-radius: 50%;
}

.urgent {
  background: #f56c6c;
}

.high {
  background: #e6a23c;
}

.medium {
  background: #409eff;
}

.low {
  background: #909399;
}

.result-panel {
  grid-column: 1 / -1;
}

.result-actions {
  margin-top: 16px;
}

@media (max-width: 1000px) {
  .create-page {
    grid-template-columns: 1fr;
  }

  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
