<template>
  <div class="login-page">
    <section class="intro-panel">
      <div class="brand-row">
        <div class="brand-mark">WO</div>
        <span>AI Agent Work Order</span>
      </div>
      <h1>企业级智能工单系统</h1>
      <p>统一接入工单、工作流、SLA 和 AI Agent，帮助团队更快识别问题、分派处理并沉淀知识。</p>
      <div class="feature-list">
        <span>JWT 网关鉴权</span>
        <span>工作流状态校验</span>
        <span>RAG 知识库</span>
        <span>SLA 超时升级</span>
      </div>
    </section>

    <section class="login-card">
      <el-segmented v-model="mode" :options="modeOptions" class="mode-switch" />

      <h2>{{ mode === 'login' ? '登录系统' : '注册账号' }}</h2>
      <p class="subtitle">
        {{ mode === 'login' ? '默认可使用 admin / admin123' : '请补全姓名和部门，注册后默认作为普通提交人使用' }}
      </p>

      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password />
        </el-form-item>

        <template v-if="mode === 'register'">
          <el-form-item label="真实姓名" prop="realName">
            <el-input v-model="form.realName" placeholder="请输入真实姓名" />
          </el-form-item>
          <el-form-item label="部门" prop="department">
            <el-select v-model="form.department" placeholder="请选择部门" class="full-field">
              <el-option label="技术部" value="技术部" />
              <el-option label="产品部" value="产品部" />
              <el-option label="客服部" value="客服部" />
              <el-option label="运维部" value="运维部" />
              <el-option label="运营部" value="运营部" />
            </el-select>
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" placeholder="可选，用于通知" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="可选，用于联系" />
          </el-form-item>
        </template>

        <el-form-item>
          <el-button type="primary" :loading="loading" @click="submit" class="full-button">
            {{ mode === 'login' ? '登录' : '注册并登录' }}
          </el-button>
        </el-form-item>
      </el-form>
    </section>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register } from '../api'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const mode = ref('login')
const modeOptions = [
  { label: '登录', value: 'login' },
  { label: '注册', value: 'register' }
]

const form = ref({
  username: 'admin',
  password: 'admin123',
  realName: '',
  department: '',
  role: 'USER',
  email: '',
  phone: ''
})

const rules = computed(() => ({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: mode.value === 'register', message: '请输入真实姓名', trigger: 'blur' }],
  department: [{ required: mode.value === 'register', message: '请选择部门', trigger: 'change' }],
  role: [{ required: mode.value === 'register', message: '请选择角色', trigger: 'change' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }]
}))

watch(mode, (value) => {
  formRef.value?.clearValidate()
  if (value === 'login') {
    form.value.username = form.value.username || 'admin'
    form.value.password = form.value.password || 'admin123'
  } else if (!form.value.realName) {
    form.value.realName = form.value.username === 'admin' ? '' : form.value.username
  }
})

const persistLogin = (data) => {
  localStorage.setItem('token', data.token)
  localStorage.setItem('userId', data.userId)
  localStorage.setItem('username', data.username)
  localStorage.setItem('realName', data.realName || data.username)
  localStorage.setItem('role', data.role || 'USER')
}

const submit = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = mode.value === 'login'
      ? await login({ username: form.value.username, password: form.value.password })
      : await register({
          username: form.value.username,
          password: form.value.password,
          realName: form.value.realName,
          department: form.value.department,
          role: form.value.role,
          email: form.value.email || undefined,
          phone: form.value.phone || undefined
        })

    if (res.code === 0) {
      persistLogin(res.data)
      ElMessage.success(mode.value === 'login' ? '登录成功' : '注册成功')
      router.push('/dashboard')
    } else {
      ElMessage.error(res.message)
    }
  } catch (e) {
    ElMessage.error(mode.value === 'login' ? '登录失败' : '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 460px;
  background: #f4f6f9;
}

.intro-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 72px;
  background: #243044;
  color: #fff;
}

.brand-row {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #c8d3e2;
  font-weight: 700;
}

.brand-mark {
  width: 42px;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #409eff;
  color: #fff;
}

.intro-panel h1 {
  margin: 36px 0 16px;
  font-size: 40px;
  line-height: 1.2;
}

.intro-panel p {
  max-width: 640px;
  margin: 0;
  color: #c8d3e2;
  line-height: 1.8;
}

.feature-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 32px;
}

.feature-list span {
  padding: 8px 12px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 6px;
  color: #e5edf7;
  font-size: 13px;
}

.login-card {
  align-self: center;
  margin: 0 48px;
  padding: 30px;
  background: #fff;
  border: 1px solid #e7eaf0;
  border-radius: 8px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}

.mode-switch {
  width: 100%;
  margin-bottom: 22px;
}

.login-card h2 {
  margin: 0;
  color: #1f2937;
}

.subtitle {
  margin: 8px 0 22px;
  color: #7b8794;
  font-size: 13px;
  line-height: 1.6;
}

.full-field,
.full-button {
  width: 100%;
}

@media (max-width: 900px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .intro-panel {
    padding: 40px 24px;
  }

  .login-card {
    margin: 24px;
  }
}
</style>
