<template>
  <div class="home-container">
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
          <div class="stat-icon" style="background: #e6a23c;">
            <el-icon size="30"><Clock /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.todayReviews || 0 }}</div>
            <div class="stat-label">今日审查</div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background: #f56c6c;">
            <el-icon size="30"><WarningFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ issueDistribution.CRITICAL || 0 }}</div>
            <div class="stat-label">严重问题</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="16">
        <el-card class="chart-card">
          <template #header>
            <span>审查趋势（最近7天）</span>
          </template>
          <div ref="trendChartRef" style="width: 100%; height: 300px;"></div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card class="chart-card">
          <template #header>
            <span>问题分布</span>
          </template>
          <div ref="pieChartRef" style="width: 100%; height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
        <el-card class="recent-card">
          <template #header>
            <div class="card-header">
              <span>最近审查记录</span>
              <el-button type="primary" size="small" @click="$router.push('/review/history')">
                查看更多
              </el-button>
            </div>
          </template>
          <el-table :data="recentReviews" stripe>
            <el-table-column prop="fileName" label="文件名" width="200" />
            <el-table-column prop="lineCount" label="代码行数" width="100" />
            <el-table-column prop="totalIssues" label="问题数" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.totalIssues > 0" type="danger" size="small">
                  {{ row.totalIssues }}
                </el-tag>
                <el-tag v-else type="success" size="small">0</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" size="small">
                  {{ statusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reviewTime" label="审查时间" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import * as echarts from 'echarts'
import { getUserOverviewStatistics, getReviewTrend, getIssueDistribution } from '@/api/statistic'
import { getReviewList } from '@/api/review'

const statistics = ref({})
const issueDistribution = ref({})
const recentReviews = ref([])
const trendChartRef = ref()
const pieChartRef = ref()

onMounted(async () => {
  await loadStatistics()
  await loadTrendChart()
  await loadPieChart()
  await loadRecentReviews()
})

const loadStatistics = async () => {
  try {
    const res = await getUserOverviewStatistics()
    statistics.value = res.data
    
    const distRes = await getIssueDistribution()
    issueDistribution.value = distRes.data
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
}

const loadTrendChart = async () => {
  const chart = echarts.init(trendChartRef.value)
  
  try {
    const res = await getReviewTrend(7)
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

const loadPieChart = async () => {
  const chart = echarts.init(pieChartRef.value)
  
  const dist = issueDistribution.value
  const data = [
    { value: dist.CRITICAL || 0, name: '严重', itemStyle: { color: '#f56c6c' } },
    { value: dist.ERROR || 0, name: '错误', itemStyle: { color: '#e6a23c' } },
    { value: dist.WARNING || 0, name: '警告', itemStyle: { color: '#409eff' } },
    { value: dist.SUGGESTION || 0, name: '建议', itemStyle: { color: '#67c23a' } }
  ]
  
  const option = {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 14, fontWeight: 'bold' }
      },
      data
    }]
  }
  
  chart.setOption(option)
}

const loadRecentReviews = async () => {
  try {
    const res = await getReviewList({ current: 1, size: 5 })
    recentReviews.value = res.data.records
  } catch (error) {
    console.error('加载最近审查记录失败', error)
  }
}

const statusType = (status) => {
  switch (status) {
    case 'COMPLETED': return 'success'
    case 'PENDING': return 'warning'
    case 'FAILED': return 'danger'
    default: return 'info'
  }
}

const statusText = (status) => {
  switch (status) {
    case 'COMPLETED': return '已完成'
    case 'PENDING': return '处理中'
    case 'FAILED': return '失败'
    default: return status
  }
}
</script>

<style scoped lang="scss">
.home-container {
  padding: 0;
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

.chart-card, .recent-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
