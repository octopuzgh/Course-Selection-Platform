# RESTful API 文档

## 8081 Selection Service (端口 8081)

### 基础信息
- **Base URL**: `http://localhost:8081/api/selection`
- **Content-Type**: `application/json`

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
    "code": "SELECT_SUCCESS",
    "message": "选课成功"
}
```

#### 错误响应

```json
{
    "success": false,
    "code": "COURSE_FULL",
    "message": "课程已满"
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
    "code": "DROP_SUCCESS",
    "message": "退课成功"
}
```

---

## 8082 Statistics Service (端口 8082)

### 基础信息
- **Base URL**: `http://localhost:8082`
- **Content-Type**: `application/json`

---

## 实时统计 (Real-time)

### 3. 库存充足榜 Top10

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
    },
    {
        "courseNo": "CS102",
        "courseName": null,
        "totalCount": 80,
        "selectedCount": 20,
        "remainingCount": 60,
        "rank": 2
    }
]
```

---

### 4. 库存充足榜（分页）

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

### 5. 累计选课总数

**GET** `/api/realtime/stats/total`

#### 响应示例

```json
1523
```

#### 说明
返回 Long 类型，表示从系统上线至今的累计选课次数（净选课，SELECT - DROP）

---

### 6. 今日统计

**GET** `/api/realtime/stats/today`

#### 响应示例

```json
{
    "totalCount": 45,
    "uniqueStudents": 38
}
```

#### 说明

| 字段 | 类型 | 说明 |
|------|------|------|
| totalCount | Long | 今日选课次数 |
| uniqueStudents | Long | 今日去重选课学生数 |

---

### 7. 课程剩余名额

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

### 8. 检查是否已选

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

### 9. 今日选课统计

**GET** `/api/realtime/daily/today`

#### 响应示例

```json
{
    "statDate": "2024-01-15",
    "dailyCount": 120,
    "dailyStudents": 105
}
```

#### 说明

| 字段 | 类型 | 说明 |
|------|------|------|
| statDate | String | 统计日期 |
| dailyCount | Long | 当日选课次数（净） |
| dailyStudents | Long | 当日选课学生数 |

---

### 10. 指定日期选课统计

**GET** `/api/realtime/daily/{date}`

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| date | String | 日期，格式 yyyy-MM-dd |

#### 请求示例

```
GET /api/realtime/daily/2024-01-15
```

#### 响应示例

```json
{
    "statDate": "2024-01-15",
    "dailyCount": 120,
    "dailyStudents": 105
}
```

---

### 11. 今日热度榜 Top10

**GET** `/api/realtime/popularity/top10`

#### 响应示例

```json
[
    {
        "courseNo": "CS101",
        "selectionCount": 25,
        "rank": 1
    },
    {
        "courseNo": "CS102",
        "selectionCount": 18,
        "rank": 2
    }
]
```

#### 说明
返回今日被选次数最多的课程（按 SELECT 次数排序，不扣 DROP）

---

### 12. 热度榜列表（分页）

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

### 13. 指定日期热度榜

**GET** `/api/realtime/popularity/{date}`

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
GET /api/realtime/popularity/2024-01-15?page=1&size=10
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

### 14. 历史课程排名

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
        "totalSelected": 500,
        "totalRecords": 520,
        "selectCount": 510,
        "dropCount": 10,
        "firstSelectTime": "2024-01-01T09:00:00",
        "lastSelectTime": "2024-01-15T18:30:00",
        "rank": 1,
        "createdAt": "2024-01-01T00:00:00",
        "updatedAt": "2024-01-15T00:00:00"
    }
]
```

#### 说明

| 字段 | 类型 | 说明 |
|------|------|------|
| totalSelected | Integer | 净选课人数 (SELECT - DROP) |
| totalRecords | Integer | 总操作次数 (SELECT + DROP) |
| selectCount | Integer | 选课次数 |
| dropCount | Integer | 退课次数 |
| firstSelectTime | LocalDateTime | 首次选课时间 |
| lastSelectTime | LocalDateTime | 最近选课时间 |
| rank | Integer | 排名 |

---

### 15. 单门课程历史统计

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
    "totalSelected": 500,
    "totalRecords": 520,
    "selectCount": 510,
    "dropCount": 10,
    "firstSelectTime": "2024-01-01T09:00:00",
    "lastSelectTime": "2024-01-15T18:30:00",
    "rank": 1,
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-15T00:00:00"
}
```

---

### 16. 每日选课趋势

**GET** `/api/offline/daily`

#### 请求参数 (Query)

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| start | LocalDate | ✅ | 开始日期 (yyyy-MM-dd) |
| end | LocalDate | ✅ | 结束日期 (yyyy-MM-dd) |

#### 请求示例

```
GET /api/offline/daily?start=2024-01-01&end=2024-01-15
```

#### 响应示例

```json
[
    {
        "statDate": "2024-01-01",
        "dailyStudents": 150,
        "dailySelections": 160,
        "selectCount": 165,
        "dropCount": 5,
        "dailyCourses": 25,
        "createdAt": "2024-01-02T00:00:00",
        "updatedAt": "2024-01-02T00:00:00"
    },
    {
        "statDate": "2024-01-02",
        "dailyStudents": 145,
        "dailySelections": 150,
        "selectCount": 155,
        "dropCount": 5,
        "dailyCourses": 23,
        "createdAt": "2024-01-03T00:00:00",
        "updatedAt": "2024-01-03T00:00:00"
    }
]
```

#### 说明

| 字段 | 类型 | 说明 |
|------|------|------|
| statDate | LocalDate | 统计日期 |
| dailyStudents | Integer | 当日选课学生数（去重） |
| dailySelections | Integer | 当日净选课次数 (SELECT - DROP) |
| selectCount | Integer | 当日选课次数 |
| dropCount | Integer | 当日退课次数 |
| dailyCourses | Integer | 当日涉及课程数 |

---

### 17. 汇总统计

**GET** `/api/offline/summary`

#### 响应示例

```json
{
    "totalSelections": 15230,
    "totalStudents": 8920,
    "totalDays": 45
}
```

#### 说明

| 字段 | 类型 | 说明 |
|------|------|------|
| totalSelections | Long | 历史累计净选课次数 |
| totalStudents | Long | 历史累计选课学生人次 |
| totalDays | Integer | 统计天数 |

---

## 接口总览

| 服务 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 8081 | POST | `/api/selection/select` | 选课 |
| 8081 | POST | `/api/selection/drop` | 退课 |
| 8082 | GET | `/api/realtime/rank/top10` | 库存充足榜 Top10 |
| 8082 | GET | `/api/realtime/rank/list` | 库存充足榜（分页） |
| 8082 | GET | `/api/realtime/stats/total` | 累计选课总数 |
| 8082 | GET | `/api/realtime/stats/today` | 今日统计 |
| 8082 | GET | `/api/realtime/course/{courseNo}/remaining` | 课程剩余名额 |
| 8082 | GET | `/api/realtime/check/selected` | 检查是否已选 |
| 8082 | GET | `/api/realtime/daily/today` | 今日选课统计 |
| 8082 | GET | `/api/realtime/daily/{date}` | 指定日期选课统计 |
| 8082 | GET | `/api/realtime/popularity/top10` | 今日热度榜 Top10 |
| 8082 | GET | `/api/realtime/popularity/list` | 热度榜（分页） |
| 8082 | GET | `/api/realtime/popularity/{date}` | 指定日期热度榜 |
| 8082 | GET | `/api/offline/course/ranking` | 历史课程排名 |
| 8082 | GET | `/api/offline/course/{courseNo}` | 单门课程历史统计 |
| 8082 | GET | `/api/offline/daily` | 每日选课趋势 |
| 8082 | GET | `/api/offline/summary` | 汇总统计 |