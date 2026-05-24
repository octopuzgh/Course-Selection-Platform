# RESTful API 文档

## 8081 Selection Service (端口 8081)

### 基础信息
- **Base URL**: `http://localhost:8081/api/selection`
- **Content-Type**: `application/json`

### 请求头说明

所有接口均需要以下请求头：

| 请求头 | 必填 | 说明 | 示例 |
|--------|------|------|------|
| X-User-Role | ✅ | 角色权限 | `STUDENT` / `ADMIN` |
| X-Operator-Id | ✅ | 操作人ID（谁做的操作） | `20240001` / `admin001` |

> **注意**：`X-Operator-Id` 用于日志追踪，记录真实操作人。
> - 学生自己操作：传学生学号
> - 管理员代操作：传管理员ID
> - 如果不传：`operator` 默认记录为 `SYSTEM`

---

### 1. 选课

**POST** `/api/selection/select`

#### 请求参数 (Body)

| 参数 | 类型 | 必填 | 说明 | 示例 |
|------|------|------|------|------|
| studentNo | String | ✅ | 学号 | "2024001" |
| courseNo | String | ✅ | 课程号 | "CS101" |

#### 请求示例

```json
{
    "studentNo": "2024001",
    "courseNo": "CS101"
}
```

#### 响应示例

```json
{
    "success": true,
    "message": "选课成功",
    "selectionId": null
}
```

#### 错误响应

```json
{
    "success": false,
    "message": "课程已满",
    "selectionId": null
}
```

---

### 2. 退课

**POST** `/api/selection/drop`

#### 请求参数 (Body)

| 参数 | 类型 | 必填 | 说明 | 示例 |
|------|------|------|------|------|
| studentNo | String | ✅ | 学号 | "2024001" |
| courseNo | String | ✅ | 课程号 | "CS101" |

#### 请求示例

```json
{
    "studentNo": "2024001",
    "courseNo": "CS101"
}
```

#### 响应示例

```json
{
    "success": true,
    "message": "退课成功",
    "selectionId": null
}
```

---

### 3. 管理员帮学生选课

**POST** `/api/selection/admin/select`

#### 请求参数 (Body)

| 参数 | 类型 | 必填 | 说明 | 示例 |
|------|------|------|------|------|
| studentNo | String | ✅ | 学号（被选课的学生） | "2024001" |
| courseNo | String | ✅ | 课程号 | "CS101" |

#### 请求示例

```json
{
    "studentNo": "2024001",
    "courseNo": "CS101"
}
```

#### 响应示例

```json
{
    "success": true,
    "message": "选课成功",
    "selectionId": null
}
```

> 日志中 `operator` 字段记录的是请求头 `X-Operator-Id` 的值，而非 `studentNo`

---

### 4. 管理员帮学生退课

**POST** `/api/selection/admin/drop`

#### 请求参数 (Body)

| 参数 | 类型 | 必填 | 说明 | 示例 |
|------|------|------|------|------|
| studentNo | String | ✅ | 学号（被退课的学生） | "2024001" |
| courseNo | String | ✅ | 课程号 | "CS101" |

#### 请求示例

```json
{
    "studentNo": "2024001",
    "courseNo": "CS101"
}
```

#### 响应示例

```json
{
    "success": true,
    "message": "退课成功",
    "selectionId": null
}
```

---

## 8082 Statistics Service (端口 8082)

### 基础信息
- **Base URL**: `http://localhost:8082`
- **Content-Type**: `application/json`

---

## 实时统计 (Real-time)

实时数据来源：Spark Streaming 消费 Kafka 消息，实时写入 Redis

---

### 1. 库存充足榜 Top10

**GET** `/api/realtime/rank/top10`

#### 响应示例

```json
[
    {
        "courseNo": "CS101",
        "courseName": null,
        "totalCount": 100,
        "selectedCount": 30,
        "remainingCount": 70,
        "rank": 1
    }
]
```

#### 说明
返回剩余库存最多的课程（按 `库存 = 容量 - 净选课人数` 排序）

---

### 2. 库存充足榜（分页）

**GET** `/api/realtime/rank/list`

#### 请求参数 (Query)

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 10 | 每页数量 |

#### 请求示例

```
GET /api/realtime/rank/list?page=1&size=10
```

#### 响应示例

```json
[
    {
        "courseNo": "CS101",
        "courseName": null,
        "totalCount": 100,
        "selectedCount": 30,
        "remainingCount": 70,
        "rank": 1
    }
]
```

---

### 3. 累计选课总数

**GET** `/api/realtime/stats/total`

#### 响应示例

```json
1523
```

#### 说明
返回 Long 类型，表示从系统上线至今的累计净选课次数（SELECT - DROP）

---

### 4. 课程剩余名额

**GET** `/api/realtime/course/{courseNo}/remaining`

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| courseNo | String | 课程号 |

#### 请求示例

```
GET /api/realtime/course/CS101/remaining
```

#### 响应示例

```json
70
```

#### 说明
返回 Integer 类型，表示该课程的剩余库存数量

---

### 5. 检查是否已选

**GET** `/api/realtime/check/selected`

#### 请求参数 (Query)

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| studentNo | String | ✅ | 学号 |
| courseNo | String | ✅ | 课程号 |

#### 请求示例

```
GET /api/realtime/check/selected?studentNo=2024001&courseNo=CS101
```

#### 响应示例

```json
true
```

#### 说明
返回 Boolean，true = 已选，false = 未选

---

### 6. 今日选课统计

**GET** `/api/realtime/daily/today`

#### 响应示例

```json
{
    "statDate": "2026-05-24",
    "dailyCount": 45,
    "dailyStudents": 38
}
```

#### 说明

| 字段 | 类型 | 说明 |
|------|------|------|
| statDate | String | 统计日期 |
| dailyCount | Long | 当日选课净次数（SELECT - DROP） |
| dailyStudents | Long | 当日选课学生数（去重） |

---

### 7. 指定日期选课统计

**GET** `/api/realtime/daily/{date}`

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| date | String | 日期，格式 yyyy-MM-dd |

#### 请求示例

```
GET /api/realtime/daily/2026-05-24
```

#### 响应示例

```json
{
    "statDate": "2026-05-24",
    "dailyCount": 120,
    "dailyStudents": 105
}
```

---

### 8. 今日热度榜 Top10

**GET** `/api/realtime/popularity/top10`

#### 响应示例

```json
[
    {
        "courseNo": "CS101",
        "selectionCount": 25,
        "rank": 1
    }
]
```

#### 说明
返回今日被选次数最多的课程（按 SELECT 次数排序，不扣 DROP）

---

### 9. 热度榜列表（分页）

**GET** `/api/realtime/popularity/list`

#### 请求参数 (Query)

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 10 | 每页数量 |

#### 请求示例

```
GET /api/realtime/popularity/list?page=1&size=10
```

#### 响应示例

```json
[
    {
        "courseNo": "CS101",
        "selectionCount": 25,
        "rank": 1
    }
]
```

#### 说明
默认查今日热度榜

---

### 10. 指定日期热度榜

**GET** `/api/realtime/popularity/{date}**

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| date | String | 日期，格式 yyyy-MM-dd |

#### 请求参数 (Query)

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 10 | 每页数量 |

#### 请求示例

```
GET /api/realtime/popularity/2026-05-24?page=1&size=10
```

#### 响应示例

```json
[
    {
        "courseNo": "CS101",
        "selectionCount": 25,
        "rank": 1
    }
]
```

---

## 离线统计 (Offline)

历史数据来源：PySpark 定时任务从 MySQL 读取 selection_log 计算

---

### 11. 历史课程排名

**GET** `/api/offline/course/ranking`

#### 请求参数 (Query)

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| limit | Integer | 否 | 10 | 返回数量 |

#### 请求示例

```
GET /api/offline/course/ranking?limit=10
```

#### 响应示例

```json
[
    {
        "courseNo": "CS101",
        "totalSelected": 80,
        "totalRecords": 85,
        "selectCount": 82,
        "dropCount": 2,
        "firstSelectTime": "2026-05-20 08:00:00",
        "lastSelectTime": "2026-05-24 16:30:00",
        "rank": 1
    }
]
```

#### 说明

| 字段 | 类型 | 说明 |
|------|------|------|
| totalSelected | Integer | 净选课人数 |
| totalRecords | Integer | 总操作次数 |
| selectCount | Integer | 选课次数 |
| dropCount | Integer | 退课次数 |
| firstSelectTime | String | 首次选课时间 |
| lastSelectTime | String | 最近选课时间 |
| rank | Integer | 排名 |

---

### 12. 单门课程历史统计

**GET** `/api/offline/course/{courseNo}`

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| courseNo | String | 课程号 |

#### 请求示例

```
GET /api/offline/course/CS101
```

#### 响应示例

```json
{
    "courseNo": "CS101",
    "totalSelected": 80,
    "totalRecords": 85,
    "selectCount": 82,
    "dropCount": 2,
    "firstSelectTime": "2026-05-20 08:00:00",
    "lastSelectTime": "2026-05-24 16:30:00",
    "rank": 1
}
```

---

### 13. 每日选课趋势

**GET** `/api/offline/daily`

#### 请求参数 (Query)

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| start | LocalDate | ✅ | 开始日期（yyyy-MM-dd） |
| end | LocalDate | ✅ | 结束日期（yyyy-MM-dd） |

#### 请求示例

```
GET /api/offline/daily?start=2026-05-20&end=2026-05-24
```

#### 响应示例

```json
[
    {
        "statDate": "2026-05-24",
        "dailyStudents": 105,
        "dailySelections": 98,
        "selectCount": 100,
        "dropCount": 2,
        "dailyCourses": 12
    }
]
```

#### 说明

| 字段 | 类型 | 说明 |
|------|------|------|
| statDate | String | 统计日期 |
| dailyStudents | Integer | 当日选课学生数（去重） |
| dailySelections | Integer | 当日净选课次数 |
| selectCount | Integer | 当日选课次数 |
| dropCount | Integer | 当日退课次数 |
| dailyCourses | Integer | 当日有操作的课程数 |

---

### 14. 统计概览

**GET** `/api/offline/summary`

#### 响应示例

```json
{
    "totalStudents": 500,
    "totalCourses": 30,
    "totalSelections": 1523,
    "totalDrops": 45,
    "avgSelectionsPerCourse": 50.8
}
```

#### 说明

| 字段 | 类型 | 说明 |
|------|------|------|
| totalStudents | Integer | 选过课的学生总数 |
| totalCourses | Integer | 开设的课程总数 |
| totalSelections | Integer | 历史累计选课次数 |
| totalDrops | Integer | 历史累计退课次数 |
| avgSelectionsPerCourse | Double | 平均每门课选课人数 |

---

## Redis Key 说明

### 实时统计 Key（Spark Streaming 写入）

| Key Pattern | 类型 | 说明 |
|------------|------|------|
| `stats:total` | String | 累计净选课总数 |
| `stats:daily:count:{date}` | String | 每日净选课次数 |
| `stats:daily:students:{date}` | Set | 每日选课学生集合 |
| `course:popularity:{date}` | Sorted Set | 每日课程热度（SELECT 次数） |
| `course:stock:{courseNo}` | String | 课程剩余库存 |
| `course:capacity:{courseNo}` | String | 课程容量 |
| `course:ranking` | Sorted Set | 库存充足榜（容量 - 净选课） |
| `selected:{studentNo}:{courseNo}` | String | 学生已选课标记 |

### 历史统计 Key（PySpark 定时任务写入）

| Key Pattern | 类型 | 说明 |
|------------|------|------|
| `offline:course:stats:{courseNo}` | Hash | 课程历史统计 |
| `offline:daily:stats:{date}` | Hash | 每日历史统计 |

