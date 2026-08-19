<template>
  <div class="iv-page">
    <!-- ===== setup ===== -->
    <div v-if="phase === 'setup'" class="iv-center">
      <div class="iv-card">
        <h2>AI 模拟面试</h2>
        <p class="iv-sub">面试官逐题提问、追问、点评，终场出总评报告</p>
        <el-form label-position="top">
          <el-form-item label="科目">
            <el-select v-model="category" placeholder="混合出题" clearable style="width: 100%">
              <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item>
          <el-form-item label="题量">
            <el-radio-group v-model="count">
              <el-radio :value="3">3 题</el-radio>
              <el-radio :value="5">5 题</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="追问强度">
            <el-radio-group v-model="maxFollow">
              <el-radio :value="0">不追问</el-radio>
              <el-radio :value="1">追 1 轮</el-radio>
              <el-radio :value="2">追 2 轮</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="面试官形象">
            <div class="avatar-pick">
              <div v-for="a in avatarOpts" :key="a.v"
                   :class="['avatar-opt', { on: avatar === a.v }]" @click="avatar = a.v">
                <div class="avatar-thumb">
                  <!-- @error兜底：图片丢了就退回SVG卡通形象，不会出现裂图 -->
                  <img :src="a.img" :alt="a.label" @error="onThumbError(a)" />
                  <InterviewerAvatar v-if="a.broken" :variant="a.v" />
                </div>
                <span>{{ a.label }}</span>
              </div>
            </div>
          </el-form-item>
        </el-form>
        <el-button type="primary" size="large" class="iv-big" @click="startInterview">开始面试</el-button>
        <el-button size="large" class="iv-big" @click="openList">面试记录（{{ records.length }}）</el-button>
        <el-button link @click="$router.push('/')">返回</el-button>
      </div>
    </div>

    <!-- ===== chat ===== -->
    <div v-else-if="phase === 'chat'" class="iv-chat">
      <header class="iv-top">
        <a class="quit" @click="quitChat">×</a>
        <span class="iv-title">AI 面试官 · {{ category || '混合' }}</span>
        <el-switch v-model="voiceOn" active-text="语音" size="small" />
        <span class="iv-count">题 {{ Math.min(qIdx + 1, questions.length) }}/{{ questions.length }}</span>
      </header>

      <!-- 面对面"视频通话"区：真人感形象图，照片自带虚化办公室背景，不再叠加假背景 -->
      <div class="iv-stage">
        <div :class="['iv-video', { speaking, listening, thinking: sending }]">
          <!-- 面试官形象：写实照片，外层叠加呼吸/点头/侧倾动效 -->
          <div class="iv-figure">
            <img :src="avatarImg" :key="avatar" alt="AI 面试官" draggable="false" @error="onStageImgError" />
            <InterviewerAvatar v-if="avatarBroken" class="iv-fallback" :speaking="speaking"
                               :listening="listening" :thinking="sending" :variant="avatar" />
          </div>
          <!-- 电影质感：噪点 + 暗角 -->
          <div class="iv-grain"></div>
          <div class="iv-vignette"></div>

          <!-- 铭牌（含通话计时）/ 状态提示 -->
          <div class="iv-badge"><i class="badge-dot"></i>AI 面试官 · {{ timerText }}</div>
          <div class="iv-status">{{ statusText }}</div>

          <!-- 你的小窗：点击可开真实摄像头 -->
          <div class="self-view" @click="toggleCam" :title="camOn ? '关闭摄像头' : '打开摄像头（试试）'">
            <video v-if="camOn" ref="camVideo" muted playsinline></video>
            <span v-else>你<i class="cam-hint">点开摄像头</i></span>
          </div>

          <!-- 实时字幕：说话时前面带声波动画 -->
          <div v-if="lastAiText" class="iv-caption">
            <span v-if="speaking" class="iv-wave"><i></i><i></i><i></i><i></i><i></i></span>
            <span class="iv-caption-text">{{ lastAiText }}</span>
          </div>
        </div>
      </div>

      <main class="iv-body" ref="chatBody">
        <div v-for="(m, i) in messages" :key="i" :class="['msg', m.role]">
          <span v-if="m.role === 'ai'" class="msg-avatar">AI</span>
          <div :class="['bubble', { streaming: m.streaming }]">{{ m.text }}</div>
        </div>
        <!-- AI不可用降级自评 -->
        <div v-if="selfEvalRef" class="msg ai">
          <div class="bubble selfeval">
            AI 暂不可用，对照参考答案自评：
            <div class="ref">{{ selfEvalRef }}</div>
            <div class="fb-actions">
              <el-button type="success" @click="selfEval(true)">答得可以</el-button>
              <el-button type="danger" @click="selfEval(false)">没答好</el-button>
            </div>
          </div>
        </div>
      </main>
      <footer class="iv-foot">
        <el-button
          :type="listening ? 'danger' : 'default'"
          :disabled="sending || !!selfEvalRef"
          @click="toggleMic">
          {{ listening ? '停止听写' : '语音答' }}
        </el-button>
        <el-input
          v-model="input" type="textarea" :rows="2"
          placeholder="写下你的回答，回车发送"
          :disabled="sending || !!selfEvalRef"
          @keydown.enter.exact.prevent="send" />
        <el-button type="primary" :loading="sending" :disabled="!input.trim() || !!selfEvalRef" @click="send">
          发送
        </el-button>
      </footer>
    </div>

    <!-- ===== report ===== -->
    <div v-else-if="phase === 'report'" class="iv-center">
      <div class="iv-card wide">
        <h2>面试总评</h2>
        <p class="iv-score">{{ report.score ?? '…' }} 分</p>
        <p :class="['iv-sub', { streaming: reportStreaming }]">{{ report.summary }}</p>
        <div class="iv-cols">
          <div v-if="report.strengths && report.strengths.length">
            <h4>强项</h4>
            <li v-for="s in report.strengths" :key="s">{{ s }}</li>
          </div>
          <div v-if="report.weaknesses && report.weaknesses.length">
            <h4>弱项</h4>
            <li v-for="s in report.weaknesses" :key="s">{{ s }}</li>
          </div>
        </div>
        <div v-if="report.suggestions && report.suggestions.length" class="iv-sug">
          <h4>备考建议</h4>
          <li v-for="s in report.suggestions" :key="s">{{ s }}</li>
        </div>
        <h4 class="iv-review-h">逐题回顾</h4>
        <div v-for="it in items" :key="it.questionId" class="review-item">
          <span :class="['dot', it.pass ? 'ok' : 'bad']"></span>
          <span class="review-stem">{{ it.stem }}</span>
          <a v-if="!it.pass && it.relatedDocId" class="review-link" @click="goDoc(it.relatedDocId)">看教材</a>
          <a v-if="!it.pass" class="review-link" @click="toWrong(it)">进错题本</a>
        </div>
        <el-button type="primary" size="large" class="iv-big" @click="phase = 'setup'">再来一场</el-button>
        <el-button size="large" @click="openList">看记录</el-button>
      </div>
    </div>

    <!-- ===== list ===== -->
    <div v-else-if="phase === 'list'" class="iv-center">
      <div class="iv-card wide">
        <h2>面试记录</h2>
        <div v-if="!records.length" class="iv-sub">还没有面试记录，去面一场吧</div>
        <div v-for="r in records" :key="r.id" class="rec-item" @click="openDetail(r.id)">
          <div class="rec-main">
            <span class="rec-cat">{{ r.category }}</span>
            <span class="rec-score">{{ r.score }} 分</span>
          </div>
          <div class="rec-time">{{ (r.createTime || '').replace('T', ' ').slice(0, 16) }}</div>
        </div>
        <el-button size="large" class="iv-big" @click="phase = 'setup'">返回</el-button>
      </div>
    </div>

    <!-- ===== detail ===== -->
    <div v-else class="iv-chat">
      <header class="iv-top">
        <a class="quit" @click="openList">×</a>
        <span class="iv-title">回看 · {{ detail.category }} · {{ detail.score }} 分</span>
      </header>
      <main class="iv-body">
        <div v-for="(m, i) in detailTranscript" :key="i" :class="['msg', m.role]">
          <span v-if="m.role === 'ai'" class="msg-avatar">AI</span>
          <div class="bubble">{{ m.text }}</div>
        </div>
        <div class="msg ai"><span class="msg-avatar">AI</span><div class="bubble">【总评】{{ detailReport.summary }}</div></div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http, { streamPost } from '../api/http.js'
import { useSpeech } from '../composables/useSpeech.js'
import InterviewerAvatar from '../components/InterviewerAvatar.vue'

const router = useRouter()
const { voiceOn, speaking, listening, asrSupported, speak, listen, stopListen } = useSpeech()
const categories = ref([])
const records = ref([])

// setup
const category = ref('')
const count = ref(3)
const maxFollow = ref(1)
const avatar = ref('m')
// 面试官形象写真图（放public/，Vite原样发布到站点根路径）；
// 加时间戳是为了浏览器缓存：同名文件被覆盖后仍能拿到新图
const _v = '?v=' + Date.now()
const avatarOpts = [
  { v: 'm', label: '男面试官', img: '/interviewer-m.png' + _v, broken: false },
  { v: 'f', label: '女面试官', img: '/interviewer-f.png' + _v, broken: false }
]
const avatarBroken = ref(false)
const avatarImg = computed(() => (avatarOpts.find(a => a.v === avatar.value) || avatarOpts[0]).img)
watch(avatar, () => { avatarBroken.value = false })
/** 视频区主图加载失败 → 退回SVG动画形象，页面不留白 */
function onStageImgError() { avatarBroken.value = true }
/** 选择器缩略图加载失败 → 该项显示SVG形象 */
function onThumbError(a) { a.broken = true }

// 通话计时
const elapsed = ref(0)
let timer = null
const timerText = computed(() => {
  const m = String(Math.floor(elapsed.value / 60)).padStart(2, '0')
  const s = String(elapsed.value % 60).padStart(2, '0')
  return `${m}:${s}`
})
function startTimer() {
  stopTimer()
  elapsed.value = 0
  timer = setInterval(() => elapsed.value++, 1000)
}
function stopTimer() {
  if (timer) clearInterval(timer)
  timer = null
}

// 本地摄像头（"我"的小窗）
const camOn = ref(false)
const camVideo = ref(null)
let camStream = null
async function toggleCam() {
  if (camOn.value) { stopCam(); return }
  try {
    camStream = await navigator.mediaDevices.getUserMedia({ video: { width: 320, height: 240 } })
    camOn.value = true
    await nextTick()
    if (camVideo.value) {
      camVideo.value.srcObject = camStream
      camVideo.value.play()
    }
  } catch {
    ElMessage.warning('摄像头打不开：请检查权限，或用 localhost 访问（局域网IP下浏览器禁止摄像头）')
  }
}
function stopCam() {
  if (camStream) camStream.getTracks().forEach(t => t.stop())
  camStream = null
  camOn.value = false
}

// chat
const phase = ref('setup')
const questions = ref([])
const qIdx = ref(0)
const followUsed = ref(0)
const messages = ref([])
const input = ref('')
const sending = ref(false)
const selfEvalRef = ref(null)   // 非null时显示自评面板，值为参考答案
const items = ref([])
const chatBody = ref(null)

// report / detail
const report = ref({})
const reportStreaming = ref(false)  // 总评summary流式上屏中
const detail = ref({})
const detailTranscript = ref([])
const detailReport = ref({})

// 字幕：面试官最近说的一句话
const lastAiText = computed(() => {
  for (let i = messages.value.length - 1; i >= 0; i--) {
    if (messages.value[i].role === 'ai') return messages.value[i].text
  }
  return ''
})

// 视频通话区的状态提示
const statusText = computed(() => {
  if (sending.value) return '面试官思考中…'
  if (speaking.value) return '面试官正在讲话…'
  if (listening.value) return '正在聆听你的回答…'
  return '等待你的回答'
})

onMounted(async () => {
  categories.value = await http.get('/drill/categories')
})

// 流式请求句柄：退出/卸载时打断
let evalCtrl = null
let reportCtrl = null

onUnmounted(() => {
  stopCam()
  stopTimer()
  evalCtrl?.abort()
  reportCtrl?.abort()
})

async function openList() {
  records.value = await http.get('/interview/list')
  phase.value = 'list'
}

async function startInterview() {
  const list = await http.post('/interview/start', {
    category: category.value, count: count.value, maxFollow: maxFollow.value
  })
  if (!list.length) {
    ElMessage.warning('该科目题库暂无题，换个科目吧')
    return
  }
  questions.value = list
  qIdx.value = 0
  followUsed.value = 0
  items.value = []
  messages.value = []
  selfEvalRef.value = null
  askCurrent()
  phase.value = 'chat'
  startTimer() // 开始通话计时
}

/** 中途退出：关摄像头、停计时、打断进行中的流式请求 */
function quitChat() {
  evalCtrl?.abort()
  stopCam()
  stopTimer()
  stopListen()
  phase.value = 'setup'
}

function askCurrent() {
  const q = questions.value[qIdx.value]
  const text = `第 ${qIdx.value + 1} 题：${q.stem}`
  messages.value.push({ role: 'ai', text })
  speak(text)
  scroll()
}

/** 语音作答：点一下开始听写、再点一下停止，识别结果实时填进输入框，说完自己确认发送 */
function toggleMic() {
  if (listening.value) {
    stopListen() // 正在听写 → 手动停止，时长由用户决定
    return
  }
  const ok = listen(
    t => (input.value = t),
    () => {},
    err => {
      if (err === 'not-allowed' || err === 'service-not-allowed') {
        ElMessage.warning('麦克风权限被拒，请在浏览器设置里允许后重试')
      } else if (err === 'network') {
        ElMessage.warning('语音识别服务网络不通，请用打字作答')
      } else {
        ElMessage.warning('语音识别不可用，请用打字作答')
      }
    }
  )
  if (!ok) ElMessage.warning('当前浏览器不支持语音识别，请用打字作答')
}

async function scroll() {
  await nextTick()
  chatBody.value?.scrollTo({ top: chatBody.value.scrollHeight, behavior: 'smooth' })
}

/** 本题对话线程：从最近一条“第X题”主问题开始到当前回答，给AI上下文避免重复追问 */
function currentThread() {
  const ms = messages.value
  let start = 0
  for (let i = ms.length - 1; i >= 0; i--) {
    if (ms[i].role === 'ai' && !ms[i].streaming && ms[i].text.startsWith(`第 ${qIdx.value + 1} 题：`)) {
      start = i
      break
    }
  }
  return ms.slice(start)
    .filter(m => !m.streaming && m.text)
    .map(m => ({ role: m.role === 'ai' ? 'interviewer' : 'user', text: m.text }))
}

function send() {
  const text = input.value.trim()
  if (!text || sending.value) return
  messages.value.push({ role: 'me', text })
  input.value = ''
  scroll()
  sending.value = true
  const q = questions.value[qIdx.value]
  // 点评气泡先占位，delta增量往里追，打字机式上屏
  const bubble = { role: 'ai', text: '', streaming: true }
  let gotDelta = false
  evalCtrl = streamPost('/interview/evaluate-stream', {
    questionId: q.id, answer: text, followUsed: followUsed.value,
    maxFollow: maxFollow.value, history: currentThread()
  }, {
    onDelta: d => {
      gotDelta = true
      bubble.text += d
      scroll()
    },
    onFallback: vo => {
      // AI完全不可用：气泡撤掉，降级自评面板
      messages.value = messages.value.filter(m => m !== bubble)
      selfEvalRef.value = vo.reference
      sending.value = false
      scroll()
    },
    onDone: vo => {
      bubble.streaming = false
      sending.value = false
      if (vo.pass === null) {
        // 流到了但格式异常：同样降级自评（已流出的点评保留）
        selfEvalRef.value = vo.reference
        scroll()
        return
      }
      if (!gotDelta && vo.comment) bubble.text = vo.comment
      if (vo.followUp) {
        messages.value.push({ role: 'ai', text: `追问：${vo.followUp}` })
        followUsed.value++
        if (!items.value.find(x => x.questionId === q.id)) {
          items.value.push({ questionId: q.id, stem: q.stem, pass: vo.pass, comment: vo.comment || bubble.text, relatedDocId: q.relatedDocId, pending: true })
        }
      } else {
        finalizeQuestion(vo.pass, vo.comment || bubble.text)
      }
      const say = [bubble.text, vo.followUp ? `追问：${vo.followUp}` : ''].filter(Boolean).join(' ')
      if (say) speak(say)
      scroll()
    },
    onError: () => {
      bubble.streaming = false
      sending.value = false
      if (!bubble.text) messages.value = messages.value.filter(m => m !== bubble)
      ElMessage.error('点评服务异常，请重试')
    }
  })
}

/** 自评面板结果 */
function selfEval(pass) {
  selfEvalRef.value = null
  finalizeQuestion(pass, '（自评）')
  scroll()
}

function finalizeQuestion(pass, comment) {
  const q = questions.value[qIdx.value]
  const exist = items.value.find(x => x.questionId === q.id)
  if (exist) {
    exist.pass = pass
    exist.comment = comment
    delete exist.pending
  } else {
    items.value.push({ questionId: q.id, stem: q.stem, pass, comment, relatedDocId: q.relatedDocId })
  }
  qIdx.value++
  followUsed.value = 0
  if (qIdx.value < questions.value.length) {
    askCurrent()
  } else {
    finish()
  }
}

function finish() {
  stopTimer()
  stopCam()
  const reportReq = {
    category: category.value || '混合',
    transcript: JSON.stringify(messages.value),
    items: items.value.map(({ pending, ...rest }) => rest)
  }
  // 报告页先上：summary流式打字，done后填充分数/强弱势
  phase.value = 'report'
  report.value = { summary: '' }
  reportStreaming.value = true
  reportCtrl = streamPost('/interview/report-stream', reportReq, {
    onDelta: d => { report.value.summary += d },
    onDone: r => {
      reportStreaming.value = false
      report.value = r
      speak(`面试结束，综合评分 ${r.score} 分`)
    },
    onError: () => {
      reportStreaming.value = false
      if (!report.value.summary) {
        ElMessage.error('总评服务异常，请重新结束本场')
        phase.value = 'chat'
      }
    }
  })
}

/** 未通过的题塞进错题本 */
async function toWrong(it) {
  await http.post('/drill/record', { questionId: it.questionId, result: 0 })
  ElMessage.success('已加入错题本')
}

async function goDoc(docId) {
  const doc = await http.get(`/doc/${docId}`)
  router.push(`/?kb=${doc.kbId}&doc=${docId}`)
}

async function openDetail(id) {
  detail.value = await http.get(`/interview/${id}`)
  try { detailTranscript.value = JSON.parse(detail.value.transcript || '[]') } catch { detailTranscript.value = [] }
  try { detailReport.value = JSON.parse(detail.value.report || '{}') } catch { detailReport.value = {} }
  phase.value = 'detail'
}
</script>

<style scoped>
.iv-page { min-height: 100vh; background: var(--kb-bg); display: flex; flex-direction: column; }

.iv-center { flex: 1; display: flex; align-items: center; justify-content: center; padding: 20px; }
.iv-card {
  width: 100%; max-width: 460px; background: #fff; border: 1px solid var(--kb-line);
  border-radius: var(--kb-radius); padding: 26px 24px; text-align: center;
  box-shadow: var(--kb-shadow-md);
}
.iv-card.wide { max-width: 680px; text-align: left; }
.iv-card h2 { margin: 0 0 4px; font-size: 24px; }
.iv-sub { color: var(--kb-ink-3); font-size: 13px; margin: 0 0 16px; }
.iv-big { width: 100%; margin-top: 8px; }
.iv-score { font-size: 44px; font-weight: 700; color: #00b96b; margin: 6px 0; }
.iv-cols { display: flex; gap: 20px; }
.iv-cols > div, .iv-sug { flex: 1; }
.iv-card h4 { margin: 12px 0 6px; font-size: 14px; color: var(--kb-ink-3); }
.iv-card li { font-size: 13px; color: var(--kb-ink-2); margin: 0 0 4px 16px; line-height: 1.6; }
.review-item { display: flex; align-items: center; gap: 8px; padding: 6px 0; font-size: 13px; }
.dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.dot.ok { background: #00b96b; }
.dot.bad { background: #f56c6c; }
.review-stem { flex: 1; min-width: 0; white-space: nowrap; text-overflow: ellipsis; overflow: hidden; }
.review-link { color: #00b96b; cursor: pointer; flex-shrink: 0; }
.rec-item { padding: 10px 8px; border-radius: 8px; cursor: pointer; }
.rec-item:hover { background: var(--kb-side-hover); }
.rec-main { display: flex; justify-content: space-between; font-size: 14px; }
.rec-score { color: #00b96b; font-weight: 600; }
.rec-time { font-size: 12px; color: var(--kb-ink-3); margin-top: 2px; }

/* ===== chat ===== */
.iv-chat { flex: 1; display: flex; flex-direction: column; max-width: 800px; width: 100%; margin: 0 auto; }
/* 顶栏容器化：白底 + 底边 + 细影 */
.iv-top {
  display: flex; align-items: center; gap: 12px; padding: 12px 16px;
  background: #fff; border-bottom: 1px solid var(--kb-line);
  border-radius: var(--kb-radius) var(--kb-radius) 0 0;
  margin-top: 10px;
  box-shadow: var(--kb-shadow-sm);
}
.quit {
  font-size: 18px; color: var(--kb-ink-3); cursor: pointer; line-height: 1;
  width: 30px; height: 30px; display: flex; align-items: center; justify-content: center;
  border-radius: 8px; transition: var(--kb-trans);
}
.quit:hover { background: var(--kb-side-hover); color: var(--kb-ink); }
.iv-title { font-weight: 600; flex: 1; }
.iv-count { font-size: 13px; color: var(--kb-ink-3); }
.iv-body { flex: 1; overflow-y: auto; padding: 8px 16px 16px; }
.msg { display: flex; margin-bottom: 12px; align-items: flex-start; gap: 8px; }
.msg.ai { justify-content: flex-start; }
.msg.me { justify-content: flex-end; }
/* AI消息头像：品牌渐变小圆徽 */
.msg-avatar {
  width: 28px; height: 28px; border-radius: 50%; flex-shrink: 0;
  background: var(--kb-brand-grad); color: #fff;
  font-size: 11px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 2px 6px rgba(0, 185, 107, .3);
}
.bubble {
  max-width: 82%; border-radius: 12px; padding: 10px 14px;
  font-size: 15px; line-height: 1.7; white-space: pre-wrap;
}
.msg.ai .bubble { background: #fff; border: 1px solid var(--kb-line); }
.msg.me .bubble { background: var(--kb-brand-grad); color: #fff; box-shadow: 0 2px 8px rgba(0, 185, 107, .25); }
.bubble.thinking { color: var(--kb-ink-3); }
/* 流式打字光标：点评/总评逐字上屏时尾部闪烁，提示“还在说” */
.bubble.streaming::after, .iv-sub.streaming::after {
  content: ''; display: inline-block; width: 2px; height: 1em;
  margin-left: 2px; vertical-align: -2px; background: currentColor;
  animation: iv-cursor 0.9s steps(2) infinite;
}
@keyframes iv-cursor { 50% { opacity: 0; } }
.bubble.selfeval { background: #fdf6ec; border-color: #f3d9ab; }
.bubble .ref { margin: 8px 0; color: var(--kb-ink-2); font-size: 13px; }
.fb-actions { display: flex; gap: 10px; }
.iv-foot {
  border-top: 1px solid var(--kb-line); background: #fff;
  padding: 10px 16px; display: flex; gap: 10px; align-items: flex-end;
  border-radius: 0 0 var(--kb-radius) var(--kb-radius);
  margin-bottom: 10px;
  box-shadow: var(--kb-shadow-sm);
}
.iv-foot .el-button { flex-shrink: 0; }

/* ===== 面对面视频通话区 ===== */
.iv-stage { padding: 0 16px; }
.iv-video {
  position: relative; height: clamp(220px, 38vh, 330px);
  border-radius: 16px; overflow: hidden;
  border: 1px solid var(--kb-line);
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.10);
  transition: box-shadow 0.3s;
}
/* 听写时红圈提示：面试官在听你说话 */
.iv-video.listening { box-shadow: 0 0 0 3px rgba(245, 108, 108, 0.5), 0 4px 18px rgba(0, 0, 0, 0.10); }

/* --- 面试官形象：写实照片，图内自带虚化办公室背景 --- */
.iv-figure {
  position: absolute; inset: 0; z-index: 0;
  animation: iv-breathe 5s ease-in-out infinite;        /* 呼吸起伏 */
  transition: rotate 0.9s cubic-bezier(0.37, 0, 0.63, 1), filter 0.6s;
  will-change: transform;
}
.iv-figure img {
  width: 100%; height: 100%; object-fit: cover; object-position: center top;
  user-select: none; display: block;
}
/* 图片加载失败时的SVG兜底形象 */
.iv-fallback {
  position: absolute; left: 50%; bottom: -6%; transform: translateX(-50%);
  height: 112%; width: auto;
}
@keyframes iv-breathe { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-6px); } }
/* 聆听你作答：轻轻点头（叠加在呼吸动画的位移上） */
.iv-video.listening .iv-figure { animation: iv-breathe 5s ease-in-out infinite, iv-nod 1.15s ease-in-out infinite; }
@keyframes iv-nod { 0%, 100% { rotate: 0deg; } 40% { rotate: 1.4deg; } 70% { rotate: -0.7deg; } }
/* 思考中：头微侧+画面轻压暗，像在斟酌措辞 */
.iv-video.thinking .iv-figure { rotate: 1.5deg; filter: saturate(0.85) brightness(0.96); }
/* 讲话中：极轻微缩放，配合字幕营造"在开口"的节奏感 */
.iv-video.speaking .iv-figure img { animation: iv-talk 1s ease-in-out infinite; }
@keyframes iv-talk { 0%, 100% { scale: 1; } 50% { scale: 1.012; } }

/* 电影质感：胶片噪点 */
.iv-grain {
  position: absolute; inset: -50%; z-index: 1; pointer-events: none; opacity: 0.5;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='300' height='300'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='2'/%3E%3CfeColorMatrix values='0 0 0 0 1 0 0 0 0 1 0 0 0 0 1 0 0 0 0.04 0'/%3E%3C/filter%3E%3Crect width='300' height='300' filter='url(%23n)'/%3E%3C/svg%3E");
  animation: grain 0.9s steps(4) infinite;
}
@keyframes grain {
  0% { transform: translate(0, 0); } 25% { transform: translate(-2%, 3%); }
  50% { transform: translate(3%, -2%); } 75% { transform: translate(-3%, -3%); }
  100% { transform: translate(2%, 2%); }
}
/* 电影质感：四周暗角，聚焦人物 */
.iv-vignette {
  position: absolute; inset: 0; z-index: 1; pointer-events: none;
  background: radial-gradient(ellipse at center, transparent 55%, rgba(10, 18, 28, 0.32) 100%);
}
/* 思考状态的暗化已合并到 .iv-figure 规则，无需单独覆盖 */

/* --- 说话时的声波动画（字幕前） --- */
.iv-wave { display: inline-flex; align-items: center; gap: 2px; height: 14px; margin-right: 8px; vertical-align: middle; }
.iv-wave i { width: 3px; height: 14px; border-radius: 2px; background: #00d97e; animation: wv 0.7s ease-in-out infinite; }
.iv-wave i:nth-child(2) { animation-delay: 0.12s; }
.iv-wave i:nth-child(3) { animation-delay: 0.24s; }
.iv-wave i:nth-child(4) { animation-delay: 0.36s; }
.iv-wave i:nth-child(5) { animation-delay: 0.48s; }
@keyframes wv { 0%, 100% { transform: scaleY(0.3); } 50% { transform: scaleY(1); } }

/* --- 视频区浮层元素 --- */
.iv-badge {
  position: absolute; left: 12px; bottom: 12px; z-index: 2;
  display: flex; align-items: center; gap: 6px;
  background: rgba(0, 0, 0, 0.45); color: #fff; font-size: 12px;
  padding: 5px 10px; border-radius: 20px; backdrop-filter: blur(4px);
}
.badge-dot { width: 7px; height: 7px; border-radius: 50%; background: #00d97e; animation: dotpulse 1.6s infinite; }
@keyframes dotpulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.35; } }
.iv-status {
  position: absolute; right: 12px; top: 12px; z-index: 2;
  background: rgba(0, 0, 0, 0.40); color: #fff; font-size: 12px;
  padding: 5px 12px; border-radius: 20px; backdrop-filter: blur(4px);
}
.self-view {
  position: absolute; right: 12px; bottom: 12px; z-index: 2;
  width: 84px; height: 62px; border-radius: 10px; overflow: hidden; cursor: pointer;
  background: linear-gradient(150deg, #3d4a5c, #232c38); color: #fff;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  font-size: 14px; border: 2px solid rgba(255, 255, 255, 0.65);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.25); transition: transform 0.2s;
}
.self-view:hover { transform: scale(1.05); }
.self-view video { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; }
.cam-hint { font-size: 10px; color: rgba(255, 255, 255, 0.65); margin-top: 2px; font-style: normal; }
/* 实时字幕：面试官正在说的话 */
.iv-caption {
  position: absolute; left: 50%; bottom: 14px; transform: translateX(-50%); z-index: 2;
  max-width: 58%; background: rgba(15, 23, 32, 0.72); color: #fff;
  font-size: 14px; line-height: 1.6; padding: 8px 14px; border-radius: 10px;
  backdrop-filter: blur(4px); text-align: left;
  display: flex; align-items: center;
}
.iv-caption-text { white-space: pre-wrap; }

/* 面试官形象选择（setup页） */
.avatar-pick { display: flex; gap: 14px; width: 100%; }
.avatar-opt {
  flex: 1; border: 2px solid var(--kb-line); border-radius: 10px; overflow: hidden;
  cursor: pointer; transition: all 0.2s; background: #fff; text-align: center;
}
.avatar-thumb {
  height: 110px; position: relative; overflow: hidden;
  background: linear-gradient(160deg, #e6edf4, #cdd8e4);
}
.avatar-thumb img { width: 100%; height: 100%; object-fit: cover; object-position: center top; display: block; }
.avatar-thumb :deep(.iv-avatar) { position: absolute; left: 50%; bottom: -2px; transform: translateX(-50%); height: 108%; width: auto; }
.avatar-opt span { display: block; font-size: 13px; color: var(--kb-ink-2); padding: 7px 0; }
.avatar-opt.on { border-color: #00b96b; box-shadow: 0 0 0 3px rgba(0, 185, 107, 0.18); }
.avatar-opt.on span { color: #00b96b; font-weight: 600; }

@media (max-width: 768px) {
  .bubble { max-width: 90%; font-size: 14px; }
  .iv-card { padding: 20px 16px; }
  .iv-video { height: 220px; }
  .self-view { width: 64px; height: 48px; }
  .cam-hint { display: none; }
  .iv-caption { max-width: 82%; font-size: 13px; bottom: 10px; }
  .iv-badge { font-size: 11px; padding: 4px 8px; }
}
</style>
