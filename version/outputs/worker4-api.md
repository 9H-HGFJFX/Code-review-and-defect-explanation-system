# 概要设计说明书 · 第5-6章

> **文件路径：** `E:\Desktop\version\outputs\worker4-api.md`
> **编写角色：** 接口和权限安全专家（Worker4）
> **版本：** v1.1（扩展章节版）
> **日期：** 2026-06-22

---

## 第5章 接口设计

### 5.1 接口规范

#### 5.1.1 RESTful 设计原则

本系统所有接口遵循 RESTful 架构风格，以资源（Resource）为中心进行 URI 设计，HTTP 方法语义严格对应 CRUD 操作。

**资源命名规范**

| 规范 | 说明 | 正确示例 | 错误示例 |
|------|------|----------|----------|
| 使用名词复数 | URI 表示资源集合或单一资源 | `/api/tasks` | `/api/getTasks` |
| 层级嵌套 | 表达资源所有关系，深度不超过3层 | `/api/tasks/{id}/issues` | `/api/issues?taskId=x` |
| 全小写 + 连字符 | 便于阅读和 SEO，不区分大小写 | `/api/task-groups` | `/api/taskGroups` |
| 无文件扩展名 | 不在 URI 中暴露服务端技术 | `/api/tasks/1` | `/api/tasks/1.json` |

**HTTP 方法语义**

| 方法 | 语义 | 幂等性 | 安全性 | 适用场景 |
|------|------|--------|--------|----------|
| GET | 读取资源，不改变服务端状态 | 是 | 是 | 查询列表、详情 |
| POST | 创建资源，提交数据到服务端 | 否 | 否 | 创建任务、新增缺陷 |
| PUT | 完整更新资源（替换），幂等 | 是 | 否 | 更新规则、替换缺陷信息 |
| DELETE | 删除资源，幂等 | 是 | 否 | 删除任务 |

> **注：** 部分更新（仅修改资源部分字段）统一使用 PATCH 方法，明确在接口清单中注明。

**路径参数 vs 查询参数**

| 类型 | 语法 | 用途 | 示例 |
|------|------|------|------|
| 路径参数 | `{path}` 在 URI 中 | 标识需操作的具体资源 | `/api/tasks/{id}` → `/api/tasks/42` |
| 查询参数 | `?key=value` 在 URI 末尾 | 筛选、排序、分页 | `/api/tasks?page=1&pageSize=20&classId=3` |

区分原则：
- **路径参数**：资源 ID、必选子资源路径（如 `tasks/{id}/issues`）
- **查询参数**：可选过滤条件、分页参数、排序字段

---

#### 5.1.2 统一返回格式

所有接口（无论成功或失败）均使用以下统一结构返回，HTTP 状态码仅表示 HTTP 层结果，业务状态码在 body 中。

```json
{
  "code": 0,
  "message": "success",
  "data": { },
  "timestamp": 1750612330000
}
```

**字段说明**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `code` | integer | 是 | 业务状态码，0=成功，非0=失败 |
| `message` | string | 是 | 状态描述文本，用于前端展示 |
| `data` | object/array/null | 是 | 响应数据体，失败时为 null |
| `timestamp` | long | 是 | 服务器时间戳（毫秒） |

**分页响应包装**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [ ],
    "pagination": {
      "page": 1,
      "pageSize": 20,
      "total": 100,
      "totalPages": 5
    }
  },
  "timestamp": 1750612330000
}
```

---

#### 5.1.3 错误码体系

错误码采用 4 位数字分段设计，结构：`[分段前缀][序号]`，便于按类别过滤日志和监控告警。

| 错误码区间 | 类别 | 说明 |
|------------|------|------|
| 1000–1999 | 认证授权类 | 登录、Token、权限校验相关错误 |
| 2000–2999 | 审查任务类 | 任务创建、查询、状态变更相关错误 |
| 3000–3999 | 缺陷管理类 | 缺陷提交、分配、状态变更相关错误 |
| 4000–4999 | 规则管理类 | 规则创建、更新、启用禁用相关错误 |
| 9000–9999 | 系统类 | 内部异常、数据库错误、服务不可用 |

**详细错误码定义**

> **异常错误码说明：** 以下错误码体系为系统标准，Worker5 内部异常类（UnauthorizedException、ForbiddenException、CircuitBreakerException、ExternalServiceException）在对外暴露时统一映射至本表定义。

| 错误码 | HTTP状态码 | 英文消息 | 说明 |
|--------|------------|----------|------|
| 0 | 200 | success | 成功 |
| 1000 | 401 | unauthorized | 未认证（UnauthorizedException） |
| 1001 | 401 | token_expired | Token 已过期 |
| 1002 | 403 | forbidden | 无权限访问（ForbiddenException） |
| 1003 | 401 | token_invalid | Token 格式无效或被篡改 |
| 1004 | 401 | token_missing | 缺少认证 Token |
| 1005 | 403 | permission_denied | 无权访问该资源 |
| 1006 | 403 | role_forbidden | 角色权限不足 |
| 1007 | 401 | refresh_token_expired | Refresh Token 已过期，需重新登录 |
| 1008 | 401 | user_disabled | 账户已被禁用 |
| 2001 | 400 | task_create_failed | 任务创建失败 |
| 2002 | 404 | task_not_found | 任务不存在 |
| 2003 | 400 | task_status_invalid | 任务状态变更无效 |
| 2004 | 403 | task_not_in_class | 当前用户不在任务所属班级 |
| 3001 | 404 | issue_not_found | 缺陷不存在 |
| 3002 | 400 | issue_assign_failed | 缺陷分配失败 |
| 3003 | 400 | issue_status_invalid | 缺陷状态变更无效 |
| 3004 | 403 | issue_not_assignable | 非任务负责人无法分配缺陷 |
| 4001 | 400 | rule_create_failed | 规则创建失败 |
| 4002 | 404 | rule_not_found | 规则不存在 |
| 4003 | 400 | rule_update_failed | 规则更新失败 |
| 9001 | 503 | circuit_breaker_open | 服务熔断中（CircuitBreakerException） |
| 9002 | 502 | external_service_error | 外部服务异常（ExternalServiceException） |
| 9003 | 500 | internal_error | 服务器内部异常 |
| 9004 | 503 | service_unavailable | 服务暂不可用（限流/维护） |
| 9005 | 429 | rate_limit_exceeded | 请求频率超限 |
| 9006 | 500 | database_error | 数据库操作异常 |

---

### 5.2 接口清单

#### 5.2.1 认证接口（Authentication）

> **Base Path:** `/api/auth`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| POST | `/api/auth/login` | 用户登录，获取 Access Token 和 Refresh Token | 公开（无需认证） |
| POST | `/api/auth/refresh` | 使用 Refresh Token 刷新 Access Token | 已登录（携带有效 Refresh Token） |
| POST | `/api/auth/logout` | 登出，将 Refresh Token 加入黑名单 | 已登录 |

---

**POST /api/auth/login**

登录接口，验证用户名密码后返回双 Token。

*Request Body：*
```json
{
  "username": "teacher_wang",
  "password": "encrypted_password_string"
}
```

*Success Response (200)：*
```json
{
  "code": 0,
  "message": "login success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 7200,
    "user": {
      "userId": 101,
      "username": "teacher_wang",
      "role": "TEACHER",
      "classId": 3,
      "nickname": "王老师"
    }
  },
  "timestamp": 1750612330000
}
```

*Error Responses：*
```json
// 密码错误 - 错误码 1001
{ "code": 1001, "message": "invalid credentials", "data": null, "timestamp": 1750612330000 }

// 账户禁用 - 错误码 1007
{ "code": 1007, "message": "account disabled", "data": null, "timestamp": 1750612330000 }
```

---

**POST /api/auth/refresh**

使用 Refresh Token 换取新的 Access Token。

*Request Body：*
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

*Success Response (200)：*
```json
{
  "code": 0,
  "message": "token refreshed",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 7200
  },
  "timestamp": 1750612340000
}
```

---

**POST /api/auth/logout**

登出接口，将 Refresh Token 加入黑名单。

*Request Header：*
```
Authorization: Bearer <accessToken>
```

*Success Response (200)：*
```json
{
  "code": 0,
  "message": "logout success",
  "data": null,
  "timestamp": 1750612350000
}
```

---

#### 5.2.2 审查任务接口（Review Tasks）

> **Base Path:** `/api/tasks`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| POST | `/api/tasks` | 创建审查任务 | 教师 / 管理员 |
| GET | `/api/tasks` | 获取任务列表（分页+班级过滤） | 已登录 |
| GET | `/api/tasks/{id}` | 获取任务详情 | 任务参与者（教师/学生） |
| PATCH | `/api/tasks/{id}/status` | 更新任务状态 | 教师（任务创建者） |
| DELETE | `/api/tasks/{id}` | 删除任务 | 管理员 |

---

**POST /api/tasks** — 创建审查任务

*Request Body：*
```json
{
  "title": "第3周代码审查",
  "description": "本次审查重点检查异常处理和边界条件",
  "classId": 3,
  "deadline": "2026-06-30T23:59:59Z",
  "ruleIds": [1, 3, 5],
  "repoUrl": "https://git.example.com/student-group/repo-a"
}
```

*Success Response (201)：*
```json
{
  "code": 0,
  "message": "task created",
  "data": {
    "taskId": 42,
    "title": "第3周代码审查",
    "status": "PENDING",
    "createdBy": 101,
    "classId": 3,
    "createdAt": "2026-06-22T17:00:00Z"
  },
  "timestamp": 1750612330000
}
```

---

**GET /api/tasks** — 任务列表查询

*Query Parameters：*

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| page | integer | 否 | 1 | 页码（从1开始） |
| pageSize | integer | 否 | 20 | 每页条数（最大100） |
| classId | integer | 否 | — | 按班级ID过滤 |
| status | string | 否 | — | 按状态过滤：PENDING / IN_PROGRESS / COMPLETED / FAILED |
| keyword | string | 否 | — | 按标题关键词模糊搜索 |
| sortBy | string | 否 | createdAt | 排序字段 |
| sortOrder | string | 否 | desc | 排序方向：asc / desc |

*Success Response (200)：*
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "taskId": 42,
        "title": "第3周代码审查",
        "status": "PENDING",
        "classId": 3,
        "className": "计算机21班",
        "createdBy": 101,
        "creatorName": "王老师",
        "issueCount": 12,
        "deadline": "2026-06-30T23:59:59Z",
        "createdAt": "2026-06-22T17:00:00Z"
      }
    ],
    "pagination": {
      "page": 1,
      "pageSize": 20,
      "total": 45,
      "totalPages": 3
    }
  },
  "timestamp": 1750612330000
}
```

**权限过滤逻辑：**
- `STUDENT` 角色：自动附加 `classId` 条件，仅返回其所在班级 (`classId = JWT.classId`) 的任务
- `TEACHER` 角色：返回其创建的班级 (`classId = JWT.classId`) 的所有任务
- `SUPER_ADMIN`：可指定 `classId` 查询任意班级数据

---

**GET /api/tasks/{id}** — 任务详情

*Path Parameters：*

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | integer | 任务ID |

*Success Response (200)：*
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "taskId": 42,
    "title": "第3周代码审查",
    "description": "本次审查重点检查异常处理和边界条件",
    "status": "IN_PROGRESS",
    "classId": 3,
    "className": "计算机21班",
    "createdBy": 101,
    "creatorName": "王老师",
    "repoUrl": "https://git.example.com/student-group/repo-a",
    "deadline": "2026-06-30T23:59:59Z",
    "ruleIds": [1, 3, 5],
    "issueCount": 12,
    "resolvedCount": 8,
    "createdAt": "2026-06-22T17:00:00Z",
    "updatedAt": "2026-06-23T09:15:00Z"
  },
  "timestamp": 1750612330000
}
```

---

**PATCH /api/tasks/{id}/status** — 更新任务状态

*Request Body：*
```json
{
  "status": "COMPLETED"
}
```

*任务状态整数值与字符串映射表：*

| 整数值（数据库） | 字符串值（API） | 中文含义 | 说明 |
|------------------|-----------------|----------|------|
| 0 | PENDING | 已创建 | 任务刚创建，尚未开始扫描 |
| 1 | IN_PROGRESS | 扫描中 | 代码审查引擎正在执行扫描 |
| 2 | COMPLETED | 完成 | 扫描成功完成 |
| 3 | FAILED | 失败 | 扫描过程中发生错误 |

> **注：** API 层使用字符串枚举（status 字段为 string 类型），数据库层使用整数值（TINYINT）。两者通过上表进行双向映射，转换逻辑在后端服务层统一处理。

*Allowed Transitions：*

| 当前状态 | 可变更至 |
|----------|----------|
| PENDING | IN_PROGRESS |
| IN_PROGRESS | COMPLETED / FAILED / PENDING（撤回） |
| COMPLETED | —（终态，不可回退） |
| FAILED | —（终态，不可回退） |

*状态流转图：*

```
                    ┌─────────────────────────────────────────────┐
                    │                                             │
                    ▼                                             │
┌──────────┐    ┌──────────────┐    ┌───────────┐    ┌──────────┐
│ PENDING  │───▶│ IN_PROGRESS  │───▶│ COMPLETED  │    │  FAILED  │
│ (已创建) │    │   (扫描中)   │    │  (完成)   │    │  (失败)  │
└──────────┘    └──────────────┘    └───────────┘    └──────────┘
     ▲                │                   │              │
     │                │                   │              │
     │                │                   │              │
     │                ▼                   │              │
     │         ┌──────────────┐          │              │
     └─────────│    撤回     │◀──────────┘              │
               └──────────────┘                         │
                                                          │
                    (终态，不可回退)◀─────────────────────┘
```

---

**DELETE /api/tasks/{id}** — 删除任务

> 仅 `SUPER_ADMIN` 可执行。删除操作级联删除该任务下所有缺陷记录，操作不可逆。

*Success Response (200)：*
```json
{
  "code": 0,
  "message": "task deleted",
  "data": null,
  "timestamp": 1750612330000
}
```

---

#### 5.2.3 缺陷接口（Issues）

> **Base Path:** `/api/issues`

**缺陷状态整数值映射表（与数据库层 issue.status 字段对齐）：**

| 整数值 | 枚举值 | 中文含义 | 说明 |
|--------|--------|----------|------|
| 0 | OPEN | 未处理/开放 | 缺陷发现后初始状态，未分配负责人 |
| 1 | ASSIGNED | 已分配 | 缺陷已分配给具体负责人 |
| 2 | RESOLVED | 已修复 | 负责人已提交修复，待教师确认 |
| 3 | CLOSED | 已关闭 | 教师审核通过，缺陷关闭（终态） |

> **注意：** API 请求/响应中 `status` 字段使用枚举字符串（OPEN / ASSIGNED / RESOLVED / CLOSED），与数据库存储的整数值（0 / 1 / 2 / 3）通过后端 Enum 映射层转换，两者语义完全一致。

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/api/tasks/{taskId}/issues` | 获取任务下的缺陷列表 | 任务参与者 |
| POST | `/api/issues/{id}/assign` | 分配缺陷负责人 | 教师 |
| PATCH | `/api/issues/{id}/status` | 更新缺陷状态 | 教师 / 缺陷负责人 |

**缺陷严重程度枚举映射**

API 层使用字符串枚举（`severity` 字段），数据库层存储整数值，二者映射关系如下：

| 整数值 | 字符串枚举 | 中文说明 |
|--------|------------|----------|
| 1 | CRITICAL | 严重 |
| 2 | HIGH | 高 |
| 3 | MEDIUM | 中 |
| 4 | LOW | 低 |

> **注：** 数据库字段建议命名为 `severity_level TINYINT`，前端与 API 统一使用字符串枚举进行交互。

---

**GET /api/tasks/{taskId}/issues** — 获取缺陷列表

*Query Parameters：*

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | integer | 否 | 页码 |
| pageSize | integer | 否 | 每页条数 |
| severity | string | 否 | 严重程度过滤：LOW / MEDIUM / HIGH / CRITICAL |
| status | string | 否 | 状态过滤：OPEN / ASSIGNED / RESOLVED / CLOSED |
| assigneeId | integer | 否 | 按负责人过滤 |

*Success Response (200)：*
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "issueId": 7,
        "taskId": 42,
        "title": "空指针异常风险：未检查返回值",
        "description": "第56行 `getUser()` 返回可能为 null，后续直接调用其属性",
        "severity": "HIGH",
        "status": "ASSIGNED",
        "filePath": "src/service/UserService.java",
        "lineNumber": 56,
        "assigneeId": 205,
        "assigneeName": "学生张三",
        "createdBy": 101,
        "createdAt": "2026-06-23T10:00:00Z",
        "updatedAt": "2026-06-23T14:30:00Z"
      }
    ],
    "pagination": {
      "page": 1,
      "pageSize": 20,
      "total": 12,
      "totalPages": 1
    }
  },
  "timestamp": 1750612330000
}
```

---

**POST /api/issues/{id}/assign** — 分配缺陷负责人

*Request Body：*
```json
{
  "assigneeId": 205
}
```

*Success Response (200)：*
```json
{
  "code": 0,
  "message": "issue assigned",
  "data": {
    "issueId": 7,
    "assigneeId": 205,
    "assigneeName": "学生张三",
    "assignedAt": "2026-06-23T14:30:00Z"
  },
  "timestamp": 1750612330000
}
```

---

**PATCH /api/issues/{id}/status** — 更新缺陷状态

*Request Body：*
```json
{
  "status": "RESOLVED"
}
```

*Allowed Transitions：*

| 当前状态 | 可变更至 | 可操作角色 |
|----------|----------|------------|
| OPEN | ASSIGNED | 教师 |
| ASSIGNED | RESOLVED | 教师 / 缺陷负责人 |
| RESOLVED | CLOSED / ASSIGNED（驳回重修） | 教师 |
| CLOSED | — | — |

---

#### 5.2.4 规则接口（Rules）

> **Base Path:** `/api/rules`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/api/rules` | 获取规则列表 | 已登录 |
| POST | `/api/rules` | 创建代码审查规则 | 管理员 |
| PUT | `/api/rules/{id}` | 更新规则内容 | 管理员 |
| PATCH | `/api/rules/{id}/enable` | 启用/禁用规则 | 管理员 |

---

**GET /api/rules** — 获取规则列表

*Query Parameters：*

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| enabled | boolean | 否 | 是否启用：true / false |
| category | string | 否 | 规则分类：STYLE / SECURITY / PERFORMANCE / BEST_PRACTICE |
| page | integer | 否 | 页码 |
| pageSize | integer | 否 | 每页条数 |

*Success Response (200)：*
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "ruleId": 1,
        "name": "禁止使用 System.exit()",
        "description": "在生产代码中禁止调用 System.exit()，应使用返回值替代",
        "category": "STYLE",
        "severity": "MEDIUM",
        "enabled": true,
        "createdBy": 1,
        "createdAt": "2026-01-01T00:00:00Z",
        "updatedAt": "2026-06-01T00:00:00Z"
      }
    ],
    "pagination": {
      "page": 1,
      "pageSize": 20,
      "total": 28,
      "totalPages": 2
    }
  },
  "timestamp": 1750612330000
}
```

---

**POST /api/rules** — 创建规则

*Request Body：*
```json
{
  "name": "禁止使用 eval()",
  "description": "JavaScript 代码中禁止使用 eval()，存在安全风险",
  "category": "SECURITY",
  "severity": "HIGH",
  "pattern": "eval\\s*\\(",
  "enabled": false
}
```

---

**PUT /api/rules/{id}** — 更新规则

> 规则更新后，自动触发应用了该规则的所有待审查任务的重新扫描。

*Request Body：*
```json
{
  "name": "禁止使用 eval()（修订版）",
  "description": "JavaScript 代码中禁止使用 eval()，存在 XSS 安全风险",
  "pattern": "eval\\s*\\(",
  "severity": "CRITICAL"
}
```

---

**PATCH /api/rules/{id}/enable** — 启用/禁用规则

*Request Body：*
```json
{
  "enabled": false
}
```

*Success Response (200)：*
```json
{
  "code": 0,
  "message": "rule updated",
  "data": {
    "ruleId": 1,
    "enabled": false,
    "updatedAt": "2026-06-22T17:30:00Z"
  },
  "timestamp": 1750612330000
}
```

---

### 5.3 接口超时与重试策略

#### 5.3.1 超时配置

接口超时分为三个层级：客户端超时、服务端超时、网关超时。超时配置应遵循"最短优先失败"原则，任一层超时均应快速返回错误，避免资源占用。

| 超时层级 | 配置对象 | 默认值 | 说明 |
|----------|----------|--------|------|
| 客户端超时 | 前端/调用方 | 30 秒 | 等待响应的最大时间，超时视为失败 |
| 网关超时 | API Gateway / Nginx | 60 秒 | 转发请求到后端的最大允许时间 |
| 服务端超时 | 后端应用层 | 30 秒 | 单个请求处理的最大时长 |
| 数据库超时 | 数据库连接池 | 10 秒 | 单条 SQL 执行的最大等待时间 |
| 缓存超时 | Redis | 5 秒 | 缓存读写操作超时 |

**不同接口类型的超时策略：**

| 接口类型 | 建议超时 | 原因 |
|----------|----------|------|
| 查询类（GET） | 10–30 秒 | 数据量可控，需快速响应 |
| 创建/更新类（POST/PUT/PATCH） | 15–30 秒 | 涉及数据库写入，需适当缓冲 |
| 批量操作类 | 60–120 秒 | 数据量大，允许较长处理时间 |
| 外部集成类（调用Git/SVN API） | 60 秒 | 依赖外部服务，响应不可控 |
| 代码审查引擎触发 | 30 秒（同步）/ 无限制（异步） | 同步路径需快速返回，异步任务由消息队列管理 |

#### 5.3.2 重试策略

仅对**幂等接口**实施重试，非幂等接口（POST 创建资源）禁止自动重试，防止重复提交。

**重试触发条件：**

| 条件 | 是否重试 | 说明 |
|------|----------|------|
| 网络超时（TCP 连接超时） | ✅ 是 | 网络抖动，可恢复 |
| HTTP 408 Request Timeout | ✅ 是 | 服务端暂时繁忙 |
| HTTP 429 Rate Limit | ✅ 是（带退避） | 限流触发，等待后重试 |
| HTTP 500/502/503/504 | ✅ 是（限次） | 服务端错误，可能瞬时恢复 |
| HTTP 400/401/403/404 | ❌ 否 | 业务逻辑错误，重试无意义 |
| 网络断开/无响应 | ❌ 否（需人工介入） | 网络不可达 |

**重试配置参数：**

| 参数 | 值 | 说明 |
|------|-----|------|
| 最大重试次数 | 3 次 | 防止无限重试 |
| 初始间隔 | 500 ms | 第一次重试等待时间 |
| 退避策略 | 指数退避（Exponential Backoff） | 间隔 × 2^n，防止惊群效应 |
| 最大间隔 | 30 秒 | 防止等待过长 |
| 抖动（Jitter） | ±100 ms | 随机化，避免多客户端同步重试 |

**指数退避公式：**

```
delay_n = min(initial_delay × 2^n + random_jitter, max_delay)
n = 0, 1, 2（最大3次重试）

示例（initial_delay = 500ms, max_delay = 30s）：
  第1次重试：delay ≈ 500ms ~ 600ms
  第2次重试：delay ≈ 1000ms ~ 1100ms
  第3次重试：delay ≈ 2000ms ~ 2100ms
```

#### 5.3.3 熔断器（Circuit Breaker）机制

对外部依赖（Git API、代码审查引擎）实施熔断保护，防止级联故障扩散：

| 熔断器状态 | 进入条件 | 行为 |
|------------|----------|------|
| **CLOSED（正常）** | — | 请求正常通过，失败计数累加 |
| **OPEN（熔断）** | 失败率 > 50%（时间窗口 10s，请求数 ≥ 10） | 拒绝所有请求，快速失败返回 |
| **HALF_OPEN（半开）** | OPEN 状态持续 30 秒 | 允许单个探测请求 |
| CLOSED ← HALF_OPEN | 探测请求成功 | 恢复正常计数 |

---

### 5.4 跨域资源共享配置

#### 5.4.1 CORS 策略定义

本系统 CORS 配置采用白名单模式，严格限制允许跨域访问的来源域名。

**配置参数：**

| 参数 | 值 | 说明 |
|------|-----|------|
| `allowedOrigins` | `["https://codesystem.example.com", "https://admin.codesystem.example.com"]` | 仅允许指定域名 |
| `allowedMethods` | `["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"]` | 支持的 HTTP 方法 |
| `allowedHeaders` | `["Content-Type", "Authorization", "X-Request-Id", "X-Class-Id"]` | 允许的请求头 |
| `exposedHeaders` | `["X-Request-Id", "X-RateLimit-Remaining", "X-RateLimit-Reset"]` | 允许前端访问的响应头 |
| `allowCredentials` | `true` | 允许携带认证信息（Cookie/Authorization） |
| `maxAge` | `3600`（秒） | 预检请求结果缓存时间，减少 OPTIONS 请求 |

> **安全红线：**
> - 禁止 `allowedOrigins` 为 `*`
> - 禁止 `allowCredentials: true` 与 `allowedOrigins: *` 组合（浏览器会拒绝）
> - 生产环境禁止内网 IP 作为 allowedOrigin

#### 5.4.2 预检请求（OPTIONS）处理

非简单请求（PUT/PATCH/DELETE、自定义 Header、Content-Type 非 `application/x-www-form-urlencoded`）会触发浏览器预检 OPTIONS 请求：

```
浏览器预检 → [1] OPTIONS /api/tasks  ←→  服务器返回 CORS 头（maxAge 缓存）
                ↓
         [2] 实际请求 → POST /api/tasks
```

**OPTIONS 响应头配置：**

```
Access-Control-Allow-Origin: https://codesystem.example.com
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Authorization, X-Request-Id
Access-Control-Allow-Credentials: true
Access-Control-Max-Age: 3600
```

#### 5.4.3 域名动态配置

CORS 白名单支持通过配置文件或环境变量管理，禁止硬编码：

```yaml
# application.yml
cors:
  allowed-origins:
    - ${CORS_ORIGIN_PRIMARY:https://codesystem.example.com}
    - ${CORS_ORIGIN_ADMIN:https://admin.codesystem.example.com}
  max-age: 3600
  credentials: true
```

---

## 第6章 安全设计

### 6.1 权限安全（RBAC 权限模型）

本系统采用基于角色的访问控制（Role-Based Access Control, RBAC）模型，设计三个互斥角色，形成三级权限体系。

```
┌─────────────────────────────────────────────────────────────┐
│                     SUPER_ADMIN (1)                        │
│         系统级管理 · 无法操作具体班级数据                    │
├───────────────────────┬────────────────────────────────────┤
│      TEACHER (2)      │         STUDENT (3)                │
│  管理自己的班级        │       仅操作自己的数据              │
│  创建审查任务          │       提交代码/查看缺陷             │
│  分配缺陷负责人        │       无法创建任务                 │
└───────────────────────┴────────────────────────────────────┘
```

#### 角色定义

| 角色枚举值 | 整数值 | 角色名称 | 数量限制 | 说明 |
|------------|--------|----------|----------|------|
| SUPER_ADMIN | 1 | 超级管理员 | 少数（1-3人） | 系统初始化账号，不可删除，拥有最高权限 |
| TEACHER | 2 | 教师 | 每班1-3人 | 负责班级代码审查任务管理 |
| STUDENT | 3 | 学生 | 每班若干人 | 参与代码审查流程，权限受限 |

#### 权限矩阵

| 操作 | SUPER_ADMIN | TEACHER | STUDENT |
|------|-------------|---------|---------|
| 管理用户账户 | ✅ 创建/编辑/禁用任意用户 | ❌ | ❌ |
| 管理班级 | ✅ 创建/编辑/删除班级 | ❌ | ❌ |
| 创建审查任务 | ❌ | ✅ 仅自己所在班级 | ❌ |
| 删除审查任务 | ✅ 可删除任意任务 | ❌ | ❌ |
| 更新任务状态 | ❌ | ✅ 仅自己创建的任务 | ❌ |
| 查看任务详情 | ✅ 可查看任意任务 | ✅ 仅所在班级任务 | ✅ 仅所在班级任务 |
| 分配缺陷 | ❌ | ✅ 所在班级的任意缺陷 | ❌ |
| 更新缺陷状态 | ❌ | ✅ 所在班级的任意缺陷 | ✅ 仅自己负责的缺陷 |
| 创建规则 | ❌ | ❌ | ❌ |
| 更新/启用禁用规则 | ✅ | ❌ | ❌ |
| 登出 | ✅ | ✅ | ✅ |

> **注：** "❌" 表示无权限，即使通过直接调用 API 也应被权限层拒绝并返回 `1005` 错误码。

---

### 6.2 身份认证安全

本系统的身份认证安全体系以 JWT 为核心，结合密码加密存储、登录保护、多因素认证预留等构成完整的安全闭环。

#### 6.2.1 数据隔离规则

数据隔离是 RBAC 模型的核心保障，通过**行级过滤（Row-Level Filtering）**机制确保用户只能访问被授权的数据。

**班级级隔离（class_id 隔离）：**

| 角色 | 隔离策略 |
|------|----------|
| STUDENT | 仅能访问 `class_id = JWT.classId` 的数据 |
| TEACHER | 仅能访问 `class_id = JWT.classId` 的数据 |
| SUPER_ADMIN | 可访问所有 class_id（无过滤） |

**隔离规则列表：**

| 数据表/实体 | 隔离字段 | STUDENT 查询条件 | TEACHER 查询条件 |
|------------|----------|------------------|------------------|
| tasks | class_id | `class_id = JWT.classId` | `class_id = JWT.classId` |
| issues | task_id → tasks.class_id | 关联过滤，同上 | 关联过滤，同上 |
| users | class_id | `class_id = JWT.classId` | `class_id = JWT.classId` |
| classes | id | 不可查询（无此权限） | `id = JWT.classId` |
| rules | — | 无隔离（所有已登录用户可读） | 同左 |

**教师所有权隔离：** 教师创建的班级和任务受所有权保护，`tasks` 表额外维护 `created_by` 字段，TEACHER 角色查询时附加 `created_by = JWT.userId` 条件。

**SQL 注入防护：**

| 防护措施 | 实现要求 |
|----------|----------|
| 参数化查询 | 使用 ORM 框架（MyBatis `#{}`、JPA `?` 占位符），禁止拼接 SQL 字符串 |
| 输入校验 | 对 `classId`、`userId` 等数字型参数，严格校验类型，非数字直接拒绝 |
| 路径参数校验 | 对 `{id}` 类型路径参数，使用正则 `^\d+$` 校验，禁止特殊字符 |
| 白名单校验 | 对 `sortBy`、`status` 等枚举型参数，使用 Enum 或 Set 白名单，拒绝非法值 |
| LIKE 查询过滤 | 模糊搜索参数禁止用户控制通配符前缀，应使用固定字段白名单 |

#### 6.2.2 JWT 双 Token 认证机制

本系统采用 Access Token + Refresh Token 双 Token 模式，实现无感知续期与安全可控的会话管理。

**Token 结构设计：**

```json
// Access Token Payload
{
  "userId": 101,
  "username": "teacher_wang",
  "role": "TEACHER",
  "classId": 3,
  "iat": 1750612330,
  "exp": 1750619530
}
```

```json
// Refresh Token Payload
{
  "userId": 101,
  "type": "refresh",
  "jti": "uuid-unique-id",
  "iat": 1750612330,
  "exp": 1751217130
}
```

**Token 生命周期：**

```
[登录] ──▶ Access Token (2h) ──▶ Refresh ──▶ 新 Access Token (2h)
                   │
     Access Token 过期 ──▶ Refresh Token 有效 ──▶ 刷新
                   │
     Refresh Token 过期 ──▶ 需重新登录
                   │
     [登出] ──▶ 黑名单生效 ──▶ 所有 Token 失效
```

**Token 安全配置：**

| 配置项 | 值 | 说明 |
|--------|-----|------|
| Access Token 有效期 | 2 小时（7200秒） | 短期令牌，兼顾安全与体验 |
| Refresh Token 有效期 | 7 天（604800秒） | 超过需重新登录 |
| 签名算法 | HS256 | HMAC-SHA256，对称签名 |
| 密钥管理 | 环境变量 / Vault | 不得硬编码源代码 |
| Token 传输 | 仅 HTTPS | 生产环境强制 TLS 1.2+ |

**黑名单机制：** 登出时将 Refresh Token 的 `jti` 加入黑名单（Redis: `token:blacklist:{jti}`，TTL = 原始过期时间 - 当前时间）。每次验证时先查询黑名单，存在则返回 `1002 token_invalid`。

#### 6.2.3 密码安全

| 安全措施 | 实现要求 |
|----------|----------|
| 密码哈希算法 | 使用 bcrypt（cost factor ≥ 12）或 Argon2id，禁止 MD5/SHA1/SHA256 直接哈希 |
| 密码强度策略 | 长度 ≥ 8 位，包含大小写字母和数字，禁止常见弱密码（字典校验） |
| 登录失败保护 | 连续 5 次失败后锁定账户 30 分钟，失败次数记录重置需正确登录 |
| 盐值（Salt） | 每个用户独立随机盐值，与密码哈希分开存储 |
| 错误消息 | 登录失败时统一返回"用户名或密码错误"，不区分具体是用户名不存在还是密码错误（防止用户名枚举） |

#### 6.2.4 认证安全辅助措施

| 措施 | 说明 |
|------|------|
| 暴力破解检测 | 同一 IP 在 15 分钟内连续失败 ≥ 20 次，临时封禁该 IP 1 小时 |
| 异地登录检测 | 登录 IP 地理位置与常用地点差异过大时，可记录审计日志（不阻断） |
| Refresh Token 单例使用 | 每次 refresh 后颁发新的 Refresh Token，旧 Token 作废（Token Rotation），防止 Refresh Token 被盗用 |
| Token 绑定设备 | 可选：将 Token 与设备指纹（User-Agent + IP 哈希）绑定，增加伪造难度 |

---

### 6.3 接口安全防护

#### 6.3.1 请求频率限制（Rate Limiting）

| 请求来源 | 限制 | 时间窗口 | 超出响应 |
|----------|------|----------|----------|
| 未登录（IP 维度） | 100 次/分钟 | 滚动窗口 | HTTP 429 + 错误码 9003 |
| 已登录（UserId 维度） | 1000 次/分钟 | 滚动窗口 | HTTP 429 + 错误码 9003 |
| 关键写操作（POST/PUT/DELETE） | 100 次/分钟 | 固定窗口 | HTTP 429 |

实现使用 Redis 滑动窗口计数器，响应头返回剩余配额：`X-RateLimit-Remaining`、`X-RateLimit-Reset`。

#### 6.3.2 输入安全

**通用输入校验规则：**

| 校验维度 | 规则 |
|----------|------|
| 类型校验 | 所有参数严格按声明类型校验（Integer、String、Boolean、Enum） |
| 长度校验 | 字符串最大长度：普通字段 ≤ 255，文本描述 ≤ 2000，代码内容 ≤ 10MB |
| 范围校验 | 分页 pageSize ≤ 100，classId > 0，数值参数在合理范围内 |
| 格式校验 | 日期格式 ISO 8601 (`YYYY-MM-DDTHH:mm:ssZ`)，URL 格式 RFC 3986 |
| 空值校验 | 非空字段（title、description、classId）禁止 null 或空字符串 |

**SQL 注入防护：**
- 所有 SQL 参数使用参数化查询，ORM 层自动防护
- 严格禁止字符串拼接构建 SQL 查询（`"SELECT * FROM users WHERE id=" + id` 严格禁止）
- LIKE 查询中用户输入的内容使用 `ESCAPE` 子句处理特殊字符（`_`、`%`、`\`）

**XSS 防护：**
- 所有用户输入在输出到 HTML 页面时进行 HTML 转义（`<` → `&lt;` 等）
- API 响应 JSON 中的富文本内容，使用白名单标签过滤（仅允许 `<b>`、`<i>`、`<code>` 等安全标签）
- HTTP 响应头强制添加 `X-Content-Type-Options: nosniff` 和 `X-XSS-Protection: 1; mode=block`

**CSRF 防护：**
- 所有状态变更请求（POST/PUT/PATCH/DELETE）必须携带 `Authorization` Header
- 额外防御：自定义 Header `X-Requested-With: XMLHttpRequest` 作为同源请求标识
- API 不依赖 Cookie 认证（Token 放在 Header 中），天然防御 CSRF

#### 6.3.3 引擎沙箱隔离

代码审查引擎在隔离的沙箱环境中执行代码，防止恶意代码影响主机或其他用户：

| 隔离维度 | 配置 | 说明 |
|----------|------|------|
| 容器化隔离 | Docker 容器 / Kubernetes Pod | 每个审查任务独占一个临时容器，执行完毕立即销毁 |
| 内存限制 | 2 GB | 单次代码执行内存上限，超限触发 OOM Kill |
| CPU 限制 | 1 核 | 防止恶意计算密集型代码 |
| 执行超时 | 5 分钟（300秒） | 超时自动终止进程 |
| 网络隔离 | 无外部网络访问 | 代码无法发起网络请求，防止反弹 Shell |
| 文件系统 | 只读根目录 + 临时写目录 | 防止读写系统关键路径 |
| 进程隔离 | 独立 PID 命名空间 | 防止横向提权 |

```
用户提交代码 ──▶ 认证鉴权 ──▶ 规则匹配 ──▶ 沙箱执行（Docker容器/2GB/5min/无网络）
                                                                │
                                                         审查结果 ──▶ 返回前端
```

#### 6.3.4 权限校验流程

每一次 API 请求均需经过以下七步校验链，任一步失败则立即拒绝请求：

```
请求 ──▶ [1] Token 解析 ──▶ [2] 黑名单查询 ──▶ [3] 签名验证 ──▶ [4] 过期检查
                │                    │                │              │
            失败→1003           失败→1002         失败→1002     失败→1001
                                              │
                                              ▼
                                    [5] 权限等级检查
                                              │
                                    - PUBLIC → 直接放行
                                    - LOGGED_IN → JWT.userId 存在
                                    - TEACHER → JWT.role ∈ {TEACHER, ADMIN}
                                    - ADMIN → JWT.role = ADMIN
                                              │
                                         失败→1005
                                              │
                                              ▼
                                    [6] 数据隔离检查
                                              │
                                    根据 JWT.classId 和 JWT.role
                                    注入查询过滤器（Row-Level Filter）
                                              │
                                              ▼
                                       [7] 业务逻辑执行
```

---

### 6.4 数据安全处理

#### 6.4.1 敏感数据分类

| 敏感等级 | 数据类型 | 处理方式 |
|----------|----------|----------|
| 高（High） | 用户密码、身份证号、银行账号 | 加密存储，不出现在日志和响应中 |
| 中（Medium） | 手机号、邮箱、姓名、IP地址 | 脱敏后返回，存储时可选加密 |
| 低（Low） | 操作日志、访问时间、班级名称 | 正常存储，但日志需脱敏处理 |

#### 6.4.2 敏感数据脱敏

响应数据中涉及个人隐私的字段，在返回给前端前必须脱敏：

| 字段类型 | 脱敏规则 | 示例 | 脱敏后 |
|----------|----------|------|--------|
| 手机号 | 中间4位隐藏 | 13812345678 | 138****5678 |
| 邮箱 | @ 后域名隐藏 | zhangsan@example.com | zhangs***@***.com |
| 姓名（TEACHER/STUDENT） | 仅返回姓 | 张三 | 张* |
| 身份证号 | 仅显示前3后4位 | 110101199001011234 | 110***********1234 |
| IP 地址 | 保留前两段 | 192.168.1.100 | 192.168.***.*** |

实现要求：
- 在数据序列化层（JSON Mapper / DTO 转换）统一处理脱敏，禁止在业务代码中手动拼接
- 日志记录中同样执行脱敏，禁止明文记录手机号、密码等高敏感字段
- 审计日志（谁在何时访问了什么数据）需完整记录，但字段值同样执行脱敏

#### 6.4.3 数据加密存储

| 数据场景 | 加密方式 | 密钥管理 |
|----------|----------|----------|
| 用户密码 | bcrypt (cost ≥ 12) | 不需要密钥（哈希），盐值按用户独立存储 |
| 敏感配置项（数据库连接串等） | AES-256-GCM | 密钥存储在 Vault / Kubernetes Secret，不在代码中明文 |
| JWT 签名密钥 | HS256 | 存储在环境变量或 Vault，进程启动时加载 |
| 备份数据 | AES-256-CBC | 备份文件加密，密钥与主密钥分开管理 |

#### 6.4.4 日志安全

| 要求 | 说明 |
|------|------|
| 禁止记录密码 | 请求/响应中的 `password` 字段自动过滤，不写入日志 |
| 脱敏处理 | 日志记录前对敏感字段执行脱敏转换 |
| 日志分级 | DEBUG 级别日志仅在非生产环境启用，生产环境最低 INFO |
| 日志隔离 | 不同班级的操作日志在逻辑上隔离，教师无法查看其他班级的详细操作记录 |
| 审计留存 | 关键操作（登录/登出/删除/权限变更）审计日志保留 ≥ 180 天 |

---

### 6.5 请求并发与线程池安全

#### 6.5.1 并发控制策略

| 场景 | 并发控制方式 | 配置 |
|------|-------------|------|
| 全局写操作 | 数据库乐观锁（Version 字段） | 冲突时返回 HTTP 409 + 错误码 9005 |
| 同一任务状态变更 | 分布式锁（Redis SETNX） | 锁超时 5 秒，防止死锁 |
| 资源竞争（引擎执行槽位） | 信号量（Semaphore）限制 | 最大并发引擎执行数 = CPU 核心数 × 2 |
| 超大请求体 | 请求体大小限制 | 最大请求体 ≤ 50 MB |

#### 6.5.2 线程池设计

服务层使用线程池处理异步任务和并发请求：

| 线程池类型 | 核心线程数 | 最大线程数 | 队列容量 | 适用场景 |
|------------|-----------|-----------|----------|----------|
| HTTP 处理线程池 | CPU 核心数 | CPU 核心数 × 2 | 无界（Tomcat 默认） | 处理 HTTP 请求 |
| 异步任务线程池 | 10 | 50 | 1000（LinkedBlockingQueue） | 代码审查执行、邮件通知 |
| 定时任务线程池 | 5 | 10 | 100 | 定时扫描、过期清理 |
| 数据库连接池 | — | 20 | —（HikariCP 默认） | 数据库访问 |

**线程池拒绝策略：** 使用 `CallerRunsPolicy`，当线程池满时由调用线程直接执行，防止任务丢失，同时提供背压机制。

#### 6.5.3 数据库连接池安全

| 配置项 | 推荐值 | 说明 |
|--------|--------|------|
| 最小空闲连接 | 5 | 保持连接池预热，减少冷启动延迟 |
| 最大连接数 | 20 | 根据数据库并发能力配置 |
| 连接超时 | 30 秒 | 获取连接最大等待时间 |
| 空闲超时 | 10 分钟 | 空闲连接自动回收 |
| 连接验证 | 每次使用前 | 使用 `SELECT 1` 验证连接有效性 |
| 泄漏检测 | 启用（60 秒） | 检测连接未正常归还，触发告警 |

#### 6.5.4 缓存并发安全

| 问题 | 解决方案 |
|------|----------|
| 缓存击穿（热点 key 失效导致大量请求同时穿透到 DB） | 使用分布式锁（Redisson）或永不过期 + 异步更新的策略 |
| 缓存雪崩（大量 key 同时过期） | 使用随机 TTL（基础 TTL ± 10% 随机偏移） |
| 缓存穿透（恶意请求查询不存在的数据） | 布隆过滤器（BloomFilter）拦截不存在 key，或缓存空值（TTL ≤ 60 秒） |
| 缓存与数据库双写一致性 | 采用 Cache-Aside 模式（先更数据库再删缓存），禁止先更新缓存 |

---

## 附录：接口权限总览

| 模块 | 接口 | PUBLIC | LOGGED_IN | TEACHER | ADMIN |
|------|------|--------|-----------|---------|-------|
| 认证 | POST /auth/login | ✅ | — | — | — |
| 认证 | POST /auth/refresh | — | ✅ | ✅ | ✅ |
| 认证 | POST /auth/logout | — | ✅ | ✅ | ✅ |
| 任务 | POST /tasks | — | — | ✅ | ✅ |
| 任务 | GET /tasks | — | ✅ | ✅ | ✅ |
| 任务 | GET /tasks/{id} | — | ✅（参与者） | ✅（参与者） | ✅ |
| 任务 | PATCH /tasks/{id}/status | — | — | ✅ | — |
| 任务 | DELETE /tasks/{id} | — | — | — | ✅ |
| 缺陷 | GET /tasks/{taskId}/issues | — | ✅（参与者） | ✅（参与者） | ✅ |
| 缺陷 | POST /issues/{id}/assign | — | — | ✅ | — |
| 缺陷 | PATCH /issues/{id}/status | — | — | ✅（全局） | — |
| 规则 | GET /rules | — | ✅ | ✅ | ✅ |
| 规则 | POST /rules | — | — | — | ✅ |
| 规则 | PUT /rules/{id} | — | — | — | ✅ |
| 规则 | PATCH /rules/{id}/enable | — | — | — | ✅ |

> **图例：** ✅ = 有权限，— = 无权限，数字标注 = 需额外数据隔离检查

---

*本章节由 Worker4（接口和权限安全专家）编写，完成日期：2026-06-22*
