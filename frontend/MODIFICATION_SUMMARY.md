# 前端多服务架构改造总结

## 📝 改造概述

本次改造将前端从单一服务架构（仅使用 8080）升级为多服务架构（8080/8081/8082），并实现了智能降级机制和环境变量配置。

## ✅ 完成的修改

### 1. app.js - 核心逻辑重构

#### 1.1 环境变量配置系统

**修改前**：
```javascript
const BACKEND_BASIC = 'http://localhost:8080'
const API = BACKEND_BASIC
const API_SELECTION = `${API}/selections`
```

**修改后**：
```javascript
// 通过 window.BACKEND_HOST 动态配置
const BACKEND_HOST = window.BACKEND_HOST || 'localhost'

const BASIC_SERVICE_PORT = 8080
const SELECTION_SERVICE_PORT = 8081
const STATISTICS_SERVICE_PORT = 8082

const BASIC_SERVICE_URL = `http://${BACKEND_HOST}:${BASIC_SERVICE_PORT}`
const SELECTION_SERVICE_URL = `http://${BACKEND_HOST}:${SELECTION_SERVICE_PORT}/api/selection`
const STATISTICS_SERVICE_URL = `http://${BACKEND_HOST}:${STATISTICS_SERVICE_PORT}/api/realtime`
```

#### 1.2 选课功能优化

**优先级策略**：
1. 优先使用 selection-service (8081) - 高并发优化
2. 失败时自动降级到 basic-service (8080)

**关键改进**：
- 添加 `X-Operator-Id` 请求头（selection-service 要求）
- 处理不同的响应格式（8081 返回 `{success, code, message}`，8080 返回 `{code, message, data}`）
- 实现 try-catch 降级逻辑

#### 1.3 退课功能优化

与选课功能相同的优先级和降级策略。

#### 1.4 实时统计功能集成

**新增功能**：
- **库存充足榜 Top10**：调用 `/api/realtime/rank/top10` (8082)
- **累计选课总数**：调用 `/api/realtime/stats/total` (8082)
- **检查是否已选**：调用 `/api/realtime/check/selected` (8082)

**降级策略**：
如果 8082 不可用，降级到 8080 的课程列表接口进行前端计算。

#### 1.5 课程列表功能

保持不变，继续使用 basic-service (8080)。

### 2. index.html - 环境配置入口

#### 2.1 添加环境变量配置脚本

```html
<script>
  // 可以通过修改这里的值来切换环境
  // window.BACKEND_HOST = '192.168.137.1'  // 测试环境
  window.BACKEND_HOST = 'localhost'  // 生产环境（默认）
</script>
```

#### 2.2 更新页面描述

```html
<meta name="description" content="学生选课前端 - 支持多服务架构 (8080/8081/8082)" />
```

#### 2.3 更新页脚信息

```html
<small>本前端为演示用途，调用后端服务：8080(basic-service) / 8081(selection-service) / 8082(statistics-service)</small>
```

### 3. 新增文件

#### 3.1 ENV_CONFIG.md - 详细配置文档

包含：
- 服务架构说明
- 环境切换方法
- 接口调用策略
- CORS 配置指南
- 响应格式差异说明
- 调试技巧
- 常见问题解答
- 待开发功能列表

#### 3.2 switch-env.bat - Windows 环境切换工具

功能：
- 交互式菜单选择环境
- 自动修改 index.html 配置
- 显示当前配置状态

#### 3.3 README.md - 更新的使用文档

新增章节：
- 架构说明
- 快速启动（多种方式）
- 环境配置
- 使用说明
- 技术特性
- 注意事项
- 故障排查
- 更新日志

## 🎯 实现的优先级策略

### 选课/退课操作

```
用户发起请求
    ↓
尝试 selection-service (8081)
    ↓
成功？→ 返回结果
    ↓
失败 → 降级到 basic-service (8080)
    ↓
返回结果
```

### 实时统计

```
用户查看统计
    ↓
尝试 statistics-service (8082)
    ↓
成功？→ 展示数据
    ↓
失败 → 降级到 basic-service (8080)
    ↓
前端计算并展示
```

## 📊 接口映射表

| 功能 | 优先服务 | 降级服务 | 接口路径 |
|------|---------|---------|---------|
| 登录 | 8080 | - | POST /users/login |
| 获取学生信息 | 8080 | - | GET /students/{studentNo} |
| 获取学生列表 | 8080 | - | GET /students |
| 添加学生 | 8080 | - | POST /students |
| 删除学生 | 8080 | - | DELETE /students/{studentNo} |
| 获取课程列表 | 8080 | - | GET /courses |
| 添加课程 | 8080 | - | POST /courses |
| 删除课程 | 8080 | - | DELETE /courses/{courseNo} |
| **选课** | **8081** | **8080** | **POST /api/selection/select** |
| **退课** | **8081** | **8080** | **POST /api/selection/drop** |
| **检查是否已选** | **8082** | **8080** | **GET /api/realtime/check/selected** |
| **Top10 排行榜** | **8082** | **8080** | **GET /api/realtime/rank/top10** |
| **累计选课数** | **8082** | **8080** | **GET /api/realtime/stats/total** |

## 🔧 环境切换方法

### 方法 1：修改代码

编辑 `index.html` 第 113-115 行：

```javascript
// 测试环境
window.BACKEND_HOST = '192.168.137.1'

// 生产环境
window.BACKEND_HOST = 'localhost'
```

### 方法 2：使用切换工具

运行 `switch-env.bat`，按提示选择环境。

### 方法 3：浏览器控制台

```javascript
window.BACKEND_HOST = '192.168.137.1'
location.reload()
```

## ⚠️ 重要注意事项

### 1. 响应格式差异

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

### 2. 请求头要求

**selection-service (8081)** 需要额外请求头：
- `X-User-Role`: 用户角色
- `X-Operator-Id`: 操作人ID

前端已自动添加。

### 3. CORS 配置

确保所有后端服务都配置了 CORS，允许前端跨域访问。

## 🧪 测试建议

### 本地测试步骤

1. **启动所有服务**：
   ```bash
   # 终端 1
   cd basic-service && mvn spring-boot:run
   
   # 终端 2
   cd selection-service && mvn spring-boot:run
   
   # 终端 3
   cd statistics-service && mvn spring-boot:run
   ```

2. **配置前端为 localhost**：
   ```javascript
   window.BACKEND_HOST = 'localhost'
   ```

3. **启动前端**：
   ```bash
   cd frontend
   python -m http.server 5500
   ```

4. **访问**：http://localhost:5500

### 测试环境步骤

1. **确认后端部署在 192.168.137.1**

2. **配置前端**：
   ```javascript
   window.BACKEND_HOST = '192.168.137.1'
   ```

3. **启动前端并访问**

### 降级测试

1. 停止 selection-service (8081)
2. 尝试选课
3. 观察控制台是否有降级提示
4. 确认功能正常

## 📈 性能优势

### 使用 8081/8082 的优势

1. **高并发支持**：selection-service 针对选课场景优化
2. **实时统计**：statistics-service 提供准实时数据
3. **负载均衡**：分散请求压力
4. **独立扩展**：各服务可独立扩容

### 降级后的影响

- 基本功能不受影响
- 可能失去部分性能优化
- 统计数据可能有延迟

## 🚀 后续优化建议

### 短期（已完成）
- ✅ 环境变量配置
- ✅ 智能降级机制
- ✅ 多服务集成

### 中期（待开发）
- [ ] 集成更多 statistics-service 接口
  - 今日统计
  - 热度榜
  - 历史排名
- [ ] 添加加载状态指示器
- [ ] 优化错误提示

### 长期
- [ ] 实现 WebSocket 实时更新
- [ ] 添加离线缓存
- [ ] PWA 支持
- [ ] 国际化支持

## 📞 问题反馈

如遇到问题，请：
1. 查看浏览器控制台错误信息
2. 检查后端服务状态
3. 参考 ENV_CONFIG.md 的故障排查部分

---

**改造完成时间**: 2026-05-23  
**版本**: v2.0  
**改造人员**: AI Assistant
