#!/bin/bash

# 项目根目录
PROJECT_DIR="/mnt/hgfs/share_files/select-platform"

# 加载 .env 文件
if [ -f "$PROJECT_DIR/.env" ]; then
    echo "Loading environment variables from .env..."
    set -a
    source "$PROJECT_DIR/.env"
    set +a
    echo "Environment loaded successfully."
else
    echo "ERROR: .env file not found at $PROJECT_DIR/.env"
    exit 1
fi

# 进入 statistics-service 目录
cd "$PROJECT_DIR/statistics-service"

# 运行应用
echo "Starting statistics-service with Spark enabled..."
mvn clean spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Dspark.enabled=true"
