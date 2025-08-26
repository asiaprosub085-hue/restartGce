# ==================== 阶段1：构建Maven项目（Builder Stage）====================
# 使用官方Java 8 Maven镜像作为构建环境（含Maven，无需额外安装）
FROM maven:3.6.3-openjdk-8 AS builder

# 设置构建工作目录（容器内路径，可自定义）
WORKDIR /app

# 1. 先复制pom.xml，利用Docker缓存机制加速后续构建
# （仅当pom.xml依赖变更时，才会重新下载依赖）
COPY pom.xml .

# 2. 下载Maven依赖（提前下载，避免每次构建重复下载）
RUN mvn dependency:go-offline -B

# 3. 复制项目源代码（src目录）
COPY src ./src

# 4. 构建Spring Boot可执行JAR包
# -DskipTests：跳过测试（生产环境建议开启测试，此处为简化构建）
# 生成的JAR包路径：/app/target/[你的JAR包名].jar（需与pom.xml一致）
RUN mvn clean package -DskipTests


# ==================== 阶段2：运行环境（Runtime Stage）====================
# 使用轻量级Java 8镜像（alpine版本，体积更小，适合Cloud Run）
# 避免使用openjdk:8-jdk，选择jre版本减少镜像体积（仅需运行JAR，无需编译）
FROM openjdk:8-jre-alpine

# 1. 安全配置：创建非root用户（Cloud Run要求避免使用root运行应用）
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 2. 设置运行工作目录
WORKDIR /app

# 3. 从构建阶段（builder）复制打好的JAR包到当前镜像
# 注意：替换「gce-restarter-java8-0.0.1-SNAPSHOT.jar」为你实际的JAR包名
# （可在pom.xml中通过<finalName>自定义，若未自定义则为 ${artifactId}-${version}.jar）
COPY --from=builder /app/target/restartGce-0.0.1-SNAPSHOT.jar ./app.jar

# 4. 权限配置：将JAR包权限赋予非root用户
RUN chown -R appuser:appgroup /app

# 5. 切换为非root用户运行（符合Cloud Run安全最佳实践）
USER appuser

# 6. 配置Cloud Run端口（关键！Cloud Run会通过PORT环境变量指定端口，必须适配）
# Spring Boot默认端口是8080，此处通过环境变量覆盖，确保与Cloud Run一致
ENV PORT=8080

# 7. 入口点：启动Spring Boot应用
# -Dserver.port=${PORT}：指定Spring Boot监听Cloud Run分配的端口
# 避免使用ENTRYPOINT ["java", "-jar", "app.jar"]（未指定端口可能启动失败）
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -jar app.jar"]