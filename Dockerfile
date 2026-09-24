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

# 第三阶段：运行环境 (改用完整版 Bookworm 镜像，彻底避开 Slim 镜像的坑)
FROM python:3.12-bookworm
WORKDIR /app

# 完整版镜像自带完整的软件源和安全补丁索引，直接安装 JRE 即可，不再需要手动写 sources.list
RUN apt-get update && apt-get install -y --no-install-recommends \
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
