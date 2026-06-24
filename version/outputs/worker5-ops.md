# 第7章 异常处理

## 7.1 错误码体系说明

> **重要声明：** 本系统错误码体系以 Worker4 第5章《接口设计》5.1.3 节为准。Worker5 内部异常类错误码为实现细节，对外暴露时统一继承 Worker4 定义的错误码规范。
>
> 本章所述内部异常错误码仅用于内部组件通信，最终对外 HTTP 响应和客户端可见错误码均遵循 Worker4 标准。

**错误码归属对照表（以 Worker4 为准）：**

| 内部异常类型 | 内部错误码 | 对外错误码（Worker4标准） | HTTP状态码 | 说明 |
|-------------|-----------|-------------------------|-----------|------|
| UnauthorizedException | 1000 | 1000 | 401 | 未认证 |
| ForbiddenException | 1002 | 1002 | 403 | 无权限 |
| CircuitBreakerException | 9001 | 9001 | 503 | 服务熔断中 |
| ExternalServiceException | 9002 | 9002 | 502 | 外部服务异常 |

---

## 7.2 全局异常分类

系统采用统一的异常分类体系，所有异常均实现 `BaseException` 接口，确保错误码和HTTP状态码的规范映射。

### 7.2.1 异常类型定义

| 异常类型 | 错误码范围 | HTTP状态码 | 说明 |
|---------|-----------|-----------|------|
| BusinessException | 1001-1999 | 400 | 业务校验失败 |
| UnauthorizedException | 1000 | 401 | 未认证 |
| ForbiddenException | 1002 | 403 | 无权限 |
| ResourceNotFoundException | 1003 | 404 | 资源不存在 |
| CircuitBreakerException | 9001 | 503 | 熔断触发 |
| ExternalServiceException | 9002 | 502 | 外部服务超时 |
| StorageException | 9003 | 500 | 文件存储失败 |
| ScanTimeoutException | 9004 | 504 | 代码扫描超时 |

### 7.2.2 异常类结构

```java
public class BaseException extends RuntimeException {
    private final int code;
    private final int httpStatus;
    
    public BaseException(int code, int httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }
    
    // getter方法省略
}
```

## 7.3 统一异常处理

### 7.3.1 Controller层统一拦截

使用 `@ControllerAdvice` 实现全局异常拦截，统一处理所有Controller抛出的异常。

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BaseException.class)
    public Result<Void> handleBaseException(BaseException e, HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        log.error("RequestId: {}, Exception: {}", requestId, e.getMessage(), e);
        
        return Result.error(e.getCode(), e.getMessage());
    }
    
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        log.error("RequestId: {}, Unexpected Exception: {}", requestId, e.getMessage(), e);
        
        return Result.error(5000, "系统内部错误");
    }
}
```

### 7.3.2 错误码转换机制

| 错误码 | 描述 | HTTP状态码 |
|-------|------|-----------|
| 1000 | 未认证 | 401 |
| 1001-1999 | 业务校验失败 | 400 |
| 1002 | 无权限访问 | 403 |
| 1003 | 资源不存在 | 404 |
| 9001 | 服务熔断中 | 503 |
| 9002 | 外部服务异常 | 502 |
| 9003 | 存储服务异常 | 500 |
| 9004 | 扫描超时 | 504 |
| 5000 | 系统内部错误 | 500 |

### 7.3.3 日志记录规范

- **ERROR级别日志**：记录所有业务异常和系统异常
- **请求ID追踪**：通过 `X-Request-ID` 头部实现全链路追踪
- **敏感信息脱敏**：日志中不记录用户密码、Token、密钥等敏感信息
- **日志格式**：

```json
{
    "timestamp": "2024-01-15T10:30:00Z",
    "level": "ERROR",
    "requestId": "req-uuid-123",
    "traceId": "trace-uuid-456",
    "message": "业务异常",
    "exception": {
        "type": "BusinessException",
        "code": 1001,
        "message": "规则ID不存在"
    }
}
```

### 7.3.4 敏感信息保护

统一异常响应结构不包含以下敏感信息：

- 数据库连接信息
- 内部系统路径
- 第三方API密钥
- 用户个人隐私数据
- 完整的异常堆栈信息（仅在特定日志级别下输出）

```java
public class ErrorResponse {
    private int code;
    private String message;
    private String requestId;
    // 不包含：stackTrace, internalPath, sensitiveData
}
```

---

## 7.3 审查引擎健壮性兜底策略

为确保代码审查引擎在异常情况下仍能提供最大可用性，系统实现了完善的降级和兜底机制，确保单个文件或规则的问题不会导致整个审查任务失败。

### 7.3.1 规则加载失败降级策略

系统采用 `fail-mode=log-and-skip` 策略处理规则加载失败场景，确保审查引擎的启动不会因单条规则错误而阻塞。

#### 7.3.1.1 策略定义

| 策略模式 | 行为描述 | 适用场景 |
|---------|---------|---------|
| `fail-mode=log-and-skip` | 加载失败时跳过该规则并记录ERROR日志，不阻止扫描启动 | 默认策略，生产环境推荐 |
| `fail-mode=strict` | 加载失败时抛出异常，中断启动流程 | 单元测试/严格校验场景 |
| `fail-mode=warn` | 加载失败时记录WARN日志并跳过 | 开发调试场景 |

#### 7.3.1.2 降级处理流程

```
规则加载流程
┌─────────────────────────────────────────────────────────────┐
│  1. 扫描规则配置文件目录                                     │
│  2. 逐个加载规则文件                                         │
│  3. 执行规则语法校验                                         │
│  4. 校验失败？                                               │
│     ├─ YES → 按 fail-mode 处理                              │
│     │         ├─ log-and-skip: 记录ERROR + 跳过该规则       │
│     │         ├─ strict: 抛出 RuleLoadException            │
│     │         └─ warn: 记录WARN + 跳过该规则                │
│     └─ NO → 注册到规则引擎                                   │
│  5. 继续处理下一个规则文件                                     │
│  6. 返回加载结果报告                                          │
└─────────────────────────────────────────────────────────────┘
```

#### 7.3.1.3 实现代码

```java
public class RuleLoader {
    
    private FailMode failMode = FailMode.LOG_AND_SKIP;
    
    public RuleLoadResult loadRules(Path rulesDir) {
        RuleLoadResult result = new RuleLoadResult();
        List<Rule> loadedRules = new ArrayList<>();
        List<FailedRuleInfo> failedRules = new ArrayList<>();
        
        File[] ruleFiles = rulesDir.toFile().listFiles(
            f -> f.getName().endsWith(".yaml") || f.getName().endsWith(".json")
        );
        
        for (File ruleFile : ruleFiles) {
            try {
                Rule rule = parseAndValidate(ruleFile);
                loadedRules.add(rule);
                log.info("Rule loaded successfully: {}", rule.getId());
            } catch (Exception e) {
                FailedRuleInfo failedInfo = new FailedRuleInfo(
                    ruleFile.getName(), e.getMessage(), e
                );
                failedRules.add(failedInfo);
                
                switch (failMode) {
                    case LOG_AND_SKIP:
                        log.error("Rule loading failed, skipping: {}", 
                            ruleFile.getName(), e);
                        break;
                    case STRICT:
                        throw new RuleLoadException(
                            "Rule loading failed in strict mode: " + ruleFile.getName(), e
                        );
                    case WARN:
                        log.warn("Rule loading failed, skipping: {}", 
                            ruleFile.getName(), e);
                        break;
                }
            }
        }
        
        result.setLoadedRules(loadedRules);
        result.setFailedRules(failedRules);
        result.setSummary(loadedRules.size(), failedRules.size());
        
        return result;
    }
}
```

#### 7.3.1.4 配置示例

```yaml
rules:
  load:
    fail-mode: log-and-skip  # 默认策略
    # 可选值: log-and-skip | strict | warn
  directory: /app/rules
  auto-reload:
    enabled: true
    interval-seconds: 10
```

### 7.3.2 AST解析异常兜底策略

系统采用单文件级别隔离策略，确保单个文件的解析异常不会影响整个审查任务的执行。

#### 7.3.2.1 策略定义

| 异常场景 | 处理策略 | 结果影响 |
|---------|---------|---------|
| 单文件语法错误 | 记录ERROR + 跳过该文件 | 返回部分结果 + 失败文件列表 |
| 单文件编码错误 | 记录WARN + 尝试自动检测编码 | 重试或跳过 |
| 单文件解析超时 | 记录WARN + 跳过该文件 | 返回部分结果 + 超时文件列表 |
| 单文件大小超限 | 记录WARN + 跳过该文件 | 返回部分结果 + 超限文件列表 |

#### 7.3.2.2 处理流程

```
AST解析流程
┌─────────────────────────────────────────────────────────────┐
│  1. 获取待扫描文件列表                                        │
│  2. 遍历每个文件                                              │
│  3. 执行AST解析                                               │
│  4. 解析成功？                                                │
│     ├─ YES → 执行规则匹配 → 收集问题                         │
│     └─ NO → 捕获解析异常                                       │
│           ├─ 记录ERROR日志（文件路径、异常类型、堆栈摘要）      │
│           ├─ 将文件添加到 failedFiles 列表                     │
│           └─ 继续处理下一个文件                               │
│  5. 返回审查结果（部分结果 + 失败文件列表）                      │
└─────────────────────────────────────────────────────────────┘
```

#### 7.3.2.3 实现代码

```java
public class ScanEngine {
    
    public ScanResult scan(List<SourceFile> files, List<Rule> rules) {
        ScanResult result = new ScanResult();
        List<Issue> allIssues = new ArrayList<>();
        List<FailedFileInfo> failedFiles = new ArrayList<>();
        
        for (SourceFile file : files) {
            try {
                // 尝试从缓存获取AST
                ASTNode ast = astCache.get(file.getPath(), 
                    k -> parseAST(file)
                );
                
                // 执行规则匹配
                List<Issue> issues = ruleEngine.match(ast, rules);
                allIssues.addAll(issues);
                
            } catch (ParseException e) {
                // 语法解析失败
                log.error("AST parse failed for file: {}, reason: {}", 
                    file.getPath(), e.getMessage());
                failedFiles.add(new FailedFileInfo(
                    file.getPath(), 
                    FailureReason.PARSE_ERROR, 
                    e.getMessage()
                ));
                
            } catch (TimeoutException e) {
                // 解析超时
                log.warn("AST parse timeout for file: {}, reason: {}", 
                    file.getPath(), e.getMessage());
                failedFiles.add(new FailedFileInfo(
                    file.getPath(), 
                    FailureReason.TIMEOUT, 
                    e.getMessage()
                ));
                
            } catch (EncodingException e) {
                // 编码错误
                log.warn("File encoding error: {}, attempt fallback: {}", 
                    file.getPath(), e.getMessage());
                failedFiles.add(new FailedFileInfo(
                    file.getPath(), 
                    FailureReason.ENCODING_ERROR, 
                    e.getMessage()
                ));
            }
        }
        
        result.setIssues(allIssues);
        result.setFailedFiles(failedFiles);
        result.setStats(new ScanStats(
            files.size(), 
            files.size() - failedFiles.size(), 
            failedFiles.size()
        ));
        
        return result;
    }
}
```

#### 7.3.2.4 响应结果结构

```json
{
    "success": true,
    "scanId": "scan-uuid-123",
    "stats": {
        "totalFiles": 100,
        "processedFiles": 97,
        "failedFiles": 3,
        "totalIssues": 45
    },
    "issues": [...],
    "failedFiles": [
        {
            "path": "/src/broken.java",
            "reason": "PARSE_ERROR",
            "detail": "Syntax error at line 42: unexpected token"
        },
        {
            "path": "/src/huge.java",
            "reason": "TIMEOUT",
            "detail": "Parse exceeded 30 seconds timeout"
        },
        {
            "path": "/src/unknown-encoding.txt",
            "reason": "ENCODING_ERROR",
            "detail": "Unable to detect file encoding"
        }
    ],
    "message": "Scan completed with partial results. 3 files failed to parse."
}
```

---

# 第8章 性能优化

## 8.1 线程池隔离

采用线程池隔离策略，将不同类型的任务分配到独立的线程池，避免相互影响。

### 8.1.1 扫描任务池

用于执行代码扫描任务，处理CPU密集型操作。

```yaml
scan-task-executor:
  core-pool-size: 5
  max-pool-size: 10
  queue-capacity: 100
  thread-name-prefix: scan-task-
  keep-alive-seconds: 60
  rejection-policy: CallerRunsPolicy
```

### 8.1.2 接口任务池

用于处理HTTP接口请求，快速响应用户操作。

```yaml
api-task-executor:
  core-pool-size: 10
  max-pool-size: 20
  queue-capacity: 500
  thread-name-prefix: api-task-
  keep-alive-seconds: 30
  rejection-policy: CallerRunsPolicy
```

### 8.1.3 规则编译池

用于编译和验证代码规则，与其他任务完全隔离。

```yaml
rule-compile-executor:
  core-pool-size: 2
  max-pool-size: 4
  queue-capacity: 50
  thread-name-prefix: rule-compile-
  keep-alive-seconds: 120
  rejection-policy: AbortPolicy
```

### 8.1.4 线程池隔离配置

```java
@Configuration
public class ExecutorConfig {
    
    @Bean("scanTaskExecutor")
    public ThreadPoolExecutor scanTaskExecutor() {
        return new ThreadPoolExecutor(
            5, 10, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadFactoryBuilder().setNameFormat("scan-task-%d").build(),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
    
    @Bean("apiTaskExecutor")
    public ThreadPoolExecutor apiTaskExecutor() {
        return new ThreadPoolExecutor(
            10, 20, 30L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(500),
            new ThreadFactoryBuilder().setNameFormat("api-task-%d").build(),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
    
    @Bean("ruleCompileExecutor")
    public ThreadPoolExecutor ruleCompileExecutor() {
        return new ThreadPoolExecutor(
            2, 4, 120L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(50),
            new ThreadFactoryBuilder().setNameFormat("rule-compile-%d").build(),
            new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
```

## 8.2 缓存策略

### 8.2.1 多级缓存架构

```
┌─────────────────────────────────────────────┐
│                  客户端请求                  │
└─────────────────┬───────────────────────────┘
                  ▼
┌─────────────────────────────────────────────┐
│              本地缓存 (L1)                    │
│         Caffeine LRU, max=1000文件           │
│              TTL: 5分钟                       │
└─────────────────┬───────────────────────────┘
                  ▼
┌─────────────────────────────────────────────┐
│              分布式缓存 (L2)                  │
│                  Redis                        │
│         热点规则/Token/结果缓存               │
│              可配置TTL                        │
└─────────────────┬───────────────────────────┘
                  ▼
┌─────────────────────────────────────────────┐
│                 数据源                        │
│            MySQL / 文件系统                   │
└─────────────────────────────────────────────┘
```

### 8.2.2 热点规则缓存

用于缓存高频访问的代码规则配置。

```yaml
cache:
  rule:
    type: redis
    ttl: 3600  # 1小时
    key-prefix: "rule:"
```

### 8.2.3 AST缓存

本地LRU缓存，用于存储已解析的AST结果。

```java
@Configuration
public class CacheConfig {
    
    @Bean
    public Cache<String, ASTNode> astCache() {
        return Caffeine.newBuilder()
            .maximumSize(1000)          // 最多缓存1000个文件的AST
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats()
            .build();
    }
}
```

### 8.2.4 审查结果缓存

```yaml
cache:
  review-result:
    type: redis
    ttl: 86400  # 24小时
    key-prefix: "review:"
```

### 8.2.5 用户Token缓存

```yaml
cache:
  user-token:
    type: redis
    ttl: 7200  # 2小时
    key-prefix: "token:"
```

## 8.3 性能指标

### 8.3.1 性能基准

| 指标名称 | 目标值 | 说明 |
|---------|-------|------|
| 单文件扫描时长 | < 5秒 | 文件大小 < 1MB |
| 100文件项目扫描时长 | < 60秒 | 包含AST解析和规则匹配 |
| 接口响应时间 P99 | < 500ms | 不包含文件上传场景 |
| 并发扫描任务上限 | 10 | 同时进行的扫描任务数 |

### 8.3.2 性能监控指标

```yaml
management:
  metrics:
    tags:
      application: code-review-system
  export:
    prometheus:
      enabled: true
```

关键监控指标：

- `scan_task_queue_length`：当前扫描任务队列长度
- `scan_task_avg_duration_seconds`：平均扫描时长
- `scan_task_error_rate`：扫描任务错误率
- `api_request_duration_seconds`：API请求响应时间
- `cache_hit_ratio`：缓存命中率

---

# 第9章 运维部署

## 9.1 Docker部署

### 9.1.1 Dockerfile（多阶段构建）

```dockerfile
# 构建阶段
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# 安全加固：非root用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /build/target/app.jar /app/app.jar

ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
```

### 9.1.2 镜像构建与发布

```bash
# 构建镜像
docker build -t code-review:v1.0.0 .

# 标签发布
docker tag code-review:v1.0.0 registry.example.com/code-review:v1.0.0

# 推送到镜像仓库
docker push registry.example.com/code-review:v1.0.0
```

## 9.2 docker-compose.yml

### 9.2.1 完整服务编排

```yaml
version: '3.8'

services:
  app:
    image: code-review:${VERSION:-latest}
    container_name: code-review-app
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/code_review?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4
      - SPRING_DATASOURCE_USERNAME=${DB_USER:-codereview}
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD:-secret}
      - SPRING_REDIS_HOST=redis
      - SPRING_REDIS_PORT=6379
      - JAVA_OPTS=-Xms512m -Xmx2g -XX:+UseG1GC
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_started
    volumes:
      - ./logs:/app/logs
      - upload-data:/app/uploads
    restart: unless-stopped
    networks:
      - code-review-network
    deploy:
      resources:
        limits:
          memory: 2G
        reservations:
          memory: 512M

  mysql:
    image: mysql:8.0
    container_name: code-review-mysql
    ports:
      - "3306:3306"
    environment:
      - MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD:-rootsecret}
      - MYSQL_DATABASE=code_review
      - MYSQL_USER=${DB_USER:-codereview}
      - MYSQL_PASSWORD=${DB_PASSWORD:-secret}
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
      - --default-authentication-plugin=mysql_native_password
    volumes:
      - mysql-data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql:ro
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped
    networks:
      - code-review-network

  redis:
    image: redis:7.0-alpine
    container_name: code-review-redis
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    volumes:
      - redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped
    networks:
      - code-review-network

  prometheus:
    image: prom/prometheus:v2.45.0
    container_name: code-review-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.enable-lifecycle'
    restart: unless-stopped
    networks:
      - code-review-network
    profiles:
      - monitoring

volumes:
  mysql-data:
  redis-data:
  prometheus-data:
  upload-data:

networks:
  code-review-network:
    driver: bridge
```

### 9.2.2 环境变量文件 (.env)

```env
# 版本号
VERSION=v1.0.0

# 数据库配置
MYSQL_ROOT_PASSWORD=root_secret_password_2024
DB_USER=codereview
DB_PASSWORD=codereview_pass_2024

# Redis配置（可选）
REDIS_PASSWORD=

# Java堆内存
JAVA_XMS=512m
JAVA_XMX=2g
```

## 9.3 版本管理

### 9.3.1 镜像标签策略

采用语义化版本号（SemVer）：

```
v{MAJOR}.{MINOR}.{PATCH}
```

- **MAJOR**：不兼容的重大变更
- **MINOR**：向后兼容的功能新增
- **PATCH**：向后兼容的问题修复

### 9.3.2 版本回滚流程

```bash
#!/bin/bash
# rollback.sh - 版本回滚脚本

# 回滚到指定版本
rollback_version() {
    local version=$1
    
    echo "开始回滚到版本: $version"
    
    # 1. 停止当前服务
    docker-compose down
    
    # 2. 拉取目标版本镜像
    docker-compose pull app:$version
    
    # 3. 使用目标版本启动服务
    VERSION=$version docker-compose up -d
    
    # 4. 验证服务健康状态
    sleep 10
    curl -f http://localhost:8080/actuator/health
    
    echo "回滚完成"
}

# 保留最近5个版本镜像，清理旧版本
cleanup_old_images() {
    echo "清理旧版本镜像..."
    docker image prune -f --filter "until=168h"  # 保留最近7天
}
```

### 9.3.3 回滚命令

```bash
# 回滚到上一版本
./rollback.sh v1.0.0

# 回滚到指定版本
./rollback.sh v0.9.5

# 查看可回滚版本
docker images | grep code-review
```

## 9.4 监控体系

### 9.4.1 Prometheus指标端点

| 端点 | 说明 |
|-----|------|
| `/actuator/prometheus` | Prometheus抓取指标 |
| `/actuator/health` | 健康检查端点 |
| `/actuator/info` | 应用信息 |

### 9.4.2 健康检查配置

```yaml
management:
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
  health:
    mysql:
      enabled: true
    redis:
      enabled: true
    diskSpace:
      enabled: true
      threshold: 1073741824  # 1GB
```

### 9.4.3 关键监控指标

| 指标名 | 类型 | 说明 | 告警阈值 |
|-------|------|------|---------|
| scan_task_queue_length | Gauge | 当前排队任务数 | > 80 |
| scan_task_avg_duration_seconds | Summary | 平均扫描时长 | > 5s |
| scan_task_error_rate | Gauge | 扫描错误率 | > 5% |
| jvm_memory_used_bytes | Gauge | JVM内存使用 | > 1.5GB |
| http_server_requests_seconds | Summary | HTTP请求耗时 | P99 > 500ms |
| database_connections_active | Gauge | 数据库活跃连接 | > 80 |
| redis_commands_seconds | Summary | Redis操作耗时 | P99 > 100ms |

### 9.4.4 Prometheus配置

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'code-review'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['app:8080']
    scrape_interval: 10s
```

---

# 第10章 未实现需求

本章记录系统规划中尚未实现的功能规划说明，仅供技术选型和后续迭代参考。

## 10.1 Webhook通知

### 10.1.1 功能描述

当代码审查任务完成时，通过Webhook自动向钉钉、飞书等平台发送通知消息。

### 10.1.2 通知场景

| 事件类型 | 触发时机 | 通知内容 |
|---------|---------|---------|
| 任务完成 | 扫描任务执行完毕 | 任务ID、发现的问题数、扫描时长 |
| 高危问题 | 发现严重级别问题 | 问题详情、文件位置、规则名称 |
| 任务失败 | 扫描任务异常终止 | 错误原因、重试建议 |

### 10.1.3 通知配置

```yaml
webhook:
  enabled: true
  providers:
    - type: dingtalk
      webhook-url: ${DINGTALK_WEBHOOK_URL}
      secret: ${DINGTALK_SECRET}
    - type: feishu
      webhook-url: ${FEISHU_WEBHOOK_URL}
```

### 10.1.4 消息格式

```json
{
    "msgtype": "markdown",
    "markdown": {
        "title": "代码审查完成",
        "content": "**任务ID**: task-12345\n**项目**: awesome-project\n**发现的问题**: 3个\n- 🔴 严重: 1个\n- 🟡 中等: 2个\n**扫描时长**: 45秒"
    }
}
```

## 10.2 GitLab/GitHub集成

### 10.2.1 功能描述

深度集成GitLab和GitHub，实现PR级别的自动代码审查，在PR详情页直接展示审查结果。

### 10.2.2 集成架构

```
┌─────────────┐     Webhook      ┌─────────────────┐
│   GitLab    │ ──────────────▶ │  代码审查系统   │
│   GitHub    │ ◀────────────── │  (PR状态更新)   │
└─────────────┘    评论/状态      └─────────────────┘
```

### 10.2.3 支持的操作

| 操作 | GitLab | GitHub | 说明 |
|-----|--------|--------|------|
| MR/PR创建触发扫描 | ✓ | ✓ | 自动创建审查任务 |
| 扫描结果评论 | ✓ | ✓ | 在MR/PR下评论审查结果 |
| 状态检查 | ✓ | ✓ | 设置commit状态（通过/失败） |
| 问题定位 | ✓ | ✓ | 评论中包含问题所在文件和行号 |

### 10.2.4 配置示例

```yaml
scm:
  gitlab:
    enabled: true
    base-url: https://gitlab.example.com
    access-token: ${GITLAB_ACCESS_TOKEN}
    webhook-secret: ${GITLAB_WEBHOOK_SECRET}
  github:
    enabled: true
    app-id: ${GITHUB_APP_ID}
    private-key: ${GITHUB_PRIVATE_KEY}
    webhook-secret: ${GITHUB_WEBHOOK_SECRET}
```

## 10.3 自定义规则SDK

### 10.3.1 功能描述

提供Java SDK，允许用户编写自定义的代码规则插件，扩展系统的代码检查能力。

### 10.3.2 SDK架构

```
┌─────────────────────────────────────────────┐
│              代码审查核心系统                 │
├─────────────────────────────────────────────┤
│  插件接口层                                   │
│  ┌─────────────────────────────────────┐    │
│  │   RulePlugin Interface              │    │
│  │   - check(ASTNode): List<Issue>     │    │
│  │   - getMetadata(): RuleMetadata     │    │
│  └─────────────────────────────────────┘    │
└─────────────────────────────────────────────┘
           ▲
           │ 实现
           │
┌─────────────────────────────────────────────┐
│           用户自定义插件                      │
│   ┌───────────┐  ┌───────────┐              │
│   │ 插件A     │  │ 插件B     │              │
│   │ 安全规则  │  │ 业务规则  │              │
│   └───────────┘  └───────────┘              │
└─────────────────────────────────────────────┘
```

### 10.3.3 SDK使用示例

```java
// 引入SDK依赖
import com.codereview.sdk.*;

// 实现自定义规则
public class MySecurityRule implements RulePlugin {
    
    @Override
    public RuleMetadata getMetadata() {
        return RuleMetadata.builder()
            .id("my-security-001")
            .name("密码硬编码检测")
            .severity(Severity.HIGH)
            .description("检测代码中是否存在硬编码密码")
            .build();
    }
    
    @Override
    public List<Issue> check(ASTNode node, CheckContext context) {
        List<Issue> issues = new ArrayList<>();
        
        // 自定义检测逻辑
        if (node.getText().contains("password = \"")) {
            issues.add(Issue.builder()
                .line(node.getLine())
                .column(node.getColumn())
                .message("检测到硬编码密码")
                .severity(Severity.HIGH)
                .ruleId("my-security-001")
                .build());
        }
        
        return issues;
    }
}

// 注册插件
@RegisterRule
public class SecurityRulePluginProvider implements RulePluginProvider {
    @Override
    public RulePlugin getPlugin() {
        return new MySecurityRule();
    }
}
```

### 10.3.4 插件打包与部署

```xml
<!-- 插件pom.xml -->
<project>
    <groupId>com.example</groupId>
    <artifactId>my-security-rules</artifactId>
    <version>1.0.0</version>
    
    <dependencies>
        <dependency>
            <groupId>com.codereview</groupId>
            <artifactId>rule-sdk</artifactId>
            <version>1.0.0</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

## 10.4 多语言深度扫描

### 10.4.1 功能描述

扩展代码分析能力，支持Rust、C++等语言的深度静态分析，检测内存安全、并发问题等特定语言缺陷。

### 10.4.2 规划支持的语言

| 语言 | 优先级 | 计划版本 | 分析类型 |
|-----|--------|---------|---------|
| Rust | P1 | v2.0 | 借用检查、生命周期、内存安全 |
| C++ | P1 | v2.0 | 内存泄漏、空指针、资源管理 |
| Go | P2 | v2.1 | Goroutine泄漏、Race条件 |
| Python | P2 | v2.1 | 类型检查、依赖分析 |
| TypeScript | P3 | v2.2 | 运行时错误防御 |

### 10.4.3 Rust深度分析规则示例

| 规则ID | 规则名称 | 说明 |
|-------|---------|------|
| RUST-S001 | 悬空引用检测 | 检测使用已释放内存的引用 |
| RUST-S002 | 生命周期标注检查 | 确保引用的生命周期正确 |
| RUST-S003 | Mutex锁粒度检查 | 检测潜在的死锁和锁竞争 |
| RUST-S004 | Clone调用分析 | 识别不必要的Clone操作 |

### 10.4.4 技术方案

```yaml
language-analyzer:
  rust:
    tool: rust-analyzer
    parser: syntex
    rules:
      - rust-s001
      - rust-s002
      - rust-s003
      - rust-s004
  cpp:
    tool: clang-tidy
    parser: libclang
    rules:
      - cpp-s001
      - cpp-s002
```

## 10.5 AI辅助修复建议

### 10.5.1 功能描述

基于GPT-4大语言模型，为发现的代码问题提供智能修复建议，帮助开发者快速理解和解决问题。

### 10.5.2 实现架构

```
┌──────────────────────────────────────────────────┐
│              代码审查系统                         │
│  ┌─────────────┐    ┌─────────────┐              │
│  │ 问题检测    │───▶│ 敏感信息    │              │
│  │ (规则引擎)  │    │ 过滤层      │              │
│  └─────────────┘    └──────┬──────┘              │
│                            │                      │
│                            ▼                      │
│  ┌─────────────────────────────────────────────┐ │
│  │           AI修复建议服务                      │ │
│  │  ┌─────────┐  ┌──────────┐  ┌───────────┐   │ │
│  │  │ 问题    │  │ 代码     │  │ 修复建议   │   │ │
│  │  │ 摘要    │→ │ 脱敏     │→ │ 生成       │   │ │
│  │  └─────────┘  └──────────┘  └───────────┘   │ │
│  └─────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────┘
```

### 10.5.3 敏感信息过滤

为防止敏感代码上传到外部AI服务，系统实施严格的信息过滤：

| 过滤内容 | 处理方式 | 示例 |
|---------|---------|------|
| API密钥 | 完全替换 | `sk-xxx` → `[REDACTED]` |
| 数据库密码 | 完全替换 | `password=xxx` → `[REDACTED]` |
| 个人身份信息 | 完全替换 | 手机号、邮箱等 |
| 业务敏感数据 | 可配置过滤 | 订单号、用户ID等 |
| 知识产权代码 | 用户可选过滤 | 核心算法、专利代码 |

### 10.5.4 提示词模板

```
你是一个资深的代码审查助手。以下是一段代码中发现的问题：

问题类型：{issue_type}
严重程度：{severity}
问题描述：{issue_description}
问题位置：{file_path}:{line_number}

代码片段（已脱敏）：
```{language}
{code_snippet}
```

请提供：
1. 问题根因分析
2. 修复建议（带代码示例）
3. 预防措施
```

### 10.5.5 配置示例

```yaml
ai-repair:
  enabled: true
  provider: openai
  model: gpt-4
  max-tokens: 2000
  temperature: 0.3
  sensitive-filter:
    enabled: true
    patterns:
      - pattern: "(api[_-]?key|secret|password)\\s*[:=]\\s*['\"]\\w+['\"]"
        replacement: "${1}=[REDACTED]"
      - pattern: "\\b\\d{11}\\b"  # 手机号
        replacement: "[PHONE]"
    custom-rules:
      - name: 订单号过滤
        pattern: "order[_-]?id\\s*[:=]\\s*['\"]?\\d+['\"]?"
        replacement: "order_id=[ORDER_ID]"
```
