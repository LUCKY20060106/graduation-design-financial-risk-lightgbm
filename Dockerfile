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

# 第三阶段：运行环境 (使用 Python 为基准，安装 JRE)
FROM python:3.12-slim-bookworm
WORKDIR /app

# 安装 OpenJDK 17 JRE 和 libgomp1 (LightGBM 必需)
RUN apt-get update && apt-get install -y \
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

# 使用国内源加速或增加超时设置（针对 Railway 可能的网络波动）
RUN pip3 install --no-cache-dir -r requirements.txt

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
