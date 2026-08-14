<template>
  <div class="resume-page">
    <!-- 顶栏：和Home同一套视觉 -->
    <header class="nav">
      <div class="nav-left">
        <span class="logo">KB</span>
        <span class="site">简历助手</span>
        <span class="sub">导入分析 · 查缺补漏 · 一键生成，适用各类岗位</span>
      </div>
      <div class="nav-right">
        <a class="back" @click="router.push('/')">← 回知识库</a>
      </div>
    </header>

    <div class="body">
      <!-- 左栏：我的简历列表 -->
      <aside class="sidebar">
        <div class="side-actions">
          <el-button size="small" type="primary" @click="createNew">+ 新建</el-button>
          <el-upload :show-file-list="false" accept=".pdf,.txt,.md" :http-request="doImport">
            <el-button size="small" :loading="importing">导入</el-button>
          </el-upload>
        </div>
        <div class="side-tip">支持 pdf / txt / md，导入后AI自动结构化</div>
        <div class="list">
          <div v-for="r in list" :key="r.id" class="item" :class="{ active: active && active.id === r.id }"
               @click="select(r)">
            <div class="t">{{ r.title }}</div>
            <div class="m">{{ r.targetJob || '未设目标岗位' }} · {{ fmt(r.updateTime) }}</div>
          </div>
          <div v-if="!list.length" class="empty">还没有简历，新建或导入一份试试</div>
        </div>
      </aside>

      <!-- 右栏：编辑/分析/生成 -->
      <main class="main" v-if="active">
        <div class="main-head">
          <el-input v-model="meta.title" class="title-input" placeholder="简历标题" />
          <el-input v-model="meta.targetJob" class="job-input" placeholder="目标岗位，如：新媒体运营 / 高中教师 / Java后端" />
          <el-button type="primary" :loading="saving" @click="save">保存</el-button>
          <el-button @click="openExport">导出</el-button>
          <el-button type="danger" plain @click="del">删除</el-button>
        </div>

        <el-tabs v-model="tab" class="tabs">
          <!-- ===== 编辑 ===== -->
          <el-tab-pane label="编辑" name="edit">
            <div class="form">
              <section class="sec">
                <h3>基本信息</h3>
                <div class="grid">
                  <el-input v-model="form.basics.name" placeholder="姓名" />
                  <el-input v-model="form.basics.phone" placeholder="电话" />
                  <el-input v-model="form.basics.email" placeholder="邮箱" />
                  <el-input v-model="form.basics.city" placeholder="城市" />
                  <el-input v-model="form.basics.github" placeholder="主页/GitHub（可选）" />
                  <el-input v-model="form.basics.blog" placeholder="博客/作品集（可选）" />
                </div>
              </section>

              <section class="sec">
                <h3>工作经历 <el-button class="add-btn" size="small" @click="addWork">+ 添加</el-button></h3>
                <div v-for="(w, i) in form.work" :key="i" class="card">
                  <div class="grid">
                    <el-input v-model="w.company" placeholder="公司/单位" />
                    <el-input v-model="w.position" placeholder="职位" />
                    <el-input v-model="w.start" placeholder="开始 2023-06" />
                    <el-input v-model="w.end" placeholder="结束 至今" />
                  </div>
                  <el-input type="textarea" :rows="3" placeholder="业绩亮点，一行一条，尽量量化（如：负责xx，实现xx增长30%）"
                            :model-value="rows(w.highlights)" @update:model-value="v => setRows(w, 'highlights', v)" />
                  <el-button size="small" text type="danger" @click="form.work.splice(i, 1)">删除</el-button>
                </div>
              </section>

              <section class="sec">
                <h3>项目/实践经历 <el-button class="add-btn" size="small" @click="addProject">+ 添加</el-button></h3>
                <div v-for="(p, i) in form.projects" :key="i" class="card">
                  <div class="grid">
                    <el-input v-model="p.name" placeholder="项目名称" />
                    <el-input v-model="p.role" placeholder="角色/职责" />
                    <el-input v-model="p.start" placeholder="开始" />
                    <el-input v-model="p.end" placeholder="结束" />
                  </div>
                  <el-input v-model="techText[i]" placeholder="关键词/技术栈，用顿号分隔" @change="setTech(p, i)" />
                  <el-input type="textarea" :rows="3" placeholder="成果亮点，一行一条（STAR法则：背景-任务-行动-结果）"
                            :model-value="rows(p.highlights)" @update:model-value="v => setRows(p, 'highlights', v)" />
                  <el-button size="small" text type="danger" @click="form.projects.splice(i, 1)">删除</el-button>
                </div>
              </section>

              <section class="sec">
                <h3>教育经历 <el-button class="add-btn" size="small" @click="addEdu">+ 添加</el-button></h3>
                <div v-for="(e, i) in form.education" :key="i" class="card">
                  <div class="grid">
                    <el-input v-model="e.school" placeholder="学校" />
                    <el-input v-model="e.degree" placeholder="学历" />
                    <el-input v-model="e.major" placeholder="专业" />
                    <el-input v-model="e.start" placeholder="开始" />
                    <el-input v-model="e.end" placeholder="结束" />
                    <el-button size="small" text type="danger" @click="form.education.splice(i, 1)">删除</el-button>
                  </div>
                </div>
              </section>

              <section class="sec">
                <h3>技能/专长 <el-button class="add-btn" size="small" @click="addSkill">+ 添加</el-button></h3>
                <div v-for="(s, i) in form.skills" :key="i" class="card">
                  <el-input v-model="s.category" placeholder="分类，如：办公技能 / 编程语言 / 语言能力" />
                  <el-input type="textarea" :rows="2" placeholder="具体项，一行一条"
                            :model-value="rows(s.items)" @update:model-value="v => setRows(s, 'items', v)" />
                  <el-button size="small" text type="danger" @click="form.skills.splice(i, 1)">删除</el-button>
                </div>
              </section>

              <section class="sec">
                <h3>荣誉奖项</h3>
                <el-input type="textarea" :rows="3" placeholder="一行一条"
                          :model-value="rows(form.awards)" @update:model-value="v => form.awards = splitLines(v)" />
              </section>
            </div>
          </el-tab-pane>

          <!-- ===== 分析 ===== -->
          <el-tab-pane label="AI分析" name="analyze">
            <div class="analyze">
              <div class="analyze-head">
                <el-button type="primary" :loading="analyzing" @click="analyze">
                  {{ analyzing ? '分析中…' : '开始AI分析' }}</el-button>
                <span class="hint">从完整度、表述质量、岗位匹配度、排版规范四个维度点评，并给出查缺补漏清单</span>
              </div>
              <div v-if="analyzeText" class="stream-box">{{ analyzeText }}</div>
              <div v-if="analysis" class="report">
                <div class="score-row">
                  <div class="score" :class="scoreClass(analysis.score)">{{ analysis.score }}<small>分</small></div>
                  <div class="dims">
                    <div v-for="(v, k) in analysis.scores" :key="k" class="dim">
                      <span>{{ k }}</span>
                      <el-progress :percentage="v" :stroke-width="8" :show-text="false" />
                      <b>{{ v }}</b>
                    </div>
                  </div>
                </div>
                <div v-if="analysis.strengths && analysis.strengths.length" class="block">
                  <h4>亮点</h4>
                  <ul><li v-for="(s, i) in analysis.strengths" :key="i">{{ s }}</li></ul>
                </div>
                <div v-if="analysis.issues && analysis.issues.length" class="block">
                  <h4>问题与建议</h4>
                  <div v-for="(it, i) in analysis.issues" :key="i" class="issue">
                    <el-tag :type="sevType(it.severity)" size="small">{{ it.severity === 'high' ? '严重' : it.severity === 'mid' ? '中等' : '轻微' }}</el-tag>
                    <span class="sec-tag">{{ secName(it.section) }}</span>
                    <span>{{ it.advice }}</span>
                  </div>
                </div>
                <div v-if="analysis.missing && analysis.missing.length" class="block">
                  <h4>查缺补漏</h4>
                  <ul><li v-for="(m, i) in analysis.missing" :key="i">{{ m }}</li></ul>
                </div>
                <div v-if="analysis.suggestKeywords && analysis.suggestKeywords.length" class="block">
                  <h4>建议补充的关键词</h4>
                  <el-tag v-for="(k, i) in analysis.suggestKeywords" :key="i" class="kw" effect="plain">{{ k }}</el-tag>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <!-- ===== JD匹配 ===== -->
          <el-tab-pane label="JD匹配" name="jd">
            <div class="jd">
              <el-input v-model="jdText" type="textarea" :rows="6"
                        placeholder="在这里粘贴目标岗位的职位描述全文（岗位职责 + 任职要求）" />
              <div class="jd-actions">
                <el-button type="primary" :loading="jdMatching" :disabled="!jdText.trim()" @click="matchJd">
                  {{ jdMatching ? '匹配中…' : '开始JD匹配' }}</el-button>
                <span class="hint">AI逐项比对简历与JD，给出匹配分、已覆盖要求、缺失项与改进建议</span>
              </div>
              <div v-if="jdStreamText" class="stream-box">{{ jdStreamText }}</div>
              <div v-if="jdResult" class="report">
                <div class="jd-score-row">
                  <div class="score" :class="scoreClass(jdResult.score)">{{ jdResult.score }}<small>%</small></div>
                  <div class="jd-score-label">JD匹配度</div>
                </div>
                <div v-if="jdResult.matched && jdResult.matched.length" class="block">
                  <h4>已覆盖的JD要求</h4>
                  <el-tag v-for="(m, i) in jdResult.matched" :key="i" class="kw" type="success" effect="plain">{{ m }}</el-tag>
                </div>
                <div v-if="jdResult.missing && jdResult.missing.length" class="block">
                  <h4>缺失项</h4>
                  <ul><li v-for="(m, i) in jdResult.missing" :key="i">{{ m }}</li></ul>
                </div>
                <div v-if="jdResult.suggestions && jdResult.suggestions.length" class="block">
                  <h4>改进建议</h4>
                  <ul><li v-for="(s, i) in jdResult.suggestions" :key="i">{{ s }}</li></ul>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <!-- ===== 生成补全 ===== -->
          <el-tab-pane label="一键生成" name="generate">
            <div class="gen">
              <div class="gen-form">
                <el-input v-model="gen.targetJob" placeholder="目标岗位（必填，AI会按岗位调整侧重点）" />
                <div class="checks" v-if="hasSiteMaterial">
                  <el-checkbox v-model="gen.useDrill">带上我的刷题记录（技术类岗位建议勾选）</el-checkbox>
                  <el-checkbox v-model="gen.useInterview">带上我的模拟面试记录</el-checkbox>
                </div>
                <el-input v-model="gen.note" type="textarea" :rows="2"
                          placeholder="补充说明（可选），如：突出运营数据 / 强调教学成果 / 应聘国企偏稳重风格" />
                <el-button type="primary" :loading="generating" @click="generate">
                  {{ generating ? '生成中…' : (hasContent ? 'AI补全润色' : 'AI一键生成') }}</el-button>
                <span class="hint">已有内容会保留事实、只补齐润色；素材不足处会用【补充：xx】占位，不会编造</span>
              </div>
              <div v-if="genText" class="stream-box mono">{{ genText }}</div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </main>

      <!-- 右栏：实时预览，改表单即所见即所得（与导出PDF同模板） -->
      <aside class="preview" v-if="active">
        <div class="preview-head">
          <span>实时预览</span>
          <a @click="openExport">导出</a>
        </div>
        <iframe class="preview-frame" :srcdoc="resumeHtml"></iframe>
      </aside>

      <main class="main placeholder" v-if="!active">
        <div>选择左侧简历，或新建/导入一份开始</div>
      </main>
    </div>

    <!-- 导出弹窗：iframe嵌入独立简历页，预览与打印效果完全一致 -->
    <el-dialog v-model="exportOpen" title="简历预览" width="860px" top="5vh">
      <iframe class="export-frame" :srcdoc="resumeHtml"></iframe>
      <template #footer>
        <span class="export-tip">导出为A4单页简历；新窗口里目标打印机选“另存为PDF”即得成品</span>
        <el-button @click="copyExport">复制Markdown</el-button>
        <el-button type="primary" @click="printPdf">导出PDF</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http, { upload, fetchText, streamPost } from '../api/http.js'

const router = useRouter()

// ===== 数据 =====
const list = ref([])
const active = ref(null)          // 当前选中的简历（含大字段）
const tab = ref('edit')
const meta = reactive({ title: '', targetJob: '' })
const form = reactive(blank())
const techText = ref([])          // 项目关键词的顿号分隔编辑态
const saving = ref(false)
const importing = ref(false)

const analyzing = ref(false)
const analyzeText = ref('')
const analysis = ref(null)

const generating = ref(false)
const genText = ref('')
const gen = reactive({ targetJob: '', useDrill: false, useInterview: false, note: '' })
const materials = ref(null)

// JD匹配：粘贴JD即时评估，不落库
const jdText = ref('')
const jdMatching = ref(false)
const jdStreamText = ref('')
const jdResult = ref(null)
const hasSiteMaterial = computed(() =>
  materials.value && (materials.value.drillTech?.length || materials.value.interviews?.length))
const hasContent = computed(() =>
  form.work.length || form.projects.length || form.education.length || form.basics.name)

const exportOpen = ref(false)
const exportText = ref('')

let analyzeCtrl = null
let genCtrl = null
let jdCtrl = null

function blank() {
  return {
    basics: { name: '', phone: '', email: '', city: '', github: '', blog: '' },
    work: [], projects: [], education: [], skills: [], awards: []
  }
}

// 多行文本 <-> 字符串数组，编辑用textarea、存储用数组
const rows = a => (a || []).join('\n')
const splitLines = v => v.split('\n').map(s => s.trim()).filter(Boolean)
const setRows = (obj, key, v) => { obj[key] = splitLines(v) }
const setTech = (p, i) => { p.techStack = (techText.value[i] || '').split(/[、,，]/).map(s => s.trim()).filter(Boolean) }

const fmt = t => t ? String(t).replace('T', ' ').slice(0, 16) : ''
const sevType = s => s === 'high' ? 'danger' : s === 'mid' ? 'warning' : 'info'
const secName = s => ({ basics: '基本信息', work: '工作经历', projects: '项目经历', education: '教育经历', skills: '技能', awards: '奖项' }[s] || s || '整体')
const scoreClass = s => s >= 80 ? 'good' : s >= 60 ? 'ok' : 'bad'

// ===== 列表与加载 =====
async function loadList(keepId) {
  list.value = await http.get('/resume/list')
  if (keepId && list.value.some(r => r.id === keepId) && active.value?.id !== keepId) {
    const r = list.value.find(x => x.id === keepId)
    await select(r)
  }
}

async function select(r) {
  abortStreams()
  const d = await http.get('/resume/' + r.id)
  active.value = d
  meta.title = d.title || ''
  meta.targetJob = d.targetJob || ''
  applyContent(d.contentJson)
  analysis.value = d.analysisJson ? tryParse(d.analysisJson) : null
  analyzeText.value = analysis.value?.summary || ''
  // 换简历就清空上一份的JD匹配结果，避免串数据
  jdResult.value = null
  jdStreamText.value = ''
  tab.value = 'edit'
}

function applyContent(json) {
  const parsed = tryParse(json) || blank()
  const b = parsed.basics || {}
  form.basics = { name: b.name || '', phone: b.phone || '', email: b.email || '', city: b.city || '', github: b.github || '', blog: b.blog || '' }
  form.work = parsed.work || []
  form.projects = parsed.projects || []
  form.education = parsed.education || []
  form.skills = parsed.skills || []
  form.awards = parsed.awards || []
  techText.value = form.projects.map(p => (p.techStack || []).join('、'))
}

function tryParse(s) {
  try { return JSON.parse(s) } catch { return null }
}

// ===== CRUD =====
async function createNew() {
  const r = await http.post('/resume', {})
  ElMessage.success('已创建空简历，开始填写吧')
  await loadList()
  await select(r)
}

async function doImport({ file }) {
  importing.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    const vo = await upload('/resume/import', fd)
    await loadList()
    await select({ id: vo.id })
    ElMessage[vo.aiParsed ? 'success' : 'warning'](
      vo.aiParsed ? '导入成功，AI已提取结构化内容，请核对后保存' : 'AI暂不可用，已建空模板，请手动填写')
  } finally {
    importing.value = false
  }
}

async function save() {
  if (!active.value) return
  saving.value = true
  try {
    await http.put('/resume/' + active.value.id, {
      title: meta.title, targetJob: meta.targetJob, contentJson: JSON.stringify(form)
    })
    ElMessage.success('已保存')
    await loadList(active.value.id)
  } finally {
    saving.value = false
  }
}

async function del() {
  await ElMessageBox.confirm('确定删除这份简历？不可恢复', '删除确认', { type: 'warning' })
  await http.delete('/resume/' + active.value.id)
  active.value = null
  ElMessage.success('已删除')
  await loadList()
}

// ===== 分析 =====
function analyze() {
  if (!active.value) return
  const id = active.value.id
  analyzing.value = true
  analyzeText.value = ''
  analysis.value = null
  let finished = false
  analyzeCtrl = streamPost('/resume/' + id + '/analyze-stream', {}, {
    onDelta: d => { analyzeText.value += d },
    onDone: a => { finished = true; analysis.value = a; analyzing.value = false },
    onFallback: a => {
      finished = true
      analysis.value = a
      if (a.summary) analyzeText.value = a.summary
      analyzing.value = false
      ElMessage.warning('AI暂不可用，已用本地规则检查')
    },
    onError: () => { analyzing.value = false; ElMessage.error('分析失败，请稍后重试') },
    // 连接断了但没收到done/fallback：后端已把结果落库，拉回来兜底，避免卡在loading
    onClose: async () => {
      analyzing.value = false
      if (finished || active.value?.id !== id) return
      const d = await http.get('/resume/' + id)
      const a = tryParse(d.analysisJson)
      if (a) {
        analysis.value = a
        if (a.summary) analyzeText.value = a.summary
        ElMessage.info('连接中断，已取回落库的分析结果')
      }
    }
  })
}

// ===== JD匹配 =====
function matchJd() {
  if (!active.value) return
  if (!jdText.value.trim()) {
    ElMessage.warning('请先粘贴目标岗位的JD')
    return
  }
  jdMatching.value = true
  jdStreamText.value = ''
  jdResult.value = null
  jdCtrl = streamPost('/resume/' + active.value.id + '/jd-stream', { jd: jdText.value.trim() }, {
    onDelta: d => { jdStreamText.value += d },
    onDone: r => { jdMatching.value = false; jdResult.value = r },
    onFallback: r => {
      jdMatching.value = false
      if (r.summary) jdStreamText.value = r.summary
      ElMessage.warning('AI暂不可用，请稍后重试')
    },
    onError: () => { jdMatching.value = false; ElMessage.error('匹配失败，请稍后重试') }
  })
}

// ===== 生成 =====
async function ensureMaterials() {
  if (!materials.value) {
    try { materials.value = await http.get('/resume/materials') } catch { materials.value = { drillTech: [], interviews: [] } }
  }
}

function generate() {
  if (!gen.targetJob.trim()) {
    ElMessage.warning('请先填写目标岗位')
    return
  }
  const id = active.value ? active.value.id : null
  generating.value = true
  genText.value = ''
  let finished = false
  genCtrl = streamPost('/resume/generate-stream', {
    id,
    targetJob: gen.targetJob.trim(),
    contentJson: active.value ? JSON.stringify(form) : null,
    useDrill: gen.useDrill,
    useInterview: gen.useInterview,
    note: gen.note
  }, {
    onDelta: d => { genText.value += d },
    onDone: async r => {
      finished = true
      generating.value = false
      ElMessage.success('生成完成，已回填表单，请核对后保存')
      await loadList()
      await select({ id: r.id })
    },
    onFallback: r => { finished = true; generating.value = false; ElMessage.warning(r.message || 'AI暂不可用') },
    onError: () => { generating.value = false; ElMessage.error('生成失败，请稍后重试') },
    // 断线兜底：后端生成完会落库，重新拉详情取回结果
    onClose: async () => {
      generating.value = false
      if (finished || !id || active.value?.id !== id) return
      ElMessage.info('连接中断，正在取回落库的生成结果')
      await loadList()
      await select({ id })
    }
  })
}

// ===== 导出 =====
async function openExport() {
  if (!active.value) return
  // 先保存最新表单，预览直接读当前表单数据，所见即所得
  await save()
  exportOpen.value = true
}

/** 转义用户输入，拼HTML防注入 */
const esc = s => String(s ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

const resumeHtml = computed(() => buildResumeHtml())

/**
 * 拼一个独立的A4简历页：预览嵌iframe、导出在新窗口打印，
 * 新窗口里只有简历本身，不会把应用页面带进PDF
 */
function buildResumeHtml() {
  const b = form.basics
  const rg = (s, e) => [s, e].filter(Boolean).join(' ~ ')
  const contacts = [
    ['电话', b.phone], ['邮箱', b.email], ['城市', b.city], ['主页', b.github || b.blog]
  ].filter(c => c[1]).map(c => `<div><span>${c[0]}</span>${esc(c[1])}</div>`).join('')
  const ln = (left, mid, date) =>
    `<div class="ln"><b>${esc(left)}</b>${mid ? `<span>${esc(mid)}</span>` : ''}${date ? `<em>${esc(date)}</em>` : ''}</div>`
  const lis = arr => (arr || []).filter(h => h && h.trim())
    .map(h => `<li>${esc(h)}</li>`).join('')

  const secs = []
  if (form.education.length) secs.push(`<section class="sec"><h3>教育经历</h3>${
    form.education.map(e => `<div class="item">${ln(e.school, [e.degree, e.major].filter(Boolean).join(' · '), rg(e.start, e.end))}</div>`).join('')
  }</section>`)
  if (form.work.length) secs.push(`<section class="sec"><h3>工作经历</h3>${
    form.work.map(w => `<div class="item">${ln(w.company, w.position, rg(w.start, w.end))}${lis(w.highlights) ? `<ul>${lis(w.highlights)}</ul>` : ''}</div>`).join('')
  }</section>`)
  if (form.projects.length) secs.push(`<section class="sec"><h3>项目 / 实践经历</h3>${
    form.projects.map(p => `<div class="item">${ln(p.name, p.role, rg(p.start, p.end))}${
      p.techStack && p.techStack.length ? `<div class="tech"><b>关键词：</b>${esc(p.techStack.join('、'))}</div>` : ''}${
      lis(p.highlights) ? `<ul>${lis(p.highlights)}</ul>` : ''}</div>`).join('')
  }</section>`)
  if (form.skills.length) secs.push(`<section class="sec skills"><h3>技能 / 专长</h3>${
    form.skills.map(s => `<div class="row"><b>${esc(s.category || '其他')}：</b>${esc((s.items || []).filter(Boolean).join('、'))}</div>`).join('')
  }</section>`)
  if (form.awards.length) secs.push(`<section class="sec"><h3>荣誉奖项</h3><ul class="awards">${lis(form.awards)}</ul></section>`)

  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<title>${esc(b.name || '简历')}</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  @page { size: A4; margin: 12mm 14mm; }
  body { font-family: "Microsoft YaHei", "PingFang SC", "Hiragino Sans GB", sans-serif; color: #2b2b2b;
         background: #e9eaec; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
  .page { width: 210mm; min-height: 296mm; margin: 0 auto; background: #fff; padding: 14mm 16mm; }
  .hd { display: flex; justify-content: space-between; align-items: flex-end; gap: 20px;
        border-bottom: 3px solid #2b5fad; padding-bottom: 12px; }
  .name { font-size: 30px; font-weight: 700; color: #1f2d3d; letter-spacing: 4px; }
  .job { margin-top: 6px; font-size: 13px; color: #2b5fad; font-weight: 600; letter-spacing: 1px; }
  .hd-contact { text-align: right; font-size: 12px; color: #333; line-height: 1.9; }
  .hd-contact span { color: #2b5fad; margin-right: 6px; }
  .sec { margin-top: 15px; }
  .sec h3 { font-size: 15px; color: #2b5fad; letter-spacing: 3px; border-bottom: 1px solid #d9e0ec;
            padding-bottom: 4px; margin-bottom: 9px; }
  .sec h3::before { content: ""; display: inline-block; width: 4px; height: 14px; background: #2b5fad;
                    margin-right: 8px; vertical-align: -1px; }
  .item { margin-bottom: 9px; }
  .ln { display: flex; align-items: baseline; gap: 10px; }
  .ln b { font-size: 13.5px; color: #222; }
  .ln span { font-size: 12.5px; color: #4a4f57; }
  .ln em { margin-left: auto; font-style: normal; font-size: 12px; color: #8a8f99; white-space: nowrap; }
  .item ul, .awards { margin: 4px 0 0 18px; }
  .item li, .awards li { font-size: 12.5px; line-height: 1.75; color: #333; }
  .tech { font-size: 12px; color: #4a4f57; margin-top: 3px; }
  .tech b { color: #2b5fad; font-weight: 600; }
  .skills .row { font-size: 12.5px; line-height: 2; color: #333; }
  .skills .row b { color: #222; }
  @media print { body { background: #fff; } .page { width: auto; min-height: 0; padding: 0; } }
</style>
</head>
<body><div class="page">
  <header class="hd">
    <div>
      <div class="name">${esc(b.name || '未填写姓名')}</div>
      ${meta.targetJob ? `<div class="job">求职意向：${esc(meta.targetJob)}</div>` : ''}
    </div>
    <div class="hd-contact">${contacts}</div>
  </header>
  ${secs.join('\n')}
</div></body></html>`
}

/** 新窗口只写简历页再打印，目标选“另存为PDF”即得可投递的成品 */
function printPdf() {
  const win = window.open('', '_blank')
  if (!win) {
    ElMessage.warning('浏览器拦截了弹窗，请允许本站弹出窗口后重试')
    return
  }
  win.document.write(buildResumeHtml())
  win.document.close()
  setTimeout(() => { win.focus(); win.print() }, 400)
}

async function copyExport() {
  if (!exportText.value) {
    exportText.value = await fetchText('/resume/' + active.value.id + '/export.md')
  }
  await navigator.clipboard.writeText(exportText.value)
  ElMessage.success('已复制到剪贴板')
}

// ===== 添加条目 =====
const addWork = () => form.work.push({ company: '', position: '', start: '', end: '', highlights: [] })
const addProject = () => {
  form.projects.push({ name: '', role: '', start: '', end: '', techStack: [], highlights: [] })
  techText.value.push('')
}
const addEdu = () => form.education.push({ school: '', degree: '', major: '', start: '', end: '' })
const addSkill = () => form.skills.push({ category: '', items: [] })

function abortStreams() {
  analyzeCtrl && analyzeCtrl.abort()
  genCtrl && genCtrl.abort()
  jdCtrl && jdCtrl.abort()
  analyzing.value = false
  generating.value = false
  jdMatching.value = false
}

onMounted(async () => {
  await loadList()
  if (list.value.length) await select(list.value[0])
  ensureMaterials()
})
onBeforeUnmount(abortStreams)
</script>

<style scoped>
.resume-page { min-height: 100vh; background: var(--kb-bg); display: flex; flex-direction: column; }
.nav {
  display: flex; align-items: center; justify-content: space-between;
  height: 60px; padding: 0 20px; background: #fff; border-bottom: 1px solid var(--kb-line);
  box-shadow: var(--kb-shadow-sm); position: relative; z-index: 10;
}
.nav-left { display: flex; align-items: center; gap: 10px; }
.logo {
  width: 28px; height: 28px; border-radius: 7px; background: var(--kb-brand-grad);
  box-shadow: 0 2px 6px rgba(0, 185, 107, .3);
  color: #fff; font-weight: 700; font-size: 13px; display: flex; align-items: center; justify-content: center;
}
.site { font-size: 16px; font-weight: 600; letter-spacing: 1px; }
.sub { font-size: 12px; color: var(--kb-ink-3); }
.back { cursor: pointer; color: var(--el-color-primary-dark-2); font-size: 13px; transition: var(--kb-trans); }
.back:hover { opacity: .75; }

/* 三栏：列表 | 编辑 | 实时预览 */
.body { flex: 1; display: flex; gap: 16px; padding: 16px 20px; max-width: 1680px; width: 100%; margin: 0 auto; }

/* 左栏 */
.sidebar { width: 230px; flex-shrink: 0; display: flex; flex-direction: column; gap: 8px; }
.side-actions { display: flex; gap: 8px; }
.side-tip { font-size: 12px; color: var(--kb-ink-3); }
.list {
  background: #fff; border: 1px solid var(--kb-line); border-radius: var(--kb-radius);
  overflow-y: auto; flex: 1; padding: 6px; box-shadow: var(--kb-shadow-sm);
}
.item { padding: 10px 12px; border-radius: 8px; cursor: pointer; transition: var(--kb-trans); }
.item:hover { background: var(--kb-side-hover); }
.item.active { background: var(--kb-side-active); }
.item .t { font-size: 14px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.item .m { font-size: 12px; color: var(--kb-ink-3); margin-top: 3px; }
.empty { padding: 30px 12px; color: var(--kb-ink-3); font-size: 13px; text-align: center; }

/* 中栏：编辑区 */
.main {
  flex: 1; background: #fff; border: 1px solid var(--kb-line); border-radius: var(--kb-radius);
  padding: 18px 22px; min-width: 0; box-shadow: var(--kb-shadow-sm); max-width: 780px;
}
.main.placeholder { display: flex; align-items: center; justify-content: center; color: var(--kb-ink-3); max-width: none; }
.main-head { display: flex; gap: 10px; margin-bottom: 8px; }
.title-input { max-width: 260px; }
.job-input { max-width: 340px; }

.form { display: flex; flex-direction: column; gap: 22px; }
.sec h3 { font-size: 15px; font-weight: 600; margin: 0 0 12px; display: flex; align-items: center; gap: 10px; }
/* 章节标题左侧品牌竖条 */
.sec h3::before {
  content: ""; width: 4px; height: 15px; border-radius: 2px;
  background: var(--kb-brand-grad); flex-shrink: 0;
}
/* 添加按钮：品牌浅底实色，比纯文字按钮醒目 */
.add-btn {
  color: var(--el-color-primary-dark-2);
  background: var(--el-color-primary-light-9);
  border: 1px solid var(--el-color-primary-light-7);
  font-weight: 500;
}
.add-btn:hover, .add-btn:focus {
  color: var(--el-color-primary-dark-2);
  background: var(--el-color-primary-light-8);
  border-color: var(--el-color-primary-light-5);
}
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 8px; margin-bottom: 8px; }
/* 条目卡片：浅灰底与白底拉开对比 */
.card {
  border: 1px solid var(--kb-line); border-radius: 10px;
  padding: 12px; margin-bottom: 10px;
  display: flex; flex-direction: column; gap: 8px;
  background: #f4f6f8;
}

/* 右栏：实时预览，滚动时吸顶 */
.preview {
  width: 380px; flex-shrink: 0;
  display: flex; flex-direction: column; gap: 8px;
  position: sticky; top: 16px; align-self: flex-start;
  height: calc(100vh - 96px);
}
.preview-head {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 13px; font-weight: 600; color: var(--kb-ink-2);
}
.preview-head a {
  color: var(--el-color-primary-dark-2); cursor: pointer;
  font-weight: 500; transition: var(--kb-trans);
}
.preview-head a:hover { opacity: .75; }
.preview-frame {
  flex: 1; width: 100%; border: 1px solid var(--kb-line);
  border-radius: var(--kb-radius); background: #e9eaec; box-shadow: var(--kb-shadow-sm);
}

/* 分析 */
.analyze-head { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.hint { font-size: 12px; color: var(--kb-ink-3); }
.stream-box {
  white-space: pre-wrap; line-height: 1.8; font-size: 13px; color: #444;
  background: #f7f8fa; border-radius: 8px; padding: 14px; margin-bottom: 14px;
  max-height: 320px; overflow-y: auto;
}
.stream-box.mono { font-family: Consolas, monospace; }
.report { display: flex; flex-direction: column; gap: 14px; }
.score-row { display: flex; align-items: center; gap: 24px; }
.score { font-size: 42px; font-weight: 700; }
.score small { font-size: 14px; font-weight: 400; margin-left: 2px; }
.score.good { color: #34c759; }
.score.ok { color: #ff9500; }
.score.bad { color: #ff3b30; }
.dims { flex: 1; display: grid; grid-template-columns: 1fr 1fr; gap: 8px 24px; max-width: 520px; }
.dim { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.dim .el-progress { flex: 1; }
.block h4 { font-size: 14px; margin: 0 0 8px; }
.block ul { margin: 0; padding-left: 20px; font-size: 13px; color: #444; line-height: 1.9; }
.issue { display: flex; align-items: baseline; gap: 8px; font-size: 13px; color: #444; padding: 5px 0; }
.sec-tag { color: var(--el-color-primary-dark-2); font-size: 12px; flex-shrink: 0; }
.kw { margin: 0 8px 8px 0; }

/* 生成 */
.gen-form { display: flex; flex-direction: column; gap: 10px; max-width: 640px; margin-bottom: 14px; }
.checks { display: flex; gap: 16px; font-size: 13px; }

/* JD匹配 */
.jd { display: flex; flex-direction: column; gap: 12px; }
.jd-actions { display: flex; align-items: center; gap: 12px; }
.jd-score-row { display: flex; align-items: baseline; gap: 14px; }
.jd-score-label { font-size: 14px; color: var(--kb-ink-2); font-weight: 600; }

/* 导出预览：iframe嵌独立简历页，和打印成品完全一致 */
.export-frame { width: 100%; height: 70vh; border: 1px solid var(--kb-line); border-radius: 8px; background: #e9eaec; }
.export-tip { font-size: 12px; color: var(--kb-ink-3); margin-right: auto; }

/* 窄屏：实时预览让位，编辑区全宽 */
@media (max-width: 1100px) {
  .preview { display: none; }
  .main { max-width: none; }
}
@media (max-width: 768px) {
  .body { flex-direction: column; }
  .sidebar { width: auto; }
}
</style>
