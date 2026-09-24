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
COPY backend/pom.xml ./backend/
COPY backend/src ./backend/src/
COPY --from=frontend-builder /frontend/dist ./backend/src/main/resources/static/
RUN mvn clean package -DskipTests -f backend/pom.xml

# 第三阶段：运行环境 (优化 Debian Slim 源配置)
FROM python:3.12-slim-bookworm
WORKDIR /app

# 修复 Debian Slim 源缺失问题并安装 Java 环境
RUN echo "deb http://deb.debian.org/debian bookworm main" > /etc/apt/sources.list && \
    echo "deb http://security.debian.org/debian-security bookworm-security main" >> /etc/apt/sources.list && \
    echo "deb http://deb.debian.org/debian bookworm-updates main" >> /etc/apt/sources.list && \
    apt-get update && \
    mkdir -p /usr/share/man/man1 && \
    apt-get install -y --no-install-recommends \
    openjdk-17-jre-headless \
    libgomp1 \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# 复制后端 Jar 包
COPY --from=backend-builder /app/backend/target/*.jar app.jar

# 复制 AI 引擎相关文件
COPY src/ ./src/
COPY requirements.txt ./
COPY lgb_model.txt ./ 

# 安装 Python 依赖
RUN pip3 install --no-cache-dir -r requirements.txt

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
