# ============================================================
# 代码审查与缺陷解释系统 - 后端 Dockerfile
# 多阶段构建：第一阶段编译，第二阶段运行时
# ============================================================

# Stage 1: 构建
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# Stage 2: 运行时
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /build/target/code-audit-server.jar app.jar

# 解决容器内时区问题
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

EXPOSE 8080
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-XX:+UseG1GC", \
    "-XX:MaxRAMPercentage=75", \
    "-jar", "app.jar"]
