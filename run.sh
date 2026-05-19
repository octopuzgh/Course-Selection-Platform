#!/bin/bash

# ============================================
# 智能选课平台 - 服务启动脚本
# ============================================

# 项目根目录
PROJECT_DIR="/mnt/hgfs/share_files/select-platform"

# 显示菜单
show_menu() {
    echo ""
    echo "=========================================="
    echo "    智能选课平台 - 服务启动脚本"
    echo "=========================================="
    echo "1. 启动 basic-service (8080)"
    echo "2. 启动 selection-service (8081)"
    echo "3. 启动 statistics-service (8082)"
    echo "4. 启动前端 (静态页面)"
    echo "5. 启动 Spark Streaming (实时统计)"
    echo "6. 运行 PySpark 每日统计"
    echo "7. 运行 PySpark 课程历史统计"
    echo "8. 一键启动所有 Spring Boot 服务"
    echo "9. 退出"
    echo "=========================================="
    echo -n "请选择 [1-9] (多选用逗号分隔，如 1,2,3): "
}

# 启动单个服务
start_service() {
    local choice=$1
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
            echo "启动前端 (8088)..."
            cd "$PROJECT_DIR/fronted"
            python3 -m http.server 8088
            ;;
        5)
            echo "启动 Spark Streaming (实时统计)..."
            cd "$PROJECT_DIR/spark-streaming"
            mvn clean package
            spark-submit --master ${SPARK_MASTER} target/spark-streaming-stats-1.0-SNAPSHOT.jar
            ;;
        6)
            echo "运行 PySpark 每日统计..."
            cd "$PROJECT_DIR/spark-pyspark"
            python3 daily_stats.py
            ;;
        7)
            echo "运行 PySpark 课程历史统计..."
            cd "$PROJECT_DIR/spark-pyspark"
            python3 course_stats.py
            ;;
        *)
            echo "无效选择: $choice"
            ;;
    esac
}

# 循环主菜单
while true; do
    show_menu
    read choice

    # 处理退出
    if [[ "$choice" == "8" ]]; then
        echo "退出"
        exit 0
    fi

    # 分割选择
    IFS=',' read -ra SELECTIONS <<< "$choice"

    for sel in "${SELECTIONS[@]}"; do
        sel=$(echo "$sel" | xargs)  # 去除空格
        case $sel in
            1|2|3|4|5|6)
                start_service $sel
                ;;
            7)
                echo "一键启动所有 Spring Boot 服务..."
                cd "$PROJECT_DIR/basic-service"
                mvn spring-boot:run &
                cd "$PROJECT_DIR/selection-service"
                mvn spring-boot:run &
                cd "$PROJECT_DIR/statistics-service"
                mvn spring-boot:run &
                wait
                ;;
            *)
                echo "无效选择: $sel"
                ;;
        esac
    done
done
