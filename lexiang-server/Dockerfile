# ===== 阶段1：Maven 编译打包 =====
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# 拷贝pom文件，缓存依赖层
COPY pom.xml ./
COPY lexiang-common/pom.xml lexiang-common/
COPY lexiang-server/pom.xml lexiang-server/

# 预下载所有依赖，缓存生效
RUN mvn dependency:go-offline -pl lexiang-server -am -q

# 拷贝全部业务源码
COPY . .
# 打包跳过测试
RUN mvn package -pl lexiang-server -am -DskipTests -q

# ===== 阶段2：轻量化JRE运行环境 =====
FROM eclipse-temurin:21-jre-alpine
# 设置东八区时区，修正时间
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /app
# 挂载日志目录（可选）
VOLUME ["/app/logs"]

# 复制打包后的jar包
COPY --from=builder /build/lexiang-server/target/*.jar app.jar

# 后端服务端口
EXPOSE 8080

# 启动命令：适配容器内存 + 指定docker环境配置
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=70.0", "-jar", "app.jar", "--spring.profiles.active=docker"]
