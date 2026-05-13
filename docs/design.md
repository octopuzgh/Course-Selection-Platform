# 学生选课系统 - 系统设计

## 一、Redis Key 设计

| Key | 类型 | 用途 | 管理服务 |
|:----|:-----|:-----|:---------|
| `lock:selection:{courseNo}` | String | 分布式锁（Redisson） | LockServiceImpl |
| `course:stock:{courseNo}` | String | 课程库存 | StockServiceImpl |
| `course:capacity:{courseNo}` | String | 课程容量 | StockServiceImpl |
| `course:ranking` | ZSet | 库存充足榜（按库存升序，库存越多排名越前） | RankingServiceImpl |
| `selected:{studentNo}:{courseNo}` | String | 学生已选课程标记（TTL 1小时） | SelectedRecordServiceImpl |
| `stats:total` | String | 累计选课总数 | Spark Streaming |
| `stats:today:count` | String | 今日选课次数 | Spark Streaming |
| `stats:today:students` | Set | 今日选课学生集合 | Spark Streaming |
| `stats:daily:count:{date}` | String | 每日选课次数 | Spark Streaming |
| `stats:daily:students:{date}` | Set | 每日选课学生集合 | Spark Streaming |
| `course:popularity:{date}` | ZSet | 每日课程热度榜（按选课次数） | Spark Streaming |

### Redis Key 设计说明

**分布式锁 Key**：`lock:selection:{courseNo}`
- 粒度：按课程号加锁，保证同一课程选课互斥
- 超时时间：10秒，防止死锁

**库存 Key**：`course:stock:{courseNo}`
- 类型：String，通过 DECR/INCR 原子操作扣减/恢复库存
- 初始化：服务启动时从 MySQL 批量加载到 Redis

**排行榜 Key**：`course:ranking`
- 类型：ZSet（有序集合）
- 分数：库存数量（stock 值）
- 用途：库存充足的课程排在前面

**已选标记 Key**：`selected:{studentNo}:{courseNo}`
- 类型：String
- TTL：1小时（防止 Redis 无限膨胀）
- 用途：快速判断学生是否已选某课程

---

## 二、MySQL 表设计

### 业务表

```sql
-- 学生表
CREATE TABLE student (
    student_no VARCHAR(20) PRIMARY KEY,
    student_name VARCHAR(50) NOT NULL,
    class_name VARCHAR(50),
    major VARCHAR(50),
    grade INT
);

-- 课程表
CREATE TABLE course (
    course_no VARCHAR(20) PRIMARY KEY,
    course_name VARCHAR(100) NOT NULL,
    total_capacity INT NOT NULL,
    remaining_capacity INT NOT NULL,
    teacher_name VARCHAR(50),
    credit DECIMAL(3,1)
);

-- 用户表
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20)
);

-- 选课记录表（SELECT 时写入）
CREATE TABLE selection_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_no VARCHAR(20) NOT NULL,
    course_no VARCHAR(20) NOT NULL,
    selection_id VARCHAR(64) NOT NULL UNIQUE,
    select_time DATETIME NOT NULL,
    UNIQUE KEY uk_student_course (student_no, course_no)
);

-- 选课日志表（SELECT/DROP 都记录）
CREATE TABLE selection_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_no VARCHAR(20) NOT NULL,
    course_no VARCHAR(20) NOT NULL,
    action VARCHAR(10) NOT NULL COMMENT 'SELECT or DROP',
    operate_time DATETIME NOT NULL,
    UNIQUE KEY uk_log (student_no, course_no, action, operate_time)
);
```

### 统计表

```sql
-- 课程历史统计表
CREATE TABLE course_history_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_no VARCHAR(20) NOT NULL,
    total_selected INT DEFAULT 0 COMMENT '净选课人数',
    total_records INT DEFAULT 0 COMMENT '总操作次数',
    select_count INT DEFAULT 0 COMMENT '选课次数',
    drop_count INT DEFAULT 0 COMMENT '退课次数',
    first_select_time DATETIME,
    last_select_time DATETIME,
    `rank` INT DEFAULT 0,
    UNIQUE KEY uk_course_no (course_no)
);

-- 每日选课统计表
CREATE TABLE daily_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stat_date DATE NOT NULL,
    daily_students INT DEFAULT 0 COMMENT '当日选课学生数',
    daily_selections INT DEFAULT 0 COMMENT '当日净选课次数',
    select_count INT DEFAULT 0 COMMENT '当日选课次数',
    drop_count INT DEFAULT 0 COMMENT '当日退课次数',
    daily_courses INT DEFAULT 0 COMMENT '当日涉及课程数',
    UNIQUE KEY uk_stat_date (stat_date)
);
```

---

## 三、Kafka Topic 设计

| Topic | 分区数 | 用途 | 消息格式 |
|:------|:-------|:-----|:---------|
| `selection-topic` | 3 | 选课/退课消息 | `{studentNo, courseNo, type}` |
| `selection-log-topic` | 3 | 选课日志（用于落库） | `{studentNo, courseNo, type, selectionId, time}` |

### 消息格式

**selection-topic**（用于 Spark Streaming）：
```json
{
    "studentNo": "2024001",
    "courseNo": "CS101",
    "type": "SELECT"
}
```

**selection-log-topic**（用于落库）：
```json
{
    "studentNo": "2024001",
    "courseNo": "CS101",
    "type": "SELECT",
    "selectionId": "uuid-xxx",
    "time": "2024-01-15 10:30:00"
}
```

---

## 四、Spark Streaming 数据源配置

```yaml
kafka:
  bootstrap-servers: ${KAFKA_HOST}:${KAFKA_PORT}
  topic: selection-topic
  group-id: stats-consumer-group

redis:
  host: ${REDIS_HOST}
  port: ${REDIS_PORT}
  database: ${REDIS_DATABASE}
  password: ${REDIS_PASSWORD}
```

### 消费模式

- `startingOffsets`: latest（从最新偏移量开始消费）
- `failOnDataLoss`: false（允许数据丢失时的容错）
- `enable.auto.commit`: false（手动提交偏移量）

---

## 五、PySpark 批处理配置

### course_stats.py

- **输入**：MySQL `selection_log` 表
- **处理**：按 course_no 分组，统计 SELECT/DROP 人数和次数
- **输出**：MySQL `course_history_stats` 表（overwrite 模式）
- **调度**：每日凌晨 2:00 执行

### daily_stats.py

- **输入**：MySQL `selection_log` 表
- **处理**：按日期分组，统计每日选课趋势
- **输出**：MySQL `daily_stats` 表（overwrite 模式）
- **调度**：每日凌晨 2:30 执行

### JDBC 配置

```python
JDBC_URL = f"jdbc:mysql://{MYSQL_HOST}:{MYSQL_PORT}/{MYSQL_DB}?useSSL=false&serverTimezone=Asia/Shanghai"
JDBC_PROPS = {
    "user": MYSQL_USER,
    "password": MYSQL_PASSWORD,
    "driver": "com.mysql.cj.jdbc.Driver",
}
```

---

## 六、定时任务配置（Linux Cron）

```cron
# 每天凌晨 2:00 执行批次处理
0 2 * * * /root/share/select-platform/spark-pyspark/run_batch.sh
```

批次处理脚本会依次执行：
1. `course_stats.py` - 课程历史统计
2. `daily_stats.py` - 每日选课趋势
