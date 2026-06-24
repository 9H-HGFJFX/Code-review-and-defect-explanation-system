# 多阶段构建 - 代码审查系统
# 阶段1：构建阶段
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 先复制pom.xml下载依赖
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码并构建
COPY src ./src
RUN mvn clean package -DskipTests -B

# 阶段2：运行阶段
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

# 创建应用目录
RUN mkdir -p /app/logs /app/uploads /app/rules

# 安全加固：非root用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# 从构建阶段复制jar包
COPY --from=builder /build/target/app.jar /app/app.jar

# 设置环境变量
ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC"
ENV SPRING_PROFILES_ACTIVE=prod

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]