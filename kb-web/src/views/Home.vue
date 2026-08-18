<template>
  <div class="kb-page">
    <!-- 顶栏：logo | 图标胶囊导航 | 大搜索 | 图标按钮+用户下拉 -->
    <header class="nav">
      <div class="nav-left">
        <a class="menu-btn" @click="sideOpen = !sideOpen">
          <svg viewBox="0 0 16 16" width="18" height="18">
            <path d="M2 4h12M2 8h12M2 12h12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
          </svg>
        </a>
        <span class="logo">KB</span>
        <span class="site">知识库</span>
      </div>

      <!-- 功能导航：图标+文字胶囊 -->
      <nav class="nav-menu">
        <a class="nav-item" @click="router.push('/drill')">
          <svg viewBox="0 0 16 16"><path d="M2 2h5.2v5.2H2zM8.8 2H14v5.2H8.8zM2 8.8h5.2V14H2zM8.8 8.8H14V14H8.8z" fill="currentColor" opacity=".85"/></svg>
          <span class="nav-txt">刷题</span>
        </a>
        <a class="nav-item" @click="router.push('/interview')">
          <svg viewBox="0 0 16 16"><path d="M8 1.5a3 3 0 0 1 3 3v3a3 3 0 0 1-6 0v-3a3 3 0 0 1 3-3zM3.5 7.5h1a4 4 0 0 0 8 0h1a5 5 0 0 1-4.5 4.97V14h1.75a.75.75 0 0 1 0 1.5h-4.5a.75.75 0 0 1 0-1.5H8v-1.53A5 5 0 0 1 3.5 7.5z" fill="currentColor" opacity=".85"/></svg>
          <span class="nav-txt">面试</span>
        </a>
        <a class="nav-item" @click="router.push('/stats')">
          <svg viewBox="0 0 16 16"><path d="M2.5 7.5h2.4V14H2.5zM6.8 4h2.4v10H6.8zM11.1 1.5h2.4V14h-2.4z" fill="currentColor" opacity=".85"/></svg>
          <span class="nav-txt">看板</span>
        </a>
        <a class="nav-item" @click="router.push('/resume')">
          <svg viewBox="0 0 16 16"><path d="M3.5 2h9a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1h-9a1 1 0 0 1-1-1V3a1 1 0 0 1 1-1zm2 3a1.8 1.8 0 1 0 3.6 0 1.8 1.8 0 0 0-3.6 0zm.4 4.4c-1.2 0-2.4.5-2.4 1.7v.9h7.6V11c0-1.2-1.2-1.7-2.4-1.7z" fill="currentColor" opacity=".85"/></svg>
          <span class="nav-txt">简历</span>
        </a>
        <a v-if="isAdmin" class="nav-item manage-entry" @click="router.push('/manage')">
          <svg viewBox="0 0 16 16"><path d="M2.5 3A1.5 1.5 0 0 1 4 1.5h8A1.5 1.5 0 0 1 13.5 3v10a1.5 1.5 0 0 1-1.5 1.5H4A1.5 1.5 0 0 1 2.5 13zm1.5.2v9.6c0 .15.12.27.27.27H12V3H4.27a.27.27 0 0 0-.27.27zM5.5 5.5h5M5.5 8h5M5.5 10.5h3" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" opacity=".85"/></svg>
          <span class="nav-txt">题库</span>
        </a>
<a v-if="isAdmin" class="nav-item manage-entry" @click="router.push('/jobs')">
          <svg viewBox="0 0 16 16"><path d="M5 4.5V3a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v1.5M2.5 6A1.5 1.5 0 0 1 4 4.5h8A1.5 1.5 0 0 1 13.5 6v6A1.5 1.5 0 0 1 12 13.5H4A1.5 1.5 0 0 1 2.5 12zM2.5 8.5h11" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" opacity=".85"/></svg>
          <span class="nav-txt">岗位</span>
        </a>
        <a v-if="isAdmin" class="nav-item manage-entry" @click="router.push('/resumes')">
          <svg viewBox="0 0 16 16"><path d="M4 1.5h5.6a1 1 0 0 1 .7.3l2.4 2.4a1 1 0 0 1 .3.7v9.6a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1v-12a1 1 0 0 1 1-1z" fill="none" stroke="currentColor" stroke-width="1.3"/><circle cx="8" cy="6" r="1.6" fill="none" stroke="currentColor" stroke-width="1.2"/><path d="M5.4 11c.3-1.2 1.3-1.8 2.6-1.8s2.3.6 2.6 1.8" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
          <span class="nav-txt">简历审阅</span>
        </a>
      </nav>

      <div class="nav-right">
        <!-- 大胶囊搜索：灰底无描边，聚焦时亮底描边 -->
        <div class="search-wrap">
          <svg class="search-ico" viewBox="0 0 16 16"><path d="M7 2.5a4.5 4.5 0 1 0 2.82 8.01l3.08 3.08a.75.75 0 1 0 1.06-1.06l-3.08-3.08A4.5 4.5 0 0 0 7 2.5zM4 7a3 3 0 1 1 6 0 3 3 0 0 1-6 0z" fill="currentColor"/></svg>
          <el-autocomplete
            class="search"
            v-model="kw"
            :fetch-suggestions="querySearch"
            placeholder="搜索文档…"
            :debounce="300"
            clearable
            @select="onSelectSearch"
          />
        </div>

        <span class="nav-sep"></span>

        <!-- 历史/审核/我的提交：图标按钮，悬停提示 -->
        <a class="icon-btn" title="浏览历史" @click="openHistory">
          <svg viewBox="0 0 16 16"><circle cx="8" cy="8" r="6" fill="none" stroke="currentColor" stroke-width="1.4"/><path d="M8 5v3.2l2.2 1.4" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" fill="none"/></svg>
        </a>
        <a v-if="isAdmin" class="icon-btn" title="待审核" @click="openAudit">
          <svg viewBox="0 0 16 16"><path d="M8 1.5l5 2v4c0 3.2-2.1 5.6-5 7-2.9-1.4-5-3.8-5-7v-4z" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round"/><path d="M5.7 8l1.6 1.6 3-3.2" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </a>
        <a v-else class="icon-btn" title="我的提交" @click="openMine">
          <svg viewBox="0 0 16 16"><path d="M4 1.5h5.6a1 1 0 0 1 .7.3l2.4 2.4a1 1 0 0 1 .3.7v9.6a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1v-12a1 1 0 0 1 1-1z" fill="none" stroke="currentColor" stroke-width="1.3"/><path d="M5.5 7h5M5.5 9.5h5M5.5 12h3.5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
        </a>

        <!-- 用户信息收进头像下拉：角色+退出 -->
        <el-dropdown trigger="click" @command="c => c === 'logout' && logout()">
          <span class="user-chip">
            <span class="avatar">{{ (user.nickname || '?').slice(0, 1) }}</span>
            <span class="user-name">{{ user.nickname }}</span>
            <svg viewBox="0 0 16 16" width="12" height="12"><path d="M4 6l4 4 4-4" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>{{ roleLabel[user.role] || user.role }}</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <div class="body">
      <!-- 移动端遮罩：点它收起侧栏 -->
      <div v-if="sideOpen" class="mask" @click="sideOpen = false"></div>

      <!-- 左侧栏：知识库信息 + 目录树 -->
      <aside :class="['sidebar', { open: sideOpen }]">
        <div class="book-head">
          <el-select v-model="kbId" size="default" class="kb-select" @change="onKbChange">
            <el-option
              v-for="kb in kbs"
              :key="kb.id"
              :label="kb.status === 1 ? kb.name : kb.name + '（' + statusLabel[kb.status] + '）'"
              :value="kb.id"
            />
          </el-select>
          <div class="book-desc">{{ currentKbDesc }}</div>
        </div>

        <div class="side-actions">
          <a @click="createKb">+ 知识库</a>
          <a v-if="isAdmin" class="danger" @click="deleteKb">删库</a>
          <a @click="createFolder">+ 文件夹</a>
          <a class="primary" @click="createDoc">+ 新文档</a>
          <a v-if="isAdmin" class="danger" @click="deleteNode">删节点</a>
          <a v-if="isAdmin" @click="reindex">重建索引</a>
        </div>

        <el-tree
          class="toc"
          :data="tree"
          node-key="id"
          :props="{ label: 'title', children: 'children' }"
          highlight-current
          @node-click="onNodeClick"
        >
          <template #default="{ data }">
            <span class="node">
              <!-- 文件夹图标 -->
              <svg v-if="data.nodeType === 'FOLDER'" class="ico folder" viewBox="0 0 16 16">
                <path d="M1.5 3.5a1 1 0 0 1 1-1h3.4a1 1 0 0 1 .8.4l.9 1.1h5.9a1 1 0 0 1 1 1v7.5a1 1 0 0 1-1 1h-11a1 1 0 0 1-1-1v-9z" fill="currentColor" opacity="0.55"/>
              </svg>
              <!-- 文档图标 -->
              <svg v-else class="ico doc" viewBox="0 0 16 16">
                <path d="M4 1.5h5.6a1 1 0 0 1 .7.3l2.4 2.4a1 1 0 0 1 .3.7v9.6a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1v-12a1 1 0 0 1 1-1z" fill="none" stroke="currentColor" stroke-width="1.2"/>
                <path d="M5.5 7h5M5.5 9.5h5M5.5 12h3.5" stroke="currentColor" stroke-width="1.1" stroke-linecap="round"/>
              </svg>
              <span class="node-title">{{ data.title }}</span>
            </span>
          </template>
        </el-tree>
      </aside>

      <!-- 主区域 -->
      <main class="main">
        <DocEditor v-if="currentDocId" :key="currentDocId" :doc-id="currentDocId" />

        <!-- 未选文档：工作台首页，快捷入口卡片（企业工作台式） -->
        <div v-else class="home-board">
          <div class="board-hi">
            <h2>{{ greeting }}，{{ user.nickname }}</h2>
            <p>今天想沉淀点什么？从左侧目录选一篇文档，或直接进入下面的工作台</p>
          </div>
          <div class="board-grid">
            <div v-for="q in quickNav" :key="q.path" class="board-card" @click="go(q.path)">
              <span class="q-ico" :style="{ background: q.tint, color: q.color }">
                <svg viewBox="0 0 16 16" width="17" height="17"><path :d="q.icon" fill="currentColor" /></svg>
              </span>
              <span class="q-txt">
                <b>{{ q.name }}</b>
                <small>{{ q.desc }}</small>
              </span>
              <span class="q-arrow">›</span>
            </div>
          </div>
        </div>
      </main>
    </div>

    <!-- 浏览历史抽屉：每人只看自己的，可删可翻更早 -->
    <el-drawer v-model="showHistory" title="浏览历史" size="320px">
      <div class="history-bar">
        <span>共 {{ historyTotal }} 条</span>
        <a v-if="historyList.length" class="history-clear" @click="clearHistory">清空</a>
      </div>
      <div v-if="!historyList.length" class="history-empty">还没有浏览记录，去看几篇吧</div>
      <div v-for="h in historyList" :key="h.id" class="history-item">
        <div class="history-main" @click="openDoc(h.docId, h.kbId)">
          <div class="history-title">{{ h.title }}</div>
          <div class="history-time">{{ (h.viewTime || '').replace('T', ' ').slice(0, 16) }}</div>
        </div>
        <a class="history-del" title="删除这条" @click="delHistory(h)">×</a>
      </div>
      <div v-if="historyList.length < historyTotal" class="history-more">
        <el-button link @click="loadMoreHistory">加载更早</el-button>
      </div>
    </el-drawer>

    <!-- 管理员审核队列 -->
    <el-drawer v-model="showAudit" title="待审核" size="360px">
      <h4 class="audit-h">知识库</h4>
      <div v-if="!auditData.bases.length" class="history-empty">暂无</div>
      <div v-for="b in auditData.bases" :key="'b' + b.id" class="history-item">
        <div class="history-title">{{ b.name }}</div>
        <div class="history-time">{{ b.description || '暂无简介' }}</div>
        <div class="audit-ops">
          <el-button size="small" link @click="viewKb(b.id)">查看</el-button>
          <el-button size="small" type="primary" @click="doAudit('base', b.id, true)">通过</el-button>
          <el-button size="small" type="danger" @click="doAudit('base', b.id, false)">驳回</el-button>
        </div>
      </div>
      <h4 class="audit-h">文件夹</h4>
      <div v-if="!auditData.folders.length" class="history-empty">暂无</div>
      <div v-for="f in auditData.folders" :key="'f' + f.id" class="history-item">
        <div class="history-title">{{ f.title }}</div>
        <div class="audit-ops">
          <el-button size="small" type="primary" @click="doAudit('folder', f.id, true)">通过</el-button>
          <el-button size="small" type="danger" @click="doAudit('folder', f.id, false)">驳回</el-button>
        </div>
      </div>
      <h4 class="audit-h">文档</h4>
      <div v-if="!auditData.docs.length" class="history-empty">暂无</div>
      <div v-for="d in auditData.docs" :key="'d' + d.id" class="history-item">
        <div class="history-title">{{ d.title }}</div>
        <div class="audit-ops">
          <el-button size="small" link @click="openDoc(d.id, d.kbId)">查看</el-button>
          <el-button size="small" type="primary" @click="doAudit('doc', d.id, true)">通过</el-button>
          <el-button size="small" type="danger" @click="doAudit('doc', d.id, false)">驳回</el-button>
        </div>
      </div>
    </el-drawer>

    <!-- 成员我的提交 -->
    <el-drawer v-model="showMine" title="我的提交" size="320px">
      <div v-if="!mineList.length" class="history-empty">没有待审核或被驳回的提交</div>
      <div v-for="s in mineList" :key="s.type + s.id" class="history-item">
        <div class="history-main" @click="s.type === '知识库' ? viewKb(s.kbId) : openDoc(s.id, s.kbId)">
          <div class="history-title">{{ s.title }}</div>
          <div class="history-time">
            {{ s.type }} · {{ statusLabel[s.status] }} · {{ (s.createTime || '').replace('T', ' ').slice(0, 16) }}
          </div>
        </div>
        <!-- 待审核/草稿的文档，本人可撤回 -->
        <el-button
          v-if="s.type === '文档' && (s.status === 0 || s.status === 3)"
          link type="danger" @click="withdraw(s)">撤回</el-button>
      </div>
    </el-drawer>

    <!-- 新建文档：选内容来源（编辑器写 / 传文件） -->
    <el-dialog v-model="showCreateDoc" title="新建文档" width="420px">
      <el-form label-position="top">
        <el-form-item label="标题">
          <el-input v-model="createDocTitle" placeholder="文档标题" />
        </el-form-item>
        <el-form-item label="内容来源">
          <el-radio-group v-model="createDocSource">
            <el-radio value="editor">编辑器里写</el-radio>
            <el-radio value="file">上传文件</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="createDocSource === 'file'" label="文件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.md,.txt,.zip,.rar,.7z,.png,.jpg,.jpeg,.gif,.webp"
            :on-change="onPickFile"
            :on-remove="() => (createDocFile = null)">
            <el-button type="primary" plain>选择文件</el-button>
            <template #tip>
              <div class="upload-tip">支持 pdf/word/ppt/excel/md/txt/zip/rar/7z/图片，单文件≤50MB</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDoc = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="confirmCreateDoc">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http.js'
import DocEditor from '../components/DocEditor.vue'

const router = useRouter()
const route = useRoute()
const user = JSON.parse(localStorage.getItem('kb-user') || '{"nickname":"","role":""}')
const roleLabel = { ADMIN: '管理员', MEMBER: '成员', VIEWER: '访客' }

// 工作台快捷入口：未选文档时展示
const quickNav = [
  { name: 'AI 刷题', desc: '按科目抽题，答完立即批改', path: '/drill',
    icon: 'M2 2h5v5H2zM9 2h5v5H9zM2 9h5v5H2zM9 9h5v5H9z', color: '#00a562', tint: 'rgba(0,185,107,.12)' },
  { name: '模拟面试', desc: 'AI 面试官提问 + 实时点评', path: '/interview',
    icon: 'M8 1.5a3.5 3.5 0 0 1 3.5 3.5v2.5a3.5 3.5 0 0 1-7 0V5A3.5 3.5 0 0 1 8 1.5zM3.5 7.5h1a4.5 4.5 0 0 0 9 0h1a5.5 5.5 0 0 1-5 5.47V14.5h2a.75.75 0 0 1 0 1.5h-5a.75.75 0 0 1 0-1.5h2v-1.53a5.5 5.5 0 0 1-5-5.47z', color: '#3370ff', tint: 'rgba(51,112,255,.12)' },
  { name: '成长看板', desc: '刷题与面试的数据足迹', path: '/stats',
    icon: 'M2.5 7h2.4v6.5H2.5zM6.8 4h2.4v9.5H6.8zM11.1 2h2.4v11.5h-2.4z', color: '#8a63d2', tint: 'rgba(138,99,210,.12)' },
  { name: '简历助手', desc: 'AI 分析、查缺补漏、一键生成', path: '/resume',
    icon: 'M4 1.5h8a1 1 0 0 1 1 1v11a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1v-11a1 1 0 0 1 1-1zm2 3a2 2 0 1 0 4 0 2 2 0 0 0-4 0zm.5 5c-1.2 0-2.5.5-2.5 1.7V12h8v-.8c0-1.2-1.3-1.7-2.5-1.7z', color: '#f5803d', tint: 'rgba(245,128,61,.12)' }
]
const go = p => router.push(p)
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '上午好'
  if (h < 18) return '下午好'
  return '晚上好'
})
// 删除类操作只给管理员看；后端同样校验，前端隐藏只是体验层
const isAdmin = computed(() => user.role === 'ADMIN')

const kbs = ref([])
const kbId = ref(null)
const tree = ref([])
const current = ref(null)
const currentDocId = ref(null)
const kw = ref('')
// 移动端侧栏抽屉开关
const sideOpen = ref(false)
// 浏览历史抽屉
const showHistory = ref(false)
const historyList = ref([])
const historyPage = ref(1)
const historyTotal = ref(0)

/** 搜索建议：调/doc/search，映射成el-autocomplete要的{value}结构 */
async function querySearch(q, cb) {
  if (!q) return cb([])
  const list = await http.get(`/doc/search?keyword=${encodeURIComponent(q)}`)
  cb(list.map(d => ({ value: d.title, id: d.id, kbId: d.kbId })))
}

/** 公共跳转：必要时切知识库，然后打开文档；移动端收起侧栏 */
async function openDoc(docId, targetKbId) {
  if (targetKbId !== kbId.value) {
    kbId.value = targetKbId
    current.value = null
    await loadTree()
  }
  currentDocId.value = docId
  showHistory.value = false
  showAudit.value = false
  if (window.innerWidth <= 768) sideOpen.value = false
}

/** 审核时查看知识库：切过去看，关掉抽屉 */
function viewKb(id) {
  kbId.value = id
  onKbChange()
  showAudit.value = false
}

/** 点搜索结果：跳过去 */
async function onSelectSearch(item) {
  kw.value = ''
  await openDoc(item.id, item.kbId)
}

/** 打开历史抽屉：第一页 */
async function openHistory() {
  showHistory.value = true
  historyPage.value = 1
  const r = await http.get('/history?page=1&size=20')
  historyList.value = r.list
  historyTotal.value = r.total
}

/** 加载更早的一页，拼到列表后面 */
async function loadMoreHistory() {
  historyPage.value++
  const r = await http.get(`/history?page=${historyPage.value}&size=20`)
  historyList.value = historyList.value.concat(r.list)
  historyTotal.value = r.total
}

/** 删一条历史 */
async function delHistory(h) {
  await http.delete(`/history/${h.id}`)
  historyList.value = historyList.value.filter(x => x.id !== h.id)
  historyTotal.value--
}

/** 清空全部历史 */
async function clearHistory() {
  try {
    await ElMessageBox.confirm('清空全部浏览历史？', '清空', { type: 'warning' })
  } catch {
    return
  }
  await http.delete('/history')
  historyList.value = []
  historyTotal.value = 0
}

// ===== 审核 / 我的提交 =====
const statusLabel = { 0: '待审核', 1: '已通过', 2: '已驳回', 3: '草稿' }
const showAudit = ref(false)
const auditData = ref({ bases: [], folders: [], docs: [] })
const showMine = ref(false)
const mineList = ref([])

/** 管理员打开审核队列 */
async function openAudit() {
  showAudit.value = true
  auditData.value = await http.get('/audit/pending')
}

/** 成员打开我的提交 */
async function openMine() {
  showMine.value = true
  mineList.value = await http.get('/audit/mine')
}

/** 通过/驳回后刷新队列和目录树 */
async function doAudit(type, id, ok) {
  await http.post(`/audit/${type}/${id}/${ok ? 'approve' : 'reject'}`)
  ElMessage.success(ok ? '已通过' : '已驳回')
  auditData.value = await http.get('/audit/pending')
  loadTree()
}

const currentKbName = computed(() => kbs.value.find(k => k.id === kbId.value)?.name || '')
const currentKbDesc = computed(() => kbs.value.find(k => k.id === kbId.value)?.description || '暂无简介')

onMounted(async () => {
  kbs.value = await http.get('/base/list')
  if (kbs.value.length) {
    // 支持?kb=xx&doc=xx直达（刷题错题跳教材用）
    const qKb = Number(route.query.kb)
    kbId.value = qKb && kbs.value.some(k => k.id === qKb) ? qKb : kbs.value[0].id
    onKbChange()
    if (route.query.doc) {
      currentDocId.value = Number(route.query.doc)
    }
  }
})

function onKbChange() {
  currentDocId.value = null
  current.value = null
  if (!kbId.value) {
    tree.value = []
    return
  }
  loadTree()
}

async function loadTree() {
  tree.value = await http.get(`/catalog/tree/${kbId.value}`)
}

function onNodeClick(data) {
  current.value = data
  currentDocId.value = data.nodeType === 'DOC' ? data.docId : null
  // 手机上选中文档后收起抽屉，露出正文
  if (data.nodeType === 'DOC' && window.innerWidth <= 768) {
    sideOpen.value = false
  }
}

/** 一键重建全部已审核文档的向量索引：后台异步跑，教材问答/出题检索会更准 */
async function reindex() {
  try {
    await ElMessageBox.confirm(
      '重建全部已审核文档的向量索引？后台异步执行，完成后AI问答和出题检索会更准确。',
      '重建索引',
      { type: 'info' }
    )
  } catch {
    return
  }
  const msg = await http.post('/doc/reindex')
  ElMessage.success(msg || '已开始重建')
}

async function createKb() {
  const { value } = await ElMessageBox.prompt('知识库名称', '新建知识库')
  if (!value) return
  await http.post('/base', { name: value })
  ElMessage.success(isAdmin.value ? '已创建' : '已提交，待管理员审核')
  kbs.value = await http.get('/base/list')
  kbId.value = kbs.value[0].id
  onKbChange()
}

/** 删除当前知识库：后端只放行本人或管理员，级联清目录/文档/文件记录 */
async function deleteKb() {
  if (!kbId.value) return
  try {
    await ElMessageBox.confirm(
      `删除「${currentKbName.value}」？库内目录、文档、文件记录会一并删除，不可恢复。`,
      '删除知识库',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return // 点取消，静默返回
  }
  await http.delete(`/base/${kbId.value}`)
  ElMessage.success('已删除')
  kbs.value = await http.get('/base/list')
  kbId.value = kbs.value.length ? kbs.value[0].id : null
  onKbChange()
}

function currentParentId() {
  return current.value && current.value.nodeType === 'FOLDER' ? current.value.id : 0
}

async function createFolder() {
  const selectedFolder = current.value && current.value.nodeType === 'FOLDER' ? current.value : null
  // 库里已有目录时必须选中父文件夹，避免"建完不知道在哪"；
  // 唯一例外：空库的第一个文件夹，放行建在根目录搭第一层架子
  if (!selectedFolder && tree.value.length > 0) {
    ElMessage.warning('请先选中父文件夹，新文件夹会建在它下面')
    return
  }
  const { value } = await ElMessageBox.prompt('文件夹名称', '新建文件夹')
  if (!value) return
  await http.post('/catalog', { kbId: kbId.value, parentId: selectedFolder ? selectedFolder.id : 0, title: value })
  if (isAdmin.value) {
    loadTree()
  } else {
    ElMessage.success('已提交，待管理员审核')
  }
}

// ===== 新建文档对话框 =====
const showCreateDoc = ref(false)
const createDocTitle = ref('')
const createDocSource = ref('editor')
const createDocFile = ref(null)
const creating = ref(false)
const uploadRef = ref(null)
const ALLOWED_EXT = ['pdf', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx', 'md', 'txt', 'zip', 'rar', '7z', 'png', 'jpg', 'jpeg', 'gif', 'webp']

/** 选文件时先验格式：不合规提示并清掉，和后端白名单一致 */
function onPickFile(f) {
  const ext = (f.name.split('.').pop() || '').toLowerCase()
  if (!ALLOWED_EXT.includes(ext)) {
    ElMessage.warning('不支持的格式：仅支持 pdf/word/ppt/excel/md/txt/zip/rar/7z/图片')
    createDocFile.value = null
    uploadRef.value?.clearFiles()
    return
  }
  createDocFile.value = f.raw
}

function createDoc() {
  // 必须先选中文件夹：文档永远挂在明确目录下，避免"建完不知道在哪"
  if (!current.value || current.value.nodeType !== 'FOLDER') {
    ElMessage.warning('请先在左侧选中一个文件夹，文档会建在它下面')
    return
  }
  createDocTitle.value = ''
  createDocSource.value = 'editor'
  createDocFile.value = null
  showCreateDoc.value = true
}

async function confirmCreateDoc() {
  if (!createDocTitle.value.trim()) return ElMessage.warning('请输入标题')
  if (createDocSource.value === 'file' && !createDocFile.value) return ElMessage.warning('请选择文件')
  creating.value = true
  try {
    const doc = await http.post('/doc', {
      kbId: kbId.value, parentId: current.value.id, title: createDocTitle.value.trim()
    })
    if (createDocSource.value === 'file') {
      // 文件作为文档内容：上传附件后成员自动提交审核
      const fd = new FormData()
      fd.append('file', createDocFile.value)
      await http.post(`/file/upload?kbId=${kbId.value}&docId=${doc.id}`, fd)
      showCreateDoc.value = false
      if (isAdmin.value) {
        await loadTree()
        ElMessage.success('文件文档已创建')
      } else {
        await http.post(`/doc/${doc.id}/submit`)
        ElMessage.success('已上传并提交审核，在"我的提交"可撤回')
      }
    } else {
      showCreateDoc.value = false
      currentDocId.value = doc.id
      if (isAdmin.value) {
        await loadTree()
      } else {
        ElMessage.success('草稿已创建，写完内容后点右上角"提交审核"')
      }
    }
  } finally {
    creating.value = false
  }
}

/** 撤回自己待审核/草稿的文档（连附件一起删） */
async function withdraw(s) {
  try {
    await ElMessageBox.confirm(`撤回「${s.title}」？附件也会一并删除。`, '撤回', { type: 'warning' })
  } catch {
    return
  }
  await http.delete(`/doc/${s.id}`)
  ElMessage.success('已撤回')
  openMine()
}

async function deleteNode() {
  if (!current.value) return ElMessage.warning('先在左侧选中一个节点')
  if (current.value.nodeType === 'DOC') {
    await http.delete(`/doc/${current.value.docId}`)
  } else {
    await http.delete(`/catalog/${current.value.id}`)
  }
  current.value = null
  currentDocId.value = null
  loadTree()
}

function logout() {
  localStorage.removeItem('kb-token')
  localStorage.removeItem('kb-user')
  router.push('/login')
}
</script>

<style scoped>
.kb-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 顶栏：白底 + 细影，浮在内容上 */
.nav {
  height: 60px;
  background: #fff;
  border-bottom: 1px solid var(--kb-line);
  box-shadow: var(--kb-shadow-sm);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
  position: relative;
  z-index: 10;
}
.nav-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.logo {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: var(--kb-brand-grad);
  box-shadow: 0 2px 6px rgba(0, 185, 107, .3);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
.site {
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
}
.nav-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 功能导航：图标+文字胶囊，悬停品牌浅绿底 */
.nav-menu { display: flex; gap: 4px; }
.nav-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--kb-ink-2);
  padding: 6px 13px;
  border-radius: 999px;
  cursor: pointer;
  transition: var(--kb-trans);
}
.nav-item svg { width: 15px; height: 15px; flex-shrink: 0; }
.nav-item:hover {
  color: var(--el-color-primary-dark-2);
  background: var(--kb-side-active);
}

/* 大胶囊搜索：灰底无描边 + 内置图标，聚焦时白底描边 */
.search-wrap { position: relative; width: 270px; }
.search-ico {
  position: absolute; left: 13px; top: 50%; transform: translateY(-50%);
  width: 14px; height: 14px; color: var(--kb-ink-3);
  pointer-events: none; z-index: 1;
}
.search { width: 100%; }
.search :deep(.el-input__wrapper) {
  background: #f2f3f5;
  border-radius: 999px;
  box-shadow: none;
  padding-left: 34px;
  transition: var(--kb-trans);
}
.search :deep(.el-input__wrapper:hover) { background: #ebedef; }
.search :deep(.el-input__wrapper.is-focus) {
  background: #fff;
  box-shadow: 0 0 0 1px var(--el-color-primary) inset, 0 2px 8px rgba(0, 185, 107, .1) !important;
}

.nav-sep { width: 1px; height: 18px; background: var(--kb-line); margin: 0 2px; }

/* 图标按钮：历史/审核/我的提交 */
.icon-btn {
  width: 32px; height: 32px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 8px;
  color: var(--kb-ink-2);
  cursor: pointer;
  transition: var(--kb-trans);
}
.icon-btn svg { width: 16px; height: 16px; }
.icon-btn:hover { background: var(--kb-side-hover); color: var(--kb-ink); }

/* 用户下拉触发器：头像+昵称胶囊 */
.user-chip {
  display: inline-flex; align-items: center; gap: 7px;
  padding: 4px 10px 4px 4px;
  border-radius: 999px;
  cursor: pointer;
  transition: var(--kb-trans);
  color: var(--kb-ink-3);
}
.user-chip:hover { background: var(--kb-side-hover); }
.user-name { font-size: 13px; color: var(--kb-ink-2); font-weight: 500; }

/* 主体两栏 */
.body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 侧栏 */
.sidebar {
  width: 268px;
  background: #fafbfc;
  border-right: 1px solid var(--kb-line);
  padding: 16px 12px;
  overflow-y: auto;
  flex-shrink: 0;
}
.book-head {
  padding: 0 6px 12px;
  border-bottom: 1px solid var(--kb-line);
  margin-bottom: 10px;
}
.kb-select {
  width: 100%;
}
.book-desc {
  margin-top: 8px;
  font-size: 12px;
  color: var(--kb-ink-3);
  line-height: 1.6;
}
.side-actions {
  display: flex;
  gap: 12px;
  padding: 0 6px 12px;
}
.side-actions a {
  font-size: 13px;
  color: var(--kb-ink-2);
  cursor: pointer;
}
.side-actions a:hover {
  color: #00b96b;
}
.side-actions a.primary {
  color: #00b96b;
}
.side-actions a.danger:hover {
  color: #f56c6c;
}

/* 目录树节点 */
.toc {
  padding: 0 2px;
}
.node {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  color: var(--kb-ink-2);
  overflow: hidden;
}
.node-title {
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
}
.ico {
  width: 15px;
  height: 15px;
  flex-shrink: 0;
}
.ico.folder,
.ico.doc {
  color: var(--kb-ico);
}
.el-tree-node.is-current .ico {
  color: var(--kb-ink);
}

/* 主区域 */
.main {
  flex: 1;
  overflow-y: auto;
  background: #fff;
}

/* 上传格式提示 */
.upload-tip {
  font-size: 12px;
  color: var(--kb-ink-3);
  line-height: 1.6;
  margin-top: 4px;
}

/* 工作台首页：问候语 + 快捷入口卡片 */
.home-board {
  max-width: 880px;
  margin: 0 auto;
  padding: 56px 24px 40px;
  background:
    radial-gradient(700px 320px at 50% 0%, rgba(0, 185, 107, .06), transparent),
    #fff;
  min-height: 100%;
}
.board-hi h2 { font-size: 26px; font-weight: 700; margin: 0 0 8px; letter-spacing: .5px; }
.board-hi p { color: var(--kb-ink-3); font-size: 14px; margin: 0 0 32px; }
.board-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.board-card {
  display: flex; align-items: center; gap: 14px;
  background: #fff; border: 1px solid var(--kb-line); border-radius: var(--kb-radius);
  padding: 20px;
  cursor: pointer;
  transition: var(--kb-trans);
  box-shadow: var(--kb-shadow-sm);
}
.board-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--kb-shadow-md);
  border-color: var(--el-color-primary-light-7);
}
.q-ico {
  width: 42px; height: 42px; border-radius: 11px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.q-txt { display: flex; flex-direction: column; gap: 3px; min-width: 0; }
.q-txt b { font-size: 15px; }
.q-txt small { font-size: 12px; color: var(--kb-ink-3); }
.q-arrow {
  margin-left: auto; color: var(--kb-ink-3); font-size: 20px; line-height: 1;
  opacity: 0; transition: var(--kb-trans);
}
.board-card:hover .q-arrow { opacity: 1; color: var(--el-color-primary-dark-2); }
@media (max-width: 768px) {
  .board-grid { grid-template-columns: 1fr; }
  .home-board { padding-top: 32px; }
}

/* 汉堡菜单按钮：桌面隐藏，移动端显示 */
.menu-btn {
  display: none;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  color: var(--kb-ink-2);
  cursor: pointer;
}
.menu-btn:hover {
  background: var(--kb-side-hover);
}

/* 用户头像：首字品牌渐变圆徽 */
.avatar {
  width: 26px; height: 26px; border-radius: 50%;
  background: var(--kb-brand-grad); color: #fff;
  font-size: 12px; font-weight: 600;
  display: inline-flex; align-items: center; justify-content: center;
  box-shadow: 0 2px 5px rgba(0, 185, 107, .28);
  flex-shrink: 0;
}

/* 历史列表项 */
.history-empty {
  color: var(--kb-ink-3);
  font-size: 13px;
  text-align: center;
  padding: 40px 0;
}
.history-item {
  padding: 10px 8px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.history-item:hover {
  background: var(--kb-side-hover);
}
.history-main {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}
.history-del {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--kb-ink-3);
  font-size: 16px;
  border-radius: 50%;
  cursor: pointer;
  opacity: 0;
}
.history-item:hover .history-del {
  opacity: 1;
}
.history-del:hover {
  color: #f56c6c;
  background: #fef0f0;
}
.history-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--kb-ink-3);
  margin-bottom: 8px;
}
.history-clear {
  color: var(--kb-ink-2);
  cursor: pointer;
}
.history-clear:hover {
  color: #f56c6c;
}
.history-more {
  text-align: center;
  margin-top: 8px;
}
.history-title {
  font-size: 14px;
  color: var(--kb-ink);
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
}
.history-time {
  font-size: 12px;
  color: var(--kb-ink-3);
  margin-top: 2px;
}

/* 审核抽屉 */
.audit-h {
  margin: 12px 0 6px;
  font-size: 13px;
  color: var(--kb-ink-3);
}
.audit-ops {
  margin-top: 6px;
  display: flex;
  gap: 8px;
}

/* 移动端遮罩 */
.mask {
  display: none;
}

/* ===== 移动端适配（<=768px）===== */
@media (max-width: 768px) {
  .menu-btn {
    display: inline-flex;
  }
  /* 侧栏变抽屉：固定定位，默认滑出屏幕，open时滑入 */
  .sidebar {
    position: fixed;
    top: 60px;
    bottom: 0;
    left: 0;
    z-index: 100;
    width: 280px;
    transform: translateX(-100%);
    transition: transform 0.25s ease;
    box-shadow: none;
  }
  .sidebar.open {
    transform: translateX(0);
    box-shadow: 0 0 24px rgba(0, 0, 0, 0.15);
  }
  .mask {
    display: block;
    position: fixed;
    inset: 60px 0 0 0;
    z-index: 99;
    background: rgba(0, 0, 0, 0.3);
  }
  /* 顶栏：导航只留图标、搜索收窄、昵称隐藏 */
  .nav {
    padding: 0 12px;
    gap: 8px;
  }
  .nav-txt { display: none; }
  .nav-item { padding: 6px 9px; }
  .search-wrap {
    width: auto;
    flex: 1;
    min-width: 70px;
  }
  .user-name {
    display: none;
  }
  .site {
    display: none;
  }
  .manage-entry {
    display: none;
  }
  /* 操作链接换行不挤 */
  .side-actions {
    flex-wrap: wrap;
    gap: 10px;
  }
}
</style>
