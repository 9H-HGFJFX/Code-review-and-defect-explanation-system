<script setup lang="ts">
import { ref } from 'vue'
import { Lock, MagicStick } from '@element-plus/icons-vue'
import type { IssueVO } from '@/types/api'

defineProps<{ issue: IssueVO; index: number }>()
const activeNames = ref<string[]>(['suggestion'])

const sevLabel: Record<string, string> = {
  CRITICAL: '严重',
  ERROR: '错误',
  WARNING: '警告',
  SUGGESTION: '建议'
}

const catLabel: Record<string, string> = {
  STYLE: '规范',
  DEFECT: '缺陷',
  SECURITY: '安全'
}
</script>

<template>
  <el-card class="issue-card" :class="['issue-' + issue.severity]" shadow="hover">
    <div class="issue-header">
      <div class="flex items-center gap-12">
        <el-tag :type="issue.severity === 'CRITICAL' ? 'danger' :
                     issue.severity === 'ERROR' ? 'warning' :
                     issue.severity === 'WARNING' ? '' : 'info'" effect="dark" size="small">
          {{ sevLabel[issue.severity] || issue.severity }}
        </el-tag>
        <el-tag size="small" :type="issue.category === 'SECURITY' ? 'danger' :
                                    issue.category === 'DEFECT' ? 'warning' : 'primary'" plain>
          {{ catLabel[issue.category] || issue.category }}
        </el-tag>
        <span class="rule-name" v-if="issue.ruleName">{{ issue.ruleName }}</span>
      </div>
      <div class="text-sub">第 <b>{{ issue.lineNumber }}</b> 行</div>
    </div>

    <div class="issue-desc">
      <el-icon v-if="issue.severity === 'CRITICAL' && issue.category === 'SECURITY'"><Lock /></el-icon>
      {{ issue.description }}
    </div>

    <el-collapse v-model="activeNames">
      <el-collapse-item v-if="issue.codeBefore || issue.codeAfter" title="代码示例" name="code">
        <div v-if="issue.codeBefore" class="code-label">修改前</div>
        <pre class="code-block">{{ issue.codeBefore }}</pre>
        <div v-if="issue.codeAfter" class="code-label">修改后</div>
        <pre class="code-block code-after">{{ issue.codeAfter }}</pre>
      </el-collapse-item>
      <el-collapse-item v-if="issue.suggestion" title="修复建议" name="suggestion">
        <div class="suggestion-text">{{ issue.suggestion }}</div>
      </el-collapse-item>
      <el-collapse-item v-if="issue.aiExplain" title="AI 缺陷解释" name="ai">
        <div class="ai-explain">
          <el-icon style="vertical-align: middle; margin-right: 4px;"><MagicStick /></el-icon>
          {{ issue.aiExplain }}
        </div>
      </el-collapse-item>
    </el-collapse>
  </el-card>
</template>

<style scoped>
.issue-card { margin-bottom: 12px; border-left-width: 4px !important; }
.issue-CRITICAL { border-left: 4px solid #dc2626; }
.issue-ERROR    { border-left: 4px solid #ea580c; }
.issue-WARNING  { border-left: 4px solid #ca8a04; }
.issue-SUGGESTION { border-left: 4px solid #2563eb; }
.issue-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.issue-desc { font-size: 14px; line-height: 1.6; color: #1f2937; }
.rule-name { color: #6b7280; font-size: 13px; }
.code-label { font-size: 12px; color: #6b7280; margin-top: 6px; margin-bottom: 4px; }
.code-block {
  background: #1e293b; color: #e2e8f0; padding: 10px 12px; border-radius: 4px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 12px;
  white-space: pre-wrap; word-break: break-all; margin: 0 0 4px;
}
.code-after { background: #064e3b; color: #d1fae5; }
.suggestion-text { color: #1e3a8a; font-size: 14px; line-height: 1.6; }
</style>
