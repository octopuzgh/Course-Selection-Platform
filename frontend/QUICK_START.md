# 快速开始 - 前端多服务架构

## 🚀 5分钟快速上手

### 步骤 1：启动后端服务

```bash
# 终端 1 - basic-service（必须）
cd basic-service
mvn spring-boot:run

# 终端 2 - selection-service（推荐）
cd selection-service
mvn spring-boot:run

# 终端 3 - statistics-service（推荐）
cd statistics-service
mvn spring-boot:run
```

### 步骤 2：配置前端环境

编辑 `frontend/index.html` 第 113-115 行：

```javascript
// 本地开发
window.BACKEND_HOST = 'localhost'

// 或测试环境
// window.BACKEND_HOST = '192.168.137.1'
```

### 步骤 3：启动前端

```bash
cd frontend
python -m http.server 5500
```

### 步骤 4：访问应用

打开浏览器访问：**http://localhost:5500**

### 步骤 5：登录测试

使用测试账号登录（具体账号请咨询后端开发人员）

---

## 📁 文件说明

| 文件 | 用途 |
|------|------|
| `index.html` | 页面入口，包含环境配置 |
| `app.js` | 核心逻辑，调用后端 API |
| `styles.css` | 样式文件 |
| `README.md` | 完整使用文档 |
| `ENV_CONFIG.md` | 详细环境配置指南 |
| `TESTING_GUIDE.md` | 测试指南 |
| `MODIFICATION_SUMMARY.md` | 改造总结 |
| `switch-env.bat` | Windows 环境切换工具 |
| `QUICK_START.md` | 本文件 |

---

## 🎯 核心功能

### 学生角色
- ✅ 查看课程列表
- ✅ 选课/退课
- ✅ 查看实时统计
- ✅ 查看个人选课状态

### 管理员角色
- ✅ 所有学生功能
- ✅ 代学生选课/退课
- ✅ 添加/删除课程
- ✅ 添加/删除学生
- ✅ 查看所有学生信息

---

## 🔧 环境切换

### 方法 1：使用工具（Windows）

```bash
cd frontend
switch-env.bat
# 按提示选择环境
```

### 方法 2：手动修改

编辑 `index.html`：

```javascript
// 测试环境
window.BACKEND_HOST = '192.168.137.1'

// 生产环境
window.BACKEND_HOST = 'localhost'
```

修改后刷新浏览器即可。

---

## 📊 服务架构

```
前端 (5500)
    ↓
    ├─→ basic-service (8080)       ← 用户、课程、学生管理
    ├─→ selection-service (8081)   ← 选课/退课（优先）
    └─→ statistics-service (8082)  ← 实时统计（优先）
```

**降级机制**：
- 8081 不可用 → 自动降级到 8080
- 8082 不可用 → 自动降级到 8080

---

## ⚡ 常用操作

### 查看当前环境

打开浏览器控制台（F12），执行：
```javascript
console.log(window.BACKEND_HOST)
```

### 查看 API 请求

打开浏览器控制台（F12）→ Network 标签

### 清除缓存

如果遇到问题，尝试：
1. 清除浏览器缓存（Ctrl+Shift+Delete）
2. 或使用无痕模式

---

## ❓ 常见问题

### Q: 无法连接到后端？

**A**: 
1. 检查后端服务是否启动
2. 确认端口（8080/8081/8082）
3. 检查 `window.BACKEND_HOST` 配置

### Q: 选课时提示失败？

**A**:
1. 检查是否已登录
2. 确认学号是否正确
3. 查看控制台是否有降级提示

### Q: 统计数据不显示？

**A**:
1. 检查 statistics-service (8082) 是否启动
2. 确认数据库中有选课记录
3. 查看控制台错误信息

---

## 📖 更多文档

- **完整文档**: [README.md](./README.md)
- **环境配置**: [ENV_CONFIG.md](./ENV_CONFIG.md)
- **测试指南**: [TESTING_GUIDE.md](./TESTING_GUIDE.md)
- **改造总结**: [MODIFICATION_SUMMARY.md](./MODIFICATION_SUMMARY.md)

---

## 🆘 获取帮助

1. 查看浏览器控制台错误信息
2. 参考 ENV_CONFIG.md 的故障排查部分
3. 联系后端开发人员确认服务状态

---

**祝使用愉快！** 🎉
