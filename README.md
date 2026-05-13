# 学生选课系统 - 高并发选课 + 实时统计 + 离线分析

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5.14-brightgreen" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Spark-4.1.1-orange" alt="Spark">
  <img src="https://img.shields.io/badge/PySpark-4.1.1-orange" alt="PySpark">
  <img src="https://img.shields.io/badge/Kafka-3.7.0-black" alt="Kafka">
  <img src="https://img.shields.io/badge/Redis-6.x-red" alt="Redis">
  <img src="https://img.shields.io/badge/Redisson-3.24.3-red" alt="Redisson">
  <img src="https://img.shields.io/badge/MySQL-8.0-blue" alt="MySQL">
</p>

## 📌 项目简介

一个完整的**课程选课数据平台**项目，覆盖 **高并发业务处理 + 实时统计 + 离线分析** 全链路。

| 核心能力 | 实现方案 |
|---------|---------|
| 高并发选课 | Redisson 分布式锁 + Redis 原子扣库存，防止超卖 |
| 数据一致性 | 唯一索引 + Kafka 幂等消费，保证数据不丢失 |
| 异步解耦 | Kafka 削峰，选课主流程不等待数据库 |
| 实时统计 | Spark Streaming 消费 Kafka，维护 Redis 实时指标 |
| 离线分析 | PySpark 批处理，计算历史热度排名 |

---

## 🛠 技术栈

| 类别 | 技术 | 版本 | 用途 |
|:----|:----|:----|:----|
| 微服务框架 | Spring Boot | 3.5.14 | 8080/8081/8082 三个独立服务 |
| ORM | MyBatis-Plus | 3.5.15 | 数据库操作 |
| 分布式锁 | Redisson | 3.24.3 | 选课并发控制 |
| 缓存 | Redis | 6.x | 库存、排行榜、已选标记 |
| 消息队列 | Kafka | 3.7.0 | 异步解耦、实时流数据源 |
| 实时计算 | Spark Structured Streaming | 4.1.1 | 消费 Kafka 维护 Redis 统计 |
| 离线计算 | PySpark | 4.1.1 | 历史报表 ETL |
| 数据库 | MySQL | 8.0 | 持久化存储 |
| API 文档 | Knife4j | - | 接口文档自动生成 |

---

## 🎯 架构图

```
                            用户请求
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                           前端 (Frontend)                                  │
└──────────────────────────────────────────────────────────────────────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        │                       │                       │
        ▼                       ▼                       ▼
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│basic-service │       │selection-    │       │statistics-   │
│   (8080)     │       │service       │       │service       │
│              │       │   (8081)     │       │   (8082)     │
│ • 学生管理    │◄─────►│              │       │              │
│ • 课程管理    │ HTTP  │ • 选课/退课   │       │ • 实时统计    │
│ • 用户登录    │       │ • 库存管理    │       │ • 离线统计    │
│ • 选课记录    │       │ • 分布式锁    │       │              │
└──────────────┘       └───────┬──────┘       └───────┬──────┘
                               │                       │
                               │ Kafka                 │ MySQL
                               │ selection-topic        │ course_history_stats
                               ▼                       │ daily_stats
                    ┌─────────────────────┐            │
                    │  Spark Streaming    │            │
                    │  (实时流处理)        │            │
                    └─────────┬───────────┘            │
                              │                        │
                              │ Redis                  │
                              ▼                        │
                    ┌─────────────────────┐            │
                    │  stats:total        │            │
                    │  stats:today:*      │            │
                    │  course:ranking     │◄───────────┘
                    │  course:popularity:*│
                    └─────────────────────┘
                              ▲
                              │
                    ┌─────────────────────┐
                    │  PySpark 批处理      │
                    │  (日度 ETL)         │
                    └─────────────────────┘
```

---

## 📊 服务职责

| 服务 | 端口 | 职责 | 数据层 |
|:-----|:-----|:-----|:-------|
| basic-service | 8080 | 课程/学生/用户 CRUD、选课记录持久化 | MySQL |
| selection-service | 8081 | 选课/退课核心业务、Redis 库存与排行榜 | Redis |
| statistics-service | 8082 | 实时统计 + 历史统计查询 | Redis + MySQL |
| Spark Streaming | - | 实时消费 Kafka，更新 Redis 统计指标 | Kafka → Redis |
| PySpark | - | 离线批处理 ETL | MySQL → MySQL |

---

## 🔄 核心数据流

### 选课流程

```
用户选课
   │
   ▼
8081 SelectionService
   ├── ① 调用 8080 校验学生/课程
   ├── ② Redis 检查已选标记
   ├── ③ Redisson 获取分布式锁
   ├── ④ Redis 检查/扣减库存
   ├── ⑤ Redis 标记已选
   ├── ⑥ Redis 更新库存充足榜
   ├── ⑦ Kafka 发送选课消息
   ├── ⑧ @LogSelection AOP → 日志落库
   └── ⑨ 释放分布式锁
         │
         ├─────────────────┬──────────────────┐
         ▼                 ▼                  ▼
  SelectionConsumer   Spark Streaming      PySpark
  → selection_record  → Redis 实时指标    → 离线统计表
  (MySQL)             (Kafka Consumer)    (日度 ETL)
```

### 数据一致性保证

```
1. 唯一索引约束
   ├── selection_record: UNIQUE(student_no, course_no)
   └── selection_log: UNIQUE(student_no, course_no, action, operate_time)

2. Kafka 消费端幂等
   ├── try-catch 捕获 DuplicateKeyException
   └── 手动 ack 跳过重复消息

3. 生产者幂等性
   └── enable.idempotence: true
```

---

## 🚀 快速开始

### 环境要求

| 组件 | 版本要求 |
|:-----|:---------|
| JDK | 17+ |
| MySQL | 8.0+ |
| Redis | 6.x+ |
| Kafka | 3.x |
| Scala | 2.13+ |
| Python | 3.8+ |

### 1. 配置环境变量

复制 `docs/.env.template` 为 `.env` 并配置：

```bash
# 数据库配置
DB_HOST=your_mysql_host
DB_PORT=3306
DB_NAME=student_class
DB_USERNAME=your_username
DB_PASSWORD=your_password

# Redis配置
REDIS_HOST=your_redis_host
REDIS_PORT=6379
REDIS_USERNAME=your_username
REDIS_PASSWORD=your_password
REDIS_DATABASE=1

# Kafka配置
KAFKA_HOST=your_kafka_host
KAFKA_PORT=9092
KAFKA_TOPIC=selection-topic
KAFKA_GROUP_ID=selection-consumer-group

# Spark配置
SPARK_MASTER=spark://your_spark_master:7077
SPARK_DRIVER_MEMORY=512m
SPARK_EXECUTOR_MEMORY=1g
```

### 2. 启动微服务（交互式菜单）

```bash
./run.sh
```

```
==========================================
    智能选课平台 - 服务启动脚本
==========================================
1. 启动 basic-service (8080)
2. 启动 selection-service (8081)
3. 启动 statistics-service (8082)
4. 启动 Spark Streaming (实时统计)
5. 运行 PySpark 每日统计
6. 运行 PySpark 课程历史统计
7. 一键启动所有 Spring Boot 服务
8. 退出
==========================================
```

### 3. 启动 Spark Streaming（实时统计）

```bash
cd spark-streaming
mvn clean package
spark-submit --class StreamingStatsApplication \
  --master spark://your_spark_master:7077 \
  target/spark-streaming-stats-1.0-SNAPSHOT.jar
```

### 4. 运行 PySpark 离线任务（日度批处理）

```bash
cd spark-pyspark
python3 course_stats.py    # 课程历史统计
python3 daily_stats.py      # 每日选课趋势
```

### 5. 配置定时任务（Linux Cron）

```bash
sudo crontab -e
0 2 * * * /root/share/select-platform/spark-pyspark/run_batch.sh
```

---

## 📁 项目文档

| 文档 | 说明 |
|:-----|:-----|
| [架构文档](./docs/architecture.md) | 完整架构、各模块详解、数据流 |
| [API 文档](./docs/api.md) | 8080/8081/8082 所有接口 |
| [系统设计](./docs/design.md) | Redis Key、MySQL 表、Kafka Topic 设计 |


## 📄 许可证

[MIT](LICENSE)

---

## 🙋 作者

**octopuz** · [GitHub](https://github.com/octopuzgh)

---

<p align="center">
  <sub>Built with ☕ by octopuz</sub>
</p>