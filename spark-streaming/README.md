# Spark Streaming Realtime Statistics

## 依赖
- Spark 4.1.1
- Scala 2.13
- Kafka connector

## 运行
```bash
# 1. 打包
mvn clean package

# 2. 提交到 Spark 集群
spark-submit --master ${SPARK_MASTER} target/spark-streaming-stats-1.0-SNAPSHOT.jar
```

## 配置
在项目根目录的 `.env` 文件中配置以下环境变量：

```bash
# Kafka
KAFKA_HOST=your_kafka_host
KAFKA_PORT=9092
KAFKA_TOPIC=selection-topic
KAFKA_GROUP_ID=stats-consumer-group

# Redis
REDIS_HOST=your_redis_host
REDIS_PORT=6379
REDIS_DATABASE=1
REDIS_PASSWORD=your_password

# Spark
SPARK_MASTER=spark://your_spark_master:7077
SPARK_DRIVER_MEMORY=512m
SPARK_EXECUTOR_MEMORY=1g
```
