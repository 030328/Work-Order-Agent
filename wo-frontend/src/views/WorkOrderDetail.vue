<template>
  <div v-if="workOrder" class="detail-page">
    <section class="summary-panel">
      <div>
        <div class="order-number">{{ workOrder.orderNo }}</div>
        <h3>{{ workOrder.title }}</h3>
        <p>{{ workOrder.description }}</p>
      </div>
      <div class="status-stack">
        <el-tag :type="getStatusType(workOrder.status)" size="large">{{ getStatusLabel(workOrder.status) }}</el-tag>
        <el-tag :type="getPriorityType(workOrder.priority)" size="large">{{ getPriorityLabel(workOrder.priority) }}</el-tag>
      </div>
    </section>

    <div class="detail-grid">
      <section class="info-panel">
        <div class="panel-title">工单信息</div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="分类">{{ workOrder.category }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ workOrder.department || '未分配' }}</el-descriptions-item>
          <el-descriptions-item label="创建人">{{ workOrder.creatorName || workOrder.creatorId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理人">{{ assigneeText }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(workOrder.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="SLA 截止">{{ formatTime(workOrder.slaDeadline) || '未分配' }}</el-descriptions-item>
          <el-descriptions-item label="解决时间">{{ formatTime(workOrder.resolvedAt) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="关闭时间">{{ formatTime(workOrder.closedAt) || '-' }}</el-descriptions-item>
        </el-descriptions>
      </section>

      <section class="action-panel">
        <div class="panel-title">处理动作</div>
        <p class="action-hint">{{ actionHint }}</p>
        <div class="action-buttons">
          <el-button v-if="canRegenerateSolution" type="primary" plain :loading="regenerating" @click="handleRegenerate">
            重新生成 AI 方案
          </el-button>
          <el-button v-if="canAssign" type="primary" @click="openAssignDialog">分配处理人</el-button>
          <el-button v-if="canCompleteAiOrder" type="success" @click="handleConfirm">处理完成</el-button>
          <el-button v-if="canEscalateToManual" type="danger" @click="handleEscalate">转人工处理</el-button>
          <el-button v-if="canClaim" type="primary" @click="handleClaim">认领工单</el-button>
          <el-button v-if="canSubmitResolution" type="success" @click="resolutionVisible = true">提交处理意见</el-button>
          <el-button v-if="canConfirmManualResult" type="success" @click="handleConfirm">确认完成</el-button>
          <el-button v-if="canRejectManualResult" type="warning" @click="rejectVisible = true">驳回处理结果</el-button>
          <el-button @click="loadData">刷新</el-button>
        </div>
      </section>
    </div>

    <section class="ai-panel">
      <div class="panel-title">AI 分析结果</div>
      <div v-if="workOrder.aiSummary" class="ai-grid">
        <div class="ai-item">
          <span>摘要</span>
          <p>{{ workOrder.aiSummary }}</p>
        </div>
        <div class="ai-item">
          <span>情绪</span>
          <el-tag :type="workOrder.aiSentiment === 'NEGATIVE' ? 'danger' : workOrder.aiSentiment === 'POSITIVE' ? 'success' : 'info'">
            {{ workOrder.aiSentiment || 'NEUTRAL' }}
          </el-tag>
        </div>
        <div class="ai-item">
          <span>建议分类</span>
          <p>{{ workOrder.aiCategorySuggestion || '-' }}</p>
        </div>
        <div class="ai-item solution">
          <span>建议方案</span>
          <p>{{ workOrder.aiSuggestedSolution || '暂无建议' }}</p>
        </div>
      </div>
      <el-empty v-else description="暂无 AI 分析结果" />
    </section>

    <section class="attachment-panel">
      <div class="section-header">
        <div class="panel-title">附件</div>
        <div class="attachment-actions">
          <el-upload :http-request="handleUploadAttachment" :show-file-list="false">
            <el-button type="primary" :loading="uploading">上传文件</el-button>
          </el-upload>
          <el-button @click="metadataVisible = true">添加链接</el-button>
        </div>
      </div>
      <el-table v-if="attachments.length" :data="attachments" stripe>
        <el-table-column prop="fileName" label="文件名" min-width="220">
          <template #default="{ row }">
            <a v-if="getAttachmentHref(row.fileUrl)" :href="getAttachmentHref(row.fileUrl)" target="_blank" rel="noreferrer">{{ row.fileName }}</a>
            <span v-else>{{ row.fileName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="fileType" label="类型" width="150" />
        <el-table-column prop="fileSize" label="大小" width="120">
          <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="uploaderName" label="上传人" width="140" />
        <el-table-column prop="createdAt" label="上传时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无附件" />
    </section>

    <div class="collaboration-grid">
      <section class="comment-panel">
        <div class="panel-title">协作评论</div>
        <div class="comment-list">
          <div v-for="comment in comments" :key="comment.id" class="comment-item">
            <div class="comment-meta">
              <strong>{{ comment.userName || `用户#${comment.userId}` }}</strong>
              <span>{{ formatTime(comment.createdAt) }}</span>
              <el-tag v-if="comment.isInternal" type="warning" size="small">内部</el-tag>
              <el-tag v-if="comment.isAiGenerated" type="info" size="small">AI</el-tag>
            </div>
            <p>{{ comment.content }}</p>
          </div>
          <el-empty v-if="comments.length === 0" description="暂无评论" />
        </div>

        <div class="comment-form">
          <el-input
            v-model="commentContent"
            type="textarea"
            :rows="3"
            maxlength="1000"
            show-word-limit
            placeholder="输入评论"
          />
          <div class="comment-actions">
            <el-checkbox v-if="canHandleManualWork" v-model="commentInternal">内部备注</el-checkbox>
            <span v-else></span>
            <el-button type="primary" :loading="commentLoading" :disabled="!commentContent.trim()" @click="handleAddComment">
              发送
            </el-button>
          </div>
        </div>
      </section>

      <section class="flow-panel">
        <div class="panel-title">流转记录</div>
        <el-timeline v-if="flowRecords.length > 0">
          <el-timeline-item
            v-for="record in flowRecords"
            :key="record.id"
            :timestamp="formatTime(record.createdAt)"
            placement="top"
          >
            <div class="flow-title">
              <strong>{{ getActionLabel(record.action) }}</strong>
              <span>{{ record.operatorName || `用户#${record.operatorId}` }}</span>
            </div>
            <div v-if="record.fromStatus || record.toStatus" class="flow-status">
              {{ getStatusLabel(record.fromStatus) || '-' }} -> {{ getStatusLabel(record.toStatus) || '-' }}
            </div>
            <p v-if="record.comment">{{ record.comment }}</p>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无流转记录" />
      </section>
    </div>

    <el-dialog v-model="resolutionVisible" title="提交处理意见" width="520px">
      <el-form label-position="top">
        <el-form-item label="处理意见">
          <el-input
            v-model="resolutionComment"
            type="textarea"
            :rows="5"
            maxlength="1000"
            show-word-limit
            placeholder="填写原因、处理过程或解决方案"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resolutionVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!resolutionComment.trim()" @click="handleResolve">提交并标记已解决</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectVisible" title="驳回处理结果" width="520px">
      <el-form label-position="top">
        <el-form-item label="驳回原因">
          <el-input
            v-model="rejectReason"
            type="textarea"
            :rows="4"
            maxlength="1000"
            show-word-limit
            placeholder="说明未解决的问题或需要补充处理的内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="warning" :disabled="!rejectReason.trim()" @click="handleReject">确认驳回</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignVisible" title="分配处理人" width="560px">
      <el-form label-position="top">
        <el-form-item label="处理人">
          <el-select v-model="assignForm.assigneeId" filterable class="full-control" placeholder="选择处理人">
            <el-option
              v-for="user in assignableUsers"
              :key="user.id"
              :label="`${user.realName || user.username} - ${user.department || '未分部门'}`"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="分配说明">
          <el-input
            v-model="assignForm.reason"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="说明分配原因或处理要求"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigning" :disabled="!assignForm.assigneeId" @click="handleAssign">确认分配</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="metadataVisible" title="添加附件链接" width="560px">
      <el-form label-position="top">
        <el-form-item label="文件名">
          <el-input v-model="metadataForm.fileName" maxlength="200" placeholder="例如：问题截图.png" />
        </el-form-item>
        <el-form-item label="文件链接">
          <el-input v-model="metadataForm.fileUrl" maxlength="500" placeholder="https:// 或内部文件地址" />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="类型">
            <el-input v-model="metadataForm.fileType" maxlength="50" placeholder="image/png" />
          </el-form-item>
          <el-form-item label="大小（字节）">
            <el-input-number v-model="metadataForm.fileSize" :min="0" class="full-control" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="metadataVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!metadataForm.fileName || !metadataForm.fileUrl" @click="handleAddMetadata">添加</el-button>
      </template>
    </el-dialog>
  </div>

  <el-empty v-else description="正在加载工单" />
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addAttachmentMetadata,
  addComment,
  assignWorkOrder,
  claimWorkOrder,
  confirmWorkOrder,
  escalateWorkOrder,
  getAttachments,
  getComments,
  getFlowRecords,
  getUsers,
  getWorkOrder,
  regenerateSolution,
  rejectWorkOrder,
  updateStatus,
  uploadAttachment
} from '../api'

const route = useRoute()
const workOrder = ref(null)
const comments = ref([])
const flowRecords = ref([])
const attachments = ref([])
const assignableUsers = ref([])
const resolutionVisible = ref(false)
const rejectVisible = ref(false)
const assignVisible = ref(false)
const metadataVisible = ref(false)
const resolutionComment = ref('')
const rejectReason = ref('')
const commentContent = ref('')
const commentInternal = ref(false)
const commentLoading = ref(false)
const regenerating = ref(false)
const assigning = ref(false)
const uploading = ref(false)
const assignForm = ref({ assigneeId: null, reason: '' })
const metadataForm = ref({ fileName: '', fileUrl: '', fileSize: 0, fileType: '' })
const currentUserId = localStorage.getItem('userId')
const currentRole = localStorage.getItem('role') || 'USER'
const canHandleManualWork = computed(() => ['ADMIN', 'MANAGER', 'AGENT'].includes(currentRole))
const canManageWork = computed(() => ['ADMIN', 'MANAGER'].includes(currentRole))
const isCreator = computed(() => workOrder.value?.creatorId && String(workOrder.value.creatorId) === String(currentUserId))

const canClaim = computed(() => workOrder.value?.status === 'ESCALATED' && canHandleManualWork.value)
const canAssign = computed(() => canManageWork.value && workOrder.value?.status !== 'CLOSED')
const canCompleteAiOrder = computed(() => workOrder.value?.status === 'AI_SOLVED' && isCreator.value)
const canEscalateToManual = computed(() => workOrder.value?.status === 'AI_SOLVED' && isCreator.value)
const canConfirmManualResult = computed(() => workOrder.value?.status === 'RESOLVED' && isCreator.value)
const canRejectManualResult = computed(() => workOrder.value?.status === 'RESOLVED' && isCreator.value)
const canRegenerateSolution = computed(() => {
  return workOrder.value?.status !== 'CLOSED' && (isCreator.value || canManageWork.value)
})

const assigneeText = computed(() => {
  if (!workOrder.value?.assigneeId) {
    return '未分配'
  }
  return workOrder.value.assigneeName || `用户#${workOrder.value.assigneeId}`
})

const canSubmitResolution = computed(() => {
  if (workOrder.value?.status !== 'IN_PROGRESS') {
    return false
  }
  return canHandleManualWork.value && (canManageWork.value || String(workOrder.value.assigneeId) === String(currentUserId))
})

const actionHint = computed(() => {
  const status = workOrder.value?.status
  if (status === 'AI_SOLVED') return isCreator.value ? 'AI 已给出处理建议，请选择完成或转人工。' : 'AI 已给出处理建议，等待创建人确认。'
  if (status === 'ESCALATED') return canHandleManualWork.value ? '工单已进入人工队列，可以认领或由管理员分配。' : '工单已进入人工处理队列。'
  if (status === 'IN_PROGRESS') return canSubmitResolution.value ? '填写处理意见后可标记为已解决。' : '处理人正在跟进。'
  if (status === 'RESOLVED') return isCreator.value ? '处理人已提交结果，请确认或驳回。' : '等待创建人确认。'
  if (status === 'OPEN') return canManageWork.value ? '工单已创建，可以分配处理人或等待 AI 分析。' : '工单已创建，等待 AI 分析。'
  if (status === 'CLOSED') return '工单已关闭。'
  return '当前状态暂无待处理动作。'
})

const loadData = async () => {
  await loadWorkOrder()
  await Promise.all([loadComments(), loadFlowRecords(), loadAttachments()])
}

const loadWorkOrder = async () => {
  try {
    const res = await getWorkOrder(route.params.id)
    if (res.code === 0) {
      workOrder.value = res.data
    } else {
      ElMessage.error(res.message)
    }
  } catch (e) {
    ElMessage.error('工单加载失败')
  }
}

const loadComments = async () => {
  try {
    const res = await getComments(route.params.id)
    comments.value = res.code === 0 ? res.data || [] : []
  } catch (e) {
    comments.value = []
  }
}

const loadFlowRecords = async () => {
  try {
    const res = await getFlowRecords(route.params.id)
    flowRecords.value = res.code === 0 ? res.data || [] : []
  } catch (e) {
    flowRecords.value = []
  }
}

const loadAttachments = async () => {
  try {
    const res = await getAttachments(route.params.id)
    attachments.value = res.code === 0 ? res.data || [] : []
  } catch (e) {
    attachments.value = []
  }
}

const openAssignDialog = async () => {
  assignForm.value = { assigneeId: workOrder.value?.assigneeId || null, reason: '' }
  await loadAssignableUsers()
  assignVisible.value = true
}

const loadAssignableUsers = async () => {
  const res = await getUsers({
    page: 1,
    size: 100,
    role: 'AGENT',
    department: workOrder.value?.department || undefined
  })
  if (res.code === 0) {
    assignableUsers.value = res.data.records || []
    if (assignableUsers.value.length === 0 && workOrder.value?.department) {
      const fallbackRes = await getUsers({ page: 1, size: 100, role: 'AGENT' })
      assignableUsers.value = fallbackRes.code === 0 ? fallbackRes.data.records || [] : []
    }
  } else {
    assignableUsers.value = []
    ElMessage.error(res.message || '处理人加载失败')
  }
}

const handleAssign = async () => {
  assigning.value = true
  try {
    const res = await assignWorkOrder(route.params.id, {
      assigneeId: assignForm.value.assigneeId,
      reason: assignForm.value.reason
    })
    if (res.code === 0) {
      ElMessage.success('工单已分配')
      assignVisible.value = false
      loadData()
    } else {
      ElMessage.error(res.message)
    }
  } finally {
    assigning.value = false
  }
}

const handleRegenerate = async () => {
  await ElMessageBox.confirm('确认重新生成 AI 方案？当前 AI 建议会被覆盖。', '确认')
  regenerating.value = true
  try {
    const res = await regenerateSolution(route.params.id)
    if (res.code === 0) {
      workOrder.value = res.data
      ElMessage.success('AI 方案已重新生成')
      loadFlowRecords()
    } else {
      ElMessage.error(res.message)
    }
  } finally {
    regenerating.value = false
  }
}

const handleUploadAttachment = async ({ file }) => {
  uploading.value = true
  try {
    const res = await uploadAttachment(route.params.id, file)
    if (res.code === 0) {
      attachments.value.unshift(res.data)
      ElMessage.success('附件已上传')
      loadFlowRecords()
    } else {
      ElMessage.error(res.message || '附件上传失败')
    }
  } finally {
    uploading.value = false
  }
}

const handleAddMetadata = async () => {
  const res = await addAttachmentMetadata(route.params.id, metadataForm.value)
  if (res.code === 0) {
    attachments.value.unshift(res.data)
    metadataVisible.value = false
    metadataForm.value = { fileName: '', fileUrl: '', fileSize: 0, fileType: '' }
    ElMessage.success('附件链接已添加')
    loadFlowRecords()
  } else {
    ElMessage.error(res.message || '添加失败')
  }
}

const handleConfirm = async () => {
  await ElMessageBox.confirm('确认关闭该工单？', '确认')
  const res = await confirmWorkOrder(route.params.id)
  if (res.code === 0) {
    ElMessage.success('工单已关闭')
    loadData()
  } else {
    ElMessage.error(res.message)
  }
}

const handleEscalate = async () => {
  await ElMessageBox.confirm('确认转人工处理？', '确认')
  const res = await escalateWorkOrder(route.params.id)
  if (res.code === 0) {
    ElMessage.success('已转人工处理')
    loadData()
  } else {
    ElMessage.error(res.message)
  }
}

const handleClaim = async () => {
  await ElMessageBox.confirm('认领此工单？', '确认')
  const res = await claimWorkOrder(route.params.id)
  if (res.code === 0) {
    ElMessage.success('认领成功')
    loadData()
  } else {
    ElMessage.error(res.message)
  }
}

const handleResolve = async () => {
  const res = await updateStatus(route.params.id, 'RESOLVED', resolutionComment.value.trim())
  if (res.code === 0) {
    ElMessage.success('处理意见已提交')
    resolutionVisible.value = false
    resolutionComment.value = ''
    loadData()
  } else {
    ElMessage.error(res.message)
  }
}

const handleReject = async () => {
  const res = await rejectWorkOrder(route.params.id, rejectReason.value.trim())
  if (res.code === 0) {
    ElMessage.success('已驳回处理结果')
    rejectVisible.value = false
    rejectReason.value = ''
    loadData()
  } else {
    ElMessage.error(res.message)
  }
}

const handleAddComment = async () => {
  commentLoading.value = true
  try {
    const res = await addComment(route.params.id, {
      content: commentContent.value.trim(),
      isInternal: canHandleManualWork.value ? commentInternal.value : false
    })
    if (res.code === 0) {
      comments.value.push(res.data)
      commentContent.value = ''
      commentInternal.value = false
      loadFlowRecords()
      ElMessage.success('评论已发送')
    } else {
      ElMessage.error(res.message)
    }
  } finally {
    commentLoading.value = false
  }
}

const getAttachmentHref = (value) => {
  if (!value) return ''
  if (/^https?:\/\//i.test(value)) return value
  const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
  const gatewayBase = apiBase.replace(/\/api\/?$/, '')
  if (value.startsWith('/api/')) return `${gatewayBase}${value}`
  if (value.startsWith('/')) return `${gatewayBase}${value}`
  return ''
}

const formatTime = (value) => {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 19)
}

const formatFileSize = (size) => {
  if (size === undefined || size === null) return '-'
  const value = Number(size)
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / 1024 / 1024).toFixed(1)} MB`
}

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
  const map = { URGENT: '紧急', HIGH: '高优先级', MEDIUM: '中优先级', LOW: '低优先级' }
  return map[priority] || priority
}

const getActionLabel = (action) => {
  const map = {
    CREATE: '创建工单',
    AI_SOLVED: 'AI 处理',
    ESCALATE: '转人工',
    CLAIM: '认领',
    ASSIGN: '分配',
    STATUS_CHANGE: '状态变更',
    CONFIRM: '确认完成',
    REJECT_RESOLUTION: '驳回结果',
    COMMENT: '评论',
    AI_COMMENT: 'AI 评论',
    ATTACHMENT: '附件',
    SLA_BREACH: 'SLA 超时'
  }
  return map[action] || action
}

onMounted(loadData)
</script>

<style scoped>
.detail-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-panel,
.info-panel,
.action-panel,
.ai-panel,
.attachment-panel,
.comment-panel,
.flow-panel {
  background: #fff;
  border: 1px solid #e7eaf0;
  border-radius: 8px;
}

.summary-panel {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 22px;
}

.order-number {
  color: #409eff;
  font-weight: 700;
}

.summary-panel h3 {
  margin: 8px 0;
  color: #111827;
  font-size: 22px;
}

.summary-panel p {
  margin: 0;
  color: #4b5563;
  line-height: 1.7;
}

.status-stack {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  flex-shrink: 0;
}

.detail-grid,
.collaboration-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 16px;
}

.info-panel,
.action-panel,
.ai-panel,
.attachment-panel,
.comment-panel,
.flow-panel {
  padding: 18px;
}

.panel-title {
  margin-bottom: 14px;
  color: #1f2937;
  font-weight: 700;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.attachment-actions,
.action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.action-hint {
  color: #6b7280;
  line-height: 1.7;
}

.ai-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.ai-item {
  padding: 14px;
  background: #f8fafc;
  border: 1px solid #edf0f5;
  border-radius: 8px;
}

.ai-item span {
  color: #7b8794;
  font-size: 12px;
}

.ai-item p {
  margin: 8px 0 0;
  color: #1f2937;
  line-height: 1.7;
}

.solution {
  grid-column: 1 / -1;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 380px;
  overflow: auto;
}

.comment-item {
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #edf0f5;
  border-radius: 8px;
}

.comment-meta,
.flow-title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: #6b7280;
  font-size: 12px;
}

.comment-meta strong,
.flow-title strong {
  color: #1f2937;
  font-size: 13px;
}

.comment-item p,
.flow-panel p {
  margin: 8px 0 0;
  color: #374151;
  line-height: 1.7;
  white-space: pre-wrap;
}

.comment-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 14px;
}

.comment-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.flow-status {
  margin-top: 6px;
  color: #409eff;
  font-size: 12px;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.full-control {
  width: 100%;
}

@media (max-width: 1000px) {
  .summary-panel,
  .detail-grid,
  .collaboration-grid {
    grid-template-columns: 1fr;
  }

  .summary-panel,
  .section-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .ai-grid,
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
