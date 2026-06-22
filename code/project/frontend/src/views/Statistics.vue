<template>
  <div class="statistics-container">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background: #409eff;">
            <el-icon size="30"><Document /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.totalReviews || 0 }}</div>
            <div class="stat-label">总审查次数</div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background: #67c23a;">
            <el-icon size="30"><User /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.totalUsers || 0 }}</div>
            <div class="stat-label">总用户数</div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background: #e6a23c;">
            <el-icon size="30"><QuestionFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.totalIssues || 0 }}</div>
            <div class="stat-label">发现问题数</div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background: #f56c6c;">
            <el-icon size="30"><Clock /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.todayReviews || 0 }}</div>
            <div class="stat-label">今日审查</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="16">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>审查趋势</span>
              <el-radio-group v-model="trendDays" size="small" @change="loadTrendData">
                <el-radio-button :value="7">近7天</el-radio-button>
                <el-radio-button :value="14">近14天</el-radio-button>
                <el-radio-button :value="30">近30天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="trendChartRef" style="width: 100%; height: 350px;"></div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card class="chart-card">
          <template #header>
            <span>问题分布</span>
          </template>
          <div ref="pieChartRef" style="width: 100%; height: 350px;"></div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>问题类型分布</span>
          </template>
          <div ref="barChartRef" style="width: 100%; height: 300px;"></div>
        </el-card>
      </el-col>
      
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>严重问题详情</span>
          </template>
          <el-table :data="criticalIssues" stripe>
            <el-table-column prop="type" label="问题类型" />
            <el-table-column prop="count" label="数量" />
            <el-table-column prop="percentage" label="占比">
              <template #default="{ row }">
                {{ row.percentage }}%
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import * as echarts from 'echarts'
import { getOverviewStatistics, getReviewTrend, getIssueDistribution, getIssueDistributionByCategory } from '@/api/statistic'

const statistics = ref({})
const trendDays = ref(7)
const trendChartRef = ref()
const pieChartRef = ref()
const barChartRef = ref()
const criticalIssues = ref([])

onMounted(async () => {
  await loadStatistics()
  await loadTrendData()
  await loadPieData()
  await loadBarData()
})

const loadStatistics = async () => {
  try {
    const res = await getOverviewStatistics()
    statistics.value = res.data
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
}

const loadTrendData = async () => {
  const chart = echarts.init(trendChartRef.value)
  
  try {
    const res = await getReviewTrend(trendDays.value)
    const data = res.data
    
    const option = {
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: data.map(d => d.date)
      },
      yAxis: { type: 'value' },
      series: [{
        name: '审查次数',
        type: 'line',
        smooth: true,
        data: data.map(d => d.count),
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
          ])
        },
        itemStyle: { color: '#409eff' }
      }]
    }
    
    chart.setOption(option)
  } catch (error) {
    console.error('加载趋势图失败', error)
  }
}

const loadPieData = async () => {
  const chart = echarts.init(pieChartRef.value)
  
  try {
    const res = await getIssueDistribution()
    const dist = res.data
    
    const data = [
      { value: dist.CRITICAL || 0, name: '严重', itemStyle: { color: '#f56c6c' } },
      { value: dist.ERROR || 0, name: '错误', itemStyle: { color: '#e6a23c' } },
      { value: dist.WARNING || 0, name: '警告', itemStyle: { color: '#409eff' } },
      { value: dist.SUGGESTION || 0, name: '建议', itemStyle: { color: '#67c23a' } }
    ]
    
    const total = data.reduce((sum, d) => sum + d.value, 0)
    
    const option = {
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: false,
        label: { show: false },
        emphasis: {
          label: { show: true, fontSize: 16, fontWeight: 'bold' }
        },
        data
      }]
    }
    
    chart.setOption(option)
    
    // 更新严重问题详情
    criticalIssues.value = [
      { type: '严重问题', count: dist.CRITICAL || 0, percentage: total ? Math.round((dist.CRITICAL || 0) / total * 100) : 0 },
      { type: '错误问题', count: dist.ERROR || 0, percentage: total ? Math.round((dist.ERROR || 0) / total * 100) : 0 },
      { type: '警告问题', count: dist.WARNING || 0, percentage: total ? Math.round((dist.WARNING || 0) / total * 100) : 0 },
      { type: '建议问题', count: dist.SUGGESTION || 0, percentage: total ? Math.round((dist.SUGGESTION || 0) / total * 100) : 0 }
    ]
  } catch (error) {
    console.error('加载饼图失败', error)
  }
}

const loadBarData = async () => {
  const chart = echarts.init(barChartRef.value)
  
  try {
    const res = await getIssueDistributionByCategory()
    const dist = res.data
    
    const option = {
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: ['安全漏洞', '代码缺陷', '代码规范']
      },
      yAxis: { type: 'value' },
      series: [{
        type: 'bar',
        data: [
          { value: dist.SECURITY || 0, itemStyle: { color: '#f56c6c' } },
          { value: dist.DEFECT || 0, itemStyle: { color: '#e6a23c' } },
          { value: dist.STYLE || 0, itemStyle: { color: '#409eff' } }
        ],
        barWidth: '50%'
      }]
    }
    
    chart.setOption(option)
  } catch (error) {
    console.error('加载柱状图失败', error)
  }
}
</script>

<style scoped lang="scss">
.statistics-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  
  .stat-icon {
    width: 60px;
    height: 60px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    margin-right: 20px;
  }
  
  .stat-info {
    .stat-value {
      font-size: 28px;
      font-weight: bold;
      color: #333;
    }
    
    .stat-label {
      font-size: 14px;
      color: #999;
      margin-top: 5px;
    }
  }
}
</style>
