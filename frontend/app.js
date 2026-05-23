// 服务地址配置 - 通过环境变量切换
// 开发环境: 192.168.137.1, 生产环境: localhost
const BACKEND_HOST = window.BACKEND_HOST || 'localhost'

// 三个微服务的端口配置
const BASIC_SERVICE_PORT = 8080      // 基础服务：用户、课程、学生管理
const SELECTION_SERVICE_PORT = 8081  // 选课服务：选退课操作（高并发）
const STATISTICS_SERVICE_PORT = 8082 // 统计服务：实时排行榜、统计数据

// 服务基础 URL
const BASIC_SERVICE_URL = `http://${BACKEND_HOST}:${BASIC_SERVICE_PORT}`
const SELECTION_SERVICE_URL = `http://${BACKEND_HOST}:${SELECTION_SERVICE_PORT}/api/selection`
const STATISTICS_SERVICE_URL = `http://${BACKEND_HOST}:${STATISTICS_SERVICE_PORT}/api/realtime`

// API 路径定义
const API_USERS = `${BASIC_SERVICE_URL}/users`
const API_STUDENTS = `${BASIC_SERVICE_URL}/students`
const API_COURSES = `${BASIC_SERVICE_URL}/courses`

const $ = id=>document.getElementById(id)

let currentUser = null
let userRole = null
let userInfo = null

function getAuthHeaders(){
  return {
    'Content-Type': 'application/json',
    'X-User-Role': userRole || 'STUDENT'
  }
}

function show(view){
  document.querySelectorAll('#main-app .view').forEach(v=>v.classList.add('hidden'))
  if(view !== 'login'){
    $(view+'-view').classList.remove('hidden')
  }
  document.querySelectorAll('nav button').forEach(b=>b.classList.remove('active'))
  const navBtn = document.getElementById('nav-'+view)
  if(navBtn) navBtn.classList.add('active')
}

function showLogin(){
  $('login-view').classList.remove('hidden')
  $('main-app').classList.add('hidden')
  $('user-info').classList.add('hidden')
}

function showMainApp(){
  $('login-view').classList.add('hidden')
  $('main-app').classList.remove('hidden')
  $('user-info').classList.remove('hidden')
  
  if(userInfo){
    $('user-studentno').textContent = `学号: ${userInfo.studentNo || currentUser}`
    $('user-name').textContent = `姓名: ${userInfo.name || '未知'}`
    $('user-major').textContent = `专业: ${userInfo.major || '未指定'}`
    $('user-grade').textContent = `年级: ${userInfo.grade || '未知'}`
  }
  
  $('current-role').textContent = userRole === 'ADMIN' ? '管理员' : '学生'
  $('current-role').className = `role-badge ${userRole.toLowerCase()}`
  
  if(userRole === 'ADMIN'){
    $('nav-admin').classList.remove('hidden')
    $('studentNo').parentElement.style.display = 'flex'
  }else{
    $('nav-admin').classList.add('hidden')
    $('studentNo').parentElement.style.display = 'none'
    $('studentNo').value = currentUser
  }
}

async function login(){
  const userid = $('login-userid').value.trim()
  const password = $('login-password').value
  
  if(!userid || !password){
    $('login-error').textContent = '请输入用户名和密码'
    return
  }
  
  try{
    const res = await fetch(API_USERS+'/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({userid, password})
    })
    
    const result = await res.json()
    if(result.code === 200){
      currentUser = result.data.userid
      userRole = result.data.role
      localStorage.setItem('currentUser', currentUser)
      localStorage.setItem('userRole', userRole)
      $('login-error').textContent = ''
      showMainApp()
      await fetchUserInfo()
      fetchCourses()
      fetchTop10()
    }else{
      $('login-error').textContent = result.message || '登录失败'
    }
  }catch(e){
    $('login-error').textContent = '登录请求失败: ' + e.message
  }
}

function logout(){
  currentUser = null
  userRole = null
  userInfo = null
  localStorage.removeItem('currentUser')
  localStorage.removeItem('userRole')
  localStorage.removeItem('userInfo')
  $('login-userid').value = ''
  $('login-password').value = ''
  showLogin()
}

async function fetchUserInfo(){
  try{
    const res = await fetch(API_STUDENTS + '/' + currentUser, {
      headers: getAuthHeaders()
    })
    const result = await res.json()
    if(result.code === 200){
      userInfo = result.data
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
      showMainApp()
    }else{
      console.log('获取用户信息失败:', result.message)
      userInfo = null
    }
  }catch(e){
    console.log('获取用户信息异常:', e)
    userInfo = null
  }
}

async function fetchCourses(){
  try{
    const res = await fetch(API_COURSES, {
      headers: getAuthHeaders()
    })
    const result = await res.json()
    if(result.code === 200){
      renderCourses(result.data)
    }else{
      alert('获取课程列表失败：' + result.message)
    }
  }catch(e){
    alert('无法获取课程列表：'+e)
  }
}

async function checkSelected(studentNo, courseNo){
  if(!studentNo) return false
  try{
    // 优先使用 statistics-service (8082) 的实时检查接口
    const url = new URL(`${STATISTICS_SERVICE_URL}/check/selected`)
    url.searchParams.set('studentNo', studentNo)
    url.searchParams.set('courseNo', courseNo)
    const res = await fetch(url, {
      headers: getAuthHeaders()
    })
    // statistics-service 直接返回 boolean，不是 Result 包装
    return await res.json()
  }catch(e){
    // 降级方案：如果 8082 不可用，使用 basic-service (8080)
    console.warn('statistics-service 不可用，降级到 basic-service')
    try{
      const url = new URL(`${BASIC_SERVICE_URL}/selections/check`)
      url.searchParams.set('studentNo', studentNo)
      url.searchParams.set('courseNo', courseNo)
      const res = await fetch(url, {
        headers: getAuthHeaders()
      })
      const result = await res.json()
      return result.code === 200 && result.data === true
    }catch(err){
      return false
    }
  }
}

async function renderCourses(list){
  const container = $('courses')
  container.innerHTML = ''
  const studentNo = userRole === 'STUDENT' ? currentUser : $('studentNo').value.trim()
  
  // 创建表格
  const table = document.createElement('table')
  table.className = 'students-table courses-table'
  table.innerHTML = `
    <thead>
      <tr>
        <th>课程号</th>
        <th>课程名称</th>
        <th>教师</th>
        <th>学分</th>
        <th>总容量</th>
        <th>剩余容量</th>
        <th>已选容量</th>
        <th>选课状态</th>
        <th>操作</th>
      </tr>
    </thead>
    <tbody></tbody>
  `
  
  const tbody = table.querySelector('tbody')
  
  // 先创建所有行
  const rows = []
  for(const item of list){
    const tr = document.createElement('tr')
    tr.innerHTML = `
      <td>${item.courseNo}</td>
      <td>${item.courseName || '未知'}</td>
      <td>${item.teacher || '未知'}</td>
      <td>${item.credit || 0}</td>
      <td>${item.totalCapacity || 0}</td>
      <td>${item.remaining ?? 0}</td>
      <td>${item.selectedCount || 0}</td>
      <td class="status-cell"></td>
      <td class="action-cell"></td>
    `
    tbody.appendChild(tr)
    rows.push({tr, item, statusCell: tr.querySelector('.status-cell'), actionCell: tr.querySelector('.action-cell')})
  }
  
  // 异步检查选课状态
  for(const {tr, item, statusCell, actionCell} of rows){
    const btn = document.createElement('button')
    btn.className = 'action small-btn'
    
    if(userRole === 'STUDENT'){
      const selected = await checkSelected(studentNo, item.courseNo)
      if(selected){
        statusCell.innerHTML = '<span class="selected-badge">✓ 已选</span>'
        btn.textContent = '退课'
        btn.className = 'action danger small-btn'
        btn.onclick = ()=> dropCourse(studentNo, item.courseNo)
      }else{
        statusCell.innerHTML = '<span class="not-selected-badge">未选</span>'
        btn.textContent = '选课'
        btn.className = 'action success small-btn'
        btn.onclick = ()=> selectCourse(studentNo, item.courseNo)
      }
    }else if(userRole === 'ADMIN'){
      statusCell.innerHTML = '<span class="admin-hint">管理员模式</span>'
      btn.textContent = '选课/退课'
      btn.onclick = ()=>{ 
        const sno = $('studentNo').value.trim()
        if(!sno){
          alert('请先在上方输入框输入学号')
          return
        }
        checkSelected(sno, item.courseNo).then(selected=>{
          if(selected){
            if(confirm(`学生 ${sno} 已选该课程，是否退课？`)){
              dropCourse(sno, item.courseNo)
            }
          }else{
            if(confirm(`为学生 ${sno} 选择该课程？`)){
              selectCourse(sno, item.courseNo)
            }
          }
        })
      }
    }
    
    actionCell.appendChild(btn)
  }
  
  container.appendChild(table)
}

async function selectCourse(studentNo, courseNo){
  if(!studentNo){ alert('请先输入学号'); return }
  try{
    // 优先使用 selection-service (8081) 的选课接口
    const res = await fetch(`${SELECTION_SERVICE_URL}/select`, {
      method:'POST',
      headers: {
        ...getAuthHeaders(),
        'X-Operator-Id': currentUser // 添加操作人ID
      },
      body: JSON.stringify({studentNo,courseNo}),
    })
    const result = await res.json()
    // selection-service 返回格式: {success, code, message}
    alert(result.message || (result.success ? '选课成功' : '选课失败'))
    await fetchCourses()
  }catch(e){ 
    // 降级方案：如果 8081 不可用，使用 basic-service (8080)
    console.warn('selection-service 不可用，降级到 basic-service')
    try{
      const res = await fetch(`${BASIC_SERVICE_URL}/selections/submit`, {
        method:'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({studentNo,courseNo}),
      })
      const result = await res.json()
      alert(result.message || (result.code===200 ? '成功' : '失败'))
      await fetchCourses()
    }catch(err){ 
      alert('选课失败：'+err) 
    }
  }
}

async function dropCourse(studentNo, courseNo){
  if(!studentNo){ alert('请先输入学号'); return }
  try{
    // 优先使用 selection-service (8081) 的退课接口
    const res = await fetch(`${SELECTION_SERVICE_URL}/drop`, {
      method:'POST',
      headers: {
        ...getAuthHeaders(),
        'X-Operator-Id': currentUser // 添加操作人ID
      },
      body: JSON.stringify({studentNo,courseNo}),
    })
    const result = await res.json()
    // selection-service 返回格式: {success, code, message}
    alert(result.message || (result.success ? '退课成功' : '退课失败'))
    await fetchCourses()
  }catch(e){ 
    // 降级方案：如果 8081 不可用，使用 basic-service (8080)
    console.warn('selection-service 不可用，降级到 basic-service')
    try{
      const res = await fetch(`${BASIC_SERVICE_URL}/selections/drop`, {
        method:'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({studentNo,courseNo}),
      })
      const result = await res.json()
      alert(result.message || (result.code===200 ? '成功' : '失败'))
      await fetchCourses()
    }catch(err){ 
      alert('退课失败：'+err) 
    }
  }
}

async function fetchTop10(){
  try{
    // 优先使用 statistics-service (8082) 的实时库存充足榜 Top10
    const res = await fetch(`${STATISTICS_SERVICE_URL}/rank/top10`, {
      headers: getAuthHeaders()
    })
    const container = $('top10'); container.innerHTML = ''
    
    // statistics-service 直接返回数组，不是 Result 包装
    const data = await res.json()
    
    if(data && data.length > 0){
      // 创建表格
      const table = document.createElement('table')
      table.className = 'students-table'
      table.innerHTML = `
        <thead>
          <tr>
            <th>排名</th>
            <th>课程号</th>
            <th>课程名称</th>
            <th>剩余名额</th>
            <th>已选人数</th>
            <th>总容量</th>
          </tr>
        </thead>
        <tbody></tbody>
      `
      
      const tbody = table.querySelector('tbody')
      
      data.forEach(item=>{
        const tr = document.createElement('tr')
        tr.innerHTML = `
          <td>${item.rank || '-'}</td>
          <td>${item.courseNo}</td>
          <td>${item.courseName || '未知'}</td>
          <td>${item.remainingCount || 0}</td>
          <td>${item.selectedCount || 0}</td>
          <td>${item.totalCount || 0}</td>
        `
        tbody.appendChild(tr)
      })
      
      container.appendChild(table)
    }else{
      container.innerHTML = '<table class="students-table"><tbody><tr><td style="text-align:center;padding:2rem;color:#95a5a6;">暂无排行数据</td></tr></tbody></table>'
    }
  }catch(e){
    // 降级方案：如果 8082 不可用，使用 basic-service (8080)
    console.warn('statistics-service 不可用，降级到 basic-service')
    try{
      const res = await fetch(API_COURSES, {
        headers: getAuthHeaders()
      })
      const result = await res.json()
      const container = $('top10'); container.innerHTML = ''
      
      if(result.code === 200 && result.data){
        const sorted = result.data
          .filter(c => c.selectedCount > 0)
          .sort((a, b) => b.selectedCount - a.selectedCount)
          .slice(0, 10)
        
        if(sorted.length === 0){
          container.innerHTML = '<table class="students-table"><tbody><tr><td style="text-align:center;padding:2rem;color:#95a5a6;">暂无选课数据</td></tr></tbody></table>'
          return
        }
        
        // 创建表格
        const table = document.createElement('table')
        table.className = 'students-table'
        table.innerHTML = `
          <thead>
            <tr>
              <th>课程号</th>
              <th>课程名称</th>
              <th>已选次数</th>
            </tr>
          </thead>
          <tbody></tbody>
        `
        
        const tbody = table.querySelector('tbody')
        
        sorted.forEach(item=>{
          const tr = document.createElement('tr')
          tr.innerHTML = `
            <td>${item.courseNo}</td>
            <td>${item.courseName || '未知'}</td>
            <td>${item.selectedCount || 0}</td>
          `
          tbody.appendChild(tr)
        })
        
        container.appendChild(table)
      }
    }catch(err){
      $('top10').innerHTML='<table class="students-table"><tbody><tr><td style="text-align:center;padding:2rem;color:#95a5a6;">无法获取排行</td></tr></tbody></table>'
    }
  }
}

async function loadTotal(){
  try{
    // 优先使用 statistics-service (8082) 的累计选课总数接口
    const res = await fetch(`${STATISTICS_SERVICE_URL}/stats/total`, {
      headers: getAuthHeaders()
    })
    // statistics-service 直接返回 Long 类型数字
    const total = await res.json()
    $('total-count').textContent = '累计选课次数：' + total
  }catch(e){
    // 降级方案：如果 8082 不可用，使用 basic-service (8080)
    console.warn('statistics-service 不可用，降级到 basic-service')
    try{
      const res = await fetch(API_COURSES, {
        headers: getAuthHeaders()
      })
      const result = await res.json()
      if(result.code === 200 && result.data){
        const total = result.data.reduce((sum, c) => sum + (c.selectedCount || 0), 0)
        $('total-count').textContent = '累计选课次数：' + total
      }else{
        $('total-count').textContent = '载入失败'
      }
    }catch(err){
      $('total-count').textContent = '载入失败'
    }
  }
}

async function fetchAllStudents(){
  if(userRole !== 'ADMIN'){
    alert('只有管理员可以查看学生列表')
    return
  }
  try{
    const res = await fetch(API_STUDENTS, {
      headers: getAuthHeaders()
    })
    const result = await res.json()
    if(result.code === 200){
      renderStudents(result.data)
    }else{
      alert('获取学生列表失败：' + result.message)
    }
  }catch(e){
    alert('无法获取学生列表：'+e)
  }
}

function renderStudents(list){
  const container = $('students-list')
  container.innerHTML = ''
  
  // 创建表格
  const table = document.createElement('table')
  table.className = 'students-table'
  table.innerHTML = `
    <thead>
      <tr>
        <th>学号</th>
        <th>姓名</th>
        <th>专业</th>
        <th>年级</th>
        <th>身份</th>
        <th>操作</th>
      </tr>
    </thead>
    <tbody></tbody>
  `
  
  const tbody = table.querySelector('tbody')
  
  list.forEach(item=>{
    const tr = document.createElement('tr')
    const role = item.role || 'STUDENT'
    const roleBadge = role === 'ADMIN' ? '<span class="role-badge admin">管理员</span>' : '<span class="role-badge student">学生</span>'
    const deleteBtn = userRole === 'ADMIN' ? `<button class="action danger small-btn" onclick="deleteStudent('${item.studentNo}')">删除</button>` : ''
    
    tr.innerHTML = `
      <td>${item.studentNo}</td>
      <td>${item.name}</td>
      <td>${item.major}</td>
      <td>${item.grade}</td>
      <td>${roleBadge}</td>
      <td>${deleteBtn}</td>
    `
    tbody.appendChild(tr)
  })
  
  container.appendChild(table)
}

async function addCourse(){
  if(userRole !== 'ADMIN'){
    alert('只有管理员可以添加课程')
    return
  }
  
  const courseNo = $('admin-course-no').value.trim()
  const courseName = $('admin-course-name').value.trim()
  const teacher = $('admin-course-teacher').value.trim()
  const credit = parseInt($('admin-course-credit').value)
  const capacity = parseInt($('admin-course-capacity').value)
  
  if(!courseNo || !courseName){
    alert('课程号和课程名称不能为空')
    return
  }
  
  try{
    const res = await fetch(API_COURSES, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({
        courseNo,
        courseName,
        teacher: teacher || '未知',
        credit: credit || 0,
        totalCapacity: capacity || 100,
        remaining: capacity || 100,
        selectedCount: 0
      })
    })
    const result = await res.json()
    if(result.code === 200){
      alert('添加课程成功')
      fetchCourses()
      $('admin-course-no').value = ''
      $('admin-course-name').value = ''
      $('admin-course-teacher').value = ''
      $('admin-course-credit').value = ''
      $('admin-course-capacity').value = ''
    }else{
      alert('添加课程失败：' + result.message)
    }
  }catch(e){
    alert('添加课程失败：'+e)
  }
}

async function addStudent(){
  if(userRole !== 'ADMIN'){
    alert('只有管理员可以添加学生')
    return
  }
  
  const studentNo = $('admin-student-no').value.trim()
  const name = $('admin-student-name').value.trim()
  const major = $('admin-student-major').value.trim()
  const grade = $('admin-student-grade').value.trim()
  
  if(!studentNo || !name){
    alert('学号和姓名不能为空')
    return
  }
  
  try{
    const res = await fetch(API_STUDENTS, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({
        studentNo,
        name,
        major: major || '未指定',
        grade: grade || '2024'
      })
    })
    const result = await res.json()
    if(result.code === 200){
      alert('添加学生成功')
      fetchAllStudents()
      $('admin-student-no').value = ''
      $('admin-student-name').value = ''
      $('admin-student-major').value = ''
      $('admin-student-grade').value = ''
    }else{
      alert('添加学生失败：' + result.message)
    }
  }catch(e){
    alert('添加学生失败：'+e)
  }
}

async function deleteCourse(){
  if(userRole !== 'ADMIN'){
    alert('只有管理员可以删除课程')
    return
  }
  
  const courseNo = $('delete-course-no').value.trim()
  if(!courseNo){
    alert('请输入要删除的课程号')
    return
  }
  
  if(!confirm(`确定要删除课程 ${courseNo} 吗？`)) return
  
  try{
    const res = await fetch(API_COURSES+'/'+courseNo, {
      method: 'DELETE',
      headers: getAuthHeaders()
    })
    const result = await res.json()
    if(result.code === 200){
      alert('删除课程成功')
      fetchCourses()
      $('delete-course-no').value = ''
    }else{
      alert('删除课程失败：' + result.message)
    }
  }catch(e){
    alert('删除课程失败：'+e)
  }
}

async function deleteStudent(studentNo){
  if(userRole !== 'ADMIN'){
    alert('只有管理员可以删除学生')
    return
  }
  
  if(!confirm(`确定要删除学生 ${studentNo} 吗？`)) return
  
  try{
    const res = await fetch(API_STUDENTS+'/'+studentNo, {
      method: 'DELETE',
      headers: getAuthHeaders()
    })
    const result = await res.json()
    if(result.code === 200){
      alert('删除学生成功')
      fetchAllStudents()
    }else{
      alert('删除学生失败：' + result.message)
    }
  }catch(e){
    alert('删除学生失败：'+e)
  }
}

document.addEventListener('DOMContentLoaded', ()=>{
  const savedUser = localStorage.getItem('currentUser')
  const savedRole = localStorage.getItem('userRole')
  const savedUserInfo = localStorage.getItem('userInfo')
  
  if(savedUser && savedRole){
    currentUser = savedUser
    userRole = savedRole
    if(savedUserInfo){
      userInfo = JSON.parse(savedUserInfo)
    }
    showMainApp()
    fetchCourses()
    fetchTop10()
    if(!userInfo){
      fetchUserInfo()
    }
  }else{
    showLogin()
  }
  
  $('login-btn').onclick = login
  $('logout-btn').onclick = logout
  
  document.getElementById('nav-courses').onclick = ()=>show('courses')
  document.getElementById('nav-stats').onclick = ()=>show('stats')
  document.getElementById('nav-admin').onclick = ()=>{
    show('admin')
    fetchAllStudents()
  }
  document.getElementById('refresh-courses').onclick = ()=>fetchCourses()
  document.getElementById('refresh-top10').onclick = ()=>fetchTop10()
  document.getElementById('load-total').onclick = ()=>loadTotal()
  document.getElementById('refresh-students').onclick = ()=>fetchAllStudents()
  document.getElementById('admin-add-course').onclick = addCourse
  document.getElementById('admin-delete-course').onclick = deleteCourse
  document.getElementById('admin-add-student').onclick = addStudent
})

window.deleteStudent = deleteStudent
window.deleteCourse = deleteCourse
