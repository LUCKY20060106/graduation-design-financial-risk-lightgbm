<template>
  <div class="risk-prediction">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span>企业财务风险实时预测</span>
            <el-tag v-if="targetStkcd" type="info" class="stkcd-tag">当前目标: {{ targetStkcd }}</el-tag>
          </div>
          <el-button type="primary" @click="fillSampleData">填入示例数据</el-button>
        </div>
      </template>

      <el-form :model="form" label-width="200px" label-position="left">
        <el-row :gutter="20">
          <!-- 偿债能力 -->
          <el-col :span="24"><h4>偿债能力指标</h4></el-col>
          <el-col :span="8" v-for="item in solvencyFields" :key="item.key">
            <el-form-item :label="item.label">
              <el-input-number v-model="form[item.key]" :precision="4" :step="0.1" style="width: 100%" />
            </el-form-item>
          </el-col>

          <!-- 营运能力 -->
          <el-col :span="24"><h4>营运能力指标</h4></el-col>
          <el-col :span="8" v-for="item in operationFields" :key="item.key">
            <el-form-item :label="item.label">
              <el-input-number v-model="form[item.key]" :precision="4" :step="0.1" style="width: 100%" />
            </el-form-item>
          </el-col>

          <!-- 发展能力 -->
          <el-col :span="24"><h4>发展能力指标</h4></el-col>
          <el-col :span="8" v-for="item in growthFields" :key="item.key">
            <el-form-item :label="item.label">
              <el-input-number v-model="form[item.key]" :precision="4" :step="0.1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item>
          <el-button type="success" :loading="loading" @click="handlePredict" size="large">立即进行风险评估</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 预测结果展示 -->
    <el-dialog v-loading="loading" v-model="resultVisible" title="风险评估报告" width="85%" destroy-on-close>
      <div v-if="predictionResult" class="result-container">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-result
              :icon="getResultIcon"
              :title="predictionResult.level"
              :sub-title="'预测风险概率: ' + (predictionResult.risk_probability * 100).toFixed(2) + '%'"
            >
              <template #extra>
                <el-tag :type="getTagType" size="large">{{ predictionResult.is_risk ? '建议重点关注' : '财务状况健康' }}</el-tag>
              </template>
            </el-result>
            <div class="result-advice">
              <div v-if="predictionResult.level === '高风险'">
                <p><strong>诊断结论：</strong><el-tag type="danger">触发高危预警</el-tag></p>
                <p><strong>专家建议：</strong>该企业财务指标触发高风险预警，建议立即进行现场审计并收紧授信额度。</p>
              </div>
              <div v-else-if="predictionResult.level === '中风险'">
                <p><strong>诊断结论：</strong><el-tag type="warning">存在潜在波动</el-tag></p>
                <p><strong>专家建议：</strong>企业存在一定财务波动，建议增加关注频率，核实利润真实性。</p>
              </div>
              <div v-else>
                <p><strong>诊断结论：</strong><el-tag type="success">财务状况稳健</el-tag></p>
                <p><strong>专家建议：</strong>各项财务指标处于安全区间，维持正常监控即可。</p>
              </div>
              <div class="shap-summary" v-if="shapSummaryText">
                <el-divider content-position="left">智能诊断依据 (SHAP)</el-divider>
                <p class="summary-text">{{ shapSummaryText }}</p>
              </div>
              <div style="margin-top: 20px; text-align: center;">
                <el-button type="success" @click="handleExportReport(predictionResult.id)">导出 Excel 报告</el-button>
              </div>
            </div>
          </el-col>
          
          <el-col :span="9">
            <div class="shap-chart-container">
              <h3>特征贡献度诊断 (SHAP Value)</h3>
              <p class="chart-tip">正值(红色)增加风险，负值(绿色)降低风险</p>
              <div ref="shapChartRef" style="height: 450px; width: 100%;"></div>
            </div>
          </el-col>

          <el-col :span="9">
            <div class="benchmark-container">
              <h3>行业对标分析</h3>
              <p class="chart-tip">对比该企业与行业均值的维度偏差</p>
              <div v-if="predictionResult.industry_benchmarking" ref="radarChartRef" style="height: 400px; width: 100%;"></div>
              <el-empty v-else description="暂无该行业对标数据" :image-size="100">
                <template #extra>
                  <p style="font-size: 12px; color: #999;">请检查该股票代码是否已录入行业信息</p>
                </template>
              </el-empty>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-dialog>

    <!-- 批量风险扫描 -->
    <el-card style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span>批量风险扫描 (Excel 导入)</span>
          <el-button type="primary" link @click="downloadTemplate">下载导入模板</el-button>
        </div>
      </template>
      <div class="batch-upload">
        <el-upload
          drag
          action="http://localhost:8080/api/predict/batch"
          :on-success="handleBatchSuccess"
          :on-error="handleBatchError"
          :before-upload="beforeBatchUpload"
          :show-file-list="true"
          accept=".xlsx, .xls"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            将财务数据文件拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              请上传包含“股票代码”、“股票简称”及17项核心财务指标的 Excel 文件。
            </div>
          </template>
        </el-upload>
      </div>

      <!-- 批量扫描结果 -->
      <el-table v-if="batchResults.length > 0" :data="batchResults" stripe style="width: 100%; margin-top: 20px;">
        <el-table-column prop="stkcd" label="股票代码" width="120" />
        <el-table-column prop="short_name" label="企业简称" width="150" />
        <el-table-column label="风险概率" width="180">
          <template #default="scope">
            <el-progress 
              :percentage="Math.round(scope.row.risk_probability * 100)" 
              :color="getProgressColor(scope.row.risk_probability)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="level" label="风险等级" width="120">
          <template #default="scope">
            <el-tag :type="getTagTypeByLevel(scope.row.level)">{{ scope.row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="scope">
            <el-button size="small" type="primary" @click="viewBatchDetail(scope.row)">诊断详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { useRoute } from 'vue-router'
import { UploadFilled } from '@element-plus/icons-vue'

const route = useRoute()
const loading = ref(false)
const resultVisible = ref(false)
const predictionResult = ref(null)
const shapChartRef = ref(null)
const radarChartRef = ref(null)
const targetStkcd = ref('')
const shapSummaryText = ref('')
const batchResults = ref([])

onMounted(() => {
  if (route.query.stkcd) {
    targetStkcd.value = route.query.stkcd
  }
})

const handleExportReport = (id) => {
  if (!id) {
    // 如果是刚刚预测的结果，可能需要从 predictionResult 中获取 ID
    id = predictionResult.value?.id
  }
  if (!id) {
    ElMessage.warning('暂无记录ID，无法导出')
    return
  }
  window.open(`http://localhost:8080/api/prediction/export/${id}`, '_blank')
}

// 字段定义
const solvencyFields = [
  { key: 'F010101A', label: '流动比率' },
  { key: 'F010201A', label: '速动比率' },
  { key: 'F010401A', label: '现金比率' },
  { key: 'F010601A', label: '营运资金' },
  { key: 'F010701B', label: '利息保障倍数' },
  { key: 'F010801B', label: '经营净现金流/流动负债' },
  { key: 'F010901B', label: '现金流利息保障倍数' },
  { key: 'F011201A', label: '资产负债率' },
  { key: 'F011401A', label: '有形资产负债率' },
  { key: 'F011601A', label: '权益乘数' },
]

const operationFields = [
  { key: 'F040202B', label: '应收账款周转率' },
  { key: 'F040502B', label: '存货周转率' },
  { key: 'F041202B', label: '流动资产周转率' },
  { key: 'F041702B', label: '总资产周转率' },
]

const growthFields = [
  { key: 'F080601A', label: '总资产增长率' },
  { key: 'F081001B', label: '净利润增长率' },
  { key: 'F081601B', label: '营业收入增长率' },
]

// 表单数据 (17个特征)
const form = reactive({
  F010101A: 0, F010201A: 0, F010401A: 0, F010601A: 0, F010701B: 0,
  F010801B: 0, F010901B: 0, F011201A: 0, F011401A: 0, F011601A: 0,
  F040202B: 0, F040502B: 0, F041202B: 0, F041702B: 0,
  F080601A: 0, F081001B: 0, F081601B: 0
})

const fillSampleData = () => {
  // 填入一组典型的“低风险”数据
  Object.assign(form, {
    F010101A: 1.5, F010201A: 1.2, F010401A: 0.8, F010601A: 50000000, F010701B: 5.0,
    F010801B: 0.2, F010901B: 3.5, F011201A: 0.45, F011401A: 0.5, F011601A: 2.0,
    F040202B: 8.0, F040502B: 5.0, F041202B: 1.2, F041702B: 0.8,
    F080601A: 0.15, F081001B: 0.2, F081601B: 0.1
  })
  ElMessage.success('示例数据已填入')
}

const resetForm = () => {
  Object.keys(form).forEach(key => form[key] = 0)
}

const handlePredict = async () => {
  loading.value = true
  try {
    // 构造后端要求的特征数组
    const features = [
      form.F010101A, form.F010201A, form.F010401A, form.F010601A, form.F010701B,
      form.F010801B, form.F010901B, form.F011201A, form.F011401A, form.F011601A,
      form.F040202B, form.F040502B, form.F041202B, form.F041702B,
      form.F080601A, form.F081001B, form.F081601B
    ]

    const response = await axios.post('http://localhost:8080/api/predict', { 
      features,
      stkcd: targetStkcd.value || '000001' 
    })
    
    if (response.data.error) {
      ElMessage.error('预测失败: ' + response.data.error)
    } else {
      predictionResult.value = response.data
      resultVisible.value = true
      
      // 生成 SHAP 智能摘要
      generateShapSummary(response.data.shap_analysis)
      
      // 等待 DOM 更新并显示图表
      await nextTick()
      setTimeout(() => {
        console.log('Initializing charts...', response.data)
        initShapChart(response.data.shap_analysis)
        if (response.data.industry_benchmarking) {
          console.log('Industry benchmarking data found:', response.data.industry_benchmarking)
          initRadarChart(response.data.industry_benchmarking)
        } else {
          console.warn('No industry benchmarking data returned from server')
        }
      }, 500)
    }
  } catch (error) {
    console.error('预测请求失败:', error)
    const errorMsg = error.response?.data?.message || error.message || '网络错误或后端服务未启动'
    ElMessage.error('预测失败: ' + errorMsg)
  } finally {
    loading.value = false
  }
}

// 结果展示逻辑
const getResultIcon = computed(() => {
  if (!predictionResult.value) return 'info'
  if (predictionResult.value.level === '高风险') return 'error'
  if (predictionResult.value.level === '中风险') return 'warning'
  return 'success'
})

const getTagType = computed(() => {
  if (!predictionResult.value) return 'info'
  if (predictionResult.value.level === '高风险') return 'danger'
  if (predictionResult.value.level === '中风险') return 'warning'
  return 'success'
})

// 生成 SHAP 智能摘要
const generateShapSummary = (shapAnalysis) => {
  if (!shapAnalysis) return
  
  const entries = Object.entries(shapAnalysis)
  const positive = entries.filter(e => e[1] > 0).sort((a, b) => b[1] - a[1])
  const negative = entries.filter(e => e[1] < 0).sort((a, b) => a[1] - b[1])
  
  let text = '模型分析显示：'
  if (positive.length > 0) {
    text += `导致风险增加的主要因素是${positive.slice(0, 2).map(e => e[0]).join('和')}；`
  }
  if (negative.length > 0) {
    text += `而${negative.slice(0, 2).map(e => e[0]).join('和')}等指标表现较好，有效降低了整体风险。`
  }
  shapSummaryText.value = text
}

// 初始化雷达图 (行业对标)
const initRadarChart = (benchmarking) => {
  if (!radarChartRef.value) {
    console.warn('Radar chart container not found')
    return
  }
  
  // 销毁旧实例
  const existingInstance = echarts.getInstanceByDom(radarChartRef.value)
  if (existingInstance) {
    existingInstance.dispose()
  }

  const chart = echarts.init(radarChartRef.value)
  
  // 计算当前企业的核心维度评分 (基于表单输入进行标准化处理)
  // 这里做一个简单的归一化模拟，实际可根据业务公式调整
  const companyMetrics = [
    Math.min(form.F010101A / 2, 1), // 偿债能力 (以流动比率2.0为满分)
    Math.min(form.F041702B / 1.5, 1), // 营运能力 (以总资产周转率1.5为满分)
    Math.min(form.F080601A / 0.3, 1), // 发展能力 (以总资产增长率30%为满分)
    Math.min(form.F010701B / 10, 1), // 盈利/保障能力
    Math.min(form.F010801B * 2, 1)   // 现金流能力
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
      center: ['50%', '50%'],
      radius: '65%',
      shape: 'circle',
      axisName: { color: '#333', fontSize: 12 },
      splitLine: { lineStyle: { color: ['#eee'] } },
      splitArea: { show: true, areaStyle: { color: ['#fff', '#fafafa'] } }
    },
    legend: {
      data: ['该企业', '行业均值'],
      bottom: 0,
      textStyle: { fontSize: 12 }
    },
    series: [{
      type: 'radar',
      data: [
        {
          value: companyMetrics,
          name: '该企业',
          areaStyle: { color: 'rgba(64, 158, 255, 0.4)' },
          lineStyle: { color: '#409EFF', width: 2 },
          itemStyle: { color: '#409EFF' }
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
          lineStyle: { color: '#67C23A', type: 'dashed', width: 2 },
          itemStyle: { color: '#67C23A' }
        }
      ]
    }]
  }
  
  chart.setOption(option)
  window.addEventListener('resize', () => chart.resize())
}

// 初始化 SHAP 贡献度图表
const initShapChart = (shapAnalysis) => {
  if (!shapChartRef.value || !shapAnalysis) return
  
  // 销毁旧实例防止冲突
  const existingInstance = echarts.getInstanceByDom(shapChartRef.value)
  if (existingInstance) {
    existingInstance.dispose()
  }
  
  const chart = echarts.init(shapChartRef.value)
  
  // 转换为数组并排序
  const data = Object.entries(shapAnalysis)
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

// 批量扫描逻辑
const handleBatchSuccess = (response) => {
  if (response.error) {
    ElMessage.error(response.error)
  } else {
    batchResults.value = response.results
    ElMessage.success(`批量扫描完成，成功处理 ${response.processed} 条企业数据`)
  }
}

const handleBatchError = (err) => {
  ElMessage.error('批量扫描失败，请检查后端服务是否启动')
}

const beforeBatchUpload = (file) => {
  const isExcel = file.name.endsWith('.xlsx') || file.name.endsWith('.xls')
  if (!isExcel) {
    ElMessage.error('只能上传 Excel 文件!')
  }
  return isExcel
}

const getProgressColor = (score) => {
  if (score > 0.7) return '#f56c6c'
  if (score > 0.3) return '#e6a23c'
  return '#67c23a'
}

const getTagTypeByLevel = (level) => {
  if (level === '高风险') return 'danger'
  if (level === '中风险') return 'warning'
  return 'success'
}

const viewBatchDetail = (result) => {
  predictionResult.value = result
  resultVisible.value = true
  
  // 生成智能摘要
  generateShapSummary(result.shap_analysis)
  
  // 渲染图表
  nextTick(() => {
    setTimeout(() => {
      initShapChart(result.shap_analysis)
      // 批量结果如果包含行业对标数据则渲染
      if (result.industry_benchmarking) {
        initRadarChart(result.industry_benchmarking)
      }
    }, 500)
  })
}

const downloadTemplate = () => {
  // 简单的模板下载说明
  ElMessage.info('模板格式：第一行包含“股票代码”、“股票简称”及17项财务指标名称')
}
</script>

<style scoped>
.risk-prediction {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
}
.stkcd-tag {
  font-weight: normal;
}
h4 {
  margin: 20px 0 10px 0;
  padding-bottom: 5px;
  border-bottom: 1px solid #eee;
  color: #409EFF;
}
.result-container {
  text-align: center;
}
.result-advice {
  margin-top: 20px;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 4px;
  text-align: left;
  font-size: 14px;
}
.shap-summary {
  margin-top: 15px;
  color: #606266;
}
.summary-text {
  line-height: 1.6;
  font-style: italic;
}
</style>
