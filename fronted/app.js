const BACKEND_BASIC = 'http://localhost:8080'
const BACKEND_SELECTION = 'http://localhost:8081'
const BACKEND_STATISTICS = 'http://localhost:8082'

const API_SELECTION = `${BACKEND_SELECTION}/api/selection`
const API_STATS = `${BACKEND_STATISTICS}/api/realtime`


// const BACKEND = 'http://localhost:8080'
// const API_SELECTION = `${BACKEND}/api/selection`
// const API_STATS = `${BACKEND}/api/realtime`

const $ = id=>document.getElementById(id)

function show(view){
  document.querySelectorAll('.view').forEach(v=>v.classList.add('hidden'))
  $(view+'-view').classList.remove('hidden')
  document.querySelectorAll('nav button').forEach(b=>b.classList.remove('active'))
  document.getElementById('nav-'+view).classList.add('active')
}

async function fetchCourses(){
  try{
    const res = await fetch(API_STATS+'/rank/list?page=1&size=100')
    const list = await res.json()
    renderCourses(list)
  }catch(e){
    alert('无法获取课程列表：'+e)
  }
}

async function checkSelected(studentNo, courseNo){
  if(!studentNo) return false
  try{
    const url = new URL(API_STATS+'/check/selected')
    url.searchParams.set('studentNo', studentNo)
    url.searchParams.set('courseNo', courseNo)
    const r = await fetch(url)
    return await r.json()
  }catch(e){return false}
}

function renderCourses(list){
  const container = $('courses')
  container.innerHTML = ''
  const studentNo = $('studentNo').value.trim()
  list.forEach(async item=>{
    const card = document.createElement('div'); card.className='card'
    const title = document.createElement('h3'); title.textContent = item.courseNo + (item.courseName? ' - '+item.courseName:'')
    const info = document.createElement('p'); info.innerHTML = `<strong>剩余：</strong>${item.remainingCount ?? item.remaining ?? 'N/A'} &nbsp; <strong>总量：</strong>${item.totalCount ?? item.totalCount ?? 'N/A'}`
    const btn = document.createElement('button'); btn.className='action'; btn.textContent='加载中'
    card.appendChild(title); card.appendChild(info); card.appendChild(btn)
    container.appendChild(card)

    const selected = await checkSelected(studentNo, item.courseNo)
    btn.textContent = selected ? '退课' : '选课'
    btn.onclick = ()=>{ if(selected) dropCourse(studentNo,item.courseNo) ; else selectCourse(studentNo,item.courseNo) }
  })
}

async function selectCourse(studentNo, courseNo){
  if(!studentNo){ alert('请先输入学号'); return }
  try{
    const res = await fetch(API_SELECTION+'/select', {method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({studentNo,courseNo})})
    const j = await res.json()
    alert(j.message || (j.code||'') )
    await fetchCourses()
  }catch(e){ alert('选课失败：'+e) }
}

async function dropCourse(studentNo, courseNo){
  if(!studentNo){ alert('请先输入学号'); return }
  try{
    const res = await fetch(API_SELECTION+'/drop', {method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({studentNo,courseNo})})
    const j = await res.json()
    alert(j.message || (j.code||''))
    await fetchCourses()
  }catch(e){ alert('退课失败：'+e) }
}

async function fetchTop10(){
  try{
    const res = await fetch(API_STATS+'/popularity/top10')
    const list = await res.json()
    const container = $('top10'); container.innerHTML = ''
    list.forEach(item=>{
      const c = document.createElement('div'); c.className='card'
      c.innerHTML = `<h3>${item.courseNo}</h3><p>今日选课次数：${item.selectionCount}</p>`
      container.appendChild(c)
    })
  }catch(e){$('top10').innerHTML='<div class="card">无法获取排行</div>'}
}

async function loadTotal(){
  try{
    const res = await fetch(API_STATS+'/stats/total')
    const n = await res.json()
    $('total-count').textContent = '累计净选课次数：' + n
  }catch(e){$('total-count').textContent = '载入失败'}
}

document.addEventListener('DOMContentLoaded', ()=>{
  document.getElementById('nav-courses').onclick = ()=>show('courses')
  document.getElementById('nav-stats').onclick = ()=>show('stats')
  document.getElementById('refresh-courses').onclick = ()=>fetchCourses()
  document.getElementById('refresh-top10').onclick = ()=>fetchTop10()
  document.getElementById('load-total').onclick = ()=>loadTotal()
  fetchCourses(); fetchTop10()
})
