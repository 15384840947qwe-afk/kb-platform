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
            <div class="t">
              {{ r.title }}
              <span v-if="r.submitStatus === 1" class="st st1">待审阅</span>
              <span v-else-if="r.submitStatus === 2" class="st st2">已驳回</span>
              <span v-else-if="r.submitStatus === 3" class="st st3">已推荐</span>
            </div>
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
          <el-button v-if="canSubmit" type="success" plain @click="openSubmit">提交给管理员</el-button>
          <el-button v-else-if="active.submitStatus === 1" plain @click="doWithdraw">撤回</el-button>
          <el-tag v-if="active.submitStatus === 1" type="warning" effect="plain">待审阅</el-tag>
          <el-tag v-else-if="active.submitStatus === 2" type="danger" effect="plain">已驳回</el-tag>
          <el-tag v-else-if="active.submitStatus === 3" type="success" effect="plain">已推荐岗位</el-tag>
        </div>

        <!-- 管理员退回理由提示 -->
        <el-alert v-if="active.submitStatus === 2 && active.remark" class="return-tip"
                  :title="'管理员驳回：' + active.remark" type="error" :closable="false" show-icon />

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

          <!-- ===== 推荐岗位：管理员为这份简历推荐的岗位，卡片式展示 ===== -->
          <el-tab-pane name="recommend">
            <template #label>推荐岗位<span v-if="recJobs.length" class="rec-badge">{{ recJobs.length }}</span></template>
            <div class="rec-wrap">
              <div v-if="recJobs.length" class="rec-tip">管理员为你的简历推荐了 {{ recJobs.length }} 个岗位，点卡片看介绍和针对性AI面试题</div>
              <div v-else class="rec-empty">还没收到推荐：提交简历后，管理员会按你的技能帮你匹配合适的岗位</div>
              <div class="rec-grid">
                <div v-for="j in recJobs" :key="j.id" class="rec-card" @click="openRecJob(j)">
                  <div class="rc-top">
                    <b class="rc-title">{{ j.title }}</b>
                    <span class="rc-sal">{{ j.salary || '薪资面议' }}</span>
                  </div>
                  <div class="rc-company">{{ j.company || '公司未公开' }}<template v-if="j.city"> · {{ j.city }}</template></div>
                  <div class="rc-chips">
                    <span v-if="j.experience" class="rc-chip">{{ j.experience }}</span>
                    <span v-if="j.education" class="rc-chip">{{ j.education }}</span>
                  </div>
                  <div class="rc-jd">{{ (j.jdText || '').trim() || '（未录入JD职责描述）' }}</div>
                  <div class="rc-more">查看详情与AI面试题 ›</div>
                </div>
              </div>
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
    <!-- 提交给管理员：可选个意向岗位，提交后管理员可在后台审阅/推荐岗位 -->
    <el-dialog v-model="submitOpen" title="提交给管理员" width="460px">
      <p class="submit-tip">提交后管理员可查看这份简历，帮你匹配合适的岗位，提交前请先保存最新内容。</p>
      <el-select v-model="submitJobId" placeholder="意向岗位（可选）" clearable filterable style="width: 100%">
        <el-option v-for="j in jobOptions" :key="j.id"
                   :label="`${j.title}${j.company ? ' · ' + j.company : ''}${j.city ? ' · ' + j.city : ''}`"
                   :value="j.id" />
      </el-select>
      <template #footer>
        <el-button @click="submitOpen = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doSubmit">确认提交</el-button>
      </template>
    </el-dialog>
    <!-- 推荐岗位详情：完整介绍 + 针对这个岗位的AI面试简答题 -->
    <el-dialog v-model="recJobOpen" :title="recJob?.title || '岗位详情'" width="640px" top="6vh">
      <template v-if="recJob">
        <div class="rjd-hero">
          <div>
            <div class="rjd-title">{{ recJob.title }}</div>
            <div class="rjd-meta">
              {{ recJob.company || '公司未公开' }}<template v-if="recJob.city"> · {{ recJob.city }}</template>
              <template v-if="recJob.experience"> · {{ recJob.experience }}</template>
              <template v-if="recJob.education"> · {{ recJob.education }}</template>
            </div>
          </div>
          <div class="rjd-sal">{{ recJob.salary || '薪资面议' }}</div>
        </div>
        <div class="rjd-sec">岗位介绍</div>
        <div class="rjd-jd">{{ (recJob.jdText || '').trim() || '（未录入JD职责描述）' }}</div>
        <div class="rjd-sec">针对性AI面试题
          <el-button size="small" type="primary" :loading="recQuestionsLoading"
                     @click="loadRecQuestions">{{ recQuestions.length ? '重新生成' : '生成面试题' }}</el-button>
        </div>
        <div v-if="recQuestionsLoading" class="rjd-q-tip">AI正在根据岗位要求出题…</div>
        <div v-else-if="recQuestions.length" class="rjd-qs">
          <div v-for="(q, i) in recQuestions" :key="i" class="rjd-q">{{ i + 1 }}. {{ q }}</div>
          <div class="rjd-q-tip">建议先在草稿里写出答案，再去模拟面试里练一练</div>
        </div>
        <div v-else class="rjd-q-tip">点上面按钮，AI会按这个岗位的具体要求出简答题，帮你提前准备</div>
      </template>
      <template #footer>
        <a v-if="recJob?.jobUrl" :href="recJob.jobUrl" target="_blank" class="rjd-link">查看原帖 ↗</a>
        <el-button @click="recJobOpen = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http, { upload, fetchText, streamPost } from '../api/http.js'
import { buildResumeHtml } from '../utils/resumeHtml.js'

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

// 提交给管理员：意向岗位选项拉已上架岗位
const submitOpen = ref(false)
const submitting = ref(false)
const submitJobId = ref(null)
const jobOptions = ref([])
const canSubmit = computed(() => !active.value
  || (active.value.submitStatus !== 1 && active.value.submitStatus !== 3))

// 推荐岗位：管理员为这份简历推荐的岗位列表（可多个）
const recJobs = ref([])
const recJobOpen = ref(false)
const recJob = ref(null)
const recQuestions = ref([])
const recQuestionsLoading = ref(false)

async function loadRecJobs(resumeId) {
  try { recJobs.value = await http.get(`/resume/${resumeId}/recommended`) }
  catch { recJobs.value = [] }
}

function openRecJob(j) {
  recJob.value = j
  recQuestions.value = []
  recJobOpen.value = true
}

/** 针对推荐岗位生成AI面试简答题（后端按岗位结构化需求出题） */
async function loadRecQuestions() {
  recQuestionsLoading.value = true
  try {
    recQuestions.value = await http.post(`/job/${recJob.value.id}/recommend`)
  } catch {
    ElMessage.error('AI出题失败，稍后再试')
  } finally {
    recQuestionsLoading.value = false
  }
}

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
  // 拉这份简历收到的推荐岗位
  loadRecJobs(d.id)
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

// ===== 提交给管理员 =====
async function openSubmit() {
  if (!active.value) return
  // 先把表单最新内容落库，管理员看到的就是刚编辑的版
  await save()
  submitJobId.value = null
  try {
    jobOptions.value = await http.get('/job/open')
  } catch { jobOptions.value = [] }
  submitOpen.value = true
}

async function doSubmit() {
  submitting.value = true
  try {
    await http.post(`/resume/${active.value.id}/submit`, submitJobId.value ? { jobId: submitJobId.value } : {})
    submitOpen.value = false
    ElMessage.success('已提交，等待管理员审阅')
    await loadList(active.value.id)
  } finally {
    submitting.value = false
  }
}

async function doWithdraw() {
  await http.post(`/resume/${active.value.id}/withdraw`)
  ElMessage.success('已撤回')
  await loadList(active.value.id)
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

const resumeHtml = computed(() => buildResumeHtml(form, meta.targetJob))

/** 新窗口只写简历页再打印，目标选“另存为PDF”即得可投递的成品 */
function printPdf() {
  const win = window.open('', '_blank')
  if (!win) {
    ElMessage.warning('浏览器拦截了弹窗，请允许本站弹出窗口后重试')
    return
  }
  win.document.write(buildResumeHtml(form, meta.targetJob))
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
.main-head { display: flex; gap: 10px; margin-bottom: 8px; flex-wrap: wrap; align-items: center; }
.title-input { max-width: 260px; }
.job-input { max-width: 340px; }
.return-tip { margin-bottom: 10px; }
.submit-tip { margin: 0 0 12px; font-size: 13px; color: var(--kb-ink-3); }
/* 侧栏简历状态角标 */
.st { font-size: 11px; padding: 1px 6px; border-radius: 8px; margin-left: 6px; white-space: nowrap; }
.st1 { background: #fff7e6; color: #d48806; }
.st2 { background: #fff1f0; color: #cf1322; }
.st3 { background: #f6ffed; color: #389e0d; }

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

/* ===== 推荐岗位：大厂招聘风格卡片 ===== */
.rec-badge {
  display: inline-block; min-width: 16px; margin-left: 5px; padding: 0 4px;
  font-size: 11px; line-height: 16px; text-align: center; border-radius: 8px;
  background: #389e0d; color: #fff;
}
.rec-wrap { padding: 6px 2px; }
.rec-tip { font-size: 13px; color: var(--kb-ink-2); margin-bottom: 12px; }
.rec-empty { font-size: 13px; color: var(--kb-ink-3); padding: 26px 0; text-align: center; }
.rec-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 12px; }
.rec-card {
  border: 1px solid var(--kb-line); border-radius: var(--kb-radius, 10px);
  background: #fff; padding: 14px 16px; cursor: pointer;
  transition: all .15s;
}
.rec-card:hover {
  border-color: #3370ff; transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(51, 112, 255, .10);
}
.rc-top { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; }
.rc-title { font-size: 15px; color: var(--kb-ink, #1f2329); }
.rc-sal { font-size: 14px; color: #389e0d; font-weight: 700; white-space: nowrap; }
.rc-company { margin-top: 5px; font-size: 13px; color: var(--kb-ink-2); }
.rc-chips { margin-top: 8px; display: flex; flex-wrap: wrap; gap: 5px; }
.rc-chip {
  font-size: 12px; color: #555; background: #f4f5f7;
  border-radius: 4px; padding: 1px 8px;
}
.rc-jd {
  margin-top: 9px; font-size: 12px; color: var(--kb-ink-3); line-height: 1.7;
  display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;
}
.rc-more { margin-top: 8px; font-size: 12px; color: #3370ff; }

/* 推荐岗位详情弹窗 */
.rjd-hero {
  display: flex; justify-content: space-between; align-items: flex-start; gap: 12px;
  padding: 14px 16px; border-radius: 10px;
  background: linear-gradient(135deg, #f0f6ff, #e8f5ee);
}
.rjd-title { font-size: 17px; font-weight: 700; }
.rjd-meta { margin-top: 5px; font-size: 13px; color: var(--kb-ink-2); }
.rjd-sal { font-size: 18px; color: #389e0d; font-weight: 700; white-space: nowrap; }
.rjd-sec {
  margin: 16px 0 8px; font-size: 14px; font-weight: 600;
  display: flex; align-items: center; gap: 10px;
}
.rjd-sec::before { content: ''; width: 3px; height: 14px; border-radius: 2px; background: #3370ff; }
.rjd-jd { font-size: 13px; line-height: 1.8; color: var(--kb-ink-2); white-space: pre-wrap;
  max-height: 240px; overflow: auto; }
.rjd-qs { display: flex; flex-direction: column; gap: 8px; }
.rjd-q {
  font-size: 13px; line-height: 1.7; color: var(--kb-ink);
  background: #f7f8fa; border-radius: 8px; padding: 8px 12px;
}
.rjd-q-tip { font-size: 12px; color: var(--kb-ink-3); margin-top: 8px; }
.rjd-link { margin-right: auto; font-size: 13px; color: #3370ff; text-decoration: none; }

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
