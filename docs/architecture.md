# 学生选课系统 - 完整架构详解

## 一、项目总览

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

## 二、各模块详解

### 1. basic-service (8080) - 基础数据服务

**定位**：提供学生、课程、用户的基础 CRUD 和选课记录的持久化。

**技术栈**：Spring Boot + MyBatis-Plus + MySQL + Knife4j (Swagger)

#### 目录结构

<details>
<summary>点击展开/收起</summary>

```
basic-service/
├── controller/
│   ├── CourseController.java        # 课程管理
│   ├── StudentController.java       # 学生管理
│   ├── UserController.java          # 用户管理（登录）
│   └── Selection_RecordController.java  # 选课记录管理
├── service/
│   ├── interf/                      # 接口定义
│   └── Impl/                        # 实现类
├── entity/
│   ├── Course.java                  # 课程实体
│   ├── Student.java                 # 学生实体
│   ├── User.java                    # 用户实体
│   └── Selection_Record.java        # 选课记录实体
├── mapper/                          # MyBatis Mapper
├── config/
│   ├── CorsConfig.java              # 跨域配置
│   ├── GlobalExceptionHandler.java  # 全局异常处理
│   ├── Knife4jConfig.java           # API 文档配置
│   └── MybatisPlusConfig.java       # MyBatis-Plus 配置
└── common/
    └── Result.java                  # 统一响应格式
```

</details>

#### API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/courses/{courseNo}` | 查询单个课程 |
| GET | `/courses` | 获取所有课程列表 |
| PUT | `/courses/{courseNo}/decrement` | 扣减课程名额 |
| GET | `/students` | 获取所有学生列表 |
| GET | `/students/{studentNo}` | 查询学生信息 |
| GET | `/students/{studentNo}/selections` | 查询学生选课列表 |
| POST | `/users/login` | 用户登录 |
| GET | `/users/{username}` | 查询用户信息 |
| POST | `/selections` | 保存选课记录 |
| GET | `/selections/check` | 检查是否已选 |
| POST | `/selections/submit` | 完整选课（原子操作） |

#### 统一响应格式

```json
{
    "code": 200,
    "message": "success",
    "data": { ... }
}
```

#### 核心实体

**Course**：
```java
courseNo, courseName, totalCapacity, remainingCapacity, teacherName, credit
```

**Student**：
```java
studentNo, studentName, className, major, grade
```

**User**：
```java
id, username, password, role
```

**Selection_Record**：
```java
id, studentNo, courseNo, selectTime
```

---

### 2. selection-service (8081) - 选课业务服务

**定位**：核心选课/退课业务逻辑，使用 Redis 做高性能库存管理和分布式锁。

**技术栈**：Spring Boot + Redis + Redisson + Kafka + AOP

#### 目录结构

<details>
<summary>点击展开/收起</summary>

```
selection-service/
├── controller/
│   └── SelectionController.java     # 选课/退课接口
├── service/
│   ├── interf/
│   │   ├── SelectionService.java    # 选课服务接口
│   │   ├── StockService.java        # 库存服务接口
│   │   ├── RankingService.java      # 排行榜服务接口
│   │   ├── SelectedRecordService.java # 已选记录服务接口
│   │   └── LockService.java         # 分布式锁服务接口
│   └── impl/
│       ├── SelectionServiceImpl.java # 选课核心逻辑
│       ├── StockServiceImpl.java    # Redis 库存管理
│       ├── RankingServiceImpl.java  # Redis 排行榜管理
│       ├── SelectedRecordServiceImpl.java # Redis 已选标记
│       └── LockServiceImpl.java     # Redisson 分布式锁
├── producer/
│   ├── SelectionMessageProducer.java # 选课消息生产者
│   └── LogMessageProducer.java      # 日志消息生产者
├── consumer/
│   ├── SelectionConsumer.java       # 选课记录消费者
│   └── LogConsumer.java             # 日志消费者
├── client/
│   └── BasicServiceClient.java      # 调用 8080 的 HTTP 客户端
├── aspect/
│   └── SelectionLogAspect.java      # AOP 日志切面
├── annotation/
│   └── LogSelection.java            # 自定义注解
├── init/
│   └── CourseStockInitializer.java  # 启动时初始化 Redis 库存
├── exception/
│   ├── BusinessException.java       # 业务异常
│   └── GlobalExceptionHandler.java  # 全局异常处理
├── config/
│   ├── RedisConfig.java             # Redis/Redisson 配置
│   └── KafkaConfig.java             # Kafka 配置
├── dto/
│   ├── SelectionRequest.java        # 选课请求 DTO
│   ├── SelectionResponse.java       # 选课响应 DTO
│   └── RankingItem.java             # 排行榜项 DTO
├── entity/
│   ├── SelectionRecord.java         # 选课记录实体
│   └── SelectionLog.java            # 选课日志实体
└── mapper/
    ├── SelectionRecordMapper.java
    └── SelectionLogMapper.java
```

</details>

#### 选课流程（核心）

```
POST /api/selection/select
        │
        ▼
SelectionServiceImpl.selectCourse()
        │
        ├── 1. 校验学生和课程是否存在（调用 8080）
        │
        ├── 2. 检查是否已选（Redis: selected:{studentNo}:{courseNo}）
        │
        ├── 3. 获取分布式锁（Redisson: lock:selection:{courseNo}）
        │
        ├── 4. 检查库存（Redis: course:stock:{courseNo}）
        │
        ├── 5. 扣减库存（Redis: course:stock:{courseNo} -1）
        │
        ├── 6. 标记已选（Redis: selected:{studentNo}:{courseNo}）
        │
        ├── 7. 更新排行榜（Redis: course:ranking ZSet -1）
        │
        ├── 8. 发送 Kafka 消息（topic: selection-topic）
        │       │
        │       ├── SelectionConsumer → MySQL: selection_record 表
        │       └── Spark Streaming → Redis 统计 key
        │
        ├── 9. @LogSelection AOP → Kafka → LogConsumer → MySQL: selection_log 表
        │
        └── 10. 释放分布式锁
```

#### 退课流程

```
POST /api/selection/drop
        │
        ▼
SelectionServiceImpl.dropCourse()
        │
        ├── 1. 校验学生和课程是否存在
        ├── 2. 检查是否已选
        ├── 3. 获取分布式锁
        ├── 4. 恢复库存（Redis: course:stock:{courseNo} +1）
        ├── 5. 取消已选标记
        ├── 6. 恢复排行榜（Redis: course:ranking ZSet +1）
        ├── 7. 发送 Kafka 退课消息
        ├── 8. @LogSelection AOP → selection_log 表
        └── 9. 释放分布式锁
```

---

### 3. statistics-service (8082) - 统计查询服务

**定位**：提供实时统计（Redis）和离线统计（MySQL）的查询接口。

**技术栈**：Spring Boot + Redis + MyBatis-Plus + MySQL

#### 目录结构

<details>
<summary>点击展开/收起</summary>

```
statistics-service/
├── controller/
│   ├── RealTimeStatsController.java  # 实时统计接口
│   └── OfflineStatsController.java   # 离线统计接口
├── service/
│   ├── RealTimeStatsService.java     # 实时统计逻辑
│   └── OfflineStatsService.java      # 离线统计逻辑
├── entity/
│   ├── CourseHistoryStats.java       # 课程历史统计实体
│   └── DailyStats.java               # 每日统计实体
├── dto/
│   ├── RankingItem.java              # 排行榜项
│   ├── TodayStats.java               # 今日统计
│   ├── DailyStatsDTO.java            # 每日统计 DTO
│   └── PopularityItem.java           # 热度榜项
└── mapper/
    ├── CourseHistoryStatsMapper.java
    └── DailyStatsMapper.java
```

</details>

#### 实时统计 API (`/api/realtime/*`)

| 接口 | 数据来源 | 说明 |
|------|----------|------|
| `/rank/top10` | Redis `course:ranking` | 库存充足榜 Top10 |
| `/rank/list` | Redis `course:ranking` | 库存充足榜（分页） |
| `/stats/total` | Redis `stats:total` | 累计选课总数 |
| `/stats/today` | Redis `stats:today:*` | 今日统计 |
| `/course/{courseNo}/remaining` | Redis `course:stock:*` | 课程剩余名额 |
| `/check/selected` | Redis `selected:*` | 检查是否已选 |
| `/daily/today` | Redis `stats:daily:*` | 今日选课统计 |
| `/daily/{date}` | Redis `stats:daily:*` | 指定日期统计 |
| `/popularity/top10` | Redis `course:popularity:*` | 今日热度榜 Top10 |
| `/popularity/list` | Redis `course:popularity:*` | 热度榜（分页） |
| `/popularity/{date}` | Redis `course:popularity:*` | 指定日期热度榜 |

#### 离线统计 API (`/api/offline/*`)

| 接口 | 数据来源 | 说明 |
|------|----------|------|
| `/course/ranking` | MySQL `course_history_stats` | 历史课程排名 |
| `/course/{courseNo}` | MySQL `course_history_stats` | 单门课程历史统计 |
| `/daily` | MySQL `daily_stats` | 每日选课趋势 |
| `/summary` | MySQL `daily_stats` | 汇总统计 |

---

### 4. Spark Streaming - 实时流处理

**定位**：消费 Kafka 选课消息，实时更新 Redis 统计指标。

**技术栈**：Spark Structured Streaming + Kafka + Redis (Jedis)

#### 核心逻辑

```scala
// 消费 Kafka topic: selection-topic
// 解析 JSON: {studentNo, courseNo, type: "SELECT"|"DROP"}

// SELECT 时更新 Redis:
jedis.incr("stats:daily:count:2024-01-15")       // 每日次数 +1
jedis.sadd("stats:daily:students:2024-01-15", studentNo)  // 每日学生
jedis.zincrby("course:popularity:2024-01-15", 1, courseNo) // 热度榜 +1
jedis.incr("stats:total")                        // 累计总数 +1
jedis.incr("stats:today:count")                  // 今日次数 +1
jedis.sadd("stats:today:students", studentNo)    // 今日学生

// DROP 时反向操作（decr/srem/zincrby -1）
```

---

### 5. PySpark - 离线批处理

**定位**：定时从 MySQL 读取选课日志，计算历史统计，写入 MySQL 统计表。

**技术栈**：PySpark + MySQL JDBC

#### course_stats.py

```
输入: MySQL selection_log 表
处理: 按 course_no 分组，区分 SELECT/DROP
      total_selected = SELECT人数 - DROP人数
      rank = 按 total_selected 降序排名
输出: MySQL course_history_stats 表 (overwrite)
```

#### daily_stats.py

```
输入: MySQL selection_log 表
处理: 按日期分组，区分 SELECT/DROP
      daily_selections = SELECT次数 - DROP次数
输出: MySQL daily_stats 表 (overwrite)
```

---

## 三、完整数据流

```
┌──────────────────────────────────────────────────────────────────────────┐
│                           选课数据流                                      │
└──────────────────────────────────────────────────────────────────────────┘

Step 1: 用户请求
────────────────
    前端
      │
      │ POST /api/selection/select
      ▼

Step 2: 选课服务处理
────────────────────
┌──────────────────────────────────────────────────────────────────┐
│ 8081 SelectionServiceImpl.selectCourse()                           │
│                                                                  │
│  ① 调用 8080 校验学生/课程                                         │
│  ② Redis 检查已选标记 (selected:{studentNo}:{courseNo})           │
│  ③ Redisson 获取分布式锁 (lock:selection:{courseNo})              │
│  ④ Redis 检查/扣减库存 (course:stock:{courseNo})                  │
│  ⑤ Redis 标记已选 (SET selected:{studentNo}:{courseNo} 1)        │
│  ⑥ Redis 更新库存充足榜 (ZINCRBY course:ranking -1 {courseNo})   │
│  ⑦ Kafka 发送选课消息 (selection-topic)                           │
│  ⑧ @LogSelection AOP → 日志落库 (selection-log-topic)            │
│  ⑨ 释放分布式锁                                                   │
└──────────────────────────────────────────────────────────────────┘
      │
      ├────────────────────────────┬────────────────────────────┐
      │                            │                            │
      ▼                            ▼                            ▼

Step 3: 消息消费
────────────────
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│ SelectionConsumer│  │   LogConsumer   │  │Spark Streaming  │
│                 │  │                 │  │                 │
│ → selection_    │  │ → selection_    │  │ → Redis 实时指标 │
│   record表      │  │   log表         │  │   stats:total   │
│   (MySQL)       │  │   (MySQL)      │  │   stats:today:* │
└─────────────────┘  └─────────────────┘  │   popularity:*  │
                                          └────────┬────────┘
                                                   │
                                                   ▼
                                          ┌─────────────────┐
                                          │     Redis       │
                                          │                 │
                                          │ stats:total     │
                                          │ stats:today:*   │
                                          │ course:ranking  │
                                          │ course:popular* │
                                          └────────┬────────┘
                                                   │
                                          ┌────────┴────────┐
                                          │   PySpark      │
                                          │   日度 ETL     │
                                          │ → MySQL 统计表  │
                                          └────────┬────────┘
                                                   │
                                                   ▼
                                          ┌─────────────────┐
                                          │     MySQL       │
                                          │                 │
                                          │course_history_  │
                                          │  stats表        │
                                          │ daily_stats表   │
                                          └────────┬────────┘
                                                   │
                                                   ▼
                                          ┌─────────────────┐
                                          │ 8082 Statistics │
                                          │    Service      │
                                          │                 │
                                          │ RealTime API → Redis
                                          │ Offline API  → MySQL
                                          └────────┬────────┘
                                                   │
                                                   ▼
                                              前端查询
```

---

## 四、技术栈总结

| 组件 | 技术 | 用途 |
|------|------|------|
| 微服务框架 | Spring Boot | 8080/8081/8082 |
| ORM | MyBatis-Plus | 数据库操作 |
| 缓存 | Redis | 库存、排行榜、已选标记 |
| 分布式锁 | Redisson | 选课并发控制 |
| 消息队列 | Kafka | 异步解耦 |
| 实时流处理 | Spark Structured Streaming | 实时统计 |
| 离线批处理 | PySpark | 历史报表 |
| 数据库 | MySQL | 持久化存储 |
| API 文档 | Knife4j (Swagger) | 8080 接口文档 |
| AOP | Spring AOP | 选课日志记录 |

---

## 五、服务端口与职责

| 服务 | 端口 | 职责 | 数据层 |
|------|------|------|--------|
| basic-service | 8080 | 基础 CRUD + 选课记录持久化 | MySQL |
| selection-service | 8081 | 选课/退课核心业务 | Redis |
| statistics-service | 8082 | 统计查询 | Redis + MySQL |
| Spark Streaming | - | 实时流处理 | Kafka → Redis |
| PySpark | - | 离线批处理 | MySQL → MySQL |

---

## 六、详细设计文档

Redis Key 设计、MySQL 表结构、Kafka Topic 配置等内容请参阅：

- [系统设计文档](./design.md)
