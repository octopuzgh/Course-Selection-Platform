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
spark-submit --master spark://192.168.152.131:7077 \
  target/spark-streaming-stats-1.0-SNAPSHOT.jar
```

## 配置
修改 `src/main/resources/application.properties`
