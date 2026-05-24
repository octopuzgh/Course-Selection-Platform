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

// 分页相关变量
let courseDisplayData = [] // 课程展示的全部数据
let courseDisplayCurrentPage = 1
let courseDisplayPageSize = 10

let courseSelectionCurrentPage = 1
let courseSelectionPageSize = 10
let courseSelectionTotalPages = null
let courseSelectionFilters = { courseNo: '', courseName: '', teacher: '' }
let courseSelectionAllData = [] // 存储退选课的全部数据

let studentsCurrentPage = 1
let studentsPageSize = 10
let studentsAllData = [] // 存储学生列表的全部数据

// 学生搜索相关变量
let studentSearchTimer = null // 实时搜索的定时器

// 个人信息相关变量
let personalCoursesCurrentPage = 1
let personalCoursesPageSize = 10
let personalCoursesAllData = [] // 存储个人选课的全部数据

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
  
  // 根据角色显示不同的导航栏
  if(userRole === 'ADMIN'){
    $('nav-admin').classList.remove('hidden')
    $('nav-students').classList.remove('hidden')
    $('nav-personal-info').classList.add('hidden')
    // 管理员显示学号输入框
    const studentLabel = $('selection-student-label')
    if(studentLabel) studentLabel.classList.remove('hidden')
  }else{
    $('nav-admin').classList.add('hidden')
    $('nav-students').classList.add('hidden')
    $('nav-personal-info').classList.remove('hidden')
    // 学生隐藏学号输入框
    const studentLabel = $('selection-student-label')
    if(studentLabel) studentLabel.classList.add('hidden')
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
      courseDisplayData = result.data
      courseDisplayCurrentPage = 1
      renderCourseDisplay()
    }else{
      alert('获取课程列表失败：' + result.message)
    }
  }catch(e){
    alert('无法获取课程列表：'+e)
  }
}

// 渲染课程展示（带分页）
function renderCourseDisplay(){
  const container = $('course-display')
  container.innerHTML = ''
  
  const totalPages = Math.ceil(courseDisplayData.length / courseDisplayPageSize)
  const start = (courseDisplayCurrentPage - 1) * courseDisplayPageSize
  const end = start + courseDisplayPageSize
  const pageData = courseDisplayData.slice(start, end)
  
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
      </tr>
    </thead>
    <tbody></tbody>
  `
  
  const tbody = table.querySelector('tbody')
  
  pageData.forEach(item=>{
    const tr = document.createElement('tr')
    tr.innerHTML = `
      <td>${item.courseNo}</td>
      <td>${item.courseName || '未知'}</td>
      <td>${item.teacher || '未知'}</td>
      <td>${item.credit || 0}</td>
      <td>${item.totalCapacity || 0}</td>
      <td>${item.remaining ?? 0}</td>
      <td>${item.selectedCount || 0}</td>
    `
    tbody.appendChild(tr)
  })
  
  container.appendChild(table)
  renderCourseDisplayPagination(totalPages)
}

// 渲染课程展示分页控件
function renderCourseDisplayPagination(totalPages){
  const container = $('course-display-pagination')
  if(totalPages <= 1){
    container.innerHTML = ''
    return
  }
  
  container.innerHTML = `
    <div class="pagination">
      <button class="page-btn" id="prev-page" ${courseDisplayCurrentPage === 1 ? 'disabled' : ''}>上一页</button>
      <span class="page-info">${courseDisplayCurrentPage}/${totalPages}</span>
      <button class="page-btn" id="next-page" ${courseDisplayCurrentPage === totalPages ? 'disabled' : ''}>下一页</button>
      <span class="page-jump">跳转到:</span>
      <input type="number" id="jump-page" min="1" max="${totalPages}" value="${courseDisplayCurrentPage}" class="page-input" />
      <button class="page-btn" id="jump-btn">跳转</button>
    </div>
  `
  
  $('prev-page').onclick = ()=>{
    if(courseDisplayCurrentPage > 1){
      courseDisplayCurrentPage--
      renderCourseDisplay()
    }
  }
  
  $('next-page').onclick = ()=>{
    if(courseDisplayCurrentPage < totalPages){
      courseDisplayCurrentPage++
      renderCourseDisplay()
    }
  }
  
  $('jump-btn').onclick = ()=>{
    const pageNum = parseInt($('jump-page').value)
    if(pageNum >= 1 && pageNum <= totalPages){
      courseDisplayCurrentPage = pageNum
      renderCourseDisplay()
    }
  }
}

// 获取退选课列表（调用8082接口）
// 获取退选课列表（前端分页）
async function fetchCourseSelection(){
  const statusIndicator = $('service-status-indicator')
  
  try{
    // 获取所有数据（不使用后端分页）
    const url = new URL(`${STATISTICS_SERVICE_URL}/rank/list`)
    url.searchParams.set('page', 1)
    url.searchParams.set('size', 1000) // 一次性获取所有数据
    
    console.log('请求URL:', url.toString())
    console.log('请求头:', getAuthHeaders())
    
    const res = await fetch(url, {
      headers: getAuthHeaders()
    })
    
    console.log('响应状态:', res.status, res.statusText)
    
    if(!res.ok){
      throw new Error(`HTTP error! status: ${res.status}`)
    }
    
    let data = await res.json()
    console.log('返回数据:', data)
    
    // 如果返回空数组，直接渲染
    if(!data || data.length === 0){
      courseSelectionAllData = []
      courseSelectionCurrentPage = 1
      renderCourseSelection()
      updateServiceStatus('normal')
      return
    }
    
    // 为每个课程补充详细信息（teacher, credit等）
    const enrichedData = await Promise.all(data.map(async (item) => {
      try{
        const courseRes = await fetch(`${API_COURSES}/${item.courseNo}`, {
          headers: getAuthHeaders()
        })
        if(courseRes.ok){
          const courseResult = await courseRes.json()
          if(courseResult.code === 200 && courseResult.data){
            return {
              ...item,
              teacher: courseResult.data.teacher || '未知',
              credit: courseResult.data.credit || 0
            }
          }
        }
      }catch(e){
        console.warn(`获取课程${item.courseNo}详情失败`, e)
      }
      return {
        ...item,
        teacher: '未知',
        credit: 0
      }
    }))
    
    // 保存所有数据到全局变量
    courseSelectionAllData = enrichedData
    courseSelectionCurrentPage = 1
    
    console.log('获取到的总数据量:', courseSelectionAllData.length)
    renderCourseSelection()
    updateServiceStatus('normal')
  }catch(e){
    console.error('获取退选课列表失败:', e)
    
    // 降级方案：如果 statistics-service 不可用，使用 basic-service
    console.warn('statistics-service 不可用，尝试使用 basic-service')
    try{
      const res = await fetch(API_COURSES, {
        headers: getAuthHeaders()
      })
      const result = await res.json()
      
      if(result.code === 200 && result.data){
        console.log('basic-service 返回数据:', result.data)
        
        // 转换为统一格式
        courseSelectionAllData = result.data.map((item, index) => ({
          rank: index + 1,
          courseNo: item.courseNo,
          courseName: item.courseName || item.name || '未知',
          teacher: item.teacher || '未知',
          credit: item.credit || 0,
          totalCount: item.capacity || item.totalCapacity || 0,
          remainingCount: item.remainingCapacity ?? item.remaining ?? 0,
          selectedCount: item.selectedCount || 0
        }))
        
        courseSelectionCurrentPage = 1
        renderCourseSelection()
        updateServiceStatus('fallback')
      }else{
        throw new Error(result.message || '获取课程列表失败')
      }
    }catch(err){
      console.error('降级方案也失败:', err)
      alert('无法获取课程列表：' + err.message + '\n\n请确保后端服务已启动（8080、8082端口）')
      $('course-selection').innerHTML = '<div style="text-align:center;padding:2rem;color:#e74c3c;">加载失败，请查看控制台错误信息</div>'
      $('course-selection-pagination').innerHTML = ''
      updateServiceStatus('error')
    }
  }
}

// 更新服务状态指示器
function updateServiceStatus(status){
  const indicator = $('service-status-indicator')
  if(!indicator) return
  
  indicator.className = 'service-status'
  
  if(status === 'normal'){
    indicator.textContent = '✓ 正常服务 (8082)'
    indicator.classList.add('normal')
  }else if(status === 'fallback'){
    indicator.textContent = '⚠ 降级服务 (8080)'
    indicator.classList.add('fallback')
  }else{
    indicator.textContent = '✗ 服务异常'
    indicator.classList.add('fallback')
  }
}

// 渲染退选课列表（前端分页）
async function renderCourseSelection(){
  const container = $('course-selection')
  container.innerHTML = ''
  
  // 应用搜索过滤器
  let filteredData = courseSelectionAllData
  if(courseSelectionFilters.courseNo || courseSelectionFilters.courseName || courseSelectionFilters.teacher){
    filteredData = courseSelectionAllData.filter(item => {
      // 课程号过滤
      if(courseSelectionFilters.courseNo){
        const match = item.courseNo && item.courseNo.toLowerCase().includes(courseSelectionFilters.courseNo.toLowerCase())
        if(!match) return false
      }
      // 课程名称过滤
      if(courseSelectionFilters.courseName){
        const match = item.courseName && item.courseName.toLowerCase().includes(courseSelectionFilters.courseName.toLowerCase())
        if(!match) return false
      }
      // 教师过滤
      if(courseSelectionFilters.teacher){
        const match = item.teacher && item.teacher.toLowerCase().includes(courseSelectionFilters.teacher.toLowerCase())
        if(!match) return false
      }
      return true
    })
  }
  
  // 计算总页数
  const totalPages = Math.ceil(filteredData.length / courseSelectionPageSize)
  
  // 计算当前页的数据范围
  const start = (courseSelectionCurrentPage - 1) * courseSelectionPageSize
  const end = start + courseSelectionPageSize
  const pageData = filteredData.slice(start, end)
  
  if(pageData.length === 0){
    container.innerHTML = '<div style="text-align:center;padding:2rem;color:#95a5a6;">暂无课程数据</div>'
    $('course-selection-pagination').innerHTML = ''
    return
  }
  
  console.log('退选课数据总数:', courseSelectionAllData.length, '总页数:', totalPages, '当前页:', courseSelectionCurrentPage)
  
  // 学生用户直接使用当前登录用户，管理员需要提示（但我们已经去掉了学号输入框）
  const studentNo = currentUser
  
  // 提前获取学生已选的课程列表（一次性获取，提高性能）
  let selectedCourses = new Set()
  if(userRole === 'STUDENT'){
    try{
      const res = await fetch(API_STUDENTS + '/' + studentNo + '/selections', {
        headers: getAuthHeaders()
      })
      const result = await res.json()
      if(result.code === 200 && result.data){
        result.data.forEach(item => {
          selectedCourses.add(item.courseNo)
        })
        console.log('学生已选课程:', Array.from(selectedCourses))
      }
    }catch(e){
      console.error('获取已选课程列表失败:', e)
    }
  }
  
  // 创建表格
  const table = document.createElement('table')
  table.className = 'students-table courses-table'
  table.innerHTML = `
    <thead>
      <tr>
        <th>排名</th>
        <th>课程号</th>
        <th>课程名称</th>
        <th>教师</th>
        <th>学分</th>
        <th>总容量</th>
        <th>剩余容量</th>
        <th>已选容量</th>
        <th>操作</th>
      </tr>
    </thead>
    <tbody></tbody>
  `
  
  const tbody = table.querySelector('tbody')
  
  // 渲染当前页数据
  const rows = []
  for(const item of pageData){
    const tr = document.createElement('tr')
    tr.innerHTML = `
      <td>${item.rank || '-'}</td>
      <td>${item.courseNo}</td>
      <td>${item.courseName || '未知'}</td>
      <td>${item.teacher || '未知'}</td>
      <td>${item.credit || 0}</td>
      <td>${item.totalCount || 0}</td>
      <td>${item.remainingCount ?? 0}</td>
      <td>${item.selectedCount || 0}</td>
      <td class="action-cell"></td>
    `
    tbody.appendChild(tr)
    rows.push({tr, item, actionCell: tr.querySelector('.action-cell')})
  }
  
  // 异步检查选课状态并添加按钮
  for(const {tr, item, actionCell} of rows){
    const btn = document.createElement('button')
    btn.className = 'action small-btn'
    
    if(userRole === 'STUDENT'){
      // 使用预先获取的已选课程列表
      const selected = selectedCourses.has(item.courseNo)
      console.log(`课程 ${item.courseNo} 选课状态:`, selected)
      if(selected){
        btn.textContent = '退课'
        btn.className = 'action danger small-btn'
        btn.onclick = ()=> dropCourse(studentNo, item.courseNo)
      }else{
        btn.textContent = '选课'
        btn.className = 'action success small-btn'
        btn.onclick = ()=> selectCourse(studentNo, item.courseNo)
      }
    }else if(userRole === 'ADMIN'){
      // 管理员模式下，使用学号输入框
      btn.textContent = '选课/退课'
      btn.onclick = ()=>{ 
        const sno = $('selection-student-no').value.trim()
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
  renderCourseSelectionPagination(totalPages)
}

// 渲染退选课分页控件（参考学生列表）
function renderCourseSelectionPagination(totalPages){
  const container = $('course-selection-pagination')
  if(!container) return
  
  if(totalPages <= 1){
    container.innerHTML = ''
    return
  }
  
  container.innerHTML = `
    <div class="pagination">
      <button class="page-btn" id="selection-prev-page" ${courseSelectionCurrentPage === 1 ? 'disabled' : ''}>上一页</button>
      <span class="page-info">${courseSelectionCurrentPage}/${totalPages}</span>
      <button class="page-btn" id="selection-next-page" ${courseSelectionCurrentPage === totalPages ? 'disabled' : ''}>下一页</button>
      <span class="page-jump">跳转到:</span>
      <input type="number" id="selection-jump-page" min="1" max="${totalPages}" value="${courseSelectionCurrentPage}" class="page-input" />
      <button class="page-btn" id="selection-jump-btn">跳转</button>
    </div>
  `
  
  // 上一页
  $('selection-prev-page').onclick = ()=>{
    if(courseSelectionCurrentPage > 1){
      courseSelectionCurrentPage--
      renderCourseSelection()
    }
  }
  
  // 下一页
  $('selection-next-page').onclick = ()=>{
    if(courseSelectionCurrentPage < totalPages){
      courseSelectionCurrentPage++
      renderCourseSelection()
    }
  }
  
  // 跳转
  $('selection-jump-btn').onclick = ()=>{
    const pageNum = parseInt($('selection-jump-page').value)
    if(pageNum >= 1 && pageNum <= totalPages){
      courseSelectionCurrentPage = pageNum
      renderCourseSelection()
    }else{
      alert(`请输入1到${totalPages}之间的页码`)
    }
  }
  
  // 回车键跳转
  $('selection-jump-page').addEventListener('keypress', (e)=>{
    if(e.key === 'Enter'){
      $('selection-jump-btn').click()
    }
  })
}

// 获取个人信息
async function fetchPersonalInfo(){
  if(userRole !== 'STUDENT'){
    alert('只有学生可以查看个人信息')
    return
  }
  
  try{
    // 获取学生信息
    const res = await fetch(API_STUDENTS + '/' + currentUser, {
      headers: getAuthHeaders()
    })
    const result = await res.json()
    
    if(result.code !== 200 || !result.data){
      alert('获取个人信息失败：' + result.message)
      return
    }
    
    const studentInfo = result.data
    
    // 获取已选课程
    const coursesRes = await fetch(API_STUDENTS + '/' + currentUser + '/selections', {
      headers: getAuthHeaders()
    })
    const coursesResult = await coursesRes.json()
    
    if(coursesResult.code === 200 && coursesResult.data){
      personalCoursesAllData = coursesResult.data
    }else{
      personalCoursesAllData = []
    }
    
    personalCoursesCurrentPage = 1
    renderPersonalInfo(studentInfo)
    
  }catch(e){
    alert('无法获取个人信息：'+e.message)
  }
}

// 渲染个人信息
function renderPersonalInfo(studentInfo){
  const container = $('personal-info-container')
  container.innerHTML = ''
  
  // 学生基本信息
  const role = studentInfo.role || 'STUDENT'
  const roleBadge = role === 'ADMIN' ? '<span class="role-badge admin">管理员</span>' : '<span class="role-badge student">学生</span>'
  
  const infoSection = document.createElement('div')
  infoSection.className = 'admin-section'
  infoSection.innerHTML = `
    <h3>学生信息</h3>
    <table class="students-table">
      <tbody>
        <tr><td><strong>学号</strong></td><td>${studentInfo.studentNo}</td></tr>
        <tr><td><strong>姓名</strong></td><td>${studentInfo.name}</td></tr>
        <tr><td><strong>专业</strong></td><td>${studentInfo.major}</td></tr>
        <tr><td><strong>年级</strong></td><td>${studentInfo.grade}</td></tr>
        <tr><td><strong>身份</strong></td><td>${roleBadge}</td></tr>
      </tbody>
    </table>
  `
  container.appendChild(infoSection)
  
  // 已选课程
  const coursesSection = document.createElement('div')
  coursesSection.className = 'admin-section'
  coursesSection.style.marginTop = '1.5rem'
  coursesSection.innerHTML = '<h3>已选课程</h3>'
  
  const coursesContainer = document.createElement('div')
  coursesContainer.id = 'personal-courses-container'
  coursesSection.appendChild(coursesContainer)
  container.appendChild(coursesSection)
  
  // 渲染选课列表（带分页）
  renderPersonalCourses()
}

// 渲染个人选课列表（带分页）
function renderPersonalCourses(){
  const container = $('personal-courses-container')
  container.innerHTML = ''
  
  if(personalCoursesAllData.length === 0){
    container.innerHTML = '<div style="text-align:center;padding:1rem;color:#95a5a6;">暂未选课</div>'
    $('personal-courses-pagination').innerHTML = ''
    return
  }
  
  // 计算总页数
  const totalPages = Math.ceil(personalCoursesAllData.length / personalCoursesPageSize)
  
  // 计算当前页的数据范围
  const start = (personalCoursesCurrentPage - 1) * personalCoursesPageSize
  const end = start + personalCoursesPageSize
  const pageData = personalCoursesAllData.slice(start, end)
  
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
        <th>选课时间</th>
      </tr>
    </thead>
    <tbody></tbody>
  `
  
  const tbody = table.querySelector('tbody')
  
  pageData.forEach(item=>{
    const tr = document.createElement('tr')
    const selectTime = item.selectTime ? new Date(item.selectTime).toLocaleString('zh-CN') : '未知'
    
    tr.innerHTML = `
      <td>${item.courseNo}</td>
      <td>${item.courseName || '未知'}</td>
      <td>${item.teacher || '未知'}</td>
      <td>${item.credit || 0}</td>
      <td>${item.totalCapacity || 0}</td>
      <td>${item.remaining ?? 0}</td>
      <td>${selectTime}</td>
    `
    tbody.appendChild(tr)
  })
  
  container.appendChild(table)
  renderPersonalCoursesPagination(totalPages)
}

// 渲染个人选课分页控件
function renderPersonalCoursesPagination(totalPages){
  const paginationContainer = document.createElement('div')
  paginationContainer.id = 'personal-courses-pagination'
  paginationContainer.className = 'pagination'
  
  const container = $('personal-info-container')
  
  // 移除旧的分页
  const oldPagination = container.querySelector('#personal-courses-pagination')
  if(oldPagination) oldPagination.remove()
  
  if(totalPages <= 1){
    paginationContainer.innerHTML = ''
    container.appendChild(paginationContainer)
    return
  }
  
  paginationContainer.innerHTML = `
    <div class="pagination">
      <button class="page-btn" id="personal-courses-prev-page" ${personalCoursesCurrentPage === 1 ? 'disabled' : ''}>上一页</button>
      <span class="page-info">${personalCoursesCurrentPage}/${totalPages}</span>
      <button class="page-btn" id="personal-courses-next-page" ${personalCoursesCurrentPage === totalPages ? 'disabled' : ''}>下一页</button>
      <span class="page-jump">跳转到:</span>
      <input type="number" id="personal-courses-jump-page" min="1" max="${totalPages}" value="${personalCoursesCurrentPage}" class="page-input" />
      <button class="page-btn" id="personal-courses-jump-btn">跳转</button>
    </div>
  `
  
  container.appendChild(paginationContainer)
  
  // 上一页
  $('personal-courses-prev-page').onclick = ()=>{
    if(personalCoursesCurrentPage > 1){
      personalCoursesCurrentPage--
      renderPersonalCourses()
    }
  }
  
  // 下一页
  $('personal-courses-next-page').onclick = ()=>{
    if(personalCoursesCurrentPage < totalPages){
      personalCoursesCurrentPage++
      renderPersonalCourses()
    }
  }
  
  // 跳转
  $('personal-courses-jump-btn').onclick = ()=>{
    const pageNum = parseInt($('personal-courses-jump-page').value)
    if(pageNum >= 1 && pageNum <= totalPages){
      personalCoursesCurrentPage = pageNum
      renderPersonalCourses()
    }else{
      alert(`请输入1到${totalPages}之间的页码`)
    }
  }
  
  // 回车键跳转
  $('personal-courses-jump-page').addEventListener('keypress', (e)=>{
    if(e.key === 'Enter'){
      $('personal-courses-jump-btn').click()
    }
  })
}

async function checkSelected(studentNo, courseNo){
  if(!studentNo) return false
  try{
    // 优先使用 statistics-service (8082) 的实时检查接口
    const url = new URL(`${STATISTICS_SERVICE_URL}/check/selected`)
    url.searchParams.set('studentNo', studentNo)
    url.searchParams.set('courseNo', courseNo)
    console.log('检查选课状态 - URL:', url.toString())
    const res = await fetch(url, {
      headers: getAuthHeaders()
    })
    console.log('检查选课状态 - 响应:', res.status)
    // statistics-service 直接返回 boolean，不是 Result 包装
    const result = await res.json()
    console.log('检查选课状态 - 结果:', result)
    return result
  }catch(e){
    // 降级方案：如果 8082 不可用，使用 basic-service (8080)
    console.warn('statistics-service 不可用，降级到 basic-service', e)
    try{
      const url = new URL(`${BASIC_SERVICE_URL}/selections/check`)
      url.searchParams.set('studentNo', studentNo)
      url.searchParams.set('courseNo', courseNo)
      console.log('降级检查选课状态 - URL:', url.toString())
      const res = await fetch(url, {
        headers: getAuthHeaders()
      })
      const result = await res.json()
      console.log('降级检查选课状态 - 结果:', result)
      return result.code === 200 && result.data === true
    }catch(err){
      console.error('检查选课状态失败:', err)
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
      studentsAllData = result.data // 保存全部数据
      studentsCurrentPage = 1 // 重置页码
      // 显示列表
      const listWrapper = $('students-list-wrapper')
      if(listWrapper) listWrapper.style.display = 'block'
      renderStudentsList(studentsAllData)
    }else{
      alert('获取学生列表失败：' + result.message)
    }
  }catch(e){
    alert('无法获取学生列表：'+e)
  }
}

function renderStudentsList(list){
  const container = $('students-list-container')
  container.innerHTML = ''
  
  if(!list || list.length === 0){
    container.innerHTML = '<div style="text-align:center;padding:2rem;color:#95a5a6;">暂无学生数据</div>'
    $('students-list-pagination').innerHTML = ''
    return
  }
  
  // 计算分页
  const totalPages = Math.ceil(list.length / studentsPageSize)
  const start = (studentsCurrentPage - 1) * studentsPageSize
  const end = start + studentsPageSize
  const pageData = list.slice(start, end)
  
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
      </tr>
    </thead>
    <tbody></tbody>
  `
  
  const tbody = table.querySelector('tbody')
  
  pageData.forEach(item=>{
    const tr = document.createElement('tr')
    const role = item.role || 'STUDENT'
    const roleBadge = role === 'ADMIN' ? '<span class="role-badge admin">管理员</span>' : '<span class="role-badge student">学生</span>'
    
    tr.innerHTML = `
      <td>${item.studentNo}</td>
      <td>${item.name}</td>
      <td>${item.major}</td>
      <td>${item.grade}</td>
      <td>${roleBadge}</td>
    `
    tbody.appendChild(tr)
  })
  
  container.appendChild(table)
  renderStudentsPagination(totalPages)
}

// 学生搜索功能
async function searchStudents(){
  const studentNo = $('search-student-no').value.trim()
  const studentName = $('search-student-name').value.trim()
  
  const container = $('student-search-results')
  const listWrapper = $('students-list-wrapper')
  
  // 如果两个搜索框都为空，清空结果并显示列表
  if(!studentNo && !studentName){
    container.innerHTML = ''
    if(listWrapper) listWrapper.style.display = 'block'
    return
  }
  
  // 有搜索条件时，隐藏列表
  if(listWrapper) listWrapper.style.display = 'none'
  
  try{
    // 获取所有学生数据
    const res = await fetch(API_STUDENTS, {
      headers: getAuthHeaders()
    })
    const result = await res.json()
    
    if(result.code !== 200 || !result.data){
      container.innerHTML = '<div style="text-align:center;padding:1rem;color:#e74c3c;">搜索失败：获取学生列表失败</div>'
      return
    }
    
    // 过滤学生数据
    let filteredStudents = result.data
    
    if(studentNo){
      filteredStudents = filteredStudents.filter(s => 
        s.studentNo && s.studentNo.toLowerCase().includes(studentNo.toLowerCase())
      )
    }
    
    if(studentName){
      filteredStudents = filteredStudents.filter(s => 
        s.name && s.name.toLowerCase().includes(studentName.toLowerCase())
      )
    }
    
    if(filteredStudents.length === 0){
      container.innerHTML = '<div style="text-align:center;padding:1rem;color:#e74c3c;">搜索失败：未找到匹配的学生</div>'
      return
    }
    
    // 显示搜索结果
    container.innerHTML = ''
    
    // 为每个匹配的学生显示信息和选课
    for(const student of filteredStudents){
      const studentSection = document.createElement('div')
      studentSection.className = 'admin-section'
      studentSection.style.marginBottom = '1.5rem'
      
      // 学生基本信息
      const role = student.role || 'STUDENT'
      const roleBadge = role === 'ADMIN' ? '<span class="role-badge admin">管理员</span>' : '<span class="role-badge student">学生</span>'
      
      studentSection.innerHTML = `
        <h3>学生信息</h3>
        <table class="students-table" style="margin-bottom:1rem;">
          <tbody>
            <tr><td><strong>学号</strong></td><td>${student.studentNo}</td></tr>
            <tr><td><strong>姓名</strong></td><td>${student.name}</td></tr>
            <tr><td><strong>专业</strong></td><td>${student.major}</td></tr>
            <tr><td><strong>年级</strong></td><td>${student.grade}</td></tr>
            <tr><td><strong>身份</strong></td><td>${roleBadge}</td></tr>
          </tbody>
        </table>
        <h3>已选课程</h3>
        <div class="student-courses-container" data-student-no="${student.studentNo}"></div>
      `
      
      container.appendChild(studentSection)
      
      // 获取该学生的选课列表
      const coursesContainer = studentSection.querySelector('.student-courses-container')
      await loadStudentCourses(student.studentNo, coursesContainer)
    }
    
  }catch(e){
    container.innerHTML = `<div style="text-align:center;padding:1rem;color:#e74c3c;">搜索失败：${e.message}</div>`
  }
}

// 加载学生的选课列表
async function loadStudentCourses(studentNo, container){
  try{
    const res = await fetch(`${API_STUDENTS}/${studentNo}/selections`, {
      headers: getAuthHeaders()
    })
    const result = await res.json()
    
    if(result.code !== 200 || !result.data || result.data.length === 0){
      container.innerHTML = '<div style="text-align:center;padding:1rem;color:#95a5a6;">该学生暂未选课</div>'
      return
    }
    
    const courses = result.data
    
    // 创建表格（与课程展示相同的样式）
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
          <th>选课时间</th>
        </tr>
      </thead>
      <tbody></tbody>
    `
    
    const tbody = table.querySelector('tbody')
    
    courses.forEach(item=>{
      const tr = document.createElement('tr')
      const selectTime = item.selectTime ? new Date(item.selectTime).toLocaleString('zh-CN') : '未知'
      
      tr.innerHTML = `
        <td>${item.courseNo}</td>
        <td>${item.courseName || '未知'}</td>
        <td>${item.teacher || '未知'}</td>
        <td>${item.credit || 0}</td>
        <td>${item.totalCapacity || 0}</td>
        <td>${item.remaining ?? 0}</td>
        <td>${selectTime}</td>
      `
      tbody.appendChild(tr)
    })
    
    container.appendChild(table)
    
  }catch(e){
    container.innerHTML = `<div style="text-align:center;padding:1rem;color:#e74c3c;">加载选课列表失败：${e.message}</div>`
  }
}

// 渲染学生列表分页控件
function renderStudentsPagination(totalPages){
  const container = $('students-list-pagination')
  if(!container) return
  
  if(totalPages <= 1){
    container.innerHTML = ''
    return
  }
  
  container.innerHTML = `
    <div class="pagination">
      <button class="page-btn" id="students-prev-page" ${studentsCurrentPage === 1 ? 'disabled' : ''}>上一页</button>
      <span class="page-info">${studentsCurrentPage}/${totalPages}</span>
      <button class="page-btn" id="students-next-page" ${studentsCurrentPage === totalPages ? 'disabled' : ''}>下一页</button>
      <span class="page-jump">跳转到:</span>
      <input type="number" id="students-jump-page" min="1" max="${totalPages}" value="${studentsCurrentPage}" class="page-input" />
      <button class="page-btn" id="students-jump-btn">跳转</button>
    </div>
  `
  
  // 上一页
  $('students-prev-page').onclick = ()=>{
    if(studentsCurrentPage > 1){
      studentsCurrentPage--
      renderStudentsList(studentsAllData)
    }
  }
  
  // 下一页
  $('students-next-page').onclick = ()=>{
    if(studentsCurrentPage < totalPages){
      studentsCurrentPage++
      renderStudentsList(studentsAllData)
    }
  }
  
  // 跳转
  $('students-jump-btn').onclick = ()=>{
    const pageNum = parseInt($('students-jump-page').value)
    if(pageNum >= 1 && pageNum <= totalPages){
      studentsCurrentPage = pageNum
      renderStudentsList(studentsAllData)
    }else{
      alert(`请输入1到${totalPages}之间的页码`)
    }
  }
  
  // 回车键跳转
  $('students-jump-page').addEventListener('keypress', (e)=>{
    if(e.key === 'Enter'){
      $('students-jump-btn').click()
    }
  })
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
      $('delete-student-no').value = ''
    }else{
      alert('删除学生失败：' + result.message)
    }
  }catch(e){
    alert('删除学生失败：'+e)
  }
}

// 管理面板 - 搜索课程
async function adminSearchCourse(){
  if(userRole !== 'ADMIN'){
    alert('只有管理员可以搜索课程')
    return
  }
  
  const courseNo = $('admin-search-course-no').value.trim()
  if(!courseNo){
    alert('请输入课程号')
    return
  }
  
  try{
    const res = await fetch(API_COURSES+'/'+courseNo, {
      headers: getAuthHeaders()
    })
    const result = await res.json()
    const container = $('admin-search-course-result')
    
    if(result.code === 200 && result.data){
      const course = result.data
      container.innerHTML = `
        <div class="search-result">
          <h4>课程信息</h4>
          <table class="students-table">
            <tbody>
              <tr><td><strong>课程号</strong></td><td>${course.courseNo}</td></tr>
              <tr><td><strong>课程名称</strong></td><td>${course.courseName || '未知'}</td></tr>
              <tr><td><strong>教师</strong></td><td>${course.teacher || '未知'}</td></tr>
              <tr><td><strong>学分</strong></td><td>${course.credit || 0}</td></tr>
              <tr><td><strong>总容量</strong></td><td>${course.totalCapacity || 0}</td></tr>
              <tr><td><strong>剩余容量</strong></td><td>${course.remaining ?? 0}</td></tr>
              <tr><td><strong>已选容量</strong></td><td>${course.selectedCount || 0}</td></tr>
            </tbody>
          </table>
        </div>
      `
    }else{
      container.innerHTML = '<div class="search-result"><p style="color:#e74c3c;">未找到该课程</p></div>'
    }
  }catch(e){
    $('admin-search-course-result').innerHTML = '<div class="search-result"><p style="color:#e74c3c;">搜索失败：'+e.message+'</p></div>'
  }
}

// 管理面板 - 搜索学生
async function adminSearchStudent(){
  if(userRole !== 'ADMIN'){
    alert('只有管理员可以搜索学生')
    return
  }
  
  const studentNo = $('admin-search-student-no').value.trim()
  if(!studentNo){
    alert('请输入学号')
    return
  }
  
  try{
    const res = await fetch(API_STUDENTS+'/'+studentNo, {
      headers: getAuthHeaders()
    })
    const result = await res.json()
    const container = $('admin-search-student-result')
    
    if(result.code === 200 && result.data){
      const student = result.data
      const role = student.role || 'STUDENT'
      const roleBadge = role === 'ADMIN' ? '<span class="role-badge admin">管理员</span>' : '<span class="role-badge student">学生</span>'
      
      container.innerHTML = `
        <div class="search-result">
          <h4>学生信息</h4>
          <table class="students-table">
            <tbody>
              <tr><td><strong>学号</strong></td><td>${student.studentNo}</td></tr>
              <tr><td><strong>姓名</strong></td><td>${student.name}</td></tr>
              <tr><td><strong>专业</strong></td><td>${student.major}</td></tr>
              <tr><td><strong>年级</strong></td><td>${student.grade}</td></tr>
              <tr><td><strong>身份</strong></td><td>${roleBadge}</td></tr>
            </tbody>
          </table>
        </div>
      `
    }else{
      container.innerHTML = '<div class="search-result"><p style="color:#e74c3c;">未找到该学生</p></div>'
    }
  }catch(e){
    $('admin-search-student-result').innerHTML = '<div class="search-result"><p style="color:#e74c3c;">搜索失败：'+e.message+'</p></div>'
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
    if(!userInfo){
      fetchUserInfo()
    }
  }else{
    showLogin()
  }
  
  $('login-btn').onclick = login
  $('logout-btn').onclick = logout
  
  // 导航栏事件
  document.getElementById('nav-course-display').onclick = ()=>show('course-display')
  document.getElementById('nav-course-selection').onclick = ()=>{
    show('course-selection')
    courseSelectionCurrentPage = 1
    fetchCourseSelection()
  }
  document.getElementById('nav-personal-info').onclick = ()=>{
    show('personal-info')
    fetchPersonalInfo()
  }
  document.getElementById('nav-students').onclick = ()=>{
    show('students')
    fetchAllStudents()
  }
  document.getElementById('nav-admin').onclick = ()=>show('admin')
  
  // 课程展示事件
  document.getElementById('refresh-course-display').onclick = ()=>fetchCourses()
  
  // 退选课事件
  document.getElementById('refresh-course-selection').onclick = ()=>{
    courseSelectionCurrentPage = 1
    fetchCourseSelection()
  }
  document.getElementById('search-course-btn').onclick = ()=>{
    courseSelectionFilters.courseNo = $('search-course-no').value.trim()
    courseSelectionFilters.courseName = $('search-course-name').value.trim()
    courseSelectionFilters.teacher = $('search-teacher').value.trim()
    courseSelectionCurrentPage = 1
    fetchCourseSelection()
  }
  document.getElementById('clear-search-btn').onclick = ()=>{
    $('search-course-no').value = ''
    $('search-course-name').value = ''
    $('search-teacher').value = ''
    courseSelectionFilters = { courseNo: '', courseName: '', teacher: '' }
    courseSelectionCurrentPage = 1
    fetchCourseSelection()
  }
  
  // 学生列表事件
  document.getElementById('refresh-students-list').onclick = ()=>{
    // 清空搜索框
    $('search-student-no').value = ''
    $('search-student-name').value = ''
    // 清空搜索结果
    $('student-search-results').innerHTML = ''
    // 显示列表
    const listWrapper = $('students-list-wrapper')
    if(listWrapper) listWrapper.style.display = 'block'
    // 刷新学生列表
    studentsCurrentPage = 1
    fetchAllStudents()
  }
  
  // 学生实时搜索事件
  const searchStudentNoInput = $('search-student-no')
  const searchStudentNameInput = $('search-student-name')
  
  // 学号输入实时搜索
  searchStudentNoInput.addEventListener('input', ()=>{
    clearTimeout(studentSearchTimer)
    studentSearchTimer = setTimeout(()=>{
      searchStudents()
    }, 300) // 300ms 延迟，避免频繁请求
  })
  
  // 姓名输入实时搜索
  searchStudentNameInput.addEventListener('input', ()=>{
    clearTimeout(studentSearchTimer)
    studentSearchTimer = setTimeout(()=>{
      searchStudents()
    }, 300) // 300ms 延迟，避免频繁请求
  })
  
  // 管理面板事件
  document.getElementById('admin-add-course').onclick = addCourse
  document.getElementById('admin-delete-course').onclick = deleteCourse
  document.getElementById('admin-add-student').onclick = addStudent
  document.getElementById('admin-delete-student').onclick = ()=>{
    const studentNo = $('delete-student-no').value.trim()
    if(!studentNo){
      alert('请输入要删除的学号')
      return
    }
    deleteStudent(studentNo)
  }
  document.getElementById('admin-search-course-btn').onclick = adminSearchCourse
  document.getElementById('admin-search-student-btn').onclick = adminSearchStudent
})

window.deleteStudent = deleteStudent
window.deleteCourse = deleteCourse
window.adminSearchCourse = adminSearchCourse
window.adminSearchStudent = adminSearchStudent
