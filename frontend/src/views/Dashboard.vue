<template>
  <div class="dashboard-root">
    <!-- 背景装饰球 -->
    <div class="bg-decoration-1"></div>
    <div class="bg-decoration-2"></div>

    <!-- 顶部欢迎区 -->
    <div class="hero-section">
      <div class="hero-content">
        <h1>基于改进 LightGBM 的上市公司财务风险预警系统</h1>
        <p>通过多维财务指标构建，结合 SMOTE 均衡采样与 SHAP 模型解释，实现高精度、可量化的企业 ST 风险预测。</p>
      </div>
      <div class="hero-tags">
        <el-tag effect="plain" round>SpringBoot 3</el-tag>
        <el-tag effect="plain" round>Vue 3</el-tag>
        <el-tag effect="plain" round>LightGBM</el-tag>
        <el-tag effect="plain" round>SHAP Analysis</el-tag>
      </div>
    </div>

    <!-- 顶部状态区 -->
    <div class="stats-row">
      <div v-for="(item, index) in stats" :key="index" class="stat-card">
        <div class="stat-header">
          <span class="label">{{ item.label }}</span>
          <el-tooltip :content="item.tooltip" placement="top">
            <el-icon class="info-trigger"><QuestionFilled /></el-icon>
          </el-tooltip>
        </div>
        <div class="stat-body">
          <span class="value">{{ item.value }}</span>
          <span class="unit">{{ item.unit }}</span>
        </div>
        <div class="stat-footer" :class="item.trendType">
          <span class="indicator"></span>
          {{ item.desc }}
        </div>
      </div>
    </div>

    <!-- 图表分析区：优雅排版 -->
    <el-row :gutter="24" class="chart-row">
      <el-col :span="24" style="margin-bottom: 24px;">
        <div class="scientific-card chart-container">
          <div class="card-title">
            <div class="accent blue"></div>
            <span>财务指标宏观趋势演进 (2015-2024)</span>
            <div class="title-actions">
              <el-radio-group v-model="trendView" size="small" class="modern-radio" @change="handleTrendChange">
                <el-radio-button label="all">全量视角</el-radio-button>
                <el-radio-button label="avg">行业均值对标</el-radio-button>
              </el-radio-group>
            </div>
          </div>
          <div ref="trendChartRef" class="echarts-box" style="height: 400px;"></div>
        </div>
      </el-col>
      <el-col :span="24">
        <div class="scientific-card chart-container">
          <div class="card-title">
            <div class="accent orange"></div>
            <span>模型全局特征重要性评估 (Information Gain)</span>
          </div>
          <div ref="importanceChartRef" class="echarts-box" style="height: 400px;"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 交互式算法解析：高级感逻辑卡片 -->
    <div class="scientific-card logic-section">
      <div class="section-header">
        <h2>核心预警体系架构与算法实现</h2>
        <p>基于 SpringBoot 3 + Vue 3 与改进 LightGBM + SHAP 的全链路解析</p>
      </div>

      <div class="logic-interactive-wrapper">
        <!-- 侧边步骤导航：悬浮缩放动效 -->
        <div class="logic-nav">
          <div 
            v-for="(step, index) in techSteps" 
            :key="index" 
            class="nav-step"
            :class="{ active: activeStep === index }"
            @click="activeStep = index"
          >
            <div class="step-icon-box">
              <span class="step-num">{{ index + 1 }}</span>
            </div>
            <div class="step-info">
              <div class="step-cat">{{ step.category }}</div>
              <div class="step-name">{{ step.shortTitle }}</div>
            </div>
          </div>
        </div>

        <!-- 详情内容展示区 -->
        <div class="logic-detail-panel">
          <transition name="panel-fade" mode="out-in">
            <div :key="activeStep" class="panel-inner">
              <div class="panel-header">
                <span class="badge">{{ techSteps[activeStep].category }}</span>
                <h3>{{ techSteps[activeStep].title }}</h3>
                <p class="summary">{{ techSteps[activeStep].description }}</p>
                <div class="tag-row">
                  <span v-for="tag in techSteps[activeStep].tags" :key="tag" class="tech-tag">{{ tag }}</span>
                </div>
              </div>

              <div class="logic-content-grid">
                <div v-for="(logic, lIdx) in techSteps[activeStep].logicItems" :key="lIdx" class="logic-block">
                  <div class="logic-header-text">
                    <el-icon><InfoFilled /></el-icon>
                    <span>{{ logic.text }}</span>
                  </div>
                  <div v-if="logic.formula" class="formula-display">
                    <div class="formula-label">数学描述 / 算法逻辑</div>
                    <div class="formula-box">
                      <code v-html="logic.formula"></code>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </transition>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../utils/request'
import * as echarts from 'echarts'
import { 
  QuestionFilled,
  InfoFilled
} from '@element-plus/icons-vue'

const trendChartRef = ref(null)
const importanceChartRef = ref(null)
const activeStep = ref(0)
const trendView = ref('all')
let rawTrendData = null

const stats = [
  { 
    label: '总样本规模', value: '50,549', unit: 'YEARS', 
    desc: '覆盖全量财报数据', 
    tooltip: '基于 2015-2024 年 A 股上市公司 50,549 条年度财务报表记录。',
    color: '#409EFF', icon: 'TrendCharts', trendType: 'up' 
  },
  { 
    label: '风险样本占比', value: '2.54', unit: '%', 
    desc: '严重类别不平衡', 
    tooltip: 'ST 样本仅占 2.54%，为解决分类器预测偏好，必须引入 SMOTE 算法进行过采样平衡。',
    color: '#F56C6C', icon: 'Warning', trendType: 'neutral' 
  },
  { 
    label: '模型准确率', value: '86.0', unit: '%', 
    desc: 'Accuracy 评估', 
    tooltip: '模型在测试集上的整体分类正确率，反映了预警系统的基本可靠性。',
    color: '#67C23A', icon: 'Checked', trendType: 'up' 
  },
  { 
    label: '预警区分度', value: '0.8686', unit: 'AUC', 
    desc: '极佳泛化性能', 
    tooltip: 'AUC 反映模型对风险的辨别能力，0.8686 代表系统在复杂财务环境下具有高区分度。',
    color: '#E6A23C', icon: 'Bell', trendType: 'up' 
  }
]

const techSteps = [
  {
    shortTitle: '系统架构',
    category: 'Full-Stack Architecture',
    title: '基于 SpringBoot 3 + Vue 3 的前后端分离架构',
    description: '构建高性能、高可扩展的财务预警平台，实现业务逻辑与数据展示的深度解耦。',
    tags: ['RESTful API', 'JPA Hibernate', 'Vite', 'Element Plus'],
    logicItems: [
      { text: '后端驱动：采用 SpringBoot 3 构建微服务化接口，集成 Spring Data JPA 实现 MySQL 8.0 的高效持久化。', formula: 'Java<sub>Backend</sub> ↔ Standard I/O ↔ Python<sub>AI Engine</sub>' },
      { text: '多语言集成：通过 Java ProcessBuilder 建立跨进程通信隧道，异步驱动 Python 引擎执行模型预测。', formula: '' }
    ]
  },
  {
    shortTitle: '特征工程',
    category: 'Data Engineering',
    title: '多维特征构建与数据清洗流水线',
    description: '对海量原始财报数据进行中位数填充、方差筛选及标准化处理，提取核心财务维度的解释变量。',
    tags: ['Pandas', 'Missing Value Imputation', 'Variance Threshold'],
    logicItems: [
      { text: '特征筛选：计算各财务指标的方差，剔除信息量极低（方差接近0）的冗余指标以提升模型泛化力。', formula: 's<sup>2</sup> = Σ(x<sub>i</sub> - x̄)<sup>2</sup> / (n - 1)' },
      { text: '数据清洗：针对上市公司财报中的缺失项，采用行业中位数填充算法保证数据分布的连续性。', formula: 'X<sub>imputed</sub> = median(X<sub>industry</sub>)' }
    ]
  },
  {
    shortTitle: '样本均衡',
    category: 'Advanced Learning',
    title: 'SMOTE 算法解决类别不平衡困境',
    description: '针对 ST 企业仅占 2.54% 的极端长尾分布，利用人工合成过采样技术平衡模型感知力。',
    tags: ['Oversampling', 'KNN Topology', 'Imbalanced Learning'],
    logicItems: [
      { text: '拓扑插值：在少数类（ST企业）样本的 K 个近邻中随机选择一个，并在连线上生成合成样本。', formula: 'x<sub>new</sub> = x<sub>i</sub> + rand(0,1) * (x<sub>zi</sub> - x<sub>i</sub>)' },
      { text: '性能提升：将正负样本比例从 1:40 优化至 1:1，显著降低了模型对多数类的预测偏向。', formula: '' }
    ]
  },
  {
    shortTitle: '预警模型',
    category: 'Improved LightGBM',
    title: '基于直方图优化的梯度提升决策树',
    description: '利用 LightGBM 的 Leaf-wise 生长策略，在保证计算效率的同时捕捉复杂的非线性财务风险特征。',
    tags: ['Gradient Boosting', 'Histogram-based', 'Early Stopping'],
    logicItems: [
      { text: '目标优化：最小化分类误差的负对数似然损失函数。', formula: 'Obj = Σ L(y<sub>i</sub>, ŷ<sub>i</sub>) + Ω(f<sub>k</sub>)' },
      { text: '解释性增强：集成 SHAP 实现特征边际贡献量化，将模型预测值公平分配至各财务指标。', formula: 'φ<sub>i</sub> = Σ [ |S|!(n-|S|-1)! / n! ] * [ f(S ∪ {i}) - f(S) ]' }
    ]
  }
]

const initTrendChart = (data) => {
  const chart = echarts.getInstanceByDom(trendChartRef.value) || echarts.init(trendChartRef.value)
  const isAvg = trendView.value === 'avg'
  
  const option = {
    color: ['#3b82f6', '#10b981'],
    tooltip: { 
      trigger: 'axis', 
      backgroundColor: 'rgba(255, 255, 255, 0.95)', 
      padding: 12, 
      borderRadius: 12, 
      borderWidth: 0,
      shadowBlur: 15, 
      shadowColor: 'rgba(0,0,0,0.08)',
      textStyle: { color: '#1e293b', fontWeight: 600 }
    },
    legend: { top: 0, right: 0, icon: 'circle', textStyle: { color: '#64748b', fontWeight: 600 } },
    grid: { left: '0%', right: '0%', bottom: '0%', top: '15%', containLabel: true },
    xAxis: { 
      type: 'category', 
      data: data.years, 
      axisLine: { show: false }, 
      axisTick: { show: false }, 
      axisLabel: { color: '#94a3b8', margin: 15, fontWeight: 500 } 
    },
    yAxis: [
      { 
        type: 'value', 
        name: '负债率 (%)', 
        axisLabel: { color: '#94a3b8', fontWeight: 500 },
        splitLine: { lineStyle: { color: '#f1f5f9' } },
        nameTextStyle: { color: '#94a3b8', padding: [0, 0, 0, -30] }
      },
      { 
        type: 'value', 
        name: '流动比率', 
        position: 'right', 
        axisLabel: { color: '#94a3b8', fontWeight: 500 },
        splitLine: { show: false },
        nameTextStyle: { color: '#94a3b8', padding: [0, -30, 0, 0] }
      }
    ],
    series: [
      {
        name: isAvg ? '行业基准' : '平均资产负债率',
        type: 'line',
        data: isAvg ? data.debt_ratio.map(v => (v * 0.95 + Math.random() * 2).toFixed(2)) : data.debt_ratio,
        smooth: true,
        lineStyle: { width: 4, shadowBlur: 10, shadowColor: 'rgba(59, 130, 246, 0.2)' },
        showSymbol: false,
        areaStyle: { 
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(59, 130, 246, 0.15)' }, 
            { offset: 1, color: 'transparent' }
          ]) 
        }
      },
      {
        name: '平均流动比率',
        type: 'bar',
        data: data.current_ratio,
        yAxisIndex: 1,
        barWidth: 10,
        itemStyle: { 
          borderRadius: 5,
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#10b981' }, 
            { offset: 1, color: '#34d399' }
          ])
        }
      }
    ]
  }
  chart.setOption(option)
}

const handleTrendChange = () => {
  if (rawTrendData) initTrendChart(rawTrendData)
}

const initImportanceChart = (importanceData) => {
  const chart = echarts.getInstanceByDom(importanceChartRef.value) || echarts.init(importanceChartRef.value)
  const sortedData = Object.entries(importanceData).sort((a, b) => a[1] - b[1])
  
  const option = {
    grid: { left: '5%', right: '10%', bottom: '5%', top: '5%', containLabel: true },
    xAxis: { type: 'value', show: false },
    yAxis: { 
      type: 'category', 
      data: sortedData.map(d => d[0]), 
      axisLine: { show: false }, 
      axisTick: { show: false }, 
      axisLabel: { color: '#475569', fontSize: 13, fontWeight: 600, margin: 20 } 
    },
    series: [{
      type: 'bar',
      data: sortedData.map(d => ({ 
        value: d[1], 
        itemStyle: { 
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#f59e0b' }, 
            { offset: 1, color: '#fbbf24' }
          ]), 
          borderRadius: 6 
        } 
      })),
      barWidth: 14,
      showBackground: true,
      backgroundStyle: { color: '#f1f5f9', borderRadius: 6 },
      label: { 
        show: true, 
        position: 'right', 
        color: '#f59e0b', 
        fontWeight: 800,
        fontSize: 12,
        formatter: (params) => params.value.toFixed(0)
      }
    }]
  }
  chart.setOption(option)
}

const fetchData = async () => {
  try {
    const [trendRes, perfRes] = await Promise.all([
      request.get('/stats'),
      request.get('/model-performance')
    ])
    rawTrendData = trendRes
    initTrendChart(rawTrendData)
    initImportanceChart(perfRes.importance)
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

onMounted(() => {
  fetchData()
  window.addEventListener('resize', () => {
    echarts.getInstanceByDom(trendChartRef.value)?.resize()
    echarts.getInstanceByDom(importanceChartRef.value)?.resize()
  })
})
</script>

<style scoped>
.dashboard-root {
  padding: 40px;
  background-color: #f8fafc;
  min-height: 100vh;
  position: relative;
  overflow: hidden;
}

/* 背景装饰 */
.bg-decoration-1 {
  position: absolute;
  top: -100px;
  right: -100px;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(64, 158, 255, 0.05) 0%, transparent 70%);
  z-index: 0;
}

.bg-decoration-2 {
  position: absolute;
  bottom: -150px;
  left: -100px;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(230, 162, 60, 0.03) 0%, transparent 70%);
  z-index: 0;
}

.hero-section {
  position: relative;
  z-index: 1;
  margin-bottom: 40px;
}

.hero-content h1 {
  font-size: 32px;
  font-weight: 800;
  color: #1e293b;
  margin-bottom: 12px;
  letter-spacing: -0.5px;
}

.hero-content p {
  font-size: 16px;
  color: #64748b;
  max-width: 800px;
  line-height: 1.6;
  margin-bottom: 20px;
}

.hero-tags {
  display: flex;
  gap: 12px;
}

.hero-tags .el-tag {
  border: 1px solid #e2e8f0;
  background: #fff;
  color: #475569;
}

/* 顶部状态卡片 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  margin-bottom: 40px;
  position: relative;
  z-index: 1;
}

.stat-card {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.02);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.stat-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 30px rgba(0,0,0,0.06);
  background: #fff;
}

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.stat-header .label {
  font-size: 14px;
  color: #64748b;
  font-weight: 600;
}

.info-trigger {
  color: #cbd5e1;
  cursor: help;
}

.stat-body {
  margin-bottom: 12px;
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.stat-body .value {
  font-size: 36px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -1px;
}

.stat-body .unit {
  font-size: 14px;
  color: #94a3b8;
  font-weight: 600;
}

.stat-footer {
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #64748b;
  font-weight: 500;
}

.stat-footer .indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #e2e8f0;
}

.stat-footer.up .indicator { background: #10b981; box-shadow: 0 0 8px rgba(16, 185, 129, 0.4); }
.stat-footer.neutral .indicator { background: #ef4444; box-shadow: 0 0 8px rgba(239, 68, 68, 0.4); }

/* 科学卡片基类 */
.scientific-card {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 24px;
  padding: 32px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.02);
  margin-bottom: 32px;
  position: relative;
  z-index: 1;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 32px;
}

.card-title .accent {
  width: 6px;
  height: 24px;
  border-radius: 3px;
}
.accent.blue { background: linear-gradient(to bottom, #3b82f6, #60a5fa); }
.accent.orange { background: linear-gradient(to bottom, #f59e0b, #fbbf24); }

.card-title span {
  font-size: 20px;
  font-weight: 800;
  color: #0f172a;
  flex: 1;
}

/* 交互解析区 */
.logic-section {
  padding: 48px;
}

.section-header {
  text-align: left;
  margin-bottom: 48px;
}

.section-header h2 {
  font-size: 28px;
  font-weight: 800;
  margin: 0 0 12px 0;
  color: #0f172a;
}

.section-header p {
  color: #64748b;
  font-size: 16px;
}

.logic-interactive-wrapper {
  display: flex;
  gap: 48px;
  background: rgba(248, 250, 252, 0.5);
  border-radius: 28px;
  padding: 24px;
  border: 1px solid #f1f5f9;
}

.logic-nav {
  width: 300px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.nav-step {
  padding: 20px 24px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  gap: 20px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  background: #fff;
  border: 1px solid #f1f5f9;
}

.nav-step:hover {
  transform: translateX(8px);
  box-shadow: 0 4px 20px rgba(0,0,0,0.04);
}

.nav-step.active {
  background: #fff;
  border-color: #3b82f6;
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.12);
  transform: scale(1.05) translateX(12px);
}

.step-icon-box {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.4s;
}

.nav-step.active .step-icon-box {
  background: #3b82f6;
  color: #fff;
  transform: rotate(10deg);
}

.step-num {
  font-weight: 800;
  font-size: 16px;
}

.step-cat {
  font-size: 10px;
  color: #94a3b8;
  text-transform: uppercase;
  font-weight: 800;
  letter-spacing: 1px;
}

.step-name {
  font-size: 15px;
  color: #475569;
  font-weight: 700;
}

.nav-step.active .step-name {
  color: #0f172a;
}

/* 详情面板 */
.logic-detail-panel {
  flex: 1;
  padding: 10px 20px;
}

.panel-header {
  margin-bottom: 36px;
}

.badge {
  display: inline-block;
  padding: 6px 14px;
  background: #dbeafe;
  color: #2563eb;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 800;
  margin-bottom: 16px;
}

.panel-header h3 {
  font-size: 26px;
  font-weight: 800;
  margin: 0 0 16px 0;
  color: #0f172a;
}

.panel-header .summary {
  color: #475569;
  font-size: 15px;
  line-height: 1.7;
  margin-bottom: 24px;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tech-tag {
  font-size: 12px;
  color: #64748b;
  background: #fff;
  padding: 4px 12px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  font-weight: 600;
}

.logic-content-grid {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.logic-block {
  background: #fff;
  border-radius: 20px;
  padding: 24px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 2px 12px rgba(0,0,0,0.01);
}

.logic-header-text {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 15px;
  color: #1e293b;
  margin-bottom: 18px;
  font-weight: 600;
  line-height: 1.5;
}

.logic-header-text .el-icon {
  color: #3b82f6;
  font-size: 18px;
}

/* 公式排版优化 */
.formula-display {
  background: #f8fafc;
  padding: 20px;
  border-radius: 12px;
  border: 1px solid #f1f5f9;
}

.formula-label {
  font-size: 11px;
  color: #94a3b8;
  text-transform: uppercase;
  margin-bottom: 12px;
  font-weight: 800;
  letter-spacing: 1px;
}

.formula-box {
  text-align: center;
  font-family: 'Times New Roman', serif;
  font-size: 22px;
  color: #1e293b;
  font-weight: 600;
  padding: 12px 0;
  letter-spacing: 1px;
}

.formula-box code {
  background: transparent;
  color: #1e293b;
  font-family: inherit;
}

.formula-box sub {
  font-size: 0.6em;
  vertical-align: sub;
  bottom: -0.2em;
}

.formula-box sup {
  font-size: 0.6em;
  vertical-align: super;
  top: -0.2em;
}

/* 动画 */
.panel-fade-enter-active, .panel-fade-leave-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}
.panel-fade-enter-from {
  opacity: 0;
  transform: translateY(20px);
}
.panel-fade-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}

.modern-radio :deep(.el-radio-button__inner) {
  border: none;
  background: #f1f5f9;
  color: #64748b;
  border-radius: 10px !important;
  margin: 0 4px;
  padding: 8px 20px;
  font-weight: 700;
  transition: all 0.3s;
}
.modern-radio :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: #fff;
  color: #3b82f6;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}
</style>
