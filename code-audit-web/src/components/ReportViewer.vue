<script setup lang="ts">
import { computed } from 'vue'
import type { ReviewResultVO } from '@/types/api'
import IssueCard from './IssueCard.vue'
import { reportApi } from '@/api/report'

const props = defineProps<{ result: ReviewResultVO }>()

const stats = computed(() => props.result.stats)

async function download(type: 'pdf' | 'word') {
  try { await reportApi.download(props.result.reviewId, type) } catch { /* noop */ }
}
</script>

<template>
  <div class="report-viewer">
    <!-- 概要 -->
    <el-card shadow="never" class="summary-card">
      <div class="flex items-center justify-between">
        <div>
          <h2 style="margin: 0 0 8px 0">审查报告 #{{ result.reviewId }}</h2>
          <div class="text-sub">
            <span v-if="result.fileName">文件：<b>{{ result.fileName }}</b></span>
            <span style="margin-left: 16px">代码行数：<b>{{ result.lineCount }}</b></span>
            <span style="margin-left: 16px">问题数：<b>{{ result.issueCount }}</b></span>
            <span v-if="result.costMs" style="margin-left: 16px">耗时：<b>{{ result.costMs }}ms</b></span>
            <span v-if="result.reviewTime" style="margin-left: 16px">时间：<b>{{ result.reviewTime }}</b></span>
          </div>
        </div>
        <div class="flex gap-12">
          <el-button type="primary" @click="download('pdf')">导出 PDF</el-button>
          <el-button @click="download('word')">导出 Word</el-button>
        </div>
      </div>
    </el-card>

    <!-- 严重级别统计 -->
    <el-card shadow="never" style="margin-top: 16px">
      <template #header><b>问题严重级别分布</b></template>
      <div class="stats-row">
        <div class="stat-box stat-critical">
          <div class="stat-num">{{ stats?.critical || 0 }}</div>
          <div class="stat-label">严重 (CRITICAL)</div>
        </div>
        <div class="stat-box stat-error">
          <div class="stat-num">{{ stats?.error || 0 }}</div>
          <div class="stat-label">错误 (ERROR)</div>
        </div>
        <div class="stat-box stat-warning">
          <div class="stat-num">{{ stats?.warning || 0 }}</div>
          <div class="stat-label">警告 (WARNING)</div>
        </div>
        <div class="stat-box stat-suggestion">
          <div class="stat-num">{{ stats?.suggestion || 0 }}</div>
          <div class="stat-label">建议 (SUGGESTION)</div>
        </div>
      </div>
    </el-card>

    <!-- 安全风险专区 -->
    <div v-if="result.securityIssues && result.securityIssues.length > 0" class="security-warning" style="margin-top: 16px">
      <h3 style="margin: 0 0 8px 0; color: #dc2626">
        ⚠ 安全风险摘要（共 {{ result.securityIssues.length }} 个）
      </h3>
      <div class="text-sub" style="margin-bottom: 8px">请优先修复所有安全类问题，避免线上高危漏洞。</div>
      <el-tag v-for="i in result.securityIssues" :key="i.id || (i.ruleId + '-' + i.lineNumber)" type="danger" effect="dark" style="margin-right: 8px">
        L{{ i.lineNumber }} {{ i.ruleName }}
      </el-tag>
    </div>

    <!-- 问题列表 -->
    <h3 style="margin-top: 24px">问题清单（{{ result.issues?.length || 0 }}）</h3>
    <div v-if="!result.issues || result.issues.length === 0" class="empty">
      <el-empty description="✓ 未发现任何问题，代码质量良好" />
    </div>
    <div v-else>
      <IssueCard
        v-for="(issue, idx) in result.issues"
        :key="issue.id || (idx + '-' + issue.lineNumber)"
        :issue="issue"
        :index="idx" />
    </div>
  </div>
</template>

<style scoped>
.summary-card { background: #f8fafc; }
.stats-row { display: flex; gap: 16px; }
.stat-box {
  flex: 1; padding: 16px; border-radius: 8px; text-align: center;
  border: 1px solid #e5e7eb;
}
.stat-num { font-size: 28px; font-weight: 700; line-height: 1; }
.stat-label { font-size: 12px; color: #6b7280; margin-top: 4px; }
.stat-critical { background: #fef2f2; color: #dc2626; }
.stat-error    { background: #fff7ed; color: #ea580c; }
.stat-warning  { background: #fefce8; color: #ca8a04; }
.stat-suggestion { background: #eff6ff; color: #2563eb; }
.empty { padding: 24px 0; }
</style>
