#!/bin/bash

# ============================================
# 智能选课平台 - 服务管理脚本
# ============================================

PROJECT_DIR="/mnt/hgfs/share_files/select-platform"

if [ -f "$PROJECT_DIR/.env" ]; then
    set -a
    source "$PROJECT_DIR/.env"
    set +a
    echo "Loaded environment variables from .env"
else
    echo "Warning: .env file not found, using defaults"
    export SPARK_MASTER="spark://192.168.152.131:7077"
    export SPARK_DRIVER_MEMORY="512m"
fi

show_menu() {
    echo ""
    echo "=========================================="
    echo "    智能选课平台 - 服务管理"
    echo "=========================================="
    echo "[S] 启动所有服务"
    echo "[1] basic-service (8080)"
    echo "[2] selection-service (8081)"
    echo "[3] statistics-service (8082)"
    echo "[4] 前端 (8088)"
    echo "-----------------------------------------"
    echo "[5] Spark Streaming (实时统计)"
    echo "[6] PySpark 每日统计"
    echo "[7] PySpark 课程历史统计"
    echo "-----------------------------------------"
    echo "[K] 关闭所有服务"
    echo "[Q] 退出"
    echo "=========================================="
    echo -n "选择: "
}

start_basic() {
    cd "$PROJECT_DIR/basic-service"
    nohup mvn spring-boot:run > ../logs/basic.log 2>&1 &
    echo "basic-service started (PID: $!)"
}

start_selection() {
    cd "$PROJECT_DIR/selection-service"
    nohup mvn spring-boot:run > ../logs/selection.log 2>&1 &
    echo "selection-service started (PID: $!)"
}

start_statistics() {
    cd "$PROJECT_DIR/statistics-service"
    nohup mvn spring-boot:run > ../logs/statistics.log 2>&1 &
    echo "statistics-service started (PID: $!)"
}

start_frontend() {
    cd "$PROJECT_DIR/frontend"
    nohup python3 -m http.server 8088 > ../logs/frontend.log 2>&1 &
    echo "frontend started (PID: $!)"
}

start_spark_streaming() {
    echo "Building jar..."
    cd "$PROJECT_DIR/spark-streaming"
    mvn clean package -DskipTests > ../logs/spark-build.log 2>&1

    if [ $? -ne 0 ]; then
        echo "Build failed!"
        cat ../logs/spark-build.log
        return 1
    fi

    echo "Starting Spark Streaming..."
    JAR_PATH=$(find target -name "spark-streaming-stats-*.jar" -not -name "*sources*" -not -name "original-*" | head -1)

    if [ -z "$JAR_PATH" ]; then
        echo "Error: JAR file not found!"
        return 1
    fi

    echo "Using JAR: $JAR_PATH"
    nohup spark-submit \
      --master ${SPARK_MASTER} \
      --class com.octopuz.spark.StreamingStatsApplication \
      --driver-memory ${SPARK_DRIVER_MEMORY:-512m} \
      --conf "spark.sql.streaming.checkpointLocation=/tmp/spark-checkpoint" \
      "$JAR_PATH" > ../logs/spark-stream.log 2>&1 &
    echo $! > ../logs/spark-streaming.pid
    echo "Spark Streaming started (PID: $!)"
}


start_pyspark_daily() {
    echo "Running PySpark daily stats..."
    cd "$PROJECT_DIR/spark-pyspark"
    nohup python3 daily_stats.py > ../logs/daily_stats.log 2>&1 &
    echo "PySpark daily stats started (PID: $!)"
}

start_pyspark_course() {
    echo "Running PySpark course stats..."
    cd "$PROJECT_DIR/spark-pyspark"
    nohup python3 course_stats.py > ../logs/course_stats.log 2>&1 &
    echo "PySpark course stats started (PID: $!)"
}

start_all() {
    echo "Starting all services..."
    start_basic
    start_selection
    start_statistics
    start_frontend
    echo ""
    echo "All services started!"
    echo "Frontend: http://localhost:8088"
}

kill_all() {
    echo "Killing all services..."
    pkill -f "basic-service" 2>/dev/null
    pkill -f "selection-service" 2>/dev/null
    pkill -f "statistics-service" 2>/dev/null
    pkill -f "http.server 8088" 2>/dev/null
    pkill -f "spring-boot:run" 2>/dev/null
    pkill -f "spark-streaming" 2>/dev/null
    pkill -f "spark-submit" 2>/dev/null
    pkill -f "daily_stats.py" 2>/dev/null
    pkill -f "course_stats.py" 2>/dev/null
    echo "All services killed"
}

while true; do
    show_menu
    read choice

    case $choice in
        S|s) start_all ;;
        1) start_basic ;;
        2) start_selection ;;
        3) start_statistics ;;
        4) start_frontend ;;
        5) start_spark_streaming ;;
        6) start_pyspark_daily ;;
        7) start_pyspark_course ;;
        K|k) kill_all ;;
        Q|q) exit 0 ;;
        *) echo "Invalid choice" ;;
    esac
done