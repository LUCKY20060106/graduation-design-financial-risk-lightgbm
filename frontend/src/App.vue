<template>
  <div class="app-wrapper">
    <!-- 如果不是登录页，显示整体布局 -->
    <el-container v-if="route.path !== '/login'" class="layout-container">
      <!-- 左侧菜单栏 -->
      <el-aside width="240px" class="aside">
        <div class="logo-container">
          <el-icon :size="24" color="#409EFF"><TrendCharts /></el-icon>
          <span class="logo-text">风险预警系统</span>
        </div>
        <el-menu
          :default-active="activeMenu"
          class="el-menu-vertical"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
          router
        >
          <el-menu-item index="/">
            <el-icon><Menu /></el-icon>
            <span>首页概览</span>
          </el-menu-item>
          <el-menu-item index="/companies">
            <el-icon><OfficeBuilding /></el-icon>
            <span>企业管理</span>
          </el-menu-item>
          <el-menu-item index="/evaluation">
            <el-icon><PieChart /></el-icon>
            <span>模型评估</span>
          </el-menu-item>
          <el-menu-item index="/import">
            <el-icon><Upload /></el-icon>
            <span>数据导入</span>
          </el-menu-item>
          <el-menu-item index="/predict">
            <el-icon><Cpu /></el-icon>
            <span>风险预测</span>
          </el-menu-item>
          <el-menu-item index="/history">
            <el-icon><Histogram /></el-icon>
            <span>历史记录</span>
          </el-menu-item>
          <el-menu-item index="/settings">
            <el-icon><Setting /></el-icon>
            <span>系统设置</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-container>
        <!-- 顶部状态栏 -->
        <el-header class="header">
          <div class="header-left">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
              <el-breadcrumb-item>{{ currentRouteName }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          <div class="header-right" style="display: flex; align-items: center; gap: 20px;">
            <el-tag type="success">{{ backendMessage }}</el-tag>
            <el-button type="primary" size="small" @click="testBackend">测试后端连接</el-button>
            <el-dropdown @command="handleCommand">
              <span class="user-info">
                {{ username }} <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item>个人中心</el-dropdown-item>
                  <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>

        <!-- 中间主内容区 -->
        <el-main class="main">
          <router-view></router-view>
        </el-main>
      </el-container>
    </el-container>

    <!-- 如果是登录页，直接渲染路由出口 -->
    <router-view v-else></router-view>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { Menu, Upload, Cpu, Histogram, Setting, TrendCharts, ArrowDown, OfficeBuilding, PieChart } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const backendMessage = ref('等待连接后端...')

const activeMenu = computed(() => route.path)

const username = computed(() => {
  const user = localStorage.getItem('user')
  return user ? JSON.parse(user).username : '未登录'
})

const currentRouteName = computed(() => {
  if (route.path === '/') return '概览'
  if (route.path === '/evaluation') return '模型评估'
  if (route.path === '/companies') return '企业管理'
  if (route.path === '/import') return '数据导入'
  if (route.path === '/predict') return '风险预测'
  if (route.path === '/history') return '历史记录'
  if (route.path === '/settings') return '系统设置'
  return ''
})

const testBackend = async () => {
  try {
    const response = await axios.get('http://localhost:8080/api/hello')
    backendMessage.value = `${response.data.message} | ${response.data.data}`
    ElMessage.success('后端连接成功！')
  } catch (error) {
    console.error('连接失败:', error)
    const errorMsg = error.response?.data?.message || error.message || '网络错误或后端服务未启动'
    ElMessage.error('连接失败: ' + errorMsg)
  }
}

const handleCommand = (command) => {
  if (command === 'logout') {
    localStorage.removeItem('user')
    router.push('/login')
    ElMessage.success('已退出登录')
  }
}

onMounted(() => {
  if (route.path !== '/login') {
    testBackend()
  }
})
</script>

<style>
body {
  margin: 0;
  padding: 0;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}

.layout-container {
  height: 100vh;
}

.aside {
  background-color: #304156;
  color: #fff;
}

.logo-container {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #2b2f3a;
  gap: 10px;
}

.logo-text {
  font-size: 18px;
  font-weight: bold;
  color: #fff;
}

.el-menu-vertical {
  border-right: none;
}

.header {
  background-color: #fff;
  border-bottom: 1px solid #dcdfe6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
}

.main {
  background-color: #f0f2f5;
}
</style>
