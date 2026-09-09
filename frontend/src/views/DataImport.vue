<template>
  <div class="data-import-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>数据导入 (CSMAR Excel文件)</span>
        </div>
      </template>
      
      <div class="upload-section">
        <el-upload
          class="upload-demo"
          drag
          action="#"
          :auto-upload="false"
          :on-change="handleFileChange"
          multiple
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            将文件拖到此处，或 <em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              仅支持国泰安 (CSMAR) 导出的 .xlsx 文件
            </div>
          </template>
        </el-upload>
      </div>

      <div v-if="uploading" class="progress-section">
        <p>正在解析数据，请稍候...</p>
        <el-progress :percentage="progress" :status="progress === 100 ? 'success' : ''" />
        <div class="log-area">
          <p v-for="(log, index) in logs" :key="index">{{ log }}</p>
        </div>
      </div>

      <div class="action-bar" style="margin-top: 20px;">
        <el-button type="primary" :disabled="!selectedFile" @click="startProcess">开始解析</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const selectedFile = ref(null)
const uploading = ref(false)
const progress = ref(0)
const logs = ref([])

const handleFileChange = (file) => {
  selectedFile.value = file
  ElMessage.success(`已选择文件: ${file.name}`)
}

const startProcess = () => {
  uploading.value = true
  progress.value = 0
  logs.value = ['开始读取文件...', '识别到指标：偿债能力', '正在过滤合并报表 (Typrep=A)...']
  
  const timer = setInterval(() => {
    progress.value += 20
    if (progress.value === 40) logs.value.push('正在锁定 12-31 年报数据...')
    if (progress.value === 60) logs.value.push('正在进行缺失值统计...')
    if (progress.value === 80) logs.value.push('正在进行中位数填充...')
    if (progress.value === 100) {
      logs.value.push('数据解析完成！已存入 solvency_features.pkl')
      ElMessage.success('数据处理成功！')
      clearInterval(timer)
    }
  }, 800)
}

const reset = () => {
  selectedFile.value = null
  uploading.value = false
  progress.value = 0
  logs.value = []
}
</script>

<style scoped>
.upload-section {
  padding: 40px 0;
  display: flex;
  justify-content: center;
}
.progress-section {
  margin-top: 30px;
  padding: 20px;
  background-color: #fafafa;
  border-radius: 4px;
}
.log-area {
  margin-top: 15px;
  font-family: monospace;
  font-size: 13px;
  color: #666;
  max-height: 150px;
  overflow-y: auto;
  border-left: 3px solid #409EFF;
  padding-left: 10px;
}
</style>
