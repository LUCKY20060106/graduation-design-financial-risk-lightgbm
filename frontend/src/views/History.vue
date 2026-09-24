<template>
  <div class="history-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>风险预测历史记录</span>
          <el-button type="primary" icon="Refresh" @click="fetchHistory">刷新记录</el-button>
        </div>
      </template>

      <el-table :data="historyData" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="stkcd" label="股票代码" width="100" />
        <el-table-column prop="predictDate" label="预测时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.predictDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="riskScore" label="风险评分" width="120">
          <template #default="scope">
            <el-progress 
              :percentage="Math.round(scope.row.riskScore * 100)" 
              :color="getProgressColor(scope.row.riskScore)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="riskLevel" label="风险等级" width="100">
          <template #default="scope">
            <el-tag :type="getRiskLevelTag(scope.row.riskLevel)">{{ scope.row.riskLevel || '未知' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isRisk" label="判定结果" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.isRisk ? 'danger' : 'success'">
              {{ scope.row.isRisk ? '高风险' : '低风险' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="scope">
            <el-button size="small" type="primary" @click="viewDetails(scope.row)">详情</el-button>
            <el-button size="small" type="success" @click="handleExport(scope.row.id)">导出 Excel</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailsVisible" title="风险评估详细分析" width="85%" destroy-on-close>
      <div v-if="selectedRow">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-descriptions title="基本信息" :column="1" border>
              <el-descriptions-item label="股票代码">{{ selectedRow.stkcd }}</el-descriptions-item>
              <el-descriptions-item label="预测时间">{{ formatDate(selectedRow.predictDate) }}</el-descriptions-item>
              <el-descriptions-item label="风险评分">
                <el-tag :type="getProgressColor(selectedRow.riskScore)">
                  {{ (selectedRow.riskScore * 100).toFixed(2) }}%
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="风险判定">
                <el-tag :type="selectedRow.isRisk ? 'danger' : 'success'">
                  {{ selectedRow.isRisk ? '高风险' : '低风险' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="风险等级">
                <el-tag :type="getRiskLevelTag(selectedRow.riskLevel)">{{ selectedRow.riskLevel || '未知' }}</el-tag>
              </el-descriptions-item>
            </el-descriptions>
            <div style="margin-top: 20px; text-align: center;">
              <el-button type="success" @click="handleExport(selectedRow.id)">重新导出 Excel</el-button>
            </div>
          </el-col>

          <el-col :span="9">
            <div v-if="selectedRow.shapValues" class="shap-chart-container">
              <h3>特征贡献度分析 (SHAP Value)</h3>
              <p class="chart-tip">正值增加风险，负值降低风险</p>
              <div ref="shapChartRef" style="height: 450px; width: 100%;"></div>
            </div>
            <div v-else class="empty-shap">
              <el-empty description="暂无 SHAP 数据"></el-empty>
            </div>
          </el-col>

          <el-col :span="9">
            <div class="benchmark-container">
              <h3>行业对标分析</h3>
              <p class="chart-tip">对比该企业与行业均值的维度偏差</p>
              <div v-if="hasBenchmarking" ref="radarChartRef" style="height: 450px; width: 100%;"></div>
              <el-empty v-else description="暂无行业对标数据" :image-size="100"></el-empty>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import request from '../utils/request'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

const historyData = ref([])
const loading = ref(false)
const detailsVisible = ref(false)
const selectedRow = ref(null)
const shapChartRef = ref(null)
const radarChartRef = ref(null)
const hasBenchmarking = ref(false)

const fetchHistory = async () => {
  loading.value = true
  try {
    const response = await request.get('/history')
    historyData.value = response
  } catch (error) {
    ElMessage.error('获取历史记录失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleExport = (id) => {
  if (!id) {
    ElMessage.warning('记录ID不存在，无法导出。')
    return
  }
  // 构建导出URL并触发下载
  const exportUrl = `/api/prediction/export/${id}`
  window.open(exportUrl, '_blank')
  ElMessage.success('正在导出报告，请稍候...')
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString()
}

const getProgressColor = (score) => {
  if (score > 0.7) return 'danger'
  if (score > 0.3) return 'warning'
  return 'success'
}

const getRiskLevelTag = (level) => {
  if (level === '高风险') return 'danger'
  if (level === '中风险') return 'warning'
  return 'success'
}

const viewDetails = async (row) => {
  selectedRow.value = row
  detailsVisible.value = true
  hasBenchmarking.value = false
  
  await nextTick()
  // 给对话框动画留一点时间，确保容器已经渲染
  setTimeout(async () => {
    if (row.shapValues) {
      initShapChart(row)
    }
    
    // 获取行业对标数据并初始化雷达图
    try {
      const response = await request.get(`/prediction/benchmarking/${row.stkcd}`)
      if (response) {
        hasBenchmarking.value = true
        await nextTick()
        initRadarChart(row, response)
      }
    } catch (error) {
      console.warn('获取对标数据失败:', error)
      hasBenchmarking.value = false
    }
  }, 500)
}

// 初始化雷达图
const initRadarChart = (row, benchmarking) => {
  if (!radarChartRef.value) return
  
  const existingInstance = echarts.getInstanceByDom(radarChartRef.value)
  if (existingInstance) {
    existingInstance.dispose()
  }
  
  const chart = echarts.init(radarChartRef.value)
  
  // 从记录中解析特征值
  let featureValues = {}
  try {
    featureValues = JSON.parse(row.featureValuesJson || '{}')
  } catch (e) {
    console.error('解析特征值失败:', e)
  }

  // 标准化评分 (模拟逻辑，与预测页保持一致)
  const companyMetrics = [
    Math.min((featureValues['流动比率'] || 0) / 2, 1),
    Math.min((featureValues['总资产周转率'] || 0) / 1.5, 1),
    Math.min((featureValues['总资产增长率'] || 0) / 0.3, 1),
    Math.min((featureValues['利息保障倍数'] || 0) / 10, 1),
    Math.min((featureValues['经营净现金流/流动负债'] || 0) * 2, 1)
  ]

  const option = {
    radar: {
      indicator: [
        { name: '偿债能力', max: 1 },
        { name: '营运能力', max: 1 },
        { name: '发展能力', max: 1 },
        { name: '盈利保障', max: 1 },
        { name: '现金流', max: 1 }
      ],
      shape: 'circle',
      radius: '65%',
      axisName: { color: '#333' },
      splitArea: { show: true, areaStyle: { color: ['#fff', '#fafafa'] } }
    },
    legend: {
      data: ['该企业', '行业均值'],
      bottom: 0
    },
    series: [{
      type: 'radar',
      data: [
        {
          value: companyMetrics,
          name: '该企业',
          areaStyle: { color: 'rgba(64, 158, 255, 0.4)' },
          lineStyle: { color: '#409EFF' }
        },
        {
          value: [
            benchmarking.avgSolvency || 0.6,
            benchmarking.avgOperation || 0.6,
            benchmarking.avgGrowth || 0.6,
            benchmarking.avgProfit || 0.6,
            benchmarking.avgCashFlow || 0.6
          ],
          name: '行业均值',
          areaStyle: { color: 'rgba(103, 194, 58, 0.2)' },
          lineStyle: { color: '#67C23A', type: 'dashed' }
        }
      ]
    }]
  }
  
  chart.setOption(option)
}

const initShapChart = (row) => {
  if (!shapChartRef.value || !row.shapValues) return
  
  // 销毁旧实例防止冲突
  const existingInstance = echarts.getInstanceByDom(shapChartRef.value)
  if (existingInstance) {
    existingInstance.dispose()
  }
  
  const chart = echarts.init(shapChartRef.value)
  const shapData = JSON.parse(row.shapValues)
  
  // 转换为数组并排序
  const data = Object.entries(shapData)
    .map(([name, value]) => ({ name, value }))
    .sort((a, b) => Math.abs(a.value) - Math.abs(b.value))

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const item = params[0]
        const val = item.value.toFixed(4)
        const direction = item.value > 0 ? '增加风险' : '降低风险'
        return `${item.name}<br/>贡献度: <b>${val}</b> (${direction})`
      }
    },
    grid: {
      left: '3%',
      right: '10%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      name: '贡献度'
    },
    yAxis: {
      type: 'category',
      data: data.map(item => item.name),
      axisLabel: { interval: 0, fontSize: 12 }
    },
    series: [
      {
        name: 'SHAP值',
        type: 'bar',
        data: data.map(item => ({
          value: item.value,
          itemStyle: {
            color: item.value > 0 ? '#f56c6c' : '#67c23a'
          }
        }))
      }
    ]
  }
  
  chart.setOption(option)
  
  // 自动缩放
  window.addEventListener('resize', () => chart.resize())
}

onMounted(() => {
  fetchHistory()
})</script>

<style scoped>
.history-container {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.shap-chart-container {
  margin-top: 30px;
  border-top: 1px solid #eee;
  padding-top: 20px;
}
.chart-tip {
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
}
.empty-shap {
  padding: 40px 0;
  text-align: center;
}
</style>
