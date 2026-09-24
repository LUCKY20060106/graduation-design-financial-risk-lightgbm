# 第一阶段：打包前端
FROM node:18-alpine AS frontend-builder
WORKDIR /frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

# 第二阶段：打包后端并运行
FROM maven:3.9-eclipse-temurin-17 AS backend-builder
WORKDIR /app
# 复制后端代码
COPY backend/pom.xml ./backend/
COPY backend/src ./backend/src/
# 复制前端打包后的文件到后端静态目录
COPY --from=frontend-builder /frontend/dist ./backend/src/main/resources/static/
# 编译后端
RUN mvn clean package -DskipTests -f backend/pom.xml

# 第三阶段：运行环境 (包含 Java 和 Python)
FROM eclipse-temurin:17-jre-focal
WORKDIR /app

# 安装 Python 和必要的运行时依赖 (如 libgomp1 用于 LightGBM)
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*
# 复制后端 Jar 包
COPY --from=backend-builder /app/backend/target/*.jar app.jar
# 复制 AI 引擎相关文件
COPY src/ ./src/
COPY requirements.txt ./
RUN pip3 install -r requirements.txt

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
