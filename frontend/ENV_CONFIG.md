# 前端环境配置说明

## 📋 概述

前端应用现已支持多服务架构，通过环境变量配置可以轻松切换开发和生产环境。

## 🏗️ 服务架构

| 服务名称 | 端口 | 用途 | 优先级 |
|---------|------|------|--------|
| **basic-service** | 8080 | 用户认证、课程管理、学生管理（基础CRUD） | 降级备用 |
| **selection-service** | 8081 | 选课/退课操作（高并发优化） | ⭐ 优先使用 |
| **statistics-service** | 8082 | 实时排行榜、统计数据 | ⭐ 优先使用 |

## 🔧 环境切换

### 方式一：修改 index.html（推荐）

在 `index.html` 第 113-115 行找到配置：

```javascript
// 测试环境
window.BACKEND_HOST = '192.168.137.1'

// 生产环境（默认）
window.BACKEND_HOST = 'localhost'
```

### 方式二：浏览器控制台临时切换

打开浏览器控制台（F12），在加载页面前执行：

```javascript
window.BACKEND_HOST = '192.168.137.1'  // 切换到测试环境
location.reload()  // 刷新页面生效
```

## 📊 接口调用策略

### 1. 选课/退课功能

**优先使用**: selection-service (8081)
- `POST /api/selection/select` - 选课
- `POST /api/selection/drop` - 退课
- `POST /api/selection/admin/select` - 管理员代选课
- `POST /api/selection/admin/drop` - 管理员代退课

**降级方案**: basic-service (8080)
- 如果 8081 不可用，自动降级到 8080

### 2. 实时统计功能

**优先使用**: statistics-service (8082)
- `GET /api/realtime/rank/top10` - 库存充足榜 Top10
- `GET /api/realtime/stats/total` - 累计选课总数
- `GET /api/realtime/check/selected` - 检查是否已选课程

**降级方案**: basic-service (8080)
- 如果 8082 不可用，自动降级到 8080

### 3. 基础数据功能

**仅使用**: basic-service (8080)
- `POST /users/login` - 用户登录
- `GET /students` - 获取学生列表
- `GET /students/{studentNo}` - 获取学生信息
- `GET /courses` - 获取课程列表
- `POST /courses` - 添加课程（管理员）
- `DELETE /courses/{courseNo}` - 删除课程（管理员）
- `POST /students` - 添加学生（管理员）
- `DELETE /students/{studentNo}` - 删除学生（管理员）

## 🚀 部署步骤

### 开发环境（测试服务器）

1. 修改 `index.html`：
   ```javascript
   window.BACKEND_HOST = '192.168.137.1'
   ```

2. 确保三个后端服务都在 192.168.137.1 上运行：
   - basic-service: `http://192.168.137.1:8080`
   - selection-service: `http://192.168.137.1:8081`
   - statistics-service: `http://192.168.137.1:8082`

3. 打开浏览器访问前端页面

### 生产环境（本地/线上）

1. 修改 `index.html`：
   ```javascript
   window.BACKEND_HOST = 'localhost'  // 或实际域名
   ```

2. 确保三个后端服务都在 localhost 上运行：
   - basic-service: `http://localhost:8080`
   - selection-service: `http://localhost:8081`
   - statistics-service: `http://localhost:8082`

3. 打开浏览器访问前端页面

## ⚠️ 注意事项

### 1. CORS 跨域问题

如果前端和后端不在同一域名/端口，需要确保后端配置了 CORS：

**basic-service** (`application.yml`):
```yaml
spring:
  mvc:
    cors:
      allowed-origins: "*"
      allowed-methods: GET,POST,PUT,DELETE,OPTIONS
      allowed-headers: "*"
      allow-credentials: true
```

**selection-service** 和 **statistics-service** 同样需要配置 CORS。

### 2. 请求头要求

**selection-service (8081)** 需要额外的请求头：
- `X-User-Role`: 用户角色（STUDENT/ADMIN）
- `X-Operator-Id`: 操作人ID（记录日志用）

前端已自动添加这些请求头。

### 3. 响应格式差异

不同服务的响应格式略有不同：

**basic-service (8080)**:
```json
{
  "code": 200,
  "message": "成功",
  "data": {...}
}
```

**selection-service (8081)**:
```json
{
  "success": true,
  "code": "SELECT_SUCCESS",
  "message": "选课成功"
}
```

**statistics-service (8082)**:
```json
// 直接返回数据，无包装
[
  {"courseNo": "CS101", "rank": 1, ...}
]
```

前端已处理这些格式差异。

### 4. 降级机制

当前端无法连接到优先服务时，会自动降级到 basic-service：

```
选课/退课: 8081 → 8080
实时统计: 8082 → 8080
```

控制台会显示警告信息：
```
selection-service 不可用，降级到 basic-service
statistics-service 不可用，降级到 basic-service
```

## 🔍 调试技巧

### 查看当前使用的服务

打开浏览器控制台（F12），观察 Network 标签：

- 请求发送到 `:8081` → 正在使用 selection-service ✅
- 请求发送到 `:8082` → 正在使用 statistics-service ✅
- 请求发送到 `:8080` → 可能在使用 basic-service 或已降级

### 强制测试降级

1. 停止 selection-service (8081)
2. 尝试选课操作
3. 观察控制台是否有降级提示
4. 确认功能是否正常（应使用 8080）

## 📝 待开发功能

以下 statistics-service (8082) 的接口尚未在前端集成，后续可以开发：

- [ ] 今日统计 (`/api/realtime/stats/today`)
- [ ] 课程剩余名额 (`/api/realtime/course/{courseNo}/remaining`)
- [ ] 今日选课统计 (`/api/realtime/daily/today`)
- [ ] 指定日期统计 (`/api/realtime/daily/{date}`)
- [ ] 今日热度榜 (`/api/realtime/popularity/top10`)
- [ ] 历史课程排名 (`/api/offline/course/ranking`)
- [ ] 每日选课趋势 (`/api/offline/daily`)
- [ ] 汇总统计 (`/api/offline/summary`)

## 🐛 常见问题

### Q: 为什么有些功能还是用的 8080？

A: 只有选课/退课和实时统计功能优先使用 8081/8082，其他基础功能（用户、课程、学生管理）仍然使用 8080。

### Q: 如何确认我连接的是测试环境还是生产环境？

A: 打开浏览器控制台，执行：
```javascript
console.log(window.BACKEND_HOST)
```

### Q: 降级后会影响性能吗？

A: 基本功能不受影响，但会失去高并发优化和实时统计的优势。建议尽快恢复 8081/8082 服务。

### Q: API 文档在哪里？

A: 
- basic-service: http://localhost:8080/doc.html (Knife4j)
- selection-service: 参考 `docs/api.md`
- statistics-service: 参考 `docs/api.md`

## 📞 技术支持

如有问题，请检查：
1. 后端服务是否全部启动
2. 端口是否正确（8080/8081/8082）
3. CORS 配置是否正确
4. 浏览器控制台是否有错误信息
