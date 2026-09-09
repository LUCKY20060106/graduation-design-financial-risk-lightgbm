<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="login-header">
          <h2>上市公司财务风险预警系统</h2>
          <p>智能监测 · 风险识别 · 数据驱动</p>
        </div>
      </template>
      
      <el-form :model="loginForm" :rules="rules" ref="loginFormRef" label-position="top">
        <el-form-item label="账号" prop="username">
          <el-input v-model="loginForm.username" prefix-icon="User" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="loginForm.password" prefix-icon="Lock" type="password" placeholder="请输入密码" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-button" :loading="loading" @click="handleLogin">立即登录</el-button>
        </el-form-item>
      </el-form>
      
      <div class="login-footer">
        <span>默认账号: admin | 密码: admin</span>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const loading = ref(false)
const loginFormRef = ref(null)

const loginForm = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const response = await axios.post('http://localhost:8080/api/login', loginForm)
        if (response.data.success) {
          localStorage.setItem('user', JSON.stringify(response.data.user))
          ElMessage.success('登录成功')
          router.push('/')
        } else {
          ElMessage.error(response.data.message || '登录失败')
        }
      } catch (error) {
        console.error(error)
        ElMessage.error('无法连接到服务器')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
}

.login-card {
  width: 400px;
  border-radius: 12px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.2);
}

.login-header {
  text-align: center;
}

.login-header h2 {
  margin: 0;
  color: #409eff;
  font-size: 24px;
}

.login-header p {
  margin: 10px 0 0;
  color: #909399;
  font-size: 14px;
}

.login-button {
  width: 100%;
  height: 40px;
  font-size: 16px;
  margin-top: 10px;
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 12px;
  color: #c0c4cc;
}
</style>
