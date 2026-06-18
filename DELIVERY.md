# 项目交付说明

## 启动方式

### 1. Docker 一键启动（推荐）
```bash
docker compose up -d
```
访问：
- 前端: http://localhost
- 后端 API: http://localhost/api
- 接口文档: http://localhost/doc.html

### 2. 本地开发模式
- 后端: `cd code-audit-server && mvn spring-boot:run`
- 前端: `cd code-audit-web && npm install && npm run dev`

## 默认账号
- 管理员：admin / 123456
- 教师：teacher / 123456
- 学生：student / 123456

## 目录速览
- `code-audit-server/` - Spring Boot 后端
- `code-audit-web/` - Vue3 前端
- `deploy/` - Nginx 配置
- `docker-compose.yml` - 一键编排
- `概要设计说明书.docx` / `详细设计说明书.docx` / `需求分析说明书.docx` - 设计文档
