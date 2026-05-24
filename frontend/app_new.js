const BACKEND_HOST = window.BACKEND_HOST || 'localhost'
const BASIC_SERVICE_PORT = 8080
const SELECTION_SERVICE_PORT = 8081
const STATISTICS_SERVICE_PORT = 8082

const BASIC_SERVICE_URL = `http://${BACKEND_HOST}:${BASIC_SERVICE_PORT}`
const SELECTION_SERVICE_URL = `http://${BACKEND_HOST}:${SELECTION_SERVICE_PORT}/api/selection`
const STATISTICS_SERVICE_URL = `http://${BACKEND_HOST}:${STATISTICS_SERVICE_PORT}/api/realtime`

const API_LOGIN = `${BASIC_SERVICE_URL}/users/login`
const API_COURSES = `${BASIC_SERVICE_URL}/courses`
const API_STUDENTS = `${BASIC_SERVICE_URL}/students`
const API_SELECTIONS_SUBMIT = `${BASIC_SERVICE_URL}/selections/submit`
const API_SELECTIONS_WITHDRAW = `${BASIC_SERVICE_URL}/selections/withdraw`
const API_SELECTIONS_LIST = `${BASIC_SERVICE_URL}/selections/list`
const API_RANKING = `${STATISTICS_SERVICE_URL}/rank/list`
const API_TOTAL_COUNT = `${STATISTICS_SERVICE_URL}/total/count`

// 分页配置
const PAGE_SIZE = 15
let allCourses = []
let courseCurrentPage = 1
let courseTotalPages = 1
let allStudents = []
let studentCurrentPage = 1
let studentTotalPages = 1
let selectionCourses = []
let selectionCurrentPage = 1
let selectionTotalPages = 1
let myCoursesData = []
let myCoursesCurrentPage = 1
let myCoursesTotalPages = 1
// 新增：用于存储管理员退选课界面的所有课程数据
let adminSelectionAllCourses = []
let adminSelectionFilteredCourses = []

let currentUser = null
let currentRole = null
let selectedStudentNo = ''

// 管理员导航元素
let navCourseDisplay, navSelection, navStudentList, navAdminPanel

// 学生导航元素
let navStudentCourseDisplay, navStudentSelection, navStudentInfo

// 视图元素
let views = {}

// 初始化DOM元素
function initDOMElements() {
  // 管理员导航
  navCourseDisplay = document.getElementById('nav-course-display')
  navSelection = document.getElementById('nav-selection')
  navStudentList = document.getElementById('nav-student-list')
  navAdminPanel = document.getElementById('nav-admin-panel')

  // 学生导航
  navStudentCourseDisplay = document.getElementById('nav-student-course-display')
  navStudentSelection = document.getElementById('nav-student-selection')
  navStudentInfo = document.getElementById('nav-student-info')

  // 视图元素
  views = {
    courseDisplay: document.getElementById('course-display-view'),
    selection: document.getElementById('selection-view'),
    studentList: document.getElementById('student-list-view'),
    adminPanel: document.getElementById('admin-panel-view'),
    studentCourseDisplay: document.getElementById('student-course-display-view'),
    studentSelection: document.getElementById('student-selection-view'),
    studentInfo: document.getElementById('student-info-view')
  }
}

// 显示指定视图
function showView(viewName) {
  // 隐藏所有视图
  Object.values(views).forEach(v => v.classList.add('hidden'))
  
  // 显示指定视图
  if (views[viewName]) {
    views[viewName].classList.remove('hidden')
  }
}

// 更新管理员导航激活状态
function updateAdminNav(activeBtn) {
  [navCourseDisplay, navSelection, navStudentList, navAdminPanel].forEach(btn => {
    btn.classList.remove('active')
  })
  activeBtn.classList.add('active')
}

// 更新学生导航激活状态
function updateStudentNav(activeBtn) {
  [navStudentCourseDisplay, navStudentSelection, navStudentInfo].forEach(btn => {
    btn.classList.remove('active')
  })
  activeBtn.classList.add('active')
}

// 管理员导航事件
function initAdminNavEvents() {
  navCourseDisplay.addEventListener('click', () => {
    updateAdminNav(navCourseDisplay)
    showView('courseDisplay')
    loadAllCourses()
  })

  navSelection.addEventListener('click', () => {
    updateAdminNav(navSelection)
    showView('selection')
    // 清空之前的数据
    selectionCourses = []
    adminSelectionAllCourses = []
    adminSelectionFilteredCourses = []
    document.getElementById('selection-courses').innerHTML = ''
  })

  navStudentList.addEventListener('click', () => {
    updateAdminNav(navStudentList)
    showView('studentList')
    loadStudents()
  })

  navAdminPanel.addEventListener('click', () => {
    updateAdminNav(navAdminPanel)
    showView('adminPanel')
  })
}

// 学生导航事件
function initStudentNavEvents() {
  navStudentCourseDisplay.addEventListener('click', () => {
    updateStudentNav(navStudentCourseDisplay)
    showView('studentCourseDisplay')
    loadStudentCourses()
  })

  navStudentSelection.addEventListener('click', () => {
    updateStudentNav(navStudentSelection)
    showView('studentSelection')
    loadMyCourses()
  })

  navStudentInfo.addEventListener('click', () => {
    updateStudentNav(navStudentInfo)
    showView('studentInfo')
    loadPersonalInfo()
  })
}

async function login(){
  const userid = document.getElementById('login-userid').value.trim()
  const password = document.getElementById('login-password').value.trim()
  const errorDiv = document.getElementById('login-error')
  errorDiv.textContent = ''

  try{
    const res = await fetch(API_LOGIN, {
      method:'POST',
      headers:{'Content-Type':'application/json'},
      body: JSON.stringify({userid, password})
    })
    const result = await res.json()
    
    console.log('登录响应数据:', result)
    
    if(result.code === 200){
      currentUser = result.data
      currentRole = result.data.role
      
      console.log('用户角色:', currentRole)
      
      // 根据角色显示不同的导航栏（不区分大小写）
      const roleLower = (currentRole || '').toLowerCase()
      if(roleLower === 'admin'){
        document.getElementById('admin-nav').classList.remove('hidden')
        document.getElementById('student-nav').classList.add('hidden')
        showView('courseDisplay')
        loadAllCourses()
      } else {
        document.getElementById('admin-nav').classList.add('hidden')
        document.getElementById('student-nav').classList.remove('hidden')
        showView('studentCourseDisplay')
        loadStudentCourses()
      }
      
      updateUserDisplay(result.data)
      document.getElementById('login-view').classList.add('hidden')
      document.getElementById('main-app').classList.remove('hidden')
    } else {
      errorDiv.textContent = result.message || '登录失败'
    }
  }catch(e){
    errorDiv.textContent = '网络错误，请检查后端服务是否启动'
    console.error(e)
  }
}

function updateUserDisplay(user){
  document.getElementById('user-studentno').textContent = user.studentNo || ''
  document.getElementById('user-name').textContent = user.name || ''
  document.getElementById('user-major').textContent = user.major || ''
  document.getElementById('user-grade').textContent = user.grade || ''
  
  const roleBadge = document.getElementById('current-role')
  // 显示角色：管理员(ADMIN) 或 学生(STUDENT)，不区分大小写
  const roleLower = (user.role || '').toLowerCase()
  if (roleLower === 'admin') {
    roleBadge.textContent = 'ADMIN'
    roleBadge.className = 'role-badge admin'
  } else {
    roleBadge.textContent = 'STUDENT'
    roleBadge.className = 'role-badge student'
  }
  
  document.getElementById('user-info').classList.remove('hidden')
}

document.getElementById('login-btn').addEventListener('click', login)
document.getElementById('login-password').addEventListener('keypress', e=>{
  if(e.key==='Enter') login()
})

// ==================== 课程展示（管理员）====================
async function loadAllCourses(){
  try{
    const res = await fetch(API_COURSES, {
      headers:{'Authorization': `Bearer ${currentUser?.token || ''}`}
    })
    const result = await res.json()
    allCourses = result.data || []
    courseCurrentPage = 1
    renderCourseDisplay(allCourses)
  }catch(e){
    console.error('加载课程失败:', e)
  }
}

function renderCourseDisplay(courses){
  const container = document.getElementById('course-display')
  container.innerHTML = ''
  
  if(!courses || courses.length === 0){
    container.innerHTML = '<p class="muted">暂无课程数据</p>'
    return
  }
  
  const totalPages = Math.ceil(courses.length / PAGE_SIZE) || 1
  const startIndex = (courseCurrentPage - 1) * PAGE_SIZE
  const endIndex = startIndex + PAGE_SIZE
  const currentPageData = courses.slice(startIndex, endIndex)
  
  const table = document.createElement('table')
  table.className = 'courses-table'
  
  const thead = document.createElement('thead')
  thead.innerHTML = `
    <tr>
      <th>课程编号</th>
      <th>课程名称</th>
      <th>教师</th>
      <th>学分</th>
      <th>总容量</th>
      <th>剩余容量</th>
      <th>已选容量</th>
    </tr>
  `
  table.appendChild(thead)
  
  const tbody = document.createElement('tbody')
  currentPageData.forEach(course=>{
    const tr = document.createElement('tr')
    tr.innerHTML = `
      <td>${course.courseNo}</td>
      <td>${course.name}</td>
      <td>${course.teacher}</td>
      <td>${course.credit}</td>
      <td>${course.capacity}</td>
      <td>${course.remainingCapacity}</td>
      <td>${course.selectedCount}</td>
    `
    tbody.appendChild(tr)
  })
  table.appendChild(tbody)
  container.appendChild(table)
  
  // 添加分页控件
  renderPagination(container, 'course', courseCurrentPage, totalPages, goToCoursePage)
}

// ==================== 退选课（管理员）====================
async function loadSelectionCourses(){
  const studentNo = document.getElementById('selection-student-no').value.trim()
  if(!studentNo){
    alert('请输入学生学号')
    return
  }
  
  selectedStudentNo = studentNo
  
  try{
    // 调用8082端口的实时排行榜接口，使用分页
    let allCoursesList = []
    let currentPage = 1
    const pageSize = 100
    
    // 循环获取所有数据
    while(true) {
      const res = await fetch(`${API_RANKING}?page=${currentPage}&size=${pageSize}`, {
        headers:{'Authorization': `Bearer ${currentUser?.token || ''}`}
      })
      const data = await res.json()
      
      // 处理响应格式（可能是直接返回数组或包装在Result中）
      let courses = Array.isArray(data) ? data : (data.data || [])
      
      if(!courses || courses.length === 0) {
        break
      }
      
      allCoursesList = allCoursesList.concat(courses)
      
      // 如果返回的数据少于pageSize，说明已经是最后一页
      if(courses.length < pageSize) {
        break
      }
      
      currentPage++
    }
    
    // 按剩余容量从大到小排序
    allCoursesList.sort((a, b) => b.remainingCapacity - a.remainingCapacity)
    
    adminSelectionAllCourses = allCoursesList
    adminSelectionFilteredCourses = allCoursesList
    selectionCurrentPage = 1
    renderSelectionCourses(adminSelectionFilteredCourses)
  }catch(e){
    console.error('加载课程失败:', e)
    alert('加载课程列表失败')
  }
}

function renderSelectionCourses(courses){
  const container = document.getElementById('selection-courses')
  container.innerHTML = ''
  
  if(!courses || courses.length === 0){
    container.innerHTML = '<p class="muted">暂无课程数据</p>'
    return
  }
  
  const totalPages = Math.ceil(courses.length / PAGE_SIZE) || 1
  const startIndex = (selectionCurrentPage - 1) * PAGE_SIZE
  const endIndex = startIndex + PAGE_SIZE
  const currentPageData = courses.slice(startIndex, endIndex)
  
  const table = document.createElement('table')
  table.className = 'courses-table'
  
  const thead = document.createElement('thead')
  thead.innerHTML = `
    <tr>
      <th>课程编号</th>
      <th>课程名称</th>
      <th>教师</th>
      <th>学分</th>
      <th>总容量</th>
      <th>剩余容量</th>
      <th>已选容量</th>
      <th>操作</th>
    </tr>
  `
  table.appendChild(thead)
  
  const tbody = document.createElement('tbody')
  currentPageData.forEach(course=>{
    const tr = document.createElement('tr')
    const isSelected = course.isSelected || false
    
    tr.innerHTML = `
      <td>${course.courseNo}</td>
      <td>${course.name}</td>
      <td>${course.teacher}</td>
      <td>${course.credit}</td>
      <td>${course.capacity}</td>
      <td>${course.remainingCapacity}</td>
      <td>${course.selectedCount}</td>
      <td>
        <button class="small-btn" onclick="handleSelection('${course.courseNo}', ${isSelected})">
          ${isSelected ? '退课' : '选课'}
        </button>
      </td>
    `
    tbody.appendChild(tr)
  })
  table.appendChild(tbody)
  container.appendChild(table)
  
  // 添加分页控件
  renderPagination(container, 'selection', selectionCurrentPage, totalPages, goToSelectionPage)
}

async function handleSelection(courseNo, isCurrentlySelected){
  if(!selectedStudentNo){
    alert('请先输入学生学号')
    return
  }
  
  try{
    if(isCurrentlySelected){
      // 退课
      await withdrawCourse(selectedStudentNo, courseNo)
    }else{
      // 选课
      await selectCourseForStudent(selectedStudentNo, courseNo)
    }
    // 重新加载课程列表
    loadSelectionCourses()
  }catch(e){
    console.error('操作失败:', e)
    alert('操作失败')
  }
}

async function selectCourseForStudent(studentNo, courseNo){
  try{
    const res = await fetch(`${SELECTION_SERVICE_URL}/select`, {
      method:'POST',
      headers:{
        'Content-Type':'application/json',
        'Authorization': `Bearer ${currentUser?.token || ''}`
      },
      body: JSON.stringify({studentNo, courseNo})
    })
    const result = await res.json()
    if(result.success || result.code === 200){
      alert('选课成功')
    }else{
      throw new Error(result.message || '选课失败')
    }
  }catch(e){
    console.warn('selection-service 不可用，降级到 basic-service')
    const res = await fetch(API_SELECTIONS_SUBMIT, {
      method:'POST',
      headers:{
        'Content-Type':'application/json',
        'Authorization': `Bearer ${currentUser?.token || ''}`
      },
      body: JSON.stringify({studentNo, courseNo})
    })
    const result = await res.json()
    if(result.code !== 200){
      throw new Error(result.message || '选课失败')
    }
    alert('选课成功')
  }
}

async function withdrawCourse(studentNo, courseNo){
  try{
    const res = await fetch(`${SELECTION_SERVICE_URL}/withdraw`, {
      method:'POST',
      headers:{
        'Content-Type':'application/json',
        'Authorization': `Bearer ${currentUser?.token || ''}`
      },
      body: JSON.stringify({studentNo, courseNo})
    })
    const result = await res.json()
    if(result.success || result.code === 200){
      alert('退课成功')
    }else{
      throw new Error(result.message || '退课失败')
    }
  }catch(e){
    console.warn('selection-service 不可用，降级到 basic-service')
    const res = await fetch(API_SELECTIONS_WITHDRAW, {
      method:'POST',
      headers:{
        'Content-Type':'application/json',
        'Authorization': `Bearer ${currentUser?.token || ''}`
      },
      body: JSON.stringify({studentNo, courseNo})
    })
    const result = await res.json()
    if(result.code !== 200){
      throw new Error(result.message || '退课失败')
    }
    alert('退课成功')
  }
}

function goToSelectionPage(page){
  selectionCurrentPage = page
  renderSelectionCourses(adminSelectionFilteredCourses)
}

// 搜索功能
function initSearchEvents() {
  document.getElementById('search-courses-btn').addEventListener('click', searchCourses)
  document.getElementById('clear-search-btn').addEventListener('click', clearSearch)
}

function searchCourses(){
  const courseNo = document.getElementById('search-course-no').value.trim().toLowerCase()
  const courseName = document.getElementById('search-course-name').value.trim().toLowerCase()
  const teacher = document.getElementById('search-course-teacher').value.trim().toLowerCase()
  
  let filtered = adminSelectionAllCourses
  
  if(courseNo){
    filtered = filtered.filter(c => c.courseNo.toLowerCase().includes(courseNo))
  }
  if(courseName){
    filtered = filtered.filter(c => c.name.toLowerCase().includes(courseName))
  }
  if(teacher){
    filtered = filtered.filter(c => c.teacher.toLowerCase().includes(teacher))
  }
  
  adminSelectionFilteredCourses = filtered
  selectionCurrentPage = 1
  renderSelectionCourses(filtered)
}

function clearSearch(){
  document.getElementById('search-course-no').value = ''
  document.getElementById('search-course-name').value = ''
  document.getElementById('search-course-teacher').value = ''
  adminSelectionFilteredCourses = adminSelectionAllCourses
  selectionCurrentPage = 1
  renderSelectionCourses(adminSelectionFilteredCourses)
}

// ==================== 学生列表（管理员）====================
async function loadStudents(){
  try{
    const res = await fetch(API_STUDENTS, {
      headers:{'Authorization': `Bearer ${currentUser?.token || ''}`}
    })
    const result = await res.json()
    allStudents = result.data || []
    studentCurrentPage = 1
    renderStudentsTable(allStudents)
  }catch(e){
    console.error('加载学生失败:', e)
  }
}

function renderStudentsTable(students){
  const container = document.getElementById('students-list')
  container.innerHTML = ''
  
  if(!students || students.length === 0){
    container.innerHTML = '<p class="muted">暂无学生数据</p>'
    return
  }
  
  const totalPages = Math.ceil(students.length / PAGE_SIZE) || 1
  const startIndex = (studentCurrentPage - 1) * PAGE_SIZE
  const endIndex = startIndex + PAGE_SIZE
  const currentPageData = students.slice(startIndex, endIndex)
  
  const table = document.createElement('table')
  table.className = 'students-table'
  
  const thead = document.createElement('thead')
  thead.innerHTML = `
    <tr>
      <th>学号</th>
      <th>姓名</th>
      <th>专业</th>
      <th>年级</th>
    </tr>
  `
  table.appendChild(thead)
  
  const tbody = document.createElement('tbody')
  currentPageData.forEach(student=>{
    const tr = document.createElement('tr')
    tr.innerHTML = `
      <td>${student.studentNo}</td>
      <td>${student.name}</td>
      <td>${student.major}</td>
      <td>${student.grade}</td>
    `
    tbody.appendChild(tr)
  })
  table.appendChild(tbody)
  container.appendChild(table)
  
  // 添加分页控件
  renderPagination(container, 'student', studentCurrentPage, totalPages, goToStudentPage)
}

function goToStudentPage(page){
  studentCurrentPage = page
  renderStudentsTable(allStudents)
}

// ==================== 管理面板（管理员）====================
// 初始化管玆面板事件
function initAdminPanelEvents() {
  // 添加课程
  document.getElementById('admin-add-course').addEventListener('click', async ()=>{
  const courseNo = document.getElementById('admin-course-no').value.trim()
  const name = document.getElementById('admin-course-name').value.trim()
  const teacher = document.getElementById('admin-course-teacher').value.trim()
  const credit = parseInt(document.getElementById('admin-course-credit').value)
  const capacity = parseInt(document.getElementById('admin-course-capacity').value)
  
  if(!courseNo || !name || !teacher || isNaN(credit) || isNaN(capacity)){
    alert('请填写完整的课程信息')
    return
  }
  
  try{
    const res = await fetch(API_COURSES, {
      method:'POST',
      headers:{
        'Content-Type':'application/json',
        'Authorization': `Bearer ${currentUser?.token || ''}`
      },
      body: JSON.stringify({courseNo, name, teacher, credit, capacity})
    })
    const result = await res.json()
    if(result.code === 200){
      alert('添加成功')
      // 清空表单
      document.getElementById('admin-course-no').value = ''
      document.getElementById('admin-course-name').value = ''
      document.getElementById('admin-course-teacher').value = ''
      document.getElementById('admin-course-credit').value = ''
      document.getElementById('admin-course-capacity').value = ''
    }else{
      alert(result.message || '添加失败')
    }
  }catch(e){
    alert('网络错误')
    console.error(e)
  }
})

// 删除课程
document.getElementById('admin-delete-course').addEventListener('click', async ()=>{
  const courseNo = document.getElementById('delete-course-no').value.trim()
  if(!courseNo){
    alert('请输入课程号')
    return
  }
  
  if(!confirm(`确定要删除课程 ${courseNo} 吗？`)){
    return
  }
  
  try{
    const res = await fetch(`${API_COURSES}/${courseNo}`, {
      method:'DELETE',
      headers:{'Authorization': `Bearer ${currentUser?.token || ''}`}
    })
    const result = await res.json()
    if(result.code === 200){
      alert('删除成功')
      document.getElementById('delete-course-no').value = ''
    }else{
      alert(result.message || '删除失败')
    }
  }catch(e){
    alert('网络错误')
    console.error(e)
  }
})

// 搜索课程
document.getElementById('admin-search-course-btn').addEventListener('click', async ()=>{
  const keyword = document.getElementById('admin-search-course').value.trim()
  if(!keyword){
    alert('请输入搜索关键词')
    return
  }
  
  try{
    const res = await fetch(`${API_COURSES}/search?keyword=${encodeURIComponent(keyword)}`, {
      headers:{'Authorization': `Bearer ${currentUser?.token || ''}`}
    })
    const result = await res.json()
    const courses = result.data || []
    
    const container = document.getElementById('admin-course-search-result')
    container.innerHTML = ''
    
    if(courses.length === 0){
      container.innerHTML = '<p class="muted">未找到相关课程</p>'
      return
    }
    
    const table = document.createElement('table')
    table.className = 'courses-table'
    
    table.innerHTML = `
      <thead>
        <tr>
          <th>课程编号</th>
          <th>课程名称</th>
          <th>教师</th>
          <th>学分</th>
          <th>总容量</th>
          <th>剩余容量</th>
          <th>已选容量</th>
        </tr>
      </thead>
      <tbody>
        ${courses.map(c => `
          <tr>
            <td>${c.courseNo}</td>
            <td>${c.name}</td>
            <td>${c.teacher}</td>
            <td>${c.credit}</td>
            <td>${c.capacity}</td>
            <td>${c.remainingCapacity}</td>
            <td>${c.selectedCount}</td>
          </tr>
        `).join('')}
      </tbody>
    `
    
    container.appendChild(table)
  }catch(e){
    alert('搜索失败')
    console.error(e)
  }
})

// 添加学生
document.getElementById('admin-add-student').addEventListener('click', async ()=>{
  const studentNo = document.getElementById('admin-student-no').value.trim()
  const name = document.getElementById('admin-student-name').value.trim()
  const major = document.getElementById('admin-student-major').value.trim()
  const grade = document.getElementById('admin-student-grade').value.trim()
  
  if(!studentNo || !name || !major || !grade){
    alert('请填写完整的学生信息')
    return
  }
  
  try{
    const res = await fetch(API_STUDENTS, {
      method:'POST',
      headers:{
        'Content-Type':'application/json',
        'Authorization': `Bearer ${currentUser?.token || ''}`
      },
      body: JSON.stringify({studentNo, name, major, grade})
    })
    const result = await res.json()
    if(result.code === 200){
      alert('添加成功')
      // 清空表单
      document.getElementById('admin-student-no').value = ''
      document.getElementById('admin-student-name').value = ''
      document.getElementById('admin-student-major').value = ''
      document.getElementById('admin-student-grade').value = ''
    }else{
      alert(result.message || '添加失败')
    }
  }catch(e){
    alert('网络错误')
    console.error(e)
  }
})

// 删除学生
document.getElementById('admin-delete-student').addEventListener('click', async ()=>{
  const studentNo = document.getElementById('delete-student-no').value.trim()
  if(!studentNo){
    alert('请输入学号')
    return
  }
  
  if(!confirm(`确定要删除学生 ${studentNo} 吗？`)){
    return
  }
  
  try{
    const res = await fetch(`${API_STUDENTS}/${studentNo}`, {
      method:'DELETE',
      headers:{'Authorization': `Bearer ${currentUser?.token || ''}`}
    })
    const result = await res.json()
    if(result.code === 200){
      alert('删除成功')
      document.getElementById('delete-student-no').value = ''
    }else{
      alert(result.message || '删除失败')
    }
  }catch(e){
    alert('网络错误')
    console.error(e)
  }
})

// 搜索学生
let adminStudentSearchCourses = []
let adminStudentSearchCurrentPage = 1

document.getElementById('admin-search-student-btn').addEventListener('click', async ()=>{
  const studentNo = document.getElementById('admin-search-student').value.trim()
  if(!studentNo){
    alert('请输入学号')
    return
  }
  
  try{
    const res = await fetch(`${API_STUDENTS}/${studentNo}`, {
      headers:{'Authorization': `Bearer ${currentUser?.token || ''}`}
    })
    const result = await res.json()
    
    const container = document.getElementById('admin-student-search-result')
    container.innerHTML = ''
    
    if(result.code !== 200 || !result.data){
      container.innerHTML = '<p class="muted">未找到该学生</p>'
      return
    }
    
    const student = result.data
    
    // 显示学生信息
    const infoDiv = document.createElement('div')
    infoDiv.className = 'card'
    infoDiv.innerHTML = `
      <h3>学生信息</h3>
      <p><strong>学号:</strong> ${student.studentNo}</p>
      <p><strong>姓名:</strong> ${student.name}</p>
      <p><strong>专业:</strong> ${student.major}</p>
      <p><strong>年级:</strong> ${student.grade}</p>
    `
    container.appendChild(infoDiv)
    
    // 获取已选课程
    try{
      const selRes = await fetch(`${API_SELECTIONS_LIST}/${studentNo}`, {
        headers:{'Authorization': `Bearer ${currentUser?.token || ''}`}
      })
      const selResult = await selRes.json()
      adminStudentSearchCourses = selResult.data || []
      adminStudentSearchCurrentPage = 1
      
      const coursesDiv = document.createElement('div')
      coursesDiv.style.marginTop = '1rem'
      coursesDiv.id = 'admin-student-courses-container'
      coursesDiv.innerHTML = '<h3>已选课程</h3>'
      
      renderAdminStudentSearchCourses(adminStudentSearchCourses, coursesDiv)
      container.appendChild(coursesDiv)
    }catch(e){
      console.error('加载选课记录失败:', e)
    }
  }catch(e){
    alert('搜索失败')
    console.error(e)
  }
})
}

function renderAdminStudentSearchCourses(courses, container){
  // 清除之前的表格和分页（保留标题）
  const existingTable = container.querySelector('table')
  const existingPagination = container.querySelector('[style*="justify-content: center"]')
  if(existingTable) existingTable.remove()
  if(existingPagination) existingPagination.remove()
  
  if(courses.length === 0){
    const noCourses = document.createElement('p')
    noCourses.className = 'muted'
    noCourses.textContent = '该学生暂未选课'
    container.appendChild(noCourses)
    return
  }
  
  const totalPages = Math.ceil(courses.length / PAGE_SIZE) || 1
  const startIndex = (adminStudentSearchCurrentPage - 1) * PAGE_SIZE
  const endIndex = startIndex + PAGE_SIZE
  const currentPageData = courses.slice(startIndex, endIndex)
  
  const table = document.createElement('table')
  table.className = 'courses-table'
  
  table.innerHTML = `
    <thead>
      <tr>
        <th>课程编号</th>
        <th>课程名称</th>
        <th>教师</th>
        <th>学分</th>
        <th>总容量</th>
        <th>剩余容量</th>
        <th>已选容量</th>
      </tr>
    </thead>
    <tbody>
      ${currentPageData.map(s => `
        <tr>
          <td>${s.courseNo}</td>
          <td>${s.courseName}</td>
          <td>${s.teacher}</td>
          <td>${s.credit}</td>
          <td>${s.capacity}</td>
          <td>${s.remainingCapacity}</td>
          <td>${s.selectedCount}</td>
        </tr>
      `).join('')}
    </tbody>
  `
  
  container.appendChild(table)
  
  // 添加分页控件
  renderPagination(container, 'adminStudentSearch', adminStudentSearchCurrentPage, totalPages, goToAdminStudentSearchPage)
}

function goToAdminStudentSearchPage(page){
  adminStudentSearchCurrentPage = page
  const container = document.getElementById('admin-student-courses-container')
  if(container) {
    renderAdminStudentSearchCourses(adminStudentSearchCourses, container)
  }
}

// ==================== 课程展示（学生）====================
async function loadStudentCourses(){
  try{
    const res = await fetch(API_COURSES, {
      headers:{'Authorization': `Bearer ${currentUser?.token || ''}`}
    })
    const result = await res.json()
    allCourses = result.data || []
    courseCurrentPage = 1
    renderStudentCourseDisplay(allCourses)
  }catch(e){
    console.error('加载课程失败:', e)
  }
}

function renderStudentCourseDisplay(courses){
  const container = document.getElementById('student-course-display')
  container.innerHTML = ''
  
  if(!courses || courses.length === 0){
    container.innerHTML = '<p class="muted">暂无课程数据</p>'
    return
  }
  
  const totalPages = Math.ceil(courses.length / PAGE_SIZE) || 1
  const startIndex = (courseCurrentPage - 1) * PAGE_SIZE
  const endIndex = startIndex + PAGE_SIZE
  const currentPageData = courses.slice(startIndex, endIndex)
  
  const table = document.createElement('table')
  table.className = 'courses-table'
  
  const thead = document.createElement('thead')
  thead.innerHTML = `
    <tr>
      <th>课程编号</th>
      <th>课程名称</th>
      <th>教师</th>
      <th>学分</th>
      <th>总容量</th>
      <th>剩余容量</th>
      <th>已选容量</th>
    </tr>
  `
  table.appendChild(thead)
  
  const tbody = document.createElement('tbody')
  currentPageData.forEach(course=>{
    const tr = document.createElement('tr')
    tr.innerHTML = `
      <td>${course.courseNo}</td>
      <td>${course.name}</td>
      <td>${course.teacher}</td>
      <td>${course.credit}</td>
      <td>${course.capacity}</td>
      <td>${course.remainingCapacity}</td>
      <td>${course.selectedCount}</td>
    `
    tbody.appendChild(tr)
  })
  table.appendChild(tbody)
  container.appendChild(table)
  
  // 添加分页控件
  renderPagination(container, 'studentCourse', courseCurrentPage, totalPages, goToStudentCoursePage)
}

// ==================== 退选课（学生）====================
async function loadMyCourses(){
  try{
    const res = await fetch(`${API_SELECTIONS_LIST}/${currentUser.studentNo}`, {
      headers:{'Authorization': `Bearer ${currentUser?.token || ''}`}
    })
    const result = await res.json()
    myCoursesData = result.data || []
    myCoursesCurrentPage = 1
    renderMyCourses(myCoursesData)
  }catch(e){
    console.error('加载我的课程失败:', e)
  }
}

function renderMyCourses(courses){
  const container = document.getElementById('my-courses')
  container.innerHTML = ''
  
  if(!courses || courses.length === 0){
    container.innerHTML = '<p class="muted">您暂未选择任何课程</p>'
    return
  }
  
  const totalPages = Math.ceil(courses.length / PAGE_SIZE) || 1
  const startIndex = (myCoursesCurrentPage - 1) * PAGE_SIZE
  const endIndex = startIndex + PAGE_SIZE
  const currentPageData = courses.slice(startIndex, endIndex)
  
  const table = document.createElement('table')
  table.className = 'courses-table'
  
  const thead = document.createElement('thead')
  thead.innerHTML = `
    <tr>
      <th>课程编号</th>
      <th>课程名称</th>
      <th>教师</th>
      <th>学分</th>
      <th>总容量</th>
      <th>剩余容量</th>
      <th>已选容量</th>
      <th>操作</th>
    </tr>
  `
  table.appendChild(thead)
  
  const tbody = document.createElement('tbody')
  currentPageData.forEach(course=>{
    const tr = document.createElement('tr')
    tr.innerHTML = `
      <td>${course.courseNo}</td>
      <td>${course.name || course.courseName}</td>
      <td>${course.teacher}</td>
      <td>${course.credit}</td>
      <td>${course.capacity}</td>
      <td>${course.remainingCapacity}</td>
      <td>${course.selectedCount}</td>
      <td>
        <button class="small-btn" onclick="withdrawMyCourse('${course.courseNo}')">退课</button>
      </td>
    `
    tbody.appendChild(tr)
  })
  table.appendChild(tbody)
  container.appendChild(table)
  
  // 添加分页控件
  renderPagination(container, 'myCourses', myCoursesCurrentPage, totalPages, goToMyCoursesPage)
}

async function withdrawMyCourse(courseNo){
  if(!confirm('确定要退选这门课程吗？')){
    return
  }
  
  try{
    await withdrawCourse(currentUser.studentNo, courseNo)
    loadMyCourses()
  }catch(e){
    console.error('退课失败:', e)
    alert('退课失败')
  }
}

function goToMyCoursesPage(page){
  myCoursesCurrentPage = page
  renderMyCourses(myCoursesData)
}

// ==================== 个人信息（学生）====================
let personalInfoSelectedCourses = []
let personalInfoCurrentPage = 1

async function loadPersonalInfo(){
  const container = document.getElementById('personal-info')
  container.innerHTML = ''
  
  const infoDiv = document.createElement('div')
  infoDiv.className = 'card'
  infoDiv.innerHTML = `
    <h3>个人信息</h3>
    <p><strong>学号:</strong> ${currentUser.studentNo}</p>
    <p><strong>姓名:</strong> ${currentUser.name}</p>
    <p><strong>专业:</strong> ${currentUser.major}</p>
    <p><strong>年级:</strong> ${currentUser.grade}</p>
  `
  container.appendChild(infoDiv)
  
  // 加载已选课程
  try{
    const res = await fetch(`${API_SELECTIONS_LIST}/${currentUser.studentNo}`, {
      headers:{'Authorization': `Bearer ${currentUser?.token || ''}`}
    })
    const result = await res.json()
    personalInfoSelectedCourses = result.data || []
    personalInfoCurrentPage = 1
    renderPersonalInfoCourses(personalInfoSelectedCourses)
  }catch(e){
    console.error('加载选课记录失败:', e)
  }
}

function renderPersonalInfoCourses(courses){
  const coursesContainer = document.getElementById('selected-courses')
  coursesContainer.innerHTML = ''
  
  if(courses.length === 0){
    coursesContainer.innerHTML = '<p class="muted">您暂未选择任何课程</p>'
    return
  }
  
  const totalPages = Math.ceil(courses.length / PAGE_SIZE) || 1
  const startIndex = (personalInfoCurrentPage - 1) * PAGE_SIZE
  const endIndex = startIndex + PAGE_SIZE
  const currentPageData = courses.slice(startIndex, endIndex)
  
  const table = document.createElement('table')
  table.className = 'courses-table'
  
  table.innerHTML = `
    <thead>
      <tr>
        <th>课程编号</th>
        <th>课程名称</th>
        <th>教师</th>
        <th>学分</th>
        <th>总容量</th>
        <th>剩余容量</th>
        <th>已选容量</th>
      </tr>
    </thead>
    <tbody>
      ${currentPageData.map(s => `
        <tr>
          <td>${s.courseNo}</td>
          <td>${s.courseName || s.name}</td>
          <td>${s.teacher}</td>
          <td>${s.credit}</td>
          <td>${s.capacity}</td>
          <td>${s.remainingCapacity}</td>
          <td>${s.selectedCount}</td>
        </tr>
      `).join('')}
    </tbody>
  `
  
  coursesContainer.appendChild(table)
  
  // 添加分页控件
  renderPagination(coursesContainer, 'personalInfo', personalInfoCurrentPage, totalPages, goToPersonalInfoPage)
}

function goToPersonalInfoPage(page){
  personalInfoCurrentPage = page
  renderPersonalInfoCourses(personalInfoSelectedCourses)
}

// ==================== 通用分页函数 ====================
function renderPagination(container, type, currentPage, totalPages, goToPageFn) {
  const paginationDiv = document.createElement('div')
  paginationDiv.style.cssText = `
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 1rem;
    margin-top: 1.5rem;
    padding: 1rem;
  `
  
  // 上一页按钮
  const prevBtn = document.createElement('button')
  prevBtn.innerHTML = '&lt;'
  prevBtn.style.cssText = `
    padding: 0.4rem 0.8rem;
    background: ${currentPage > 1 ? '#3498db' : '#bdc3c7'};
    color: white;
    border: none;
    border-radius: 4px;
    cursor: ${currentPage > 1 ? 'pointer' : 'not-allowed'};
    font-weight: bold;
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
  `
  if (currentPage > 1) {
    prevBtn.onclick = () => goToPageFn(currentPage - 1)
    prevBtn.onmouseover = () => { if (currentPage > 1) prevBtn.style.background = '#2980b9' }
    prevBtn.onmouseout = () => { if (currentPage > 1) prevBtn.style.background = '#3498db' }
  } else {
    prevBtn.disabled = true
  }
  
  // 页码显示
  const pageInfo = document.createElement('span')
  pageInfo.textContent = `${currentPage}/${totalPages}`
  pageInfo.style.cssText = `
    font-size: 1rem;
    color: #2c3e50;
    font-weight: 600;
    min-width: 60px;
    text-align: center;
  `
  
  // 下一页按钮
  const nextBtn = document.createElement('button')
  nextBtn.innerHTML = '&gt;'
  nextBtn.style.cssText = `
    padding: 0.4rem 0.8rem;
    background: ${currentPage < totalPages ? '#3498db' : '#bdc3c7'};
    color: white;
    border: none;
    border-radius: 4px;
    cursor: ${currentPage < totalPages ? 'pointer' : 'not-allowed'};
    font-weight: bold;
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
  `
  if (currentPage < totalPages) {
    nextBtn.onclick = () => goToPageFn(currentPage + 1)
    nextBtn.onmouseover = () => { if (currentPage < totalPages) nextBtn.style.background = '#2980b9' }
    nextBtn.onmouseout = () => { if (currentPage < totalPages) nextBtn.style.background = '#3498db' }
  } else {
    nextBtn.disabled = true
  }
  
  // 输入框
  const input = document.createElement('input')
  input.type = 'number'
  input.min = 1
  input.max = totalPages
  input.placeholder = '页码'
  input.style.cssText = `
    width: 60px;
    padding: 0.4rem;
    border: 1px solid #ddd;
    border-radius: 4px;
    text-align: center;
  `
  
  // 跳转按钮
  const jumpBtn = document.createElement('button')
  jumpBtn.textContent = '跳转'
  jumpBtn.style.cssText = `
    padding: 0.4rem 1rem;
    background: #2ecc71;
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
  `
  jumpBtn.onmouseover = () => jumpBtn.style.background = '#27ae60'
  jumpBtn.onmouseout = () => jumpBtn.style.background = '#2ecc71'
  
  jumpBtn.onclick = () => {
    const pageNum = parseInt(input.value)
    if (!pageNum || pageNum < 1 || pageNum > totalPages) {
      alert(`请输入1到${totalPages}之间的页码`)
      return
    }
    goToPageFn(pageNum)
  }
  
  input.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
      jumpBtn.click()
    }
  })
  
  // 组装顺序：上一页 | 页码 | 下一页 | 输入框 | 跳转
  paginationDiv.appendChild(prevBtn)
  paginationDiv.appendChild(pageInfo)
  paginationDiv.appendChild(nextBtn)
  paginationDiv.appendChild(input)
  paginationDiv.appendChild(jumpBtn)
  
  container.appendChild(paginationDiv)
}

function goToCoursePage(page){
  courseCurrentPage = page
  renderCourseDisplay(allCourses)
}

function goToStudentCoursePage(page){
  courseCurrentPage = page
  renderStudentCourseDisplay(allCourses)
}

// 初始化
document.addEventListener('DOMContentLoaded', ()=>{
  console.log('前端应用已启动')
  console.log('后端服务地址:', {
    basic: BASIC_SERVICE_URL,
    selection: SELECTION_SERVICE_URL,
    statistics: STATISTICS_SERVICE_URL
  })
  
  // 初始化DOM元素
  initDOMElements()
  
  // 初始化事件监听器
  initAdminNavEvents()
  initStudentNavEvents()
  initSearchEvents()
  initAdminPanelEvents()
  
  // 添加退选课加载按钮事件
  const loadSelectionBtn = document.getElementById('load-selection-courses-btn')
  if(loadSelectionBtn) {
    loadSelectionBtn.addEventListener('click', loadSelectionCourses)
  }
  
  // 添加退出登录事件
  document.getElementById('logout-btn').addEventListener('click', ()=>{
    currentUser = null
    currentRole = null
    document.getElementById('user-info').classList.add('hidden')
    document.getElementById('admin-nav').classList.add('hidden')
    document.getElementById('student-nav').classList.add('hidden')
    document.getElementById('login-view').classList.remove('hidden')
    document.getElementById('main-app').classList.add('hidden')
    
    // 重置所有导航按钮
    updateAdminNav(navCourseDisplay)
    updateStudentNav(navStudentCourseDisplay)
  })
})

