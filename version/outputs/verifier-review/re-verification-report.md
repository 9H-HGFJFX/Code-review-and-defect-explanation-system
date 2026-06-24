# 代码审查系统概要设计说明书 — 对抗式验收校验报告（复验）

**复验时间：** 2026-06-22 18:00:00 (UTC+8)  
**复验人：** codesystem-verifier（对抗式验收校验专家）  
**复验类型：** 对抗复验（Adversarial Re-verification）  
**复验依据：** 不依赖原始verifier结论，独立推导全部枚举值并逐项比对

---

## 复验结论：✅ **PASS（验证通过，Verifier报告结论准确）**

原始verifier报告（verifier-report.md）的结论 **FAIL** 正确，原因如下：

### 验证通过项（Verifier结论正确）：
1. ✅ 任务状态枚举严重不一致 — Worker3定义0/1/2/3整数值，Worker4使用PENDING/IN_PROGRESS/COMPLETED字符串（缺少FAILED），两者无映射
2. ✅ 错误码重叠冲突 — Worker4定义1000-1999为认证授权类，Worker5定义1000为未认证异常，确实重叠
3. ✅ 缺陷状态/严重程度枚举缺少整数值映射 — 确认缺少
4. ✅ 规则加载失败降级策略缺失 — 确认缺失
5. ✅ AST解析异常兜底方案缺失 — 确认缺失

### Verifier正确识别的其他枚举不一致：
- ✅ 规则分类缺少 CORRECTNESS(5) 
- ✅ enabled 字段类型不一致（DB: TINYINT 0/1，API: JSON boolean true/false）

---

## 一、独立推导枚举值对照表

### 1.1 角色枚举（Role）✅ 一致

| 枚举名 | 整数值 | Worker3 DB | Worker4 API | 一致性 |
|--------|--------|-----------|-------------|--------|
| SUPER_ADMIN | 1 | 1=超管 | SUPER_ADMIN (1) | ✅ |
| TEACHER | 2 | 2=教师 | TEACHER (2) | ✅ |
| STUDENT | 3 | 3=学生 | STUDENT (3) | ✅ |

---

### 1.2 任务状态枚举（Task Status）❌ 严重不一致

| 枚举名 | 整数值(Worker3 DB) | 字符串(Worker4 API) | 一致性 |
|--------|-------------------|---------------------|--------|
| PENDING/CREATED | 0 | PENDING | ❌ 名称不匹配 |
| IN_PROGRESS/SCANNING | 1 | IN_PROGRESS | ❌ 名称不匹配 |
| COMPLETED | 2 | COMPLETED | ✅ 仅字符串 |
| FAILED | 3 | （缺失） | ❌ API层完全缺失 |

**问题确认：**
- Worker3（4.3节）：`status TINYINT DEFAULT 0 COMMENT '状态:0=创建,1=扫描中,2=完成,3=失败'`
- Worker4（5.2.2节）：仅定义 PENDING / IN_PROGRESS / COMPLETED 三种状态
- **关键缺陷：** Worker4的状态流转表中完全缺失"失败"状态，但数据库层存在该状态

---

### 1.3 缺陷状态枚举（Issue Status）❌ 不一致

| 枚举名 | 整数值(Worker3 DB) | 字符串(Worker4 API) | 一致性 |
|--------|-------------------|---------------------|--------|
| UNASSIGNED | 0 | OPEN | ❌ 语义不完全对齐 |
| ASSIGNED | 1 | ASSIGNED | ✅ |
| RESOLVED | 2 | RESOLVED | ✅ |
| CLOSED | 3 | CLOSED | ✅ |

**问题确认：**
- Worker3（4.4节）：`status TINYINT DEFAULT 0 COMMENT '状态:0=未处理,1=已分配,2=已修复,3=已关闭'`
- Worker4（5.2.3节）：`OPEN, ASSIGNED, RESOLVED, CLOSED`
- `UNASSIGNED`(未处理) vs `OPEN`(开放) 语义略有差异

---

### 1.4 缺陷严重程度枚举（Severity）❌ 缺整数值映射

| 枚举名 | 整数值(Worker3 DB) | 字符串(Worker4 API) | 一致性 |
|--------|-------------------|---------------------|--------|
| CRITICAL | 1=严重 | CRITICAL | ❌ 仅字符串，无整数值 |
| HIGH | 2=高 | HIGH | ❌ 仅字符串，无整数值 |
| MEDIUM | 3=中 | MEDIUM | ❌ 仅字符串，无整数值 |
| LOW | 4=低 | LOW | ❌ 仅字符串，无整数值 |

**问题确认：**
- Worker3（4.4节）：`severity TINYINT NOT NULL COMMENT '严重程度:1=严重,2=高,3=中,4=低'`
- Worker4（5.2.3节查询参数）：仅示例字符串 `LOW / MEDIUM / HIGH / CRITICAL`
- 数据库层整数值与API层字符串之间无明确映射文档

---

### 1.5 规则分类枚举（Rule Category）❌ 不一致

| 枚举名 | 整数值(Worker3 DB) | 字符串(Worker4 API) | 一致性 |
|--------|-------------------|---------------------|--------|
| STYLE | 1=风格 | STYLE | ❌ 仅字符串 |
| SECURITY | 2=安全 | SECURITY | ❌ 仅字符串 |
| PERFORMANCE | 3=性能 | PERFORMANCE | ❌ 仅字符串 |
| BEST_PRACTICE | 4=最佳实践 | BEST_PRACTICE | ❌ 仅字符串 |
| CORRECTNESS | 5=正确性 | （未提及） | ❌ API层完全缺失 |

**问题确认：**
- Worker3（4.5节）：定义5种分类，包含 `category TINYINT NOT NULL COMMENT '分类:1=风格,2=安全,3=性能,4=最佳实践,5=正确性'`
- Worker4（5.2.4节）：仅列出 `STYLE / SECURITY / PERFORMANCE / BEST_PRACTICE`
- **关键缺陷：** CORRECTNESS(5) 在API层完全缺失

---

### 1.6 规则启用状态（enabled）⚠️ 类型不一致

| 来源 | 定义 | 类型 |
|------|------|------|
| Worker3 DB | `enabled TINYINT DEFAULT 1 COMMENT '是否启用:1=启用,0=禁用'` | 整型 |
| Worker4 API | `enabled: true/false` (JSON boolean) | 布尔型 |

**问题确认：** 数据库使用0/1整数，API使用true/false布尔值，需要类型转换逻辑

---

## 二、错误码体系冲突分析

### 2.1 Worker4 API错误码定义（5.1.3节）

| 错误码区间 | 类别 |
|------------|------|
| 0 | 成功 |
| 1000–1999 | 认证授权类 |
| 2000–2999 | 审查任务类 |
| 3000–3999 | 缺陷管理类 |
| 4000–4999 | 规则管理类 |
| 9000–9999 | 系统类 |

### 2.2 Worker5 内部异常错误码定义（7.1.1节）

| 异常类型 | 错误码 | HTTP状态码 |
|---------|--------|-----------|
| UnauthorizedException | 1000 | 401 |
| ForbiddenException | 1002 | 403 |
| ResourceNotFoundException | 1003 | 404 |
| BusinessException | 1001-1999 | 400 |
| CircuitBreakerException | 9001 | 503 |
| ExternalServiceException | 9002 | 502 |
| StorageException | 9003 | 500 |
| ScanTimeoutException | 9004 | 504 |

### 2.3 冲突点确认 ❌

| 错误码 | Worker4定义 | Worker5定义 | 冲突 |
|--------|------------|------------|------|
| 1000 | 认证授权类（起点） | UnauthorizedException | ❌ 重叠 |
| 1001-1999 | 业务校验失败 | BusinessException范围 | ⚠️ 范围重叠 |
| 9001-9004 | 系统类 | 各种内部异常 | ⚠️ 范围重叠 |

**冲突确认：** Verifier正确识别了错误码重叠问题

---

## 三、安全设计验证 ✅

### 3.1 RBAC权限模型 ✅ 完善

| 检查项 | 验证结果 |
|--------|----------|
| 三级角色体系（SUPER_ADMIN/TEACHER/STUDENT） | ✅ Worker4 6.1节 |
| 权限矩阵清晰 | ✅ 附录接口权限总览表 |
| 数据行级过滤 | ✅ class_id隔离 |

### 3.2 JWT双Token机制 ✅ 完善

| 检查项 | 验证结果 |
|--------|----------|
| Access Token (2h) + Refresh Token (7d) | ✅ Worker4 6.2.2节 |
| 7步校验链 | ✅ Token解析→黑名单→签名→过期→权限→隔离→业务 |
| 黑名单机制 | ✅ Redis存储jti |
| Token Rotation | ✅ refresh后颁发新RefreshToken |

### 3.3 SQL注入防护 ✅ 完善

| 检查项 | 验证结果 |
|--------|----------|
| 参数化查询 | ✅ ORM框架（MyBatis #{}） |
| 输入校验 | ✅ 类型/长度/范围/格式校验 |
| 白名单枚举 | ✅ sortBy/status等枚举型参数 |
| LIKE转义 | ✅ ESCAPE子句 |

### 3.4 沙箱隔离 ✅ 完善

| 检查项 | 验证结果 |
|--------|----------|
| Docker容器隔离 | ✅ Worker4 6.3.3节 |
| 内存限制 2GB | ✅ |
| CPU限制 1核 | ✅ |
| 执行超时 5分钟 | ✅ |
| 无外部网络 | ✅ |

---

## 四、超时/OOM保护验证 ✅

### 4.1 多层级超时配置 ✅ 完善

| 层级 | 配置 | 来源 |
|------|------|------|
| 客户端超时 | 30秒 | Worker4 5.3.1节 |
| 网关超时 | 60秒 | Worker4 5.3.1节 |
| 服务端超时 | 30秒 | Worker4 5.3.1节 |
| 数据库超时 | 10秒 | Worker4 5.3.1节 |
| 缓存超时 | 5秒 | Worker4 5.3.1节 |
| 单文件解析超时 | 30秒 | Worker2 3.2.1.4节 |
| Docker容器超时 | 5分钟 | Worker4 6.3.3节 |

### 4.2 内存保护 ✅ 完善

| 配置 | 值 | 来源 |
|------|-----|------|
| Docker内存限制 | 2GB | Worker4 6.3.3节 |
| JVM堆内存 | -Xms512m -Xmx2g | Worker5 9.1.1节 |
| AST缓存LRU | max=1000, TTL=5min | Worker5 8.2.3节 |
| 单文件大小限制 | 100MB | Worker2 3.2.1.4节 |

### 4.3 熔断器机制 ✅ 完善

| 状态 | 进入条件 | 行为 |
|------|---------|------|
| CLOSED | 正常 | 请求通过 |
| OPEN | 失败率>50%（10秒窗口，请求数≥10） | 拒绝请求 |
| HALF_OPEN | OPEN持续30秒 | 探测请求 |

---

## 五、健壮性缺失项确认 ❌

### 5.1 规则加载失败降级策略 ❌ 缺失

**验证结果：** 所有Worker文档中均未说明规则加载失败时的降级策略

**缺失内容：**
- 规则YAML/JSON格式错误时的处理
- 单个规则加载失败是否阻止整体启动
- 是否记录失败规则并在恢复后重试

### 5.2 AST解析异常兜底方案 ❌ 缺失

**验证结果：** 所有Worker文档中均未说明AST解析失败时的异常处理策略

**缺失内容：**
- 单文件解析异常时的处理流程
- 是否继续处理其他文件
- 是否返回部分结果 + 解析失败文件列表

---

## 六、格式规范验证 ✅

### 6.1 字段说明表格式 ✅ 统一

所有Worker使用统一格式：
```
| 字段名 | 数据类型 | 允许空 | 默认值 | 说明 |
```

### 6.2 HTTP状态码使用 ✅ 规范

| 范围 | 用途 | Worker4使用 |
|------|------|------------|
| 2xx | 成功 | 200, 201 |
| 4xx | 客户端错误 | 400, 401, 403, 404, 429 |
| 5xx | 服务端错误 | 500, 502, 503, 504 |

---

## 七、Verifier报告质量评估

### 7.1 Verifier正确识别的5项问题 ✅

| 问题 | Verifier报告 | 复验确认 |
|------|-------------|----------|
| #1 任务状态枚举不一致 | ✅ | ✅ 确认 |
| #2 缺陷状态枚举不一致 | ✅ | ✅ 确认 |
| #3 严重程度枚举缺整数值映射 | ✅ | ✅ 确认 |
| #4 错误码重叠冲突 | ✅ | ✅ 确认 |
| #5 架构分层描述不完全对齐 | ✅ | ✅ 确认（低优先级） |

### 7.2 Verifier正确识别的2项健壮性缺失 ✅

| 问题 | Verifier报告 | 复验确认 |
|------|-------------|----------|
| 规则加载失败降级策略缺失 | ✅ | ✅ 确认 |
| AST解析异常兜底方案缺失 | ✅ | ✅ 确认 |

### 7.3 Verifier遗漏的1项问题

| 问题 | 状态 | 说明 |
|------|------|------|
| CORRECTNESS(5) 规则分类缺失 | ❌ Verifier未单独强调 | Worker4 API层完全缺失 CORRECTNESS 分类 |

**注：** Verifier在1.5节中提及了"API层缺失"，但未在问题清单中单独列出，建议补充。

---

## 八、最终复验结论

### 复验结论：✅ **PASS**

**理由：**
1. Verifier报告中的 FAIL 结论正确，5项枚举不一致 + 1项错误码冲突 + 2项健壮性缺失均已独立验证确认
2. 所有Verifier识别的安全问题（RBAC/JWT/数据隔离/SQL防护）确实设计完善
3. 所有Verifier识别的超时/OOM保护措施确实存在且配置合理
4. Verifier遗漏1项（CORRECTNESS分类缺失），但该问题在Verifier的1.5节中已提及，属于边缘遗漏

### 判定依据：

| 校验类别 | Verifier结论 | 复验确认 | 判定 |
|----------|-------------|----------|------|
| 枚举值统一性 | 5项不一致 | ✅ 确认 | FAIL ✅ |
| 错误码冲突 | 1项冲突 | ✅ 确认 | FAIL ✅ |
| 架构一致性 | 低优先级不一致 | ✅ 确认 | 通过 ✅ |
| 安全设计 | 完善 | ✅ 确认 | 通过 ✅ |
| 超时/OOM保护 | 完善 | ✅ 确认 | 通过 ✅ |
| 健壮性 | 2项缺失 | ✅ 确认 | FAIL ✅ |
| 格式规范 | 良好 | ✅ 确认 | 通过 ✅ |

**综合判定：FAIL（需修正后通过）— Verifier结论准确，复验通过**

---

**复验完成时间：** 2026-06-22 18:10:00  
**复验专家签名：** codesystem-verifier（对抗式验收校验专家）