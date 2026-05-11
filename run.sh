#!/bin/bash

# ============================================
# 智能选课平台 - 服务启动脚本
# ============================================

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

# 显示菜单
echo ""
echo "=========================================="
echo "    智能选课平台 - 服务启动脚本"
echo "=========================================="
echo "1. 启动 basic-service (8080)"
echo "2. 启动 selection-service (8081)"
echo "3. 启动 statistics-service (8082)"
echo "4. 启动 Spark Streaming (实时统计)"
echo "5. 运行 PySpark 每日统计"
echo "6. 运行 PySpark 课程历史统计"
echo "7. 退出"
echo "=========================================="
echo -n "请选择 [1-7]: "

read choice

case $choice in
    1)
        echo "启动 basic-service (8080)..."
        cd "$PROJECT_DIR/basic-service"
        mvn spring-boot:run
        ;;
    2)
        echo "启动 selection-service (8081)..."
        cd "$PROJECT_DIR/selection-service"
        mvn spring-boot:run
        ;;
    3)
        echo "启动 statistics-service (8082)..."
        cd "$PROJECT_DIR/statistics-service"
        mvn spring-boot:run
        ;;
    4)
        echo "启动 Spark Streaming (实时统计)..."
        cd "$PROJECT_DIR/spark-streaming"
        mvn clean package
        spark-submit --master spark://192.168.152.131:7077 \
          target/spark-streaming-stats-1.0-SNAPSHOT.jar
        ;;
    5)
        echo "运行 PySpark 每日统计..."
        cd "$PROJECT_DIR/spark-pyspark"
        python3 daily_stats.py
        ;;
    6)
        echo "运行 PySpark 课程历史统计..."
        cd "$PROJECT_DIR/spark-pyspark"
        python3 course_stats.py
        ;;
    7)
        echo "退出"
        exit 0
        ;;
    *)
        echo "无效选择，请重试"
        exit 1
        ;;
esac
