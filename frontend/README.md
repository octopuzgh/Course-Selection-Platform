# 选课平台前端（演示）

说明：这是一个最小静态前端示例，演示如何调用后端服务实现查看课程、选课与退课以及简单统计。

依赖的后端服务（请确保这些服务已运行并允许来自该前端的跨域请求）：
- 如果后端统一运行在 `http://localhost:8080`，请确认该服务提供选课与统计接口；否则需要启动对应的 `selection-service`（8081）和 `statistics-service`（8082）。

主要文件：
- `index.html` — 页面入口
- `styles.css` — 简单样式
- `app.js` — 前端逻辑，调用 API

快速启动：
1. 在项目 `fronted` 目录打开终端。
2. 使用 Python 简单静态服务器（推荐）运行：

```bash
# python3 -m http.server 5500
# 然后打开 http://localhost:5500/ 在浏览器中访问
```

使用说明：
- 在页面左上输入学号，点击“刷新课程”加载课程列表（从 `http://localhost:8082/api/realtime/rank/list` 获取）。
- 点击“选课/退课”会向 `http://localhost:8081/api/selection/admin/select` 或 `/api/selection/admin/drop` 发送管理员请求，并带上 `X-User-Role: ADMIN` 和 `X-Operator-Id: admin001` 请求头。
- 统计页会展示今日热度 Top10（`/api/realtime/popularity/top10`）和累计选课次数（`/api/realtime/stats/total`）。

注意：如果后端启用了 CSRF 或特定鉴权，请根据实际环境修改 `app.js` 中的请求头或认证逻辑。
