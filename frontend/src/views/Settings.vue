<template>
  <div class="settings-container">
    <el-card class="settings-card">
      <template #header>
        <div class="card-header">
          <span>系统参数设置</span>
        </div>
      </template>

      <el-form :model="settings" label-width="150px">
        <el-divider content-position="left">风险预警阈值</el-divider>
        <el-form-item label="高风险阈值">
          <el-slider v-model="settings.threshold_high" :min="0" :max="1" :step="0.05" show-input />
          <div class="tip">当风险概率高于此值时，标记为“高风险”</div>
        </el-form-item>
        <el-form-item label="中风险阈值">
          <el-slider v-model="settings.threshold_medium" :min="0" :max="1" :step="0.05" show-input />
          <div class="tip">当风险概率高于此值且低于高风险阈值时，标记为“中风险”</div>
        </el-form-item>

        <el-divider content-position="left">数据管理</el-divider>
        <el-form-item label="清空预测历史">
          <el-popconfirm
            title="确定要清空所有历史预测记录吗？此操作不可撤销。"
            @confirm="handleClearHistory"
          >
            <template #reference>
              <el-button type="danger">立即清空历史记录</el-button>
            </template>
          </el-popconfirm>
        </el-form-item>

        <el-form-item style="margin-top: 40px">
          <el-button type="primary" @click="handleSaveSettings" :loading="saving">保存配置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const settings = ref({
  threshold_high: 0.7,
  threshold_medium: 0.3
})
const saving = ref(false)

const fetchSettings = async () => {
  try {
    const response = await axios.get('http://localhost:8080/api/settings')
    settings.value.threshold_high = parseFloat(response.data.threshold_high) || 0.7
    settings.value.threshold_medium = parseFloat(response.data.threshold_medium) || 0.3
  } catch (error) {
    ElMessage.error('获取设置失败')
  }
}

const handleSaveSettings = async () => {
  saving.value = true
  try {
    await axios.post('http://localhost:8080/api/settings', {
      threshold_high: settings.value.threshold_high.toString(),
      threshold_medium: settings.value.threshold_medium.toString()
    })
    ElMessage.success('配置已保存，实时生效')
  } catch (error) {
    ElMessage.error('保存配置失败')
  } finally {
    saving.value = false
  }
}

const handleClearHistory = async () => {
  try {
    await axios.delete('http://localhost:8080/api/settings/history/clear')
    ElMessage.success('历史记录已清空')
  } catch (error) {
    ElMessage.error('清空失败')
  }
}

onMounted(() => {
  fetchSettings()
})
</script>

<style scoped>
.settings-container {
  padding: 20px;
}
.settings-card {
  max-width: 800px;
  margin: 0 auto;
}
.tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
  line-height: 1.2;
}
</style>
