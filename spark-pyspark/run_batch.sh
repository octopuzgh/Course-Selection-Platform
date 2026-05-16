#!/bin/bash

DATE=$(date +"%Y-%m-%d %H:%M:%S")
LOG_DIR="/root/share/select-platform/spark-pyspark/logs"
PYTHON_BIN="/usr/bin/python3"
SCRIPT_DIR="/root/share/select-platform/spark-pyspark"

mkdir -p $LOG_DIR

echo "========== PySpark Batch Start $DATE ==========" >> $LOG_DIR/batch.log

cd $SCRIPT_DIR

$PYTHON_BIN course_stats.py >> $LOG_DIR/course_stats.log 2>&1
echo "[$(date +"%Y-%m-%d %H:%M:%S")] course_stats.py done, exit: $?" >> $LOG_DIR/batch.log

$PYTHON_BIN daily_stats.py >> $LOG_DIR/daily_stats.log 2>&1
echo "[$(date +"%Y-%m-%d %H:%M:%S")] daily_stats.py done, exit: $?" >> $LOG_DIR/batch.log

echo "========== PySpark Batch End $(date +"%Y-%m-%d %H:%M:%S") ==========" >> $LOG_DIR/batch.log
