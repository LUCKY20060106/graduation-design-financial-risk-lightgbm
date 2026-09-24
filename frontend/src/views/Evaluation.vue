<template>
  <div class="evaluation-container">
    <!-- 第一行：核心 KPI 指标卡片 -->
    <el-row :gutter="20" class="kpi-row">
      <el-col :span="6" v-for="(val, key) in metrics?.overall" :key="key">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-content">
            <div class="kpi-label">{{ formatLabel(key) }}</div>
            <div class="kpi-value" :class="key">{{ (val * 100).toFixed(2) }}%</div>
          </div>
          <div class="kpi-footer">模型实时评估结果</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第二行：三图对齐布局 -->
    <el-row :gutter="20" class="charts-row">
      <!-- 混淆矩阵 -->
      <el-col :span="8">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-header">
              <el-icon><Grid /></el-icon>
              <span>混淆矩阵 (Confusion Matrix)</span>
            </div>
          </template>
          <div ref="cmChartRef" style="height: 350px"></div>
          <div class="chart-tip">准确识别风险样本的能力直观展示</div>
        </el-card>
      </el-col>

      <!-- ROC 曲线 -->
      <el-col :span="8">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-header">
              <el-icon><DataLine /></el-icon>
              <span>ROC 曲线 (AUC={{ metrics?.auc.toFixed(3) }})</span>
            </div>
          </template>
          <div ref="rocChartRef" style="height: 350px"></div>
          <div class="chart-tip">曲线越趋向左上角，识别精度越高</div>
        </el-card>
      </el-col>

      <!-- 模型对比实验室 -->
      <el-col :span="8">
        <el-card shadow="never" class="chart-card comparison-card">
          <template #header>
            <div class="chart-header">
              <el-icon><Medal /></el-icon>
              <span>模型对比实验室 (Compare)</span>
            </div>
          </template>
          <div ref="comparisonChartRef" style="height: 350px"></div>
          <div class="chart-tip">LightGBM 与主流算法性能博弈</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第三行：详细分类报告 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card shadow="never">
          <template #header>
            <div class="chart-header">
              <el-icon><Memo /></el-icon>
              <span>详细分类报告 (Classification Report)</span>
            </div>
          </template>
          <el-table v-if="reportData.length" :data="reportData" border stripe class="modern-table">
            <el-table-column prop="class" label="评估维度" width="180">
              <template #default="scope">
                <span class="class-name">{{ scope.row.class }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="precision" label="精确率 (Precision)">
              <template #default="scope">
                <el-progress :percentage="Math.round(scope.row.precision * 100)" :stroke-width="8" color="#409EFF" />
              </template>
            </el-table-column>
            <el-table-column prop="recall" label="召回率 (Recall)">
              <template #default="scope">
                <el-progress :percentage="Math.round(scope.row.recall * 100)" :stroke-width="8" color="#67C23A" />
              </template>
            </el-table-column>
            <el-table-column prop="f1" label="F1-Score">
              <template #default="scope">
                <el-progress :percentage="Math.round(scope.row.f1 * 100)" :stroke-width="8" color="#E6A23C" />
              </template>
            </el-table-column>
            <el-table-column prop="support" label="测试样本量" width="120" align="center" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import request from '../utils/request'
import * as echarts from 'echarts'
import { Grid, DataLine, Medal, Memo } from '@element-plus/icons-vue'

const metrics = ref(null)
const reportData = ref([])
const rocChartRef = ref(null)
const cmChartRef = ref(null)
const comparisonChartRef = ref(null)
const comparisonData = ref(null)

const fetchMetrics = async () => {
  try {
    const [metricsRes, comparisonRes] = await Promise.all([
      request.get('/evaluation/metrics'),
      request.get('/evaluation/comparison')
    ])
    
    metrics.value = metricsRes
    comparisonData.value = comparisonRes
    
    // 转换分类报告数据
    const report = metricsRes.classification_report
    reportData.value = [
      { class: '正常 (0)', ...extractMetrics(report['0'] || report['0.0']) },
      { class: '风险 (1)', ...extractMetrics(report['1'] || report['1.0']) },
      { class: '宏平均 (Macro Avg)', ...extractMetrics(report['macro avg']) },
      { class: '加权平均 (Weighted Avg)', ...extractMetrics(report['weighted avg']) }
    ]

    await nextTick()
    initRocChart()
    initCmChart()
    initComparisonChart()
  } catch (error) {
    console.error('获取评估数据失败', error)
  }
}

const extractMetrics = (obj) => ({
  precision: obj.precision,
  recall: obj.recall,
  f1: obj['f1-score'],
  support: obj.support
})

const formatLabel = (key) => {
  const map = { accuracy: '准确率', precision: '精确率', recall: '召回率', f1_score: 'F1得分' }
  return map[key] || key
}

const initRocChart = () => {
  const chart = echarts.init(rocChartRef.value)
  const option = {
    tooltip: { trigger: 'axis' },
    grid: { bottom: '15%', top: '10%', containLabel: true },
    xAxis: { type: 'value', name: '假正率 (FPR)', min: 0, max: 1 },
    yAxis: { type: 'value', name: '真正率 (TPR)', min: 0, max: 1 },
    series: [{
      data: metrics.value.roc_curve.fpr.map((f, i) => [f, metrics.value.roc_curve.tpr[i]]),
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.1 },
      lineStyle: { width: 3, color: '#409EFF' }
    }, {
      data: [[0, 0], [1, 1]],
      type: 'line',
      lineStyle: { type: 'dashed', color: '#999' },
      symbol: 'none'
    }]
  }
  chart.setOption(option)
}

const initCmChart = () => {
  const chart = echarts.init(cmChartRef.value)
  const cm = metrics.value.confusion_matrix
  const option = {
    tooltip: { position: 'top' },
    grid: { height: '60%', top: '10%', bottom: '25%', containLabel: true },
    xAxis: { type: 'category', data: ['预测正常', '预测风险'], name: '预测值' },
    yAxis: { type: 'category', data: ['真实正常', '真实风险'], name: '真实值' },
    visualMap: { min: 0, max: Math.max(...cm.flat()), calculable: true, orient: 'horizontal', left: 'center', bottom: '0%' },
    series: [{
      name: '样本数',
      type: 'heatmap',
      data: [
        [0, 0, cm[0][0]], [1, 0, cm[0][1]],
        [0, 1, cm[1][0]], [1, 1, cm[1][1]]
      ],
      label: { show: true }
    }]
  }
  chart.setOption(option)
}

const initComparisonChart = () => {
  if (!comparisonChartRef.value || !comparisonData.value) return
  const chart = echarts.init(comparisonChartRef.value)
  
  const colors = ['#409EFF', '#67C23A', '#E6A23C']
  const series = Object.entries(comparisonData.value).map(([name, data], idx) => ({
    name: `${name} (AUC=${data.auc.toFixed(3)})`,
    type: 'line',
    smooth: true,
    symbol: 'none',
    lineStyle: { width: 2, color: colors[idx] },
    data: data.fpr.map((f, i) => [f, data.tpr[i]])
  }))

  const option = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { 
      bottom: '0%', 
      left: 'center',
      itemGap: 8,
      textStyle: { fontSize: 10 }
    },
    grid: { bottom: '25%', top: '10%', containLabel: true },
    xAxis: { type: 'value', name: '假正率 (FPR)', min: 0, max: 1 },
    yAxis: { type: 'value', name: '真正率 (TPR)', min: 0, max: 1 },
    series: [
      ...series,
      {
        name: '随机基准',
        type: 'line',
        lineStyle: { type: 'dashed', color: '#999' },
        symbol: 'none',
        data: [[0, 0], [1, 1]]
      }
    ]
  }
  chart.setOption(option)
}

onMounted(() => {
  fetchMetrics()
})
</script>

<style scoped>
.evaluation-container {
  padding: 24px;
  background-color: #f8fafc;
  min-height: 100vh;
}

.kpi-row {
  margin-bottom: 24px;
}

.kpi-card {
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #ffffff 0%, #f1f5f9 100%);
}

.kpi-content {
  padding: 10px 0;
}

.kpi-label {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 8px;
}

.kpi-value {
  font-size: 32px;
  font-weight: 800;
  letter-spacing: -1px;
}

.kpi-value.accuracy { color: #409EFF; }
.kpi-value.precision { color: #67C23A; }
.kpi-value.recall { color: #E6A23C; }
.kpi-value.f1_score { color: #F56C6C; }

.kpi-footer {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 12px;
  border-top: 1px solid #e2e8f0;
  padding-top: 8px;
}

.chart-card {
  border-radius: 12px;
  margin-bottom: 0;
}

.chart-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  color: #1e293b;
}

.chart-header .el-icon {
  color: #409EFF;
  font-size: 18px;
}

.chart-tip {
  font-size: 12px;
  color: #94a3b8;
  text-align: center;
  margin-top: 10px;
  font-style: italic;
}

.modern-table {
  border-radius: 8px;
  overflow: hidden;
}

.class-name {
  font-weight: 600;
  color: #475569;
}

:deep(.el-card__header) {
  padding: 15px 20px;
  background-color: #fcfdfe;
  border-bottom: 1px solid #f1f5f9;
}
</style>
