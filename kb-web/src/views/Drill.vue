<template>
  <div class="drill-page">
    <!-- ===== setup：选科目/模式/题量 ===== -->
    <div v-if="phase === 'setup'" class="drill-center">
      <div class="setup-card">
        <h2>刷题</h2>
        <p class="setup-sub">已练 {{ stats.total }} 题 · 掌握率 {{ stats.rate }}%</p>
        <el-form label-position="top">
          <el-form-item label="科目">
            <el-select v-model="category" placeholder="全部科目" clearable style="width: 100%">
              <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item>
          <el-form-item label="模式">
            <el-radio-group v-model="mode">
              <el-radio value="random">随机练习</el-radio>
              <el-radio value="smart">AI 针对练</el-radio>
            </el-radio-group>
            <p v-if="mode === 'smart'" class="mode-tip">根据各科目正确率优先抽薄弱科目的题，帮你定向补短板</p>
          </el-form-item>
          <el-form-item label="题量">
            <el-radio-group v-model="count">
              <el-radio :value="5">5</el-radio>
              <el-radio :value="10">10</el-radio>
              <el-radio :value="20">20</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
        <el-button type="primary" size="large" class="start-btn" @click="start">开始</el-button>
        <el-button size="large" class="start-btn" :disabled="!wrongItems.length" @click="phase = 'book'">
          错题本（{{ wrongItems.length }}）
        </el-button>
        <div class="setup-links">
          <el-button v-if="isAdmin" link @click="$router.push('/manage')">题库管理</el-button>
          <el-button link @click="$router.push('/stats')">成长看板</el-button>
          <el-button link @click="$router.push('/')">返回</el-button>
        </div>
      </div>
    </div>

    <!-- ===== session：一题一屏 ===== -->
    <div v-else-if="phase === 'session'" class="drill-session">
      <header class="session-top">
        <a class="quit" @click="quit">×</a>
        <div class="progress"><div class="progress-fill" :style="{ width: progressPct }"></div></div>
        <span class="combo" v-if="combo > 1" :key="combo">连对{{ combo }}</span>
        <span class="combo" v-else>{{ idx + 1 }}/{{ questions.length }}</span>
      </header>

      <main class="session-body">
        <div class="q-card">
        <div class="q-type">{{ typeLabel[current.type] }}</div>
        <h2 class="q-stem">{{ current.stem }}</h2>

        <!-- 单选 -->
        <div v-if="current.type === 'SINGLE'" class="opts">
          <button v-for="(op, i) in parsedOptions" :key="i"
            :class="['opt', {
              picked: answer === letter(i),
              right: feedback && feedback.correct === true && answer === letter(i),
              wrong: feedback && feedback.correct === false && answer === letter(i)
            }]"
            @click="!feedback && (answer = letter(i))">
            <span class="opt-letter">{{ letter(i) }}</span>{{ op }}
          </button>
        </div>
        <!-- 多选 -->
        <div v-else-if="current.type === 'MULTI'" class="opts">
          <button v-for="(op, i) in parsedOptions" :key="i"
            :class="['opt', {
              picked: multiSel.includes(letter(i)),
              right: feedback && feedback.correct === true && multiSel.includes(letter(i)),
              wrong: feedback && feedback.correct === false && multiSel.includes(letter(i))
            }]"
            @click="!feedback && toggleMulti(letter(i))">
            <span class="opt-letter">{{ letter(i) }}</span>{{ op }}
          </button>
        </div>
        <!-- 填空 -->
        <el-input v-else-if="current.type === 'FILL'" v-model="answer" size="large" placeholder="填写答案" />
        <!-- 简答 -->
        <el-input v-else v-model="answer" type="textarea" :rows="6" placeholder="写下你的答题思路，交给AI批改…" />
        </div>
      </main>

      <!-- 底部：检查按钮 或 反馈条（多邻国节拍） -->
      <footer v-if="!feedback" class="session-foot">
        <el-button type="primary" size="large" :disabled="!answerReady" :loading="checking" @click="check">
          {{ current.type === 'SHORT' ? '提交批改' : '检查' }}
        </el-button>
      </footer>
      <footer v-else :class="['feedback', feedbackKind]">
        <div class="fb-title">{{ fbTitle }}</div>
        <div class="fb-line" v-if="current.type === 'SHORT' && feedback.correctAnswer">参考答案：{{ feedback.correctAnswer }}</div>
        <div class="fb-line" v-else>正确答案：{{ feedback.correctAnswer }}</div>
        <!-- 简答流式批改：AI点评逐字上屏，尾部光标提示还在写 -->
        <div class="fb-line" v-if="feedback.aiComment || feedback.streaming">
          AI点评：<span :class="{ typing: feedback.streaming }">{{ feedback.aiComment }}</span>
        </div>
        <div class="fb-line" v-if="feedback.explanation">解析：{{ feedback.explanation }}</div>
        <div class="fb-actions">
          <!-- 流式还没出结果：先不给操作 -->
          <template v-if="feedback.streaming"></template>
          <!-- AI不可用时降级自评 -->
          <template v-else-if="feedback.correct === null">
            <el-button size="large" type="success" @click="finishShort(1)">会了</el-button>
            <el-button size="large" type="danger" @click="finishShort(0)">不会</el-button>
          </template>
          <el-button v-else size="large" type="primary" @click="next">继续</el-button>
        </div>
      </footer>
    </div>

    <!-- ===== book：错题本 ===== -->
    <div v-else-if="phase === 'book'" class="drill-center">
      <div class="setup-card wide">
        <h2>错题本</h2>
        <p class="setup-sub">最近一次仍答错的题；答对一次就毕业</p>
        <div class="wrong-list">
          <div v-for="w in wrongItems" :key="w.id" class="wrong-item">
            <div class="wrong-stem">
              {{ w.stem }}
              <span class="wrong-count">错{{ w.wrongCount }}次</span>
            </div>
            <div class="wrong-ops">
              <a @click="w.show = !w.show">{{ w.show ? '收起' : '看解析' }}</a>
              <a v-if="w.relatedDocId" class="wrong-doc" @click="goDoc(w.relatedDocId)">去看教材</a>
            </div>
            <div v-if="w.show" class="wrong-detail">
              <div>正确答案：{{ w.answer }}</div>
              <div v-if="w.explanation">解析：{{ w.explanation }}</div>
            </div>
          </div>
        </div>
        <el-button type="primary" size="large" class="start-btn" @click="startWrong">刷这些错题</el-button>
        <el-button size="large" @click="phase = 'setup'">返回</el-button>
      </div>
    </div>

    <!-- ===== result：战绩 ===== -->
    <div v-else class="drill-center">
      <div class="setup-card">
        <h2>本轮战绩</h2>
        <p class="result-big">{{ rightCount }} / {{ questions.length }}</p>
        <p class="setup-sub">正确率 {{ pct }}% · 最高连对 {{ maxCombo }}</p>
        <p v-if="pct >= 80" class="cheer">状态神了，这科目稳了！</p>
        <p v-else-if="pct >= 60" class="cheer soft">不错，错题再刷一轮就稳了</p>
        <div v-if="wrongList.length" class="wrong-list">
          <h4>错题回顾</h4>
          <div v-for="w in wrongList" :key="w.id" class="wrong-item">
            <div class="wrong-stem">{{ w.stem }}</div>
            <a v-if="w.relatedDocId" class="wrong-doc" @click="goDoc(w.relatedDocId)">去看教材</a>
          </div>
        </div>
        <el-button type="primary" size="large" class="start-btn" @click="phase = 'setup'">再来一轮</el-button>
        <el-button size="large" @click="$router.push('/')">返回</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http, { streamPost } from '../api/http.js'

const router = useRouter()
const user = JSON.parse(localStorage.getItem('kb-user') || '{"role":""}')
const isAdmin = computed(() => user.role === 'ADMIN')
const typeLabel = { SINGLE: '单选题', MULTI: '多选题', FILL: '填空题', SHORT: '简答题' }

// setup状态
const categories = ref([])
const stats = ref({ total: 0, known: 0, rate: 0 })
const category = ref('')
const count = ref(10)
// 抽题模式：random随机 / smart薄弱点针对练（后端按科目正确率加权抽样）
const mode = ref('smart')
// 错题本：最近一次仍答错的题
const wrongItems = ref([])

// session状态
const phase = ref('setup')
const questions = ref([])
const idx = ref(0)
const answer = ref('')
const multiSel = ref([])
const checking = ref(false)
const feedback = ref(null)       // check接口返回
const pendingResult = ref(null)  // 自动判分的结果，next时落库
const combo = ref(0)
const maxCombo = ref(0)
const rightCount = ref(0)
const wrongList = ref([])

onMounted(async () => {
  categories.value = await http.get('/drill/categories')
  stats.value = await http.get('/drill/stats')
  wrongItems.value = (await http.get('/drill/wrong')).map(x => ({ ...x, show: false }))
})

const current = computed(() => questions.value[idx.value] || {})
const parsedOptions = computed(() => {
  try { return JSON.parse(current.value.options || '[]') } catch { return [] }
})
const letter = i => String.fromCharCode(65 + i)
const answerReady = computed(() => {
  if (current.value.type === 'MULTI') return multiSel.value.length > 0
  return (answer.value || '').trim().length > 0
})
const progressPct = computed(() => (idx.value / questions.value.length) * 100 + '%')
const pct = computed(() => questions.value.length ? Math.round(rightCount.value * 100 / questions.value.length) : 0)

// 反馈条样式与标题
const feedbackKind = computed(() => {
  if (feedback.value?.streaming || feedback.value?.correct === null) return 'neutral'
  return feedback.value?.correct ? 'good' : 'bad'
})
const fbTitle = computed(() => {
  if (feedback.value?.streaming) return 'AI 正在批改…'
  if (feedback.value?.correct === null) return '对照参考答案，自评一下'
  return feedback.value?.correct ? '回答正确！' : '答错了'
})

// 简答流式批改句柄：退出/卸载时打断
let checkCtrl = null
onUnmounted(() => checkCtrl?.abort())

async function start() {
  const list = await http.get(`/drill/pick?mode=${mode.value}&n=${count.value}` +
    (category.value ? `&category=${encodeURIComponent(category.value)}` : ''))
  if (!list.length) {
    ElMessage.warning('该科目暂无题目')
    return
  }
  beginSession(list)
}

/** 用错题本里的题直接开一轮 */
function startWrong() {
  beginSession(wrongItems.value.map(w => ({ id: w.id, type: w.type, stem: w.stem, options: w.options })))
}

function beginSession(list) {
  questions.value = list
  idx.value = 0
  combo.value = 0
  maxCombo.value = 0
  rightCount.value = 0
  wrongList.value = []
  resetAnswer()
  phase.value = 'session'
}

function resetAnswer() {
  answer.value = ''
  multiSel.value = []
  feedback.value = null
  pendingResult.value = null
}

function toggleMulti(l) {
  const i = multiSel.value.indexOf(l)
  if (i >= 0) multiSel.value.splice(i, 1)
  else multiSel.value.push(l)
}

function currentAnswerStr() {
  if (current.value.type === 'MULTI') return [...multiSel.value].sort().join('')
  return answer.value
}

async function check() {
  // 简答走流式：AI点评逐字上屏；客观题仍用同步判分
  if (current.value.type === 'SHORT') {
    checkShortStream()
    return
  }
  checking.value = true
  try {
    const vo = await http.post('/drill/check', { questionId: current.value.id, answer: currentAnswerStr() })
    feedback.value = vo
    if (vo.correct !== null) {
      pendingResult.value = vo.correct ? 1 : 0
    }
  } finally {
    checking.value = false
  }
}

/** 简答流式批改：反馈条先弹出，点评打字机式上屏，done后定对错 */
function checkShortStream() {
  checking.value = true
  feedback.value = { correct: null, correctAnswer: '', aiComment: '', streaming: true }
  let gotDelta = false
  checkCtrl = streamPost('/drill/check-stream', {
    questionId: current.value.id, answer: currentAnswerStr()
  }, {
    onDelta: d => { gotDelta = true; feedback.value.aiComment += d },
    onFallback: vo => {
      // AI不可用：进自评（correct=null）
      checking.value = false
      feedback.value = { ...vo, aiComment: '', streaming: false }
    },
    onDone: vo => {
      checking.value = false
      if (!gotDelta && vo.aiComment) feedback.value.aiComment = vo.aiComment
      feedback.value = { ...vo, streaming: false }
      if (vo.correct !== null) {
        pendingResult.value = vo.correct ? 1 : 0
      }
    },
    onError: () => {
      checking.value = false
      feedback.value = { ...feedback.value, streaming: false, correct: null,
        aiComment: feedback.value.aiComment || 'AI批改服务异常，请自评一下' }
    }
  })
}

/** 简答AI不可用时自评落库并前进 */
async function finishShort(r) {
  await http.post('/drill/record', { questionId: current.value.id, result: r })
  tally(r)
  advance()
}

/** 自动判分的题：继续时落库 */
async function next() {
  await http.post('/drill/record', { questionId: current.value.id, result: pendingResult.value })
  tally(pendingResult.value)
  advance()
}

function tally(r) {
  if (r === 1) {
    rightCount.value++
    combo.value++
    maxCombo.value = Math.max(maxCombo.value, combo.value)
  } else {
    combo.value = 0
    wrongList.value.push({ id: current.value.id, stem: current.value.stem, relatedDocId: feedback.value?.relatedDocId })
  }
}

function advance() {
  if (idx.value + 1 >= questions.value.length) {
    phase.value = 'result'
    http.get('/drill/stats').then(s => (stats.value = s))
    http.get('/drill/wrong').then(l => (wrongItems.value = l.map(x => ({ ...x, show: false }))))
  } else {
    idx.value++
    resetAnswer()
  }
}

function quit() {
  checkCtrl?.abort()
  checking.value = false
  phase.value = 'setup'
}

/** 错题跳教材：回首页并打开对应文档 */
async function goDoc(docId) {
  const doc = await http.get(`/doc/${docId}`)
  router.push(`/?kb=${doc.kbId}&doc=${docId}`)
}
</script>

<style scoped>
.drill-page {
  min-height: 100vh;
  background: var(--kb-bg);
  display: flex;
  flex-direction: column;
}

/* setup / result 居中卡片 */
.drill-center {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}
.setup-card {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: var(--kb-radius);
  padding: 28px 24px;
  text-align: center;
  border: 1px solid var(--kb-line);
  box-shadow: var(--kb-shadow-md);
}
.setup-card h2 { margin: 0 0 4px; font-size: 24px; }
.setup-card.wide { max-width: 640px; text-align: left; }
.setup-sub { color: var(--kb-ink-3); font-size: 13px; margin: 0 0 18px; }
.mode-tip { color: var(--kb-ink-3); font-size: 12px; margin: 4px 0 0; }
.setup-card :deep(.el-form-item) { margin-bottom: 14px; }
.setup-card :deep(.el-form-item__label) { padding-bottom: 2px; }
.start-btn { width: 100%; margin-top: 6px; }
.setup-links { margin-top: 10px; }
.result-big { font-size: 40px; font-weight: 700; color: #00b96b; margin: 8px 0; }

/* 错题回顾 */
.wrong-list { text-align: left; margin: 14px 0; }
.wrong-list h4 { margin: 0 0 6px; font-size: 14px; color: var(--kb-ink-3); }
.wrong-item { padding: 8px 10px; border-radius: 8px; background: #fef0f0; margin-bottom: 6px; }
.wrong-stem { font-size: 14px; color: var(--kb-ink); }
.wrong-count {
  font-size: 11px;
  color: #f56c6c;
  background: #fef0f0;
  border-radius: 4px;
  padding: 1px 6px;
  margin-left: 6px;
}
.wrong-ops { margin-top: 4px; display: flex; gap: 12px; }
.wrong-ops a { font-size: 12px; color: #00b96b; cursor: pointer; }
.wrong-detail {
  margin-top: 6px;
  font-size: 13px;
  color: var(--kb-ink-2);
  line-height: 1.7;
  border-top: 1px dashed #e5e7eb;
  padding-top: 6px;
}
.wrong-doc { font-size: 12px; color: #00b96b; cursor: pointer; }

/* ===== session ===== */
.drill-session {
  flex: 1;
  display: flex;
  flex-direction: column;
  max-width: 760px;
  width: 100%;
  margin: 0 auto;
}
.session-top {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
}
.quit {
  font-size: 18px;
  color: var(--kb-ink-3);
  cursor: pointer;
  line-height: 1;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  transition: var(--kb-trans);
}
.quit:hover { background: var(--kb-side-hover); color: var(--kb-ink); }
.progress {
  flex: 1;
  height: 10px;
  background: #e5e7eb;
  border-radius: 6px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: var(--kb-brand-grad);
  border-radius: 6px;
  transition: width 0.3s;
}
.combo { font-size: 13px; color: #b88230; font-weight: 600; white-space: nowrap; }

.session-body {
  flex: 1;
  padding: 8px 16px 120px;
}
/* 题面卡：题干+选项收进白卡，灰底上层次分明 */
.q-card {
  background: #fff;
  border: 1px solid var(--kb-line);
  border-radius: var(--kb-radius);
  box-shadow: var(--kb-shadow-sm);
  padding: 24px 26px;
}
.q-type {
  display: inline-block;
  font-size: 12px;
  color: var(--el-color-primary-dark-2);
  background: var(--kb-side-active);
  border-radius: 999px;
  padding: 2px 10px;
  margin-bottom: 12px;
  font-weight: 500;
}
.q-stem { font-size: 19px; line-height: 1.6; margin: 0 0 20px; color: var(--kb-ink); }

/* 选项大卡片：悬停微浮，选中高亮 */
.opts { display: flex; flex-direction: column; gap: 10px; }
.opt {
  display: flex;
  align-items: center;
  gap: 10px;
  text-align: left;
  font-size: 15px;
  color: var(--kb-ink);
  background: #fff;
  border: 2px solid #e5e7eb;
  border-radius: 10px;
  padding: 13px 14px;
  cursor: pointer;
  transition: var(--kb-trans);
}
.opt:hover { border-color: var(--el-color-primary-light-5); box-shadow: var(--kb-shadow-sm); transform: translateY(-1px); }
.opt.picked { border-color: #00b96b; background: #e8f7ef; }
.opt-letter {
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: #f3f4f6;
  color: var(--kb-ink-2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
}
.opt.picked .opt-letter { background: #00b96b; color: #fff; }

/* ===== 动效：多邻国式即时反馈 ===== */
.opt.picked { animation: pop 0.25s ease; }
.opt.right { border-color: #00b96b; background: #e8f7ef; }
.opt.wrong { border-color: #f56c6c; background: #fef0f0; animation: shake 0.3s ease; }
.opt.wrong .opt-letter { background: #f56c6c; color: #fff; }
@keyframes pop {
  0% { transform: scale(1); }
  50% { transform: scale(1.03); }
  100% { transform: scale(1); }
}
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-6px); }
  75% { transform: translateX(6px); }
}
.feedback { animation: slideUp 0.3s ease; }
@keyframes slideUp {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}
.combo { animation: pop 0.3s ease; }
.result-big { animation: pop 0.4s ease; }
.cheer { color: #00b96b; font-weight: 600; margin: 4px 0; }
.cheer.soft { color: #b88230; }

/* 底部检查按钮 */
.session-foot {
  position: fixed;
  left: 0; right: 0; bottom: 0;
  background: #fff;
  border-top: 1px solid var(--kb-line);
  padding: 12px 16px;
  display: flex;
  justify-content: center;
}
.session-foot .el-button { width: 100%; max-width: 720px; }

/* 反馈条：多邻国式底部弹层 */
.feedback {
  position: fixed;
  left: 0; right: 0; bottom: 0;
  padding: 16px;
  border-radius: 16px 16px 0 0;
  max-height: 55vh;
  overflow-y: auto;
}
.feedback.good { background: #e8f7ef; }
.feedback.bad { background: #fef0f0; }
.feedback.neutral { background: #fdf6ec; }
.fb-title { font-size: 18px; font-weight: 700; margin-bottom: 8px; }
.feedback.good .fb-title { color: #00b96b; }
.feedback.bad .fb-title { color: #f56c6c; }
.feedback.neutral .fb-title { color: #b88230; }
.fb-line { font-size: 14px; color: var(--kb-ink-2); margin-bottom: 6px; line-height: 1.7; }
/* AI点评打字机光标 */
.fb-line .typing::after {
  content: ''; display: inline-block; width: 2px; height: 1em;
  margin-left: 2px; vertical-align: -2px; background: currentColor;
  animation: drillCursor 0.9s steps(2) infinite;
}
@keyframes drillCursor { 50% { opacity: 0; } }
.fb-actions { margin-top: 12px; display: flex; gap: 10px; }
.fb-actions .el-button { flex: 1; }

@media (max-width: 768px) {
  .q-stem { font-size: 17px; }
  .q-card { padding: 18px 16px; }
  .session-body { padding: 4px 12px 130px; }
}
</style>
