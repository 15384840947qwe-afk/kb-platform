<template>
  <div class="doc-page">
    <!-- 文档工具条：左侧状态区 + 右侧操作区 -->
    <div class="doc-toolbar">
      <div class="tb-left">
        <span :class="['state-dot', { editing: !saved }]"></span>
        <span class="doc-state">{{ saved ? '已保存' : '编辑中…' }}</span>
        <span v-if="status === 0" class="doc-tag">待审核</span>
        <span v-if="status === 2" class="doc-tag rejected">已驳回 · 可修改后重提</span>
        <span v-if="status === 3" class="doc-tag">草稿</span>
      </div>
      <div class="tb-right">
        <el-button v-if="status === 3 || status === 2" type="success" size="small" @click="submit">
          提交审核
        </el-button>
        <el-button v-if="isAdmin" size="small" :loading="generating" @click="generate">
          生成练习题
        </el-button>
        <el-button type="primary" size="small" :loading="saving" @click="save">保存</el-button>
      </div>
    </div>

    <div class="doc-body">
      <h1 class="doc-title">{{ title }}</h1>
      <div class="doc-meta">版本 v{{ version }} · 块编辑器</div>

      <!-- 文档附件：文件型文档的内容本体 -->
      <div v-if="attachments.length" class="attach-list">
        <div v-for="f in attachments" :key="f.id" class="attach-item">
          <span class="attach-ico">
            <svg viewBox="0 0 16 16" width="16" height="16"><path d="M4 1.5h5.6a1 1 0 0 1 .7.3l2.4 2.4a1 1 0 0 1 .3.7v9.6a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1v-12a1 1 0 0 1 1-1z" fill="none" stroke="currentColor" stroke-width="1.3"/><path d="M5.5 7h5M5.5 9.5h5M5.5 12h3.5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
          </span>
          <div class="attach-info">
            <div class="attach-name">{{ f.originalName }}</div>
            <div class="attach-size">{{ (f.size / 1024).toFixed(1) }} KB</div>
          </div>
          <el-button size="small" type="primary" plain @click="downloadAttach(f)">下载</el-button>
        </div>
      </div>

      <div ref="holder" class="editor-holder"></div>
    </div>

    <!-- 问AI：悬浮按钮+抽屉聊天，基于本篇内容的流式问答 -->
    <div class="ask-fab" title="针对本篇文档提问" @click="askOpen = true">
      <svg viewBox="0 0 16 16" width="15" height="15"><path d="M8 1l1.6 4.2L14 6.8l-4.4 1.6L8 12.6 6.4 8.4 2 6.8l4.4-1.6z" fill="currentColor"/></svg>
      问 AI
    </div>
    <el-drawer v-model="askOpen" title="问 AI · 本篇教材" size="400px" @close="askCtrl?.abort()">
      <div class="ask-box">
        <div class="ask-msgs" ref="askMsgs">
          <div v-if="!askMessages.length" class="ask-hint">
            针对这篇文档提问，AI只依据正文内容回答，没提到会明说。
          </div>
          <div v-for="(m, i) in askMessages" :key="i" :class="['ask-msg', m.role]">
            <div :class="['ask-bubble', { typing: m.streaming }]">{{ m.text }}</div>
          </div>
        </div>
        <div class="ask-input">
          <el-input v-model="askInput" type="textarea" :rows="2" placeholder="问问这篇教材…回车发送"
                    :disabled="asking" @keydown.enter.exact.prevent="sendAsk" />
          <el-button type="primary" :disabled="!askInput.trim()" :loading="asking" @click="sendAsk">发送</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import EditorJS from '@editorjs/editorjs'
import Header from '@editorjs/header'
import ListTool from '@editorjs/list'
import CodeTool from '@editorjs/code'
import ImageTool from '@editorjs/image'
import { ElMessage } from 'element-plus'
import http, { streamPost } from '../api/http.js'

// 父组件用:key=docId保证切换文档时组件重建，只处理onMounted一次
const props = defineProps({ docId: Number })

const holder = ref(null)
const title = ref('')
const version = ref(0)
// 审核状态：0待审核 1通过 2驳回 3草稿
const status = ref(1)
const user = JSON.parse(localStorage.getItem('kb-user') || '{"role":""}')
const isAdmin = computed(() => user.role === 'ADMIN')
const generating = ref(false)
const saving = ref(false)
const saved = ref(true)
const attachments = ref([])
let editor = null
// 图片懒加载观察器：编辑器插进来的img统一加loading=lazy，滚到视口才加载
let observer = null

async function load() {
  const doc = await http.get(`/doc/${props.docId}`)
  title.value = doc.title
  version.value = doc.version
  status.value = doc.status
  attachments.value = await http.get(`/file/list?docId=${props.docId}`)
  let data = { blocks: [] }
  try {
    data = JSON.parse(doc.content || '{"blocks":[]}')
  } catch (e) {
    // 内容损坏当空文档处理
  }
  await nextTick()
  editor = new EditorJS({
    holder: holder.value,
    data,
    placeholder: '开始输入内容…',
    // 注册标题/列表/代码块工具，导入的Markdown内容靠它们渲染
    tools: {
      header: {
        class: Header,
        config: { levels: [1, 2, 3, 4, 5, 6] }
      },
      list: ListTool,
      code: CodeTool,
      image: ImageTool
    },
    onChange: () => {
      saved.value = false
    }
  })
}

async function save() {
  saving.value = true
  try {
    const output = await editor.save()
    const doc = await http.put(`/doc/${props.docId}`, {
      content: JSON.stringify(output),
      version: version.value
    })
    version.value = doc.version
    saved.value = true
    ElMessage.success('已保存')
  } finally {
    saving.value = false
  }
}

/** 草稿/被驳回的文档提交审核 */
async function submit() {
  await http.post(`/doc/${props.docId}/submit`)
  status.value = 0
  ElMessage.success('已提交审核')
}

/** AI从本篇教材生成练习题，生成的题自动关联回本文档 */
async function generate() {
  generating.value = true
  try {
    const list = await http.post(`/question/generate?docId=${props.docId}`)
    ElMessage.success(`已生成${list.length}道练习题，去刷题页试试，答错能跳回本篇教材`)
  } finally {
    generating.value = false
  }
}

/** 附件下载：预签名URL浏览器直连MinIO */
async function downloadAttach(f) {
  const url = await http.get(`/file/${f.id}/url`)
  window.open(url)
}

// ===== 问AI：针对本篇文档的流式问答；切文档时组件重建，会话天然清空 =====
const askOpen = ref(false)
const askMessages = ref([])   // {role:'user'|'assistant', text, streaming?}
const askInput = ref('')
const asking = ref(false)
const askMsgs = ref(null)
let askCtrl = null

function scrollAsk() {
  nextTick(() => askMsgs.value?.scrollTo({ top: askMsgs.value.scrollHeight, behavior: 'smooth' }))
}

function sendAsk() {
  const q = askInput.value.trim()
  if (!q || asking.value) return
  askInput.value = ''
  askMessages.value.push({ role: 'user', text: q })
  // 最近2轮问答带给后端做多轮上下文（不含正在流的这条）
  const history = askMessages.value
    .filter(m => !m.streaming)
    .slice(0, -1)
    .slice(-4)
    .map(m => ({ role: m.role, text: m.text }))
  const bubble = { role: 'assistant', text: '', streaming: true }
  askMessages.value.push(bubble)
  asking.value = true
  scrollAsk()
  askCtrl = streamPost(`/doc/${props.docId}/ask`, { question: q, history }, {
    onDelta: d => { bubble.text += d; scrollAsk() },
    onDone: () => { bubble.streaming = false; asking.value = false },
    onError: () => {
      bubble.streaming = false
      asking.value = false
      if (!bubble.text) bubble.text = 'AI服务异常，请稍后再试'
    }
  })
}

onMounted(() => {
  observer = new MutationObserver(() => {
    holder.value
      ?.querySelectorAll('img:not([loading])')
      .forEach(img => { img.loading = 'lazy' })
  })
  observer.observe(holder.value, { childList: true, subtree: true })
  load()
})
onBeforeUnmount(() => {
  observer && observer.disconnect()
  editor && editor.destroy()
  askCtrl?.abort()
})
</script>

<style scoped>
.doc-page {
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

/* 顶部工具条：左状态右操作 */
.doc-toolbar {
  position: sticky;
  top: 0;
  z-index: 10;
  height: 52px;
  background: rgba(255, 255, 255, .92);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--kb-line);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 0 24px;
}
.tb-left { display: flex; align-items: center; gap: 10px; }
.tb-right { display: flex; align-items: center; gap: 8px; }
/* 保存状态呼吸点：绿=已存、橙=编辑中 */
.state-dot {
  width: 8px; height: 8px; border-radius: 50%;
  background: var(--el-color-primary);
  transition: var(--kb-trans);
}
.state-dot.editing { background: #e6a23c; animation: docpulse 1.2s ease-in-out infinite; }
@keyframes docpulse { 50% { opacity: .35; } }
.doc-state {
  font-size: 12px;
  color: var(--kb-ink-3);
}

/* 审核状态标签：药丸式 */
.doc-tag {
  font-size: 12px;
  color: #b88230;
  background: #fdf6ec;
  border: 1px solid #f3d9ab;
  border-radius: 999px;
  padding: 1px 10px;
  font-weight: 500;
}
.doc-tag.rejected {
  color: #f56c6c;
  background: #fef0f0;
  border-color: #fbc4c4;
}

/* 附件卡片 */
.attach-list {
  margin-bottom: 20px;
}
.attach-item {
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid var(--kb-line);
  background: #fafbfc;
  border-radius: 10px;
  padding: 10px 14px;
  margin-bottom: 8px;
  transition: var(--kb-trans);
}
.attach-item:hover { border-color: var(--el-color-primary-light-7); box-shadow: var(--kb-shadow-sm); }
.attach-item .el-button { margin-left: auto; flex-shrink: 0; }
.attach-ico {
  width: 34px; height: 34px; border-radius: 9px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary-dark-2);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.attach-name {
  font-size: 14px;
  color: var(--kb-ink);
  word-break: break-all;
}
.attach-size {
  font-size: 12px;
  color: var(--kb-ink-3);
  margin-top: 2px;
}

/* 正文区：仿语雀文档的居中窄栏排版 */
.doc-body {
  max-width: 820px;
  width: 100%;
  margin: 0 auto;
  padding: 40px 24px 100px;
}
.doc-title {
  margin: 0 0 10px;
  font-size: 30px;
  font-weight: 700;
  color: var(--kb-ink);
  line-height: 1.4;
}
.doc-meta {
  font-size: 13px;
  color: var(--kb-ink-3);
  margin-bottom: 28px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--kb-line);
}

/* 正文排版：字号行高向舒适阅读看齐 */
.editor-holder {
  font-size: 16px;
  line-height: 1.9;
  color: var(--kb-ink);
}

/* 问AI悬浮按钮：品牌渐变胶囊 */
.ask-fab {
  position: fixed;
  right: 28px;
  bottom: 28px;
  z-index: 50;
  background: var(--kb-brand-grad);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  padding: 11px 20px;
  border-radius: 22px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 7px;
  box-shadow: 0 6px 18px rgba(0, 185, 107, 0.38);
  transition: var(--kb-trans);
}
.ask-fab:hover { transform: translateY(-2px); box-shadow: 0 8px 22px rgba(0, 185, 107, .45); }

/* 问AI抽屉内部 */
.ask-box { display: flex; flex-direction: column; height: 100%; }
.ask-msgs { flex: 1; overflow-y: auto; padding: 4px 2px 12px; }
.ask-hint { font-size: 13px; color: var(--kb-ink-3); line-height: 1.8; padding: 8px 4px; }
.ask-msg { display: flex; margin-bottom: 10px; }
.ask-msg.user { justify-content: flex-end; }
.ask-bubble {
  max-width: 86%; border-radius: 10px; padding: 8px 12px;
  font-size: 14px; line-height: 1.7; white-space: pre-wrap;
}
.ask-msg.user .ask-bubble { background: var(--kb-brand-grad); color: #fff; }
.ask-msg.assistant .ask-bubble { background: #f5f6f7; border: 1px solid var(--kb-line); }
/* 答案流式上屏时的打字光标 */
.ask-bubble.typing::after {
  content: ''; display: inline-block; width: 2px; height: 1em;
  margin-left: 2px; vertical-align: -2px; background: currentColor;
  animation: askCursor 0.9s steps(2) infinite;
}
@keyframes askCursor { 50% { opacity: 0; } }
.ask-input { display: flex; gap: 8px; align-items: flex-end; border-top: 1px solid var(--kb-line); padding-top: 10px; }
.ask-input .el-button { flex-shrink: 0; }

/* 移动端：边距收窄、标题缩小 */
@media (max-width: 768px) {
  .doc-body {
    padding: 24px 14px 80px;
  }
  .doc-title {
    font-size: 24px;
  }
}
</style>
