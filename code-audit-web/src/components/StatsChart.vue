<script setup lang="ts">
import { ref, onMounted, watch, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts/core'
import { PieChart, BarChart, LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([PieChart, BarChart, LineChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

const props = defineProps<{ overview: any }>()

const pieEl = ref<HTMLDivElement>()
const barEl = ref<HTMLDivElement>()
const lineEl = ref<HTMLDivElement>()

let pie: echarts.ECharts | null = null
let bar: echarts.ECharts | null = null
let line: echarts.ECharts | null = null

function buildOptions() {
  const data = props.overview
  if (!data) return

  // 严重级别饼图
  if (pieEl.value) {
    pie = pie || echarts.init(pieEl.value)
    pie.setOption({
      title: { text: '严重级别分布', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'item' },
      legend: { bottom: 0, left: 'center' },
      color: ['#dc2626', '#ea580c', '#ca8a04', '#2563eb'],
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        label: { show: true, formatter: '{b}: {c}' },
        data: [
          { name: 'CRITICAL',   value: data.severityDistribution?.CRITICAL || 0 },
          { name: 'ERROR',      value: data.severityDistribution?.ERROR || 0 },
          { name: 'WARNING',    value: data.severityDistribution?.WARNING || 0 },
          { name: 'SUGGESTION', value: data.severityDistribution?.SUGGESTION || 0 }
        ]
      }]
    })
  }

  // 分类柱状图
  if (barEl.value) {
    bar = bar || echarts.init(barEl.value)
    bar.setOption({
      title: { text: '问题分类分布', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      grid: { left: '8%', right: '8%', bottom: '12%', top: '18%', containLabel: true },
      xAxis: { type: 'category', data: ['STYLE', 'DEFECT', 'SECURITY'] },
      yAxis: { type: 'value', name: '问题数' },
      color: ['#6366f1'],
      series: [{
        type: 'bar',
        barWidth: '50%',
        data: [
          data.categoryDistribution?.STYLE || 0,
          data.categoryDistribution?.DEFECT || 0,
          data.categoryDistribution?.SECURITY || 0
        ],
        label: { show: true, position: 'top' }
      }]
    })
  }

  // 7天趋势折线
  if (lineEl.value) {
    line = line || echarts.init(lineEl.value)
    const days = Object.keys(data.trend7Days || {})
    const counts = Object.values(data.trend7Days || {}).map(v => Number(v))
    line.setOption({
      title: { text: '最近 7 天审查量', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      grid: { left: '8%', right: '8%', bottom: '12%', top: '18%', containLabel: true },
      xAxis: { type: 'category', data: days },
      yAxis: { type: 'value', name: '审查次数' },
      color: ['#10b981'],
      series: [{
        type: 'line',
        smooth: true,
        areaStyle: { opacity: 0.3 },
        data: counts,
        label: { show: true }
      }]
    })
  }
}

function resize() {
  pie?.resize(); bar?.resize(); line?.resize()
}

onMounted(buildOptions)
watch(() => props.overview, buildOptions, { deep: true })
window.addEventListener('resize', resize)
onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  pie?.dispose(); bar?.dispose(); line?.dispose()
})
</script>

<template>
  <div class="stats-charts">
    <div ref="pieEl" class="chart" />
    <div ref="barEl" class="chart" />
    <div ref="lineEl" class="chart" style="grid-column: span 2" />
  </div>
</template>

<style scoped>
.stats-charts { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.chart { height: 320px; background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 8px; }
</style>
