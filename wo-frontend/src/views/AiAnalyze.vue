<template>
  <div class="analyze-page">
    <section class="form-panel">
      <div class="panel-header">
        <div>
          <h3>AI 工单分析</h3>
          <span>独立调用后端 `/api/ai/analyze`，用于验证摘要、分类、优先级和解决建议。</span>
        </div>
        <el-button @click="fillSample">填充示例</el-button>
      </div>

      <el-form :model="form" label-width="88px" class="analysis-form">
        <el-form-item label="标题">
          <el-input v-model="form.title" maxlength="120" placeholder="例如：登录后控制台白屏" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="7"
            maxlength="2000"
            show-word-limit
            placeholder="描述现象、影响范围、复现步骤、错误日志和已尝试方案"
          />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="分类">
            <el-select v-model="form.category" class="full-control">
              <el-option label="BUG" value="BUG" />
              <el-option label="需求" value="FEATURE" />
              <el-option label="咨询" value="QUESTION" />
              <el-option label="维护" value="MAINTENANCE" />
              <el-option label="故障" value="INCIDENT" />
            </el-select>
          </el-form-item>
          <el-form-item label="优先级">
            <el-select v-model="form.priority" class="full-control">
              <el-option label="低" value="LOW" />
              <el-option label="中" value="MEDIUM" />
              <el-option label="高" value="HIGH" />
              <el-option label="紧急" value="URGENT" />
            </el-select>
          </el-form-item>
        </div>
      </el-form>

      <div class="similar-header">
        <strong>相似历史工单</strong>
        <el-button size="small" @click="addSimilarOrder">添加一条</el-button>
      </div>
      <div class="similar-list">
        <div v-for="(item, index) in form.similarWorkOrders" :key="index" class="similar-item">
          <el-input v-model="item.orderNo" placeholder="工单编号" />
          <el-input v-model="item.title" placeholder="标题" />
          <el-input v-model="item.resolution" placeholder="历史解决方案" />
          <el-button text type="danger" @click="removeSimilarOrder(index)">删除</el-button>
        </div>
      </div>

      <div class="actions">
        <el-button type="primary" :loading="loading" :disabled="!form.title || !form.description" @click="handleAnalyze">
          开始分析
        </el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </section>

    <section class="result-panel">
      <div class="panel-title">分析结果</div>
      <el-empty v-if="!result" description="提交后展示 AI 分析结果" />
      <div v-else class="result-grid">
        <div class="result-item">
          <span>建议分类</span>
          <strong>{{ result.suggestedCategory || '-' }}</strong>
        </div>
        <div class="result-item">
          <span>建议优先级</span>
          <strong>{{ result.suggestedPriority || '-' }}</strong>
        </div>
        <div class="result-item">
          <span>情绪</span>
          <el-tag :type="result.sentiment === 'NEGATIVE' ? 'danger' : result.sentiment === 'POSITIVE' ? 'success' : 'info'">
            {{ result.sentiment || 'NEUTRAL' }}
          </el-tag>
        </div>
        <div class="result-item wide">
          <span>摘要</span>
          <p>{{ result.summary || '-' }}</p>
        </div>
        <div class="result-item wide">
          <span>建议方案</span>
          <p>{{ result.suggestedSolution || '-' }}</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { analyzeWorkOrder } from '../api'

const createForm = () => ({
  title: '',
  description: '',
  category: 'QUESTION',
  priority: 'MEDIUM',
  similarWorkOrders: []
})

const form = ref(createForm())
const result = ref(null)
const loading = ref(false)

const handleAnalyze = async () => {
  loading.value = true
  try {
    const payload = {
      ...form.value,
      similarWorkOrders: form.value.similarWorkOrders.filter(item => item.title || item.resolution)
    }
    const res = await analyzeWorkOrder(payload)
    if (res.code === 0) {
      result.value = res.data
      ElMessage.success('分析完成')
    } else {
      ElMessage.error(res.message || '分析失败')
    }
  } finally {
    loading.value = false
  }
}

const fillSample = () => {
  form.value = {
    title: '登录后控制台白屏',
    description: '用户反馈登录成功后进入控制台页面白屏，浏览器控制台提示 401 和 token expired。影响客服部多个账号，刷新页面后仍然跳回登录页。',
    category: 'INCIDENT',
    priority: 'HIGH',
    similarWorkOrders: [
      {
        orderNo: 'WO-DEMO-001',
        title: 'Token 过期导致页面跳转异常',
        resolution: '增加前端 401 自动刷新 token，刷新失败时再清理登录态。'
      }
    ]
  }
  result.value = null
}

const reset = () => {
  form.value = createForm()
  result.value = null
}

const addSimilarOrder = () => {
  form.value.similarWorkOrders.push({ orderNo: '', title: '', description: '', resolution: '', status: 'CLOSED' })
}

const removeSimilarOrder = (index) => {
  form.value.similarWorkOrders.splice(index, 1)
}
</script>

<style scoped>
.analyze-page {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(340px, 0.9fr);
  gap: 16px;
}

.form-panel,
.result-panel {
  background: #fff;
  border: 1px solid #e7eaf0;
  border-radius: 8px;
  padding: 20px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
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

.analysis-form {
  max-width: 900px;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.full-control {
  width: 100%;
}

.similar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0 12px;
  color: #1f2937;
}

.similar-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.similar-item {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr) minmax(0, 1.4fr) 64px;
  gap: 8px;
  align-items: center;
}

.actions {
  display: flex;
  gap: 10px;
  margin-top: 18px;
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.result-item {
  padding: 14px;
  background: #f8fafc;
  border: 1px solid #edf0f5;
  border-radius: 8px;
}

.result-item span {
  display: block;
  color: #7b8794;
  font-size: 12px;
}

.result-item strong,
.result-item p {
  display: block;
  margin: 8px 0 0;
  color: #1f2937;
  line-height: 1.7;
}

.wide {
  grid-column: 1 / -1;
}

@media (max-width: 1040px) {
  .analyze-page,
  .form-row,
  .result-grid,
  .similar-item {
    grid-template-columns: 1fr;
  }
}
</style>
