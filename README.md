# 代码审查与缺陷解释系统 (CodeInsight - Defect Decoder)

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.4-42b883)](https://vuejs.org)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1)](https://www.mysql.com)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

> 基于 JavaParser 的 Java 静态代码审查与缺陷解释系统，集成 MiniMax M3 API 提供 AI 增强缺陷解释。

## 功能特性

### 10 大核心模块
- **M01 代码提交与展示**：Monaco Editor 语法高亮、文件上传、实时行数校验
- **M02 规范检查引擎**：类名/方法名/常量名/格式/方法长度（AST 静态分析）
- **M03 缺陷检测引擎**：空指针、资源泄漏、空 catch、宽 catch
- **M04 安全扫描引擎**：SQL 注入、硬编码密码、XSS、命令注入
- **M05 修改建议生成**：每条问题附带修复建议 + 修改前/后代码示例
- **M06 审查报告输出**：结构化报告 + PDF/Word 双格式导出
- **M07 规则库管理**：CRUD + 启用/禁用 + Redis 热更新
- **M08 历史记录管理**：分页查询、详情查看
- **M09 用户管理**：三级 RBAC（STUDENT/TEACHER/ADMIN）+ JWT 认证
- **M10 数据统计看板**：ECharts 可视化、高频问题 Top10

### 技术栈
- **后端**：Spring Boot 3.2 + MyBatis-Plus + JavaParser 3.25 + Spring Security + JWT
- **前端**：Vue 3.4 + TypeScript + Vite 5 + Element Plus 2.6 + Monaco Editor + ECharts 5
- **数据库**：MySQL 8.0 + Redis 7.0
- **AI 增强**：MiniMax M3 API（可选，用于缺陷解释）
- **部署**：Docker Compose 一键编排

## 快速开始

### 方式一：Docker Compose 一键启动（推荐）

```bash
# 1. 克隆项目
git clone <your-repo> code-insight
cd code-insight

# 2. 启动所有服务（MySQL + Redis + Backend + Nginx）
docker compose up -d

# 3. 等待服务就绪（首次约 2-3 分钟）
docker compose logs -f backend

# 4. 访问
# 前端: http://localhost
# API:  http://localhost/api
# 文档: http://localhost/doc.html
```

### 方式二：本地开发模式

#### 后端

```bash
cd code-audit-server

# 1. 启动 MySQL 和 Redis（可使用 docker）
docker run -d --name mysql-dev -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root123 -e MYSQL_DATABASE=code_audit mysql:8.0
docker run -d --name redis-dev -p 6379:6379 redis:7.0-alpine

# 2. 初始化数据库
mysql -uroot -proot123 < src/main/resources/db/init.sql

# 3. 编译运行（需要 JDK 17+ 和 Maven 3.9+）
mvn spring-boot:run

# 后端将运行在 http://localhost:8080
```

#### 前端

```bash
cd code-audit-web

# 1. 安装依赖（需要 Node.js 18+）
npm install

# 2. 启动开发服务器
npm run dev

# 前端将运行在 http://localhost:5173
```

## 默认账号

| 角色 | 用户名 | 密码 | 权限 |
|------|--------|------|------|
| 管理员 | `admin` | `123456` | 全部权限 |
| 教师 | `teacher` | `123456` | 规则管理、数据统计、班级管理 |
| 学生 | `student` | `123456` | 提交代码、查看个人记录 |

## AI 缺陷解释（可选）

默认未启用。配置 MiniMax M3 API 启用 AI 解释：

```bash
# docker-compose.yml 中修改
M3_ENABLED: "true"
M3_API_KEY: "your-m3-api-key"
M3_BASE_URL: "https://api.MiniMax.chat/v1"
M3_MODEL: "MiniMax-M3"
```

启动后，每次审查的每个问题都会异步调用 M3 API 补充"AI 缺陷解释"，包含根本原因、风险评估、推荐方案。

## 项目结构

```
code-insight--defect-decoder/
├── code-audit-server/                # Spring Boot 后端
│   ├── src/main/java/com/codeaudit/
│   │   ├── CodeAuditApplication.java # 启动类
│   │   ├── config/                   # 配置（CORS, JWT, Security, Redis, MyBatis, Async）
│   │   ├── security/                 # 安全（JwtUtil, JwtFilter, SecurityUtil）
│   │   ├── common/                   # 公共（Result, PageResult, 异常体系）
│   │   ├── entity/                   # 实体类
│   │   ├── repository/               # MyBatis-Plus Mapper
│   │   ├── dto/ vo/                  # DTO / VO
│   │   ├── engine/                   # 审查引擎核心 ⭐
│   │   │   ├── ReviewEngine.java     # 引擎主控
│   │   │   ├── parser/               # JavaParser 包装
│   │   │   ├── checker/              # 规范检查器（3 个）
│   │   │   ├── detector/             # 缺陷检测器（4 个）
│   │   │   ├── scanner/              # 安全扫描器（4 个）
│   │   │   └── executor/             # RuleExecutor 接口
│   │   ├── service/                  # 业务服务
│   │   ├── controller/               # REST 控制器
│   │   └── ai/                       # MiniMax M3 客户端
│   └── src/main/resources/
│       ├── application.yml           # 主配置
│       ├── application-dev.yml       # 开发环境
│       ├── application-prod.yml      # 生产环境
│       └── db/init.sql               # 数据库初始化脚本
│
├── code-audit-web/                   # Vue3 前端
│   ├── src/
│   │   ├── api/                      # API 封装
│   │   ├── components/               # 通用组件（CodeEditor/IssueCard/ReportViewer/StatsChart）
│   │   ├── views/                    # 页面（Home/Login/Register/Review/History/Rules/Stats）
│   │   ├── router/                   # 路由
│   │   ├── store/                    # Pinia
│   │   ├── types/                    # TypeScript 类型
│   │   ├── utils/                    # 工具（request.ts）
│   │   └── styles/                   # 全局样式
│   └── package.json
│
├── deploy/                           # 部署配置
│   ├── nginx/
│   │   ├── nginx.conf
│   │   └── conf.d/code-audit.conf
├── docker-compose.yml                # 一键编排
├── 概要设计说明书.docx               # 项目设计文档
├── 详细设计说明书.docx
├── 需求分析说明书.docx
└── README.md                         # 本文件
```

## 内置规则（MVP 13 条）

| 分类 | 规则 | 严重级别 | 检查方式 |
|------|------|----------|----------|
| STYLE | 类名大驼峰 | WARNING | AST |
| STYLE | 方法名小驼峰 | WARNING | AST |
| STYLE | 常量全大写 | WARNING | AST |
| STYLE | 单行字符超长 | SUGGESTION | AST |
| STYLE | 方法过长 | WARNING | AST |
| DEFECT | 空指针风险 | ERROR | AST |
| DEFECT | 资源未关闭 | ERROR | AST |
| DEFECT | 空 catch 块 | WARNING | AST |
| DEFECT | 捕获过宽异常 | WARNING | AST |
| SECURITY | SQL 注入 | CRITICAL | REGEX |
| SECURITY | 硬编码密码 | CRITICAL | REGEX |
| SECURITY | XSS 风险 | CRITICAL | REGEX |
| SECURITY | 命令注入 | CRITICAL | REGEX |

## 核心 API

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 认证 | POST | `/api/auth/register` | 用户注册 |
| 认证 | POST | `/api/auth/login` | 登录 |
| 认证 | POST | `/api/auth/refresh` | 刷新 Token |
| 认证 | POST | `/api/auth/logout` | 登出 |
| 审查 | POST | `/api/review/submit` | 提交代码审查 |
| 审查 | GET | `/api/review/{id}` | 审查详情 |
| 审查 | GET | `/api/review/list` | 个人历史 |
| 审查 | GET | `/api/review/list-all` | 全平台历史（教师/管理员） |
| 报告 | GET | `/api/report/export/{reviewId}?type=pdf\|word` | 导出报告 |
| 规则 | GET | `/api/rule/list` | 规则列表 |
| 规则 | POST | `/api/rule/add` | 新增规则 |
| 规则 | PUT | `/api/rule/update/{id}` | 编辑规则（自动热更新） |
| 规则 | DELETE | `/api/rule/delete/{id}` | 删除规则 |
| 规则 | PUT | `/api/rule/toggle/{id}` | 启用/禁用 |
| 规则 | POST | `/api/rule/refresh` | 手动刷新缓存 |
| 统计 | GET | `/api/statistic/overview` | 数据看板 |
| 班级 | POST | `/api/class/add` | 创建班级 |
| 班级 | POST | `/api/class/member/add` | 批量添加学生 |

完整接口文档：<http://localhost/doc.html>

## 性能指标

- 单次审查响应时间：500 行代码 ≤ 3 秒
- 同步审查支持：50 用户并发
- 列表查询响应：≤ 500ms

## 安全设计

- **认证**：JWT（Access 2h + Refresh 7d）+ Redis 黑名单
- **密码**：BCrypt 单向加密
- **限流**：单用户每分钟最多 5 次审查
- **跨域**：网关层统一处理
- **注入防护**：MyBatis-Plus 预编译 + 引擎内置 SQL 注入检测
- **逻辑删除**：所有表统一 `is_deleted` 字段，支持软删除
- **审计日志**：敏感操作完整记录

## 验收对应

按需求分析说明书 6.2 重点验收项：
- ✅ 限定代码规模（500 行）：前端 + 后端双重校验
- ✅ 安全提示：报告中红色/橙色高亮 + 独立"安全风险摘要"区
- ✅ 错误建议评测：内置 13 条规则覆盖 STYLE/DEFECT/SECURITY 三大类
- ✅ 误报/漏报：可通过规则管理界面关闭误报规则

## 已实现 vs V2 规划

### V1（当前 MVP）
- ✅ 全部 10 个核心模块
- ✅ 13 条内置规则（覆盖需求 6.1 验收项）
- ✅ 完整 RBAC + JWT
- ✅ Docker 一键部署
- ✅ AI 缺陷解释（接口预留）
- ✅ PDF/Word 报告导出

### V2（规划）
- ⏳ 异步审查任务（500-5000 行）
- ⏳ 规则可视化编辑器（拖拽式配置）
- ⏳ 班级私有规则扩展
- ⏳ 实时协作审查
- ⏳ Git 集成（commit 时自动审查）
- ⏳ IDEA 插件

## 开发工具

- AI 编程工具：[MiniMax Code](https://code.MiniMax.chat)
- 手工代码修改：VSCode / IntelliJ IDEA

## License

MIT
