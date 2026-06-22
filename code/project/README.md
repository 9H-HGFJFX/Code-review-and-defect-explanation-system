# 代码审查与缺陷解释系统

基于 B/S 架构的代码审查与缺陷解释系统，采用前后端分离设计。

## 技术栈

### 后端
- Spring Boot 3.2+
- Spring Security + JWT
- MyBatis-Plus 3.5+
- MySQL 8.0
- Redis
- JavaParser (AST解析)
- Knife4j (API文档)

### 前端
- Vue 3.4+
- Vite 5.0+
- Element Plus
- Monaco Editor
- ECharts
- Pinia

## 项目结构

```
project/
├── backend/                 # 后端 Spring Boot 项目
│   ├── src/
│   │   └── main/
│   │       ├── java/com/codereview/
│   │       │   ├── controller/    # 控制器层
│   │       │   ├── service/       # 服务层
│   │       │   ├── mapper/        # 数据访问层
│   │       │   ├── entity/       # 实体类
│   │       │   ├── dto/           # 数据传输对象
│   │       │   ├── config/        # 配置类
│   │       │   ├── engine/        # 代码审查引擎
│   │       │   └── exception/     # 异常处理
│   │       └── resources/
│   │           └── application.yml
│   └── pom.xml
│
├── frontend/               # 前端 Vue3 项目
│   ├── src/
│   │   ├── api/            # API 接口
│   │   ├── assets/         # 静态资源
│   │   ├── components/     # 公共组件
│   │   ├── router/         # 路由配置
│   │   ├── stores/         # 状态管理
│   │   ├── utils/          # 工具函数
│   │   ├── views/          # 页面组件
│   │   ├── App.vue
│   │   └── main.js
│   ├── index.html
│   ├── package.json
│   └── vite.config.js
│
└── database/               # 数据库脚本
    └── init.sql
```

## 快速开始

### 1. 环境准备

- JDK 17+
- MySQL 8.0+
- Redis 7.0+
- Node.js 18+
- Maven 3.9+

### 2. 数据库初始化

```bash
mysql -u root -p < database/init.sql
```

### 3. 后端启动

```bash
cd backend

# 修改 src/main/resources/application.yml 中的数据库配置
# 修改 Redis 配置

# 启动项目
mvn spring-boot:run
```

后端启动后访问：http://localhost:8080/doc.html (API文档)

### 4. 前端启动

```bash
cd frontend

# 安装依赖
npm install

# 开发模式启动
npm run dev

# 生产环境打包
npm run build
```

前端启动后访问：http://localhost:3000

### 5. 默认账号

| 角色   | 用户名   | 密码       |
|--------|----------|------------|
| 管理员 | admin    | admin123   |
| 教师   | teacher  | admin123   |
| 学生   | student  | admin123   |

## 系统功能

### 1. 代码审查
- 支持 Java 代码在线提交审查
- 基于 AST 的静态代码分析
- 支持同步/异步审查模式
- 代码行数限制（最大5000行）

### 2. 审查规则
- 三大类规则：安全漏洞、代码缺陷、代码规范
- 支持正则表达式和 AST 匹配
- 规则热更新（无需重启）
- 支持班级私有规则

### 3. 问题分类
- CRITICAL: 严重高危漏洞
- ERROR: 代码错误
- WARNING: 代码警告
- SUGGESTION: 优化建议

### 4. 班级管理（教师）
- 创建和管理班级
- 添加/移除班级学生
- 班级私有规则配置

### 5. 数据统计
- 审查趋势分析
- 问题分布统计
- 多维度数据可视化

### 6. 权限控制
- 三级角色权限模型
- RBAC 权限管理
- JWT 无状态认证

## API 接口

详细 API 文档请访问：http://localhost:8080/doc.html

主要接口：

| 模块 | 接口 | 说明 |
|------|------|------|
| 认证 | POST /api/auth/login | 用户登录 |
| 认证 | POST /api/auth/register | 用户注册 |
| 审查 | POST /api/review/submit | 提交代码审查 |
| 审查 | GET /api/review/list | 查询审查历史 |
| 规则 | GET /api/rule/list | 规则列表 |
| 规则 | POST /api/rule/add | 创建规则 |
| 班级 | POST /api/class/add | 创建班级 |
| 统计 | GET /api/statistic/overview | 统计概览 |

## 开发说明

### 代码审查引擎

代码审查引擎位于 `backend/src/main/java/com/codereview/engine/`，采用插件化设计：

- `SecurityScanner`: 安全扫描器（SQL注入、XSS、硬编码等）
- `DefectDetector`: 缺陷检测器（空指针、资源泄漏等）
- `StyleChecker`: 风格检查器（命名规范、方法长度等）

### 规则热更新

1. 修改数据库中的规则
2. 调用 `/api/rule/refresh-cache` 接口
3. 规则自动同步到所有服务节点

### 异步审查

- 代码行数 > 500 行时自动切换为异步模式
- 前端通过轮询 `/api/review/async/result/{taskId}` 获取结果
- 后台使用独立线程池处理，不会阻塞主线程

## License

MIT
