// API 统一响应
export interface ApiResult<T = any> {
  code: number
  message: string
  data: T
}

export interface PageResp<T> {
  records: T[]
  total: number
  current: number
  size: number
}

export interface UserInfo {
  id: number
  username: string
  role: 'STUDENT' | 'TEACHER' | 'ADMIN'
  email?: string
  realName?: string
}

export interface LoginResp {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: UserInfo
}

export interface ReviewSubmitReq {
  code: string
  fileName?: string
}

export interface IssueVO {
  id?: number
  ruleId?: number
  ruleName?: string
  category: 'STYLE' | 'DEFECT' | 'SECURITY'
  severity: 'CRITICAL' | 'ERROR' | 'WARNING' | 'SUGGESTION'
  lineNumber: number
  endLine?: number
  colNumber?: number
  description: string
  suggestion?: string
  codeBefore?: string
  codeAfter?: string
  aiExplain?: string
}

export interface SeverityStats {
  critical: number
  error: number
  warning: number
  suggestion: number
  total: number
}

export interface ReviewResultVO {
  reviewId: number
  fileName?: string
  lineCount: number
  issueCount: number
  status: 'PENDING' | 'COMPLETED' | 'FAILED'
  costMs?: number
  reviewTime?: string
  stats?: SeverityStats
  issues?: IssueVO[]
  securityIssues?: IssueVO[]
}

export interface ReviewSummary {
  id: number
  userId: number
  fileName?: string
  lineCount: number
  issueCount: number
  status: string
  reviewTime: string
  costMs?: number
}

export interface RuleVO {
  id: number
  category: string
  name: string
  code: string
  patternType: string
  severity: string
  description?: string
  suggestionTemplate?: string
  executorBean?: string
  enabled: number
  isBuiltin: number
  classId?: number
  createTime?: string
}

export interface RuleSaveReq {
  name: string
  code: string
  category: string
  severity: string
  patternType: string
  pattern?: string
  description?: string
  suggestionTemplate?: string
  executorBean?: string
  classId?: number
  enabled?: number
}
