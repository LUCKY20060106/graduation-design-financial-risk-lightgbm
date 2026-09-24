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
          <el-button @click="resetForm">重置所有数据</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 预测结果展示 (Dashboard 布局) -->
    <transition name="el-zoom-in-top">
      <div v-if="predictionResult" class="result-dashboard" ref="resultDashboardRef">
        <el-card class="dashboard-card" v-loading="loading">
          <template #header>
            <div class="card-header">
              <span class="dashboard-title"><el-icon><data-analysis /></el-icon> 智能风险诊断报告</span>
              <div class="header-actions">
                <el-button type="success" plain size="small" @click="handleExportReport(predictionResult.id)">
                  <el-icon><download /></el-icon> 导出 PDF 深度报告
                </el-button>
                <el-button type="info" plain size="small" @click="scrollToForm">
                  <el-icon><top /></el-icon> 返回修改参数
                </el-button>
              </div>
            </div>
          </template>

          <el-row :gutter="20">
            <!-- 左侧：核心概览与 AI 诊断 -->
            <el-col :span="7">
              <div class="side-panel">
                <div class="risk-kpi-card" :class="predictionResult.level">
                  <div class="kpi-label">当前风险等级</div>
                  <div class="kpi-value">{{ predictionResult.level }}</div>
                  <div class="kpi-prob">预测概率: {{ (predictionResult.risk_probability * 100).toFixed(2) }}%</div>
                  <div class="kpi-status">{{ predictionResult.is_risk ? '建议重点关注' : '财务状况健康' }}</div>
                </div>

                <!-- 智能医生诊断 -->
                <div class="doctor-diagnosis section-card">
                  <div class="section-title">
                    <el-icon><first-aid-kit /></el-icon> 智能财务医生诊断
                  </div>
                  <div class="diagnosis-content">
                    <div v-for="(advice, index) in diagnosisAdvices" :key="index" class="advice-item">
                      <el-tag :type="advice.type" size="small" effect="dark">{{ advice.tag }}</el-tag>
                      <span class="advice-text">{{ advice.text }}</span>
                    </div>
                  </div>
                  <div class="overall-suggestion" v-if="predictionResult.level !== '低风险'">
                    <strong>核心整改建议：</strong>
                    <p>{{ coreSuggestion }}</p>
                  </div>
                </div>

                <!-- AI 专家深度点评 -->
                <div class="ai-diagnosis section-card">
                  <div class="section-title">
                    <el-icon><chat-dot-round /></el-icon> AI 专家深度点评
                  </div>
                  <div class="ai-content">
                    <div v-if="aiCommentary" class="ai-text-box">
                      {{ aiCommentary }}
                    </div>
                    <div v-else class="ai-action">
                      <el-button 
                        type="primary" 
                        size="small" 
                        :loading="aiLoading" 
                        @click="fetchAiCommentary"
                      >
                        获取 AI 专家点评
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>
            </el-col>
            
            <!-- 右侧：可视化图表 -->
            <el-col :span="17">
              <el-row :gutter="20">
                <el-col :span="12">
                  <div class="chart-card section-card">
                    <div class="section-title">特征贡献度诊断 (SHAP Value)</div>
                    <p class="chart-tip">正向(红色)增加风险，负向(绿色)降低风险</p>
                    <div ref="shapChartRef" style="height: 400px; width: 100%;"></div>
                  </div>
                </el-col>
                <el-col :span="12">
                  <div class="chart-card section-card">
                    <div class="section-title">行业对标分析</div>
                    <p class="chart-tip">对比该企业与行业均值的维度偏差</p>
                    <div v-if="predictionResult.industry_benchmarking" ref="radarChartRef" style="height: 400px; width: 100%;"></div>
                    <el-empty v-else description="暂无该行业对标数据" :image-size="100" />
                  </div>
                </el-col>
              </el-row>

              <!-- 敏感度模拟器 -->
              <div class="simulation-card section-card">
                <div class="section-title">
                  <el-icon><operation /></el-icon> 风险敏感度模拟器 (What-if Analysis)
                </div>
                <div class="simulation-body">
                  <el-row :gutter="40">
                    <el-col :span="12">
                      <div class="simulation-sliders">
                        <div v-for="field in topSimFields" :key="field.key" class="slider-item">
                          <div class="slider-label">
                            <span>{{ field.label }}</span>
                            <el-tag size="small" type="info">{{ (simForm[field.key] || 0).toFixed(2) }}</el-tag>
                          </div>
                          <el-slider 
                            v-model="simForm[field.key]" 
                            :min="field.min" 
                            :max="field.max" 
                            :step="0.01"
                            @input="handleSimulate"
                          />
                        </div>
                      </div>
                    </el-col>
                    <el-col :span="12">
                      <div class="simulation-result">
                        <div class="gauge-container" ref="simGaugeRef" style="height: 250px;"></div>
                        <div class="sim-comparison">
                          <div class="sim-card original">
                            <span class="label">原始风险</span>
                            <span class="value">{{ (predictionResult.risk_probability * 100).toFixed(2) }}%</span>
                          </div>
                          <div class="sim-arrow"><el-icon><right /></el-icon></div>
                          <div class="sim-card simulated">
                            <span class="label">模拟风险</span>
                            <span class="value" :class="simRiskLevelClass">{{ ((simRiskProb || 0) * 100).toFixed(2) }}%</span>
                          </div>
                        </div>
                      </div>
                    </el-col>
                  </el-row>
                </div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </div>
    </transition>

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
          action="/api/predict/batch"
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
import request from '../utils/request'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { useRoute } from 'vue-router'
import { UploadFilled, Operation, Right, FirstAidKit, ChatDotRound, DataAnalysis, Download, Top } from '@element-plus/icons-vue'

// 手写简易 debounce 函数
function debounce(fn, delay) {
  let timer = null
  return function (...args) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      fn.apply(this, args)
    }, delay)
  }
}

const route = useRoute()
const loading = ref(false)
const predictionResult = ref(null)
const shapChartRef = ref(null)
const radarChartRef = ref(null)
const simGaugeRef = ref(null)
const resultDashboardRef = ref(null)
const targetStkcd = ref('')
const shapSummaryText = ref('')
const batchResults = ref([])
const diagnosisAdvices = ref([])
const coreSuggestion = ref('')
const aiCommentary = ref('')
const aiLoading = ref(false)

// 敏感度模拟器数据
const simRiskProb = ref(0)
const simForm = reactive({})
const topSimFields = ref([
  { key: 'F011201A', label: '资产负债率', min: 0, max: 1.5 },
  { key: 'F080601A', label: '总资产增长率', min: -0.5, max: 0.5 },
  { key: 'F010701B', label: '利息保障倍数', min: -5, max: 20 },
  { key: 'F010601A', label: '营运资金 (亿)', min: -10, max: 100, scale: 1e8 },
  { key: 'F011601A', label: '权益乘数', min: 0.5, max: 10 }
])

const simRiskLevelClass = computed(() => {
  if (simRiskProb.value > 0.7) return 'text-danger'
  if (simRiskProb.value > 0.3) return 'text-warning'
  return 'text-success'
})

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
  window.open(`/api/prediction/export/pdf/${id}`, '_blank')
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
  predictionResult.value = null
}

const scrollToForm = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
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

    const response = await request.post('/predict', { 
      features,
      stkcd: targetStkcd.value || '000001' 
    })
    
    if (response.error) {
      ElMessage.error('预测失败: ' + response.error)
    } else {
      predictionResult.value = response
      aiCommentary.value = '' // 重置 AI 点评
      
      // 生成智能诊断建议
      generateDiagnosis(response)
      
      // 生成 SHAP 智能摘要
      generateShapSummary(response.shap_analysis)
      
      // 等待 DOM 更新并显示图表
      await nextTick()
      
      // 平滑滚动到结果区域
      setTimeout(() => {
        if (resultDashboardRef.value) {
          resultDashboardRef.value.scrollIntoView({ behavior: 'smooth', block: 'start' })
        }
        initShapChart(response.data.shap_analysis)
        if (response.data.industry_benchmarking) {
          initRadarChart(response.data.industry_benchmarking)
        }
        initSimulation()
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

// 生成智能诊断建议
const generateDiagnosis = (result) => {
  const shap = result.shap_analysis
  const benchmark = result.industry_benchmarking
  const advices = []
  
  // 1. 偿债风险识别
  if (shap['资产负债率'] > 0.05) {
    advices.push({ type: 'danger', tag: '债务预警', text: '资产负债率过高，显著增加了财务杠杆风险。' })
  }
  if (shap['利息保障倍数'] > 0.05) {
    advices.push({ type: 'warning', tag: '利息压力', text: '利息保障倍数不足，可能面临短期债务违约。' })
  }
  
  // 2. 营运与增长识别
  if (shap['总资产增长率'] > 0.05) {
    advices.push({ type: 'danger', tag: '增长停滞', text: '资产增长动能严重不足，反映出业务扩张受阻。' })
  }
  if (shap['总资产周转率'] > 0.03) {
    advices.push({ type: 'warning', tag: '效率低下', text: '资产周转速度低于预期，资源利用效率有待提升。' })
  }
  
  // 3. 对标分析识别
  if (benchmark) {
    if (benchmark.avgSolvency > 0.7) {
      advices.push({ type: 'info', tag: '行业对标', text: '偿债能力明显弱于行业均值，存在结构性短板。' })
    }
  }

  // 兜底建议
  if (advices.length === 0) {
    advices.push({ type: 'success', tag: '表现良好', text: '各项核心财务指标均处于良性区间。' })
  }
  
  diagnosisAdvices.value = advices.slice(0, 4) // 最多显示4条
  
  // 核心整改建议
  if (result.level === '高风险') {
    coreSuggestion.value = '建议立即启动财务合规性审查，削减非核心业务开支，并寻求紧急融资或债务重组。'
  } else if (result.level === '中风险') {
    coreSuggestion.value = '建议优化现金流管理，收紧信用销售政策，并加强对子公司的财务审计频率。'
  } else {
    coreSuggestion.value = '继续保持稳健的财务政策，定期进行压力测试即可。'
  }
}

const fetchAiCommentary = async () => {
  if (!predictionResult.value) return
  
  aiLoading.value = true
  try {
    const response = await request.post('/ai/commentary', {
      id: predictionResult.value.id, // 传入 ID 以便保存
      level: predictionResult.value.level,
      risk_probability: predictionResult.value.risk_probability,
      shap_analysis: predictionResult.value.shap_analysis
    })
    aiCommentary.value = response.commentary
    // 更新本地结果中的点评，确保导出 PDF 时能包含它
    predictionResult.value.commentary = response.commentary
  } catch (error) {
    console.error('获取 AI 点评失败:', error)
    ElMessage.error('获取 AI 点评失败')
  } finally {
    aiLoading.value = false
  }
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

// 敏感度模拟器逻辑
const initSimulation = () => {
  // 复制当前表单数据到模拟表单
  topSimFields.value.forEach(field => {
    let val = form[field.key]
    if (field.scale) val = val / field.scale
    simForm[field.key] = val
  })
  simRiskProb.value = predictionResult.value.risk_probability
  
  nextTick(() => {
    updateSimGauge()
  })
}

const handleSimulate = debounce(async () => {
  // 构造模拟特征数组
  const simFeatures = [
    form.F010101A, form.F010201A, form.F010401A, form.F010601A, form.F010701B,
    form.F010801B, form.F010901B, form.F011201A, form.F011401A, form.F011601A,
    form.F040202B, form.F040502B, form.F041202B, form.F041702B,
    form.F080601A, form.F081001B, form.F081601B
  ]
  
  // 使用模拟值替换原值
  topSimFields.value.forEach(field => {
    const idx = [...solvencyFields, ...operationFields, ...growthFields].findIndex(f => f.key === field.key)
    if (idx !== -1) {
      let val = simForm[field.key]
      if (field.scale) val = val * field.scale
      simFeatures[idx] = val
    }
  })
  
  try {
    const response = await request.post('/predict', { 
      features: simFeatures,
      stkcd: targetStkcd.value || '000001'
    })
    simRiskProb.value = response.risk_probability
    updateSimGauge()
  } catch (error) {
    console.error('模拟预测失败:', error)
  }
}, 300)

const updateSimGauge = () => {
  if (!simGaugeRef.value) return
  
  let chart = echarts.getInstanceByDom(simGaugeRef.value)
  if (!chart) chart = echarts.init(simGaugeRef.value)
  
  const option = {
    series: [{
      type: 'gauge',
      startAngle: 180,
      endAngle: 0,
      min: 0,
      max: 1,
      splitNumber: 8,
      axisLine: {
        lineStyle: {
          width: 6,
          color: [
            [0.3, '#67C23A'],
            [0.7, '#E6A23C'],
            [1, '#F56C6C']
          ]
        }
      },
      pointer: { icon: 'path://M12.8,0.7l12,20.1c0.6,1,0.3,2.3-0.7,2.9c-0.3,0.2-0.6,0.3-0.9,0.3H1.4c-1.2,0-2.1-0.9-2.1-2.1c0-0.3,0.1-0.6,0.3-0.9l12-20.1C12,0.4,12.4,0.3,12.8,0.7z', length: '12%', width: 20, offsetCenter: [0, '-60%'], itemStyle: { color: 'auto' } },
      axisTick: { length: 12, lineStyle: { color: 'auto', width: 2 } },
      splitLine: { length: 20, lineStyle: { color: 'auto', width: 5 } },
      axisLabel: { color: '#464646', fontSize: 12, distance: -60, formatter: (value) => value === 0.8 ? '高风险' : value === 0.5 ? '中风险' : value === 0.2 ? '低风险' : '' },
      title: { offsetCenter: [0, '-20%'], fontSize: 16 },
      detail: { fontSize: 30, offsetCenter: [0, '0%'], valueAnimation: true, formatter: (value) => (value * 100).toFixed(2) + '%', color: 'inherit' },
      data: [{ value: simRiskProb.value, name: '模拟风险概率' }]
    }]
  }
  chart.setOption(option)
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
  
  // 生成智能诊断建议
  generateDiagnosis(result)
  
  // 生成智能摘要
  generateShapSummary(result.shap_analysis)
  
  // 渲染图表
  nextTick(() => {
    setTimeout(() => {
      if (resultDashboardRef.value) {
        resultDashboardRef.value.scrollIntoView({ behavior: 'smooth', block: 'start' })
      }
      initShapChart(result.shap_analysis)
      // 批量结果如果包含行业对标数据则渲染
      if (result.industry_benchmarking) {
        initRadarChart(result.industry_benchmarking)
      }
      initSimulation()
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
/* Dashboard 布局样式 */
.result-dashboard {
  margin-top: 25px;
  animation: fadeIn 0.5s ease-out;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
.dashboard-card {
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
}
.dashboard-title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}
.header-actions {
  display: flex;
  gap: 10px;
}
.section-card {
  background: #fff;
  border-radius: 8px;
  padding: 15px;
  border: 1px solid #ebeef5;
  margin-bottom: 20px;
}
.section-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 15px;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 左侧 KPI 卡片 */
.risk-kpi-card {
  padding: 25px;
  border-radius: 12px;
  text-align: center;
  margin-bottom: 20px;
  color: #fff;
  background: linear-gradient(135deg, #909399, #606266);
}
.risk-kpi-card.高风险 { background: linear-gradient(135deg, #f56c6c, #f78989); }
.risk-kpi-card.中风险 { background: linear-gradient(135deg, #e6a23c, #ebb563); }
.risk-kpi-card.低风险 { background: linear-gradient(135deg, #67c23a, #85ce61); }

.kpi-label { font-size: 14px; opacity: 0.9; margin-bottom: 8px; }
.kpi-value { font-size: 32px; font-weight: 800; margin-bottom: 8px; }
.kpi-prob { font-size: 14px; opacity: 0.9; margin-bottom: 12px; }
.kpi-status { 
  display: inline-block;
  padding: 4px 12px;
  background: rgba(255,255,255,0.2);
  border-radius: 20px;
  font-size: 12px;
}

/* 智能医生诊断样式 */
.doctor-diagnosis {
  background: #fffaf0;
  border-left: 4px solid #e6a23c;
}
.diagnosis-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.advice-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  line-height: 1.4;
}
.advice-text { color: #606266; }
.overall-suggestion {
  margin-top: 15px;
  padding-top: 12px;
  border-top: 1px dashed #e6a23c;
  font-size: 13px;
}
.overall-suggestion p {
  margin-top: 5px;
  color: #cf9236;
  font-style: italic;
}

/* AI 点评样式 */
.ai-diagnosis { background: #f0f7ff; }
.ai-text-box {
  padding: 12px;
  font-size: 13px;
  line-height: 1.6;
  color: #409eff;
  white-space: pre-line;
}
.ai-action { text-align: center; }

/* 图表与模拟器 */
.chart-tip { font-size: 12px; color: #909399; margin-bottom: 10px; }
.simulation-card { background: #fff; }
.slider-item { margin-bottom: 20px; }
.slider-label {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
}
.simulation-result {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.sim-comparison {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-top: -20px;
}
.sim-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 15px;
  border-radius: 8px;
  background: #f8f9fa;
  min-width: 90px;
}
.sim-card .label { font-size: 11px; color: #909399; }
.sim-card .value { font-size: 16px; font-weight: bold; }
.sim-arrow { font-size: 20px; color: #dcdfe6; }

.text-danger { color: #f56c6c; }
.text-warning { color: #e6a23c; }
.text-success { color: #67c23a; }

.batch-upload {
  padding: 20px;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  text-align: center;
  transition: border-color 0.3s;
}
.batch-upload:hover { border-color: #409eff; }
</style>
