# 前端多服务架构快速测试指南

## 🎯 测试目标

验证前端是否正确集成了三个后端服务，并测试降级机制。

## 📋 前置条件

### 1. 后端服务准备

确保以下服务可以访问：

**本地环境**：
- basic-service: http://localhost:8080
- selection-service: http://localhost:8081
- statistics-service: http://localhost:8082

**测试环境**：
- basic-service: http://192.168.137.1:8080
- selection-service: http://192.168.137.1:8081
- statistics-service: http://192.168.137.1:8082

### 2. 前端启动

```bash
cd frontend
python -m http.server 5500
```

访问：http://localhost:5500

## ✅ 测试清单

### 测试 1：环境配置验证

**步骤**：
1. 打开浏览器控制台（F12）
2. 执行：`console.log(window.BACKEND_HOST)`

**预期结果**：
- 显示当前配置的主机地址（localhost 或 192.168.137.1）

---

### 测试 2：用户登录（8080）

**步骤**：
1. 输入用户名和密码
2. 点击"登录"

**预期结果**：
- 成功登录
- Network 标签显示请求发送到 `:8080/users/login`
- 显示用户信息

**验证点**：
```
✅ 请求 URL: http://{HOST}:8080/users/login
✅ 响应格式: {code: 200, message: "...", data: {...}}
```

---

### 测试 3：课程列表加载（8080）

**步骤**：
1. 登录后自动加载课程列表
2. 或点击"刷新课程"按钮

**预期结果**：
- 显示课程表格
- Network 标签显示请求发送到 `:8080/courses`

**验证点**：
```
✅ 请求 URL: http://{HOST}:8080/courses
✅ 显示课程号、名称、教师、学分、容量等信息
```

---

### 测试 4：选课功能（优先 8081，降级 8080）

#### 4a. 正常情况（8081 可用）

**步骤**：
1. 在课程列表中点击"选课"按钮
2. 观察 Network 标签

**预期结果**：
- 请求发送到 `:8081/api/selection/select`
- 显示"选课成功"提示
- 课程状态更新为"已选"

**验证点**：
```
✅ 请求 URL: http://{HOST}:8081/api/selection/select
✅ 请求头包含: X-User-Role, X-Operator-Id
✅ 响应格式: {success: true, code: "SELECT_SUCCESS", message: "选课成功"}
✅ Console 无警告信息
```

#### 4b. 降级情况（8081 不可用）

**步骤**：
1. 停止 selection-service (8081)
2. 尝试选课
3. 观察 Console 和 Network 标签

**预期结果**：
- Console 显示警告："selection-service 不可用，降级到 basic-service"
- 请求发送到 `:8080/selections/submit`
- 功能仍然正常

**验证点**：
```
⚠️ Console 警告: "selection-service 不可用，降级到 basic-service"
✅ 请求 URL: http://{HOST}:8080/selections/submit
✅ 响应格式: {code: 200, message: "...", data: {...}}
✅ 功能正常工作
```

---

### 测试 5：退课功能（优先 8081，降级 8080）

与测试 4 类似，但操作是退课。

**验证点**：
```
✅ 正常: POST http://{HOST}:8081/api/selection/drop
✅ 降级: POST http://{HOST}:8080/selections/drop
```

---

### 测试 6：检查是否已选（优先 8082，降级 8080）

**步骤**：
1. 加载课程列表时自动检查
2. 观察 Network 标签

#### 6a. 正常情况（8082 可用）

**预期结果**：
- 请求发送到 `:8082/api/realtime/check/selected?studentNo=...&courseNo=...`
- 返回 boolean 值（true/false）

**验证点**：
```
✅ 请求 URL: http://{HOST}:8082/api/realtime/check/selected?...
✅ 响应: true 或 false（直接返回，无包装）
✅ Console 无警告
```

#### 6b. 降级情况（8082 不可用）

**步骤**：
1. 停止 statistics-service (8082)
2. 刷新课程列表

**预期结果**：
- Console 显示警告
- 请求发送到 `:8080/selections/check`

**验证点**：
```
⚠️ Console 警告: "statistics-service 不可用，降级到 basic-service"
✅ 请求 URL: http://{HOST}:8080/selections/check?...
✅ 响应格式: {code: 200, data: true/false}
```

---

### 测试 7：Top10 排行榜（优先 8082，降级 8080）

**步骤**：
1. 切换到"统计"标签
2. 观察 Top10 数据加载

#### 7a. 正常情况（8082 可用）

**预期结果**：
- 请求发送到 `:8082/api/realtime/rank/top10`
- 显示排名、课程号、课程名称、剩余名额、已选人数、总容量

**验证点**：
```
✅ 请求 URL: http://{HOST}:8082/api/realtime/rank/top10
✅ 表格列: 排名 | 课程号 | 课程名称 | 剩余名额 | 已选人数 | 总容量
✅ 响应: 数组格式 [{rank, courseNo, courseName, remainingCount, ...}]
✅ Console 无警告
```

#### 7b. 降级情况（8082 不可用）

**步骤**：
1. 停止 statistics-service (8082)
2. 点击"刷新排行"

**预期结果**：
- Console 显示警告
- 请求发送到 `:8080/courses`
- 前端计算并显示前 10 个课程

**验证点**：
```
⚠️ Console 警告: "statistics-service 不可用，降级到 basic-service"
✅ 请求 URL: http://{HOST}:8080/courses
✅ 表格列: 课程号 | 课程名称 | 已选次数
✅ 数据通过前端排序和截取
```

---

### 测试 8：累计选课总数（优先 8082，降级 8080）

**步骤**：
1. 在统计页点击"加载累计选课次数"

#### 8a. 正常情况（8082 可用）

**预期结果**：
- 请求发送到 `:8082/api/realtime/stats/total`
- 显示累计选课次数

**验证点**：
```
✅ 请求 URL: http://{HOST}:8082/api/realtime/stats/total
✅ 响应: 数字类型（如 1523）
✅ 显示: "累计选课次数：1523"
✅ Console 无警告
```

#### 8b. 降级情况（8082 不可用）

**步骤**：
1. 停止 statistics-service (8082)
2. 点击"加载累计选课次数"

**预期结果**：
- Console 显示警告
- 请求发送到 `:8080/courses`
- 前端累加计算总数

**验证点**：
```
⚠️ Console 警告: "statistics-service 不可用，降级到 basic-service"
✅ 请求 URL: http://{HOST}:8080/courses
✅ 前端计算: sum(course.selectedCount)
✅ 显示正确的累计数
```

---

### 测试 9：管理员功能（8080）

**步骤**：
1. 使用管理员账号登录
2. 切换到"管理面板"
3. 测试添加/删除课程和学生

**预期结果**：
- 所有请求发送到 `:8080`
- 功能正常工作

**验证点**：
```
✅ 添加课程: POST http://{HOST}:8080/courses
✅ 删除课程: DELETE http://{HOST}:8080/courses/{courseNo}
✅ 添加学生: POST http://{HOST}:8080/students
✅ 删除学生: DELETE http://{HOST}:8080/students/{studentNo}
```

---

### 测试 10：环境切换

**步骤**：
1. 运行 `switch-env.bat`
2. 选择切换到测试环境
3. 刷新浏览器
4. 执行任意操作

**预期结果**：
- 所有请求发送到 192.168.137.1
- 功能正常

**验证点**：
```
✅ index.html 中 window.BACKEND_HOST = '192.168.137.1'
✅ 请求 URL 变为 http://192.168.137.1:{PORT}/...
✅ 功能正常工作
```

---

## 🔍 调试技巧

### 1. 查看 Network 标签

打开浏览器开发者工具（F12）→ Network 标签

**过滤技巧**：
- 输入 `:8080` 查看 basic-service 请求
- 输入 `:8081` 查看 selection-service 请求
- 输入 `:8082` 查看 statistics-service 请求

### 2. 查看 Console 标签

关注以下信息：
- ⚠️ 降级警告
- ❌ 错误信息
- ℹ️ 日志信息

### 3. 检查请求头

对于 selection-service (8081) 的请求，确认包含：
```
X-User-Role: STUDENT 或 ADMIN
X-Operator-Id: 学号或管理员ID
```

### 4. 检查响应格式

不同服务的响应格式不同，确认前端正确解析。

---

## 📊 测试结果记录表

| 测试项 | 8081/8082 可用 | 8081/8082 不可用 | 备注 |
|--------|---------------|-----------------|------|
| 登录 (8080) | ☐ | N/A | |
| 课程列表 (8080) | ☐ | N/A | |
| 选课 (8081→8080) | ☐ | ☐ | |
| 退课 (8081→8080) | ☐ | ☐ | |
| 检查已选 (8082→8080) | ☐ | ☐ | |
| Top10 (8082→8080) | ☐ | ☐ | |
| 累计总数 (8082→8080) | ☐ | ☐ | |
| 管理员功能 (8080) | ☐ | N/A | |
| 环境切换 | ☐ | ☐ | |

---

## ⚠️ 常见问题

### Q1: 所有请求都失败

**可能原因**：
- 后端服务未启动
- 主机地址配置错误
- CORS 未配置

**解决方案**：
1. 检查后端服务状态
2. 确认 `window.BACKEND_HOST` 配置
3. 检查浏览器控制台的 CORS 错误

### Q2: 降级不生效

**可能原因**：
- basic-service 也未启动
- 网络问题

**解决方案**：
1. 确保 basic-service (8080) 正在运行
2. 检查网络连接

### Q3: 统计数据为空

**可能原因**：
- 数据库中没有选课记录
- statistics-service 未同步数据

**解决方案**：
1. 先进行几次选课操作
2. 等待统计数据同步
3. 刷新页面

### Q4: 选课时提示"课程已满"

**可能原因**：
- 课程容量已达上限

**解决方案**：
1. 使用管理员账号增加课程容量
2. 或选择其他课程

---

## 🎉 测试完成标准

所有测试项通过后，确认：

- ✅ 登录功能正常
- ✅ 课程列表显示正常
- ✅ 选课/退课功能正常（8081 优先）
- ✅ 实时统计功能正常（8082 优先）
- ✅ 降级机制工作正常
- ✅ 环境切换功能正常
- ✅ 管理员功能正常
- ✅ 无严重的控制台错误

---

**测试日期**: _______________  
**测试人员**: _______________  
**测试结果**: ☐ 通过  ☐ 部分通过  ☐ 失败  
**备注**: _______________________________________
