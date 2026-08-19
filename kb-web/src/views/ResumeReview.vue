<template>
  <div class="rv-page">
    <header class="rv-top">
      <h2>简历审阅</h2>
      <div class="rv-ops">
        <el-select v-model="filterStatus" placeholder="全部状态" clearable style="width: 160px" @change="reload">
          <el-option label="待审阅" :value="1" />
          <el-option label="已驳回" :value="2" />
          <el-option label="已推荐" :value="3" />
        </el-select>
        <el-select v-model="filterEdu" placeholder="全部学历" clearable style="width: 120px" @change="reload">
          <el-option v-for="d in ['博士', '硕士', '本科', '大专', '其他']" :key="d" :label="d" :value="d" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜姓名/标题/目标岗位" clearable style="width: 200px"
                  @keyup.enter="reload" @clear="reload" />
        <el-button @click="reload">查询</el-button>
        <el-button link @click="$router.push('/')">返回首页</el-button>
      </div>
    </header>

    <!-- 统计总览：提交/推荐情况、AI均分、学历城市分布 -->
    <div class="rv-stats" v-if="stats">
      <div class="stat"><b>{{ stats.total }}</b><span>简历总数</span></div>
      <div class="stat"><b>{{ stats.pending }}</b><span>待审阅</span></div>
      <div class="stat"><b>{{ stats.assigned }}</b><span>已推荐</span></div>
      <div class="stat"><b>{{ stats.returned }}</b><span>已驳回</span></div>
      <div class="stat"><b>{{ stats.avgScore }}</b><span>AI均分</span></div>
      <div class="dist" v-if="stats.byEducation?.length">
        <span class="dist-t">学历</span>
        <el-tag v-for="d in stats.byEducation" :key="d.name" size="small" effect="plain" class="dist-tag">
          {{ d.name }} {{ d.count }}</el-tag>
      </div>
      <div class="dist" v-if="stats.byCity?.length">
        <span class="dist-t">城市</span>
        <el-tag v-for="c in stats.byCity" :key="c.name" size="small" type="info" effect="plain" class="dist-tag">
          {{ c.name }} {{ c.count }}</el-tag>
      </div>
    </div>

    <el-table :data="list" v-loading="loading" :header-cell-style="{ background: '#fafafa', color: '#595959' }">
      <el-table-column label="求职者" width="130">
        <template #default="{ row }">
          <div class="rv-name">{{ row.resume.name || '—' }}</div>
          <div class="rv-sub">{{ row.nickname }}</div>
        </template>
      </el-table-column>
      <el-table-column label="简历" min-width="170" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="rv-title">{{ row.resume.title }}</div>
          <div class="rv-sub">{{ row.resume.targetJob || '未设目标岗位' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="电话" width="120">
        <template #default="{ row }">{{ row.resume.phone || '—' }}</template>
      </el-table-column>
      <el-table-column label="城市" width="80">
        <template #default="{ row }">{{ row.resume.city || '—' }}</template>
      </el-table-column>
      <el-table-column label="学历" width="70">
        <template #default="{ row }">{{ row.resume.education || '—' }}</template>
      </el-table-column>
      <el-table-column label="年限" width="65">
        <template #default="{ row }">{{ row.resume.workYears == null ? '—' : row.resume.workYears + '年' }}</template>
      </el-table-column>
      <el-table-column label="AI分" width="60">
        <template #default="{ row }">
          <b :class="scoreClass(row.resume.aiScore)">{{ row.resume.aiScore ?? '—' }}</b>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="statusType[row.resume.submitStatus] || 'info'">
            {{ statusLabel[row.resume.submitStatus] || '未提交' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="意向/推荐岗位" min-width="170" show-overflow-tooltip>
        <template #default="{ row }">
          <template v-if="row.recommendedJobs?.length">
            <el-tag v-for="t in row.recommendedJobs.slice(0, 2)" :key="t" class="rec-tag"
                    size="small" type="success" effect="dark">→ {{ t }}</el-tag>
            <span v-if="row.recommendedJobs.length > 2" class="rec-more">+{{ row.recommendedJobs.length - 2 }}</span>
          </template>
          <div v-else-if="row.appliedJob" class="rv-sub">意向：{{ row.appliedJob }}</div>
          <span v-else class="rv-sub">—</span>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="140">
        <template #default="{ row }">{{ fmt(row.resume.submitTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <el-button link @click="openDetail(row)">详情</el-button>
          <!-- 已驳回是终态：不能再通过（推荐），等用户改完重新提交再说 -->
          <el-button v-if="row.resume.submitStatus === 1 || row.resume.submitStatus === 3"
                     link type="primary" @click="openAssign(row)">推荐岗位</el-button>
          <el-button v-if="row.resume.submitStatus === 1"
                     link type="danger" @click="sendBack(row)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="rv-pager" v-model:current-page="page" :page-size="size" :total="total"
                   layout="prev, pager, next, total" @current-change="load" />

    <!-- 简历详情：完整A4简历页预览（和用户导出PDF同一套排版） -->
    <el-dialog v-model="showDetail" :title="detail ? ((detail.name || '简历') + ' - 完整简历') : '简历详情'"
               width="920px" top="4vh">
      <template v-if="detail">
        <div class="detail-meta">
          <el-tag size="small" :type="statusType[detail.submitStatus] || 'info'">
            {{ statusLabel[detail.submitStatus] || '未提交' }}</el-tag>
          <span v-if="detail.targetJob">目标岗位：{{ detail.targetJob }}</span>
          <span v-if="detail.education">{{ detail.education }}</span>
          <span v-if="detail.workYears != null">{{ detail.workYears }}年经验</span>
          <span v-if="detail.aiScore != null">AI评分 {{ detail.aiScore }}</span>
        </div>
        <div v-if="detail.remark && detail.submitStatus === 2" class="detail-remark">驳回理由：{{ detail.remark }}</div>
        <iframe class="detail-frame" :srcdoc="detailHtml"></iframe>
      </template>
      <template #footer>
        <el-button @click="showDetail = false">关闭</el-button>
        <el-button v-if="detail && detail.submitStatus === 1"
                   type="danger" plain @click="sendBack(detailRow)">驳回</el-button>
        <el-button v-if="detail && (detail.submitStatus === 1 || detail.submitStatus === 3)"
                   type="primary" @click="openAssign(detailRow)">推荐岗位</el-button>
      </template>
    </el-dialog>

    <!-- 推荐岗位：同类岗位归成分类，点分类看卡片，卡片上直接显示公司/薪资/地点/要求/JD -->
    <el-dialog v-model="showAssign" :title="`为「${assignFor?.resume?.name || assignFor?.nickname || ''}」推荐岗位`"
               width="840px" top="5vh">
      <div v-if="!jobOptions.length" class="assign-empty">暂无可选岗位，先去爬取或手动录入</div>
      <template v-else>
        <!-- 简历画像：推荐依据一目了然 -->
        <div v-if="matchResume" class="rec-summary">
          <b>按简历匹配：</b>
          <span v-if="matchResume.targetJob">目标岗位 {{ matchResume.targetJob }}</span>
          <span v-if="matchResume.education">{{ matchResume.education }}</span>
          <span v-if="matchResume.workYears != null">{{ matchResume.workYears }}年经验</span>
          <span>{{ matchResume.tokens.length }}项技能</span>
          <span class="rec-n">→ 推荐 {{ recJobs.length }} 个合适岗位</span>
        </div>
        <!-- 已推荐清单：点×可撤，可继续从下方追加 -->
        <div class="rec-bar">
          <span class="rec-bar-label">已推荐 {{ recommendedIds.length }} 个：</span>
          <template v-if="recommendedIds.length">
            <span v-for="j in recommendedJobObjs" :key="j.id" class="rec-chip">
              {{ j.title }}<i @click.stop="unassign(j)">×</i>
            </span>
          </template>
          <span v-else class="rec-bar-none">还没推荐，点下方卡片上的"推荐"加入，可推多个</span>
        </div>
        <!-- 岗位搜索：搜岗位名/公司/技能/JD，搜索时忽略分类直接全局找 -->
        <div class="job-search-row">
          <el-input v-model="jobSearch" class="job-search" clearable
                    placeholder="搜岗位：岗位名 / 公司 / 技能，如：华为 Spring" />
          <span v-if="jobSearch.trim()" class="job-search-n">找到 {{ currentJobs.length }} 个</span>
        </div>
        <!-- 岗位分类：同名岗位归一组（忽略大小写），首位是按简历算出的智能推荐 -->
        <div class="cat-bar">
          <span v-for="g in groups" :key="g.key" class="cat"
                :class="{ active: g.key === currentCat }" @click="currentCat = g.key">
            {{ g.label }}<b>{{ g.jobs.length }}</b></span>
        </div>
        <!-- 关键词筛选：像电商筛选一样，从岗位标题/薪资/JD里抽出的福利特征 -->
        <div v-if="kwBar.length" class="kw-bar">
          <span class="kw-label">筛选</span>
          <span v-for="k in kwBar" :key="k.name" class="kw"
                :class="{ active: activeKws.includes(k.name) }" @click="toggleKw(k.name)">
            {{ k.name }}<b>{{ k.count }}</b></span>
          <el-button v-if="activeKws.length" link type="primary" size="small" @click="activeKws = []">清空</el-button>
        </div>
        <!-- 当前分类下的岗位卡片：大厂招聘风格，点开看详情 -->
        <div class="job-cards">
          <div v-for="j in currentJobs" :key="j.id" class="job-card"
               :class="{ picked: recommendedIds.includes(j.id) }" @click="pickAndDetail(j)">
            <div class="jc-top">
              <b class="jc-title">{{ j.title }}</b>
              <span v-if="recommendedIds.includes(j.id)" class="jc-rec">✓ 已推荐</span>
              <span class="jc-score" :class="'ms-' + scoreLevel(msOf(j).score)"
                    :title="'技能命中：' + (msOf(j).matched.join('、') || '无')">匹配 {{ msOf(j).score }}</span>
              <span class="jc-salary">{{ j.salary || '薪资面议' }}</span>
            </div>
            <div class="jc-company">
              <span class="jc-co">{{ j.company || '未知公司' }}</span>
              <el-tag v-if="j.status !== 1" size="small" type="warning" effect="plain">未上架</el-tag>
            </div>
            <div class="jc-chips">
              <span v-if="j.city" class="chip">{{ j.city }}</span>
              <span class="chip">{{ j.experience || '经验不限' }}</span>
              <span class="chip">{{ j.education || '学历不限' }}</span>
              <span class="chip chip-src">{{ j.source === 'BOSS' ? '爬虫' : '手动' }} · {{ fmt(j.createTime) }}</span>
            </div>
            <div v-if="jobSkills(j).length" class="jc-tags">
              <el-tag v-for="s in jobSkills(j)" :key="s" size="small" effect="plain">{{ s }}</el-tag>
            </div>
            <div v-if="jobKeywords(j).length" class="jc-kw">
              <el-tag v-for="k in jobKeywords(j)" :key="k" size="small" type="success" effect="plain">{{ k }}</el-tag>
            </div>
            <div v-if="msOf(j).matched.length" class="jc-match">
              <span class="jm-label">匹配技能</span>
              <el-tag v-for="s in msOf(j).matched.slice(0, 8)" :key="s"
                      size="small" type="success" effect="dark">{{ s }}</el-tag>
            </div>
            <div v-if="msOf(j).reasons.length" class="jc-reason">
              <span v-for="r in msOf(j).reasons" :key="r.text" class="reason"
                    :class="{ warn: !r.ok }">{{ r.text }}</span>
            </div>
            <div class="jc-jd">{{ (j.jdText || '').trim() || '（未录入JD职责描述）' }}
              <span class="jd-more">查看详情 ›</span>
            </div>
          </div>
        </div>
        <div class="assign-tip">共 {{ jobOptions.length }} 个岗位，待上架 {{ pendingJobCount }} 个，已按匹配度从高到低排序
          <el-button v-if="pendingJobCount > 0" link type="primary" :loading="approving"
                     @click="approveCrawled">一键上架爬虫岗位</el-button>
        </div>
      </template>
      <template #footer>
        <el-button @click="closeAssign">关闭</el-button>
        <el-button type="primary" @click="finishAssign">完成（已推荐 {{ recommendedIds.length }} 个）</el-button>
      </template>
    </el-dialog>

    <!-- 岗位详情：点卡片弹出，完整JD/公司/匹配点，可直接推荐（append-to-body盖在推荐弹窗上） -->
    <el-dialog v-model="showJobDetail" :title="jobDetail?.title || '岗位详情'"
               width="680px" top="6vh" append-to-body>
      <template v-if="jobDetail">
        <div class="jd-hero">
          <div>
            <div class="jd-salary">{{ jobDetail.salary || '薪资面议' }}</div>
            <div class="jd-chips">
              <span v-if="jobDetail.city" class="chip">{{ jobDetail.city }}</span>
              <span class="chip">{{ jobDetail.experience || '经验不限' }}</span>
              <span class="chip">{{ jobDetail.education || '学历不限' }}</span>
              <span v-if="jobDetail.status !== 1" class="chip chip-warn">未上架</span>
            </div>
          </div>
          <span class="jc-score big" :class="'ms-' + scoreLevel(msOf(jobDetail).score)">匹配 {{ msOf(jobDetail).score }}</span>
        </div>
        <div class="jd-block">
          <div class="jd-sec-t">公司信息</div>
          <div class="jd-co">
            <b>{{ jobDetail.company || '未知公司' }}</b>
            <span class="rv-sub">{{ jobDetail.source === 'BOSS' ? '来源：Boss直聘爬取' : '来源：手动录入' }} · 收录于 {{ fmt(jobDetail.createTime) }}</span>
          </div>
        </div>
        <div v-if="msOf(jobDetail).matched.length" class="jd-block">
          <div class="jd-sec-t">与简历匹配的技能</div>
          <div class="jd-tags">
            <el-tag v-for="s in msOf(jobDetail).matched" :key="s" type="success" effect="dark">{{ s }}</el-tag>
          </div>
        </div>
        <div v-if="jobSkills(jobDetail).length" class="jd-block">
          <div class="jd-sec-t">岗位技能标签</div>
          <div class="jd-tags">
            <el-tag v-for="s in jobSkills(jobDetail)" :key="s" effect="plain">{{ s }}</el-tag>
          </div>
        </div>
        <div v-if="jobKeywords(jobDetail).length" class="jd-block">
          <div class="jd-sec-t">福利特征</div>
          <div class="jd-tags">
            <el-tag v-for="k in jobKeywords(jobDetail)" :key="k" type="success" effect="plain">{{ k }}</el-tag>
          </div>
        </div>
        <div class="jd-block">
          <div class="jd-sec-t">职位描述</div>
          <div class="jd-body">{{ (jobDetail.jdText || '').trim() || '（未录入JD职责描述）' }}</div>
        </div>
      </template>
      <template #footer>
        <a v-if="jobDetail?.jobUrl" :href="jobDetail.jobUrl" target="_blank" class="jd-link">查看原帖 ↗</a>
        <el-button @click="showJobDetail = false">关闭</el-button>
        <el-button v-if="jobDetail && recommendedIds.includes(jobDetail.id)"
                   type="danger" plain :loading="assigning" @click="unassign(jobDetail)">撤销推荐</el-button>
        <el-button v-else type="primary" :loading="assigning" @click="doAssign(jobDetail)">推荐这个岗位</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http.js'
import { buildResumeHtml } from '../utils/resumeHtml.js'

const router = useRouter()
const statusLabel = { 0: '未提交', 1: '待审阅', 2: '已驳回', 3: '已推荐' }
const statusType = { 0: 'info', 1: 'warning', 2: 'danger', 3: 'success' }

const list = ref([])
const loading = ref(false)
const stats = ref(null)
// 不选=全部已提交的（未提交属于私人草稿，管理员不可见）
const filterStatus = ref(null)
const filterEdu = ref(null)
const keyword = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)

onMounted(() => {
  // 简历审阅只给管理员；误入弹回首页
  const user = JSON.parse(localStorage.getItem('kb-user') || '{"role":""}')
  if (user.role !== 'ADMIN') {
    ElMessage.warning('简历审阅仅管理员可用')
    router.replace('/')
    return
  }
  load()
  loadStats()
})

async function load() {
  loading.value = true
  try {
    const params = new URLSearchParams({ page: page.value, size: size.value })
    // 选了具体状态才传；不传后端默认只查已提交的
    if (filterStatus.value !== null && filterStatus.value !== '') {
      params.set('submitStatus', filterStatus.value)
    }
    if (filterEdu.value) params.set('education', filterEdu.value)
    if (keyword.value.trim()) params.set('keyword', keyword.value.trim())
    const data = await http.get('/resume/admin/page?' + params)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try { stats.value = await http.get('/resume/admin/stats') } catch { stats.value = null }
}

function reload() {
  page.value = 1
  load()
}

const fmt = t => t ? String(t).replace('T', ' ').slice(0, 16) : '—'
const scoreClass = s => s == null ? '' : s >= 80 ? 'good' : s >= 60 ? 'ok' : 'bad'

// ===== 详情 =====
const showDetail = ref(false)
const detail = ref(null)
const detailRow = ref(null)
// 完整简历页HTML：和用户端导出PDF同一套A4排版
const detailHtml = computed(() => {
  if (!detail.value) return ''
  let content = {}
  try { content = JSON.parse(detail.value.contentJson || '{}') } catch { content = {} }
  return buildResumeHtml(content, detail.value.targetJob)
})

async function openDetail(row) {
  detailRow.value = row
  // 拉全量（含contentJson），列表页没带正文
  detail.value = await http.get('/resume/admin/' + row.resume.id)
  showDetail.value = true
}

// ===== 推荐岗位 =====
const showAssign = ref(false)
const assignFor = ref(null)
// 已推荐岗位ID集合：一份简历可推多个，支持追加/撤销
const recommendedIds = ref([])
const assigning = ref(false)
const approving = ref(false)
const jobOptions = ref([])
// 待上架岗位数：爬虫抓回来的默认待审，一键上架后可直接推荐
const pendingJobCount = computed(() => jobOptions.value.filter(j => j.status === 0).length)

// 同名岗位归一组，点分类看具体岗位；按数量降序
// 智能推荐分组的岗位集合：按简历匹配分筛出合适的，不够就拿分数最高的前3个兑底
const recJobs = computed(() => {
  const sorted = [...jobOptions.value].sort((a, b) => msOf(b).score - msOf(a).score)
  const good = sorted.filter(j => msOf(j).score >= 50)
  return good.length ? good : sorted.slice(0, 3)
})
const groups = computed(() => {
  const map = new Map()
  for (const j of jobOptions.value) {
    const key = catKey(j.title)
    if (!map.has(key)) map.set(key, [])
    map.get(key).push(j)
  }
  const arr = [...map.entries()]
    .map(([key, jobs]) => ({ key, label: normalizeTitle(jobs[0].title), jobs }))
    .sort((a, b) => b.jobs.length - a.jobs.length)
  // 首位放按简历算出来的"智能推荐"，其次"全部"，配合关键词筛选跨分类看结果
  return [
    { key: 'REC', label: '智能推荐', jobs: recJobs.value },
    { key: 'ALL', label: '全部', jobs: jobOptions.value },
    ...arr
  ]
})
const currentCat = ref('REC')
const currentJobs = computed(() => {
  const q = jobSearch.value.trim().toLowerCase()
  // 搜岗位时无视分类全局找；不搜才按当前分类看
  const pool = q ? jobOptions.value : (groups.value.find(g => g.key === currentCat.value)?.jobs || [])
  let filtered = pool
  // 选了福利关键词时只留全部命中的（和电商筛选一样是"且"的关系）
  if (activeKws.value.length) {
    filtered = filtered.filter(j => {
      const ks = jobKeywords(j)
      return activeKws.value.every(k => ks.includes(k))
    })
  }
  // 搜文本：岗位名/公司/城市/薪资/技能/JD都参与，多个词是"且"
  if (q) {
    filtered = filtered.filter(j => q.split(/\s+/).every(t => jobHit(j, t)))
  }
  // 匹配度高的排前面
  return [...filtered].sort((a, b) => msOf(b).score - msOf(a).score)
})

function jobHit(j, t) {
  const hay = `${j.title || ''} ${j.company || ''} ${j.city || ''} ${j.salary || ''} ${j.skillsJson || ''} ${j.jdText || ''} ${jobKeywords(j).join('')}`.toLowerCase()
  return hay.includes(t)
}

/** 岗位标题归一化：去括号内容、去级别前后缀、去空格，保留"实习"让实习岗单独成组 */
function normalizeTitle(t) {
  return String(t || '未命名')
    .replace(/[（(【\[].*?[）)】\]]/g, '')
    .replace(/^(高级|资深|初级|中级|助理)/, '')
    .replace(/\d+.*年(经验)?$/g, '')
    .replace(/\s+/g, '')
    .trim() || '未命名'
}
// 分组key再转小写：java/Java/JAVA开发工程师才能归到同一组
const catKey = t => normalizeTitle(t).toLowerCase()

// ===== 关键词筛选：从标题/薪资/JD里抽福利特征 =====
const KEYWORD_DICT = [
  ['双休', /双休|周末双休/],
  ['五险一金', /五险一金|五险|六险一金|社保/],
  ['年终奖', /年终奖|年底双薪|\d+薪/],
  ['实习', /实习/],
  ['远程', /远程办公|居家办公|远程工作/],
  ['弹性工作', /弹性(工作|上班|时间)/],
  ['期权股票', /期权|股票激励/],
  ['餐补', /餐补|包餐|免费(三餐|午餐|晚餐)/],
  ['房补', /房补|住房补贴|包住/],
  ['交通补助', /交通补(助|贴)|车补/],
  ['大小周', /大小周/],
]
const jobKeywords = j => {
  const text = `${j.title || ''} ${j.salary || ''} ${j.jdText || ''}`
  return KEYWORD_DICT.filter(([, re]) => re.test(text)).map(([k]) => k)
}
// 全部岗位里每个关键词出现次数，按热度降序展示成筛选条
const kwBar = computed(() => {
  const count = new Map()
  for (const j of jobOptions.value) {
    for (const k of jobKeywords(j)) count.set(k, (count.get(k) || 0) + 1)
  }
  return [...count.entries()].map(([name, c]) => ({ name, count: c }))
    .sort((a, b) => b.count - a.count)
})
const activeKws = ref([])
function toggleKw(k) {
  const i = activeKws.value.indexOf(k)
  if (i >= 0) activeKws.value.splice(i, 1)
  else activeKws.value.push(k)
}
// 搜岗位：按岗位名/公司/技能等全文找
const jobSearch = ref('')

const jobSkills = j => {
  try { return JSON.parse(j.skillsJson || '[]') } catch { return [] }
}

// ===== 匹配打分：技能命中50 + 目标岗位20 + 学历15 + 年限15 =====
const EDU_RANK = { '博士': 4, '硕士': 3, '本科': 2, '大专': 1 }
const matchResume = ref(null)   // {tokens, targetJob, education, workYears}
const matchMap = ref(new Map()) // jobId -> {score, matched}
const msOf = j => matchMap.value.get(j.id) || { score: 0, matched: [], reasons: [] }
const scoreLevel = s => s >= 75 ? 'good' : s >= 50 ? 'ok' : 'low'

/** 从简历详情里抽技能词：优先contentJson.skills，退化用拍平的skills列 */
function parseMatchResume(detail, row) {
  let tokens = []
  try {
    const c = JSON.parse(detail?.contentJson || '{}')
    for (const s of c.skills || []) {
      const items = Array.isArray(s) ? s : (s.items || [])
      for (const it of items) tokens.push(typeof it === 'string' ? it : it?.name)
    }
  } catch { /* 解析失败用拍平列兑底 */ }
  if (!tokens.length && row.resume.skills) tokens = row.resume.skills.split(/[、,，]/)
  tokens = [...new Set(tokens.map(t => String(t || '').trim().toLowerCase()).filter(t => t.length >= 2))]
  return { tokens, targetJob: row.resume.targetJob || '', education: row.resume.education || '', workYears: row.resume.workYears }
}

function matchScore(j) {
  const r = matchResume.value
  if (!r) return { score: 0, matched: [], reasons: [] }
  let score = 0
  const reasons = []
  // 1.技能命中（50分）：简历技能词在岗位标题/标签/需求/JD里出现几个
  const jobText = `${j.title || ''} ${j.skillsJson || ''} ${j.requireJson || ''} ${j.jdText || ''}`.toLowerCase()
  const matched = r.tokens.filter(t => jobText.includes(t))
  score += Math.round(50 * Math.min(1, matched.length / Math.max(1, Math.min(r.tokens.length, 6))))
  if (matched.length) reasons.push({ text: `命中${matched.length}项简历技能`, ok: true })
  // 2.目标岗位与岗位名（20分）：整体包含满分，词级重叠半分
  const tj = r.targetJob.toLowerCase().replace(/\s+/g, '')
  const jt = (j.title || '').toLowerCase().replace(/\s+/g, '')
  if (!tj) score += 10
  else if (jt.includes(tj) || tj.includes(jt)) {
    score += 20
    reasons.push({ text: '与目标岗位吻合', ok: true })
  } else if (tj.split(/[、,，/]+/).filter(x => x.length >= 2).some(t => jt.includes(t))) {
    score += 10
    reasons.push({ text: '与目标岗位部分相关', ok: true })
  }
  // 3.学历（15分）：达标满分，差一级半分，差两级不给；任一侧缺失视为不限
  const rEdu = EDU_RANK[r.education]
  const jobEdu = ['博士', '硕士', '本科', '大专'].find(d => (j.education || '').includes(d))
  if (!rEdu || !jobEdu) score += 15
  else {
    const diff = rEdu - EDU_RANK[jobEdu]
    score += diff >= 0 ? 15 : diff === -1 ? 6 : 0
    if (diff >= 0) reasons.push({ text: '学历达标', ok: true })
    else if (diff === -1) reasons.push({ text: `学历差一级(需${jobEdu})`, ok: false })
  }
  // 4.年限（15分）：优先用AI解析的minExpYears，退化从"3-5年"原文取最小值
  let minYears = null
  try {
    const rq = JSON.parse(j.requireJson || '{}')
    if (rq.minExpYears != null) minYears = Number(rq.minExpYears)
  } catch { /* ignore */ }
  if (minYears == null) {
    const exp = String(j.experience || '')
    const m = exp.match(/(\d+)/)
    if (m && !/不限|应届|在校/.test(exp)) minYears = Number(m[1])
  }
  if (minYears == null || minYears <= 0) score += 15
  else if (r.workYears == null) score += 8
  else {
    const d = r.workYears - minYears
    score += d >= 0 ? 15 : d === -1 ? 9 : d === -2 ? 4 : 0
    if (d >= 0) reasons.push({ text: '年限达标', ok: true })
    else if (d === -1) reasons.push({ text: '年限略差一年', ok: false })
  }
  return { score, matched, reasons }
}

// ===== 岗位详情：点卡片弹出，大厂风格浏览完整信息 =====
const showJobDetail = ref(false)
const jobDetail = ref(null)
/** 点卡片＝开详情，看完可在详情里推荐/撤销 */
function pickAndDetail(j) {
  jobDetail.value = j
  showJobDetail.value = true
}

/** 已推荐岗位的完整对象，顶部清单条展示用 */
const recommendedJobObjs = computed(() =>
  recommendedIds.value.map(id => jobOptions.value.find(j => j.id === id)).filter(Boolean))

async function loadJobs() {
  // 拉全部岗位（含爬虫待审的），卡片上会标出未上架的
  const data = await http.get('/job/page?size=200')
  jobOptions.value = data.records
}

async function openAssign(row) {
  assignFor.value = row
  recommendedIds.value = []
  activeKws.value = []
  jobSearch.value = ''
  jobDetail.value = null
  // 简历详情（拿完整技能）/全部岗位/已推荐清单，三者互不依赖并行拉
  let detail = null
  const detailPromise = http.get('/resume/admin/' + row.resume.id)
    .then(d => { detail = d }).catch(() => { /* 拉不到就用列表拍平字段兑底 */ })
  const recPromise = http.get(`/resume/admin/${row.resume.id}/jobs`)
    .then(js => { recommendedIds.value = js.map(j => j.id) }).catch(() => {})
  await loadJobs()
  await Promise.all([detailPromise, recPromise])
  matchResume.value = parseMatchResume(detail, row)
  matchMap.value = new Map(jobOptions.value.map(j => [j.id, matchScore(j)]))
  // 默认直接看按简历算出的智能推荐
  currentCat.value = 'REC'
  showAssign.value = true
}

async function approveCrawled() {
  approving.value = true
  try {
    const n = await http.post('/job/approve-crawled')
    ElMessage.success(n > 0 ? `已上架 ${n} 个爬虫岗位` : '没有待上架的爬虫岗位')
    await loadJobs()
  } finally {
    approving.value = false
  }
}

/** 追加推荐一个岗位：弹窗不关，可继续挑下一个 */
async function doAssign(j) {
  if (!j) return
  assigning.value = true
  try {
    await http.post(`/resume/admin/${assignFor.value.resume.id}/assign`, { jobId: j.id })
    if (!recommendedIds.value.includes(j.id)) recommendedIds.value.push(j.id)
    ElMessage.success(`已推荐「${j.title}」，可继续追加`)
  } finally {
    assigning.value = false
  }
}

/** 撤销某个已推荐岗位；全撤完简历自动回到待审阅 */
async function unassign(j) {
  assigning.value = true
  try {
    await http.post(`/resume/admin/${assignFor.value.resume.id}/unassign`, { jobId: j.id })
    recommendedIds.value = recommendedIds.value.filter(id => id !== j.id)
    ElMessage.success(recommendedIds.value.length ? '已撤销该岗位' : '已全部撤销，简历回到待审阅')
  } finally {
    assigning.value = false
  }
}

function closeAssign() {
  showAssign.value = false
  showJobDetail.value = false
}

function finishAssign() {
  closeAssign()
  showDetail.value = false
  load()
  loadStats()
}

// ===== 驳回 =====
async function sendBack(row) {
  let remark
  try {
    const r = await ElMessageBox.prompt('填写驳回理由，用户修改后可重新提交', '驳回简历', {
      confirmButtonText: '驳回', cancelButtonText: '取消', inputType: 'textarea',
      inputPlaceholder: '如：缺少项目经历，请补充后再提交'
    })
    remark = r.value
  } catch { return }
  await http.post(`/resume/admin/${row.resume.id}/return`, { remark: remark || '' })
  ElMessage.success('已驳回')
  showDetail.value = false
  load()
  loadStats()
}
</script>

<style scoped>
.rv-page {
  min-height: 100vh;
  background: var(--kb-bg);
  padding: 20px;
}
.rv-top {
  max-width: 1280px;
  margin: 0 auto 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.rv-top h2 { margin: 0; font-size: 22px; font-weight: 700; letter-spacing: 1px; }
.rv-ops { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }

.rv-stats {
  max-width: 1280px;
  margin: 0 auto 14px;
  display: flex;
  align-items: center;
  gap: 22px;
  flex-wrap: wrap;
  background: #fff;
  border: 1px solid var(--kb-line);
  border-radius: var(--kb-radius);
  padding: 12px 18px;
  box-shadow: var(--kb-shadow-sm);
}
.stat { display: flex; flex-direction: column; align-items: center; }
.stat b { font-size: 20px; }
.stat span { font-size: 12px; color: #8c8c8c; }
.dist { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.dist-t { font-size: 12px; color: #8c8c8c; }
.dist-tag { margin-right: 2px; }

.rv-page :deep(.el-table) {
  max-width: 1280px;
  margin: 0 auto;
  border-radius: var(--kb-radius);
  overflow: hidden;
  box-shadow: var(--kb-shadow-sm);
  border: 1px solid var(--kb-line);
}
.rv-pager { max-width: 1280px; margin: 14px auto 0; justify-content: flex-end; display: flex; }
.rv-name { font-weight: 600; }
.rv-title { font-weight: 600; }
.rv-sub { font-size: 12px; color: #8c8c8c; }
.good { color: #389e0d; }
.ok { color: #d48806; }
.bad { color: #cf1322; }

.detail-meta { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; color: #595959; margin-bottom: 10px; }
.detail-remark { background: #fff1f0; color: #cf1322; border-radius: 8px; padding: 8px 12px; font-size: 13px; margin-bottom: 12px; }
/* 完整简历预览：A4页宽约794px，缩进弹窗里显示 */
.detail-frame {
  width: 100%;
  height: 70vh;
  border: 1px solid var(--kb-line);
  border-radius: 8px;
  background: #e9eaec;
}
.assign-tip { margin-top: 10px; font-size: 12px; color: #8c8c8c; display: flex; align-items: center; gap: 8px; }
.assign-empty { text-align: center; color: #8c8c8c; padding: 30px 0; }

/* 简历画像条：说明推荐依据 */
.rec-summary {
  display: flex; flex-wrap: wrap; align-items: center; gap: 6px 14px;
  font-size: 13px; color: #444; margin-bottom: 10px;
  padding: 8px 12px; border-radius: 8px;
  background: #f0f6ff; border: 1px solid #dbe7ff;
}
.rec-summary b { color: #3370ff; }
.rec-n { color: #389e0d; font-weight: 600; }

/* 已推荐清单条：点×撤销 */
.rec-bar {
  display: flex; flex-wrap: wrap; align-items: center; gap: 6px;
  margin-bottom: 10px; font-size: 13px;
}
.rec-bar-label { color: #389e0d; font-weight: 600; }
.rec-bar-none { color: #999; font-size: 12px; }
.rec-chip {
  display: inline-flex; align-items: center; gap: 5px;
  font-size: 12px; color: #389e0d; background: #f6ffed; border: 1px solid #b7eb8f;
  border-radius: 12px; padding: 2px 9px;
}
.rec-chip i {
  font-style: normal; cursor: pointer; color: #999; font-weight: 700;
}
.rec-chip i:hover { color: #f5222d; }
.rec-tag { margin-right: 4px; }
.rec-more { font-size: 12px; color: #389e0d; font-weight: 600; margin-left: 2px; }
.jc-rec {
  font-size: 12px; color: #389e0d; background: #f6ffed; border: 1px solid #b7eb8f;
  border-radius: 10px; padding: 1px 8px; white-space: nowrap;
}

/* 岗位搜索行 */
.job-search-row { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.job-search { max-width: 420px; }
.job-search-n { font-size: 12px; color: #389e0d; white-space: nowrap; }

/* 推荐弹窗：岗位分类条 */
.cat-bar { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
.cat {
  font-size: 13px; padding: 5px 12px; border-radius: 16px; cursor: pointer;
  border: 1px solid var(--kb-line); background: #fafafa; color: #444;
  transition: all .15s;
}
.cat b { margin-left: 5px; font-size: 12px; color: #999; }
.cat:hover { border-color: #3370ff; color: #3370ff; }
.cat.active { background: #3370ff; border-color: #3370ff; color: #fff; }
.cat.active b { color: rgba(255, 255, 255, .85); }

/* 关键词筛选条：像电商筛选，选中变绿 */
.kw-bar {
  display: flex; flex-wrap: wrap; align-items: center; gap: 8px;
  margin-bottom: 12px; padding: 8px 10px;
  background: #fafafa; border: 1px dashed var(--kb-line); border-radius: 8px;
}
.kw-label { font-size: 12px; color: #8c8c8c; }
.kw {
  font-size: 12px; padding: 3px 10px; border-radius: 14px; cursor: pointer;
  border: 1px solid #d9d9d9; background: #fff; color: #555; transition: all .15s;
}
.kw b { margin-left: 4px; font-size: 11px; color: #aaa; font-weight: 400; }
.kw:hover { border-color: #389e0d; color: #389e0d; }
.kw.active { background: #389e0d; border-color: #389e0d; color: #fff; }
.kw.active b { color: rgba(255, 255, 255, .85); }

/* 岗位卡片：大厂招聘风格，层次分明有悬浮感 */
.job-cards { max-height: 48vh; overflow-y: auto; display: flex; flex-direction: column; gap: 10px; padding-right: 4px; }
.job-card {
  border: 1px solid var(--kb-line); border-radius: 12px; padding: 14px 16px; cursor: pointer;
  background: #fff; transition: all .18s;
}
.job-card:hover {
  border-color: #b7ccff; transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(51, 112, 255, .10);
}
.job-card.picked { border-color: #3370ff; background: #f0f6ff; box-shadow: 0 0 0 2px rgba(51, 112, 255, .15); }
.jc-top { display: flex; align-items: center; gap: 8px; }
.jc-title { font-size: 15px; color: #1f2329; letter-spacing: .3px; }
.jc-score {
  font-size: 12px; padding: 1px 8px; border-radius: 10px; white-space: nowrap;
}
.jc-score.big { font-size: 14px; padding: 4px 14px; border-radius: 14px; font-weight: 600; }
.ms-good { background: #389e0d; color: #fff; }
.ms-ok { background: #fff7e6; color: #d48806; border: 1px solid #ffe7ba; }
.ms-low { background: #f5f5f5; color: #999; border: 1px solid #e8e8e8; }
.jc-salary { margin-left: auto; color: #e02020; font-weight: 700; font-size: 15px; white-space: nowrap; }
.jc-company { display: flex; align-items: center; gap: 8px; margin-top: 6px; }
.jc-co { font-size: 13px; color: #595959; font-weight: 500; }
.jc-chips { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }
.chip {
  font-size: 12px; color: #595959; background: #f5f6f7; border-radius: 4px;
  padding: 3px 8px; line-height: 1.3;
}
.chip-src { color: #999; background: transparent; padding-left: 0; }
.chip-warn { color: #d48806; background: #fff7e6; }
.jc-tags { margin-top: 8px; display: flex; flex-wrap: wrap; gap: 4px; }
.jc-kw { margin-top: 6px; display: flex; flex-wrap: wrap; gap: 4px; }
.jc-match { margin-top: 8px; display: flex; flex-wrap: wrap; align-items: center; gap: 4px; }
.jm-label { font-size: 12px; color: #389e0d; font-weight: 600; margin-right: 2px; }
.jc-reason { margin-top: 7px; display: flex; flex-wrap: wrap; gap: 6px; }
.reason {
  font-size: 12px; color: #389e0d; background: #f6ffed; border: 1px solid #d9f7be;
  border-radius: 4px; padding: 1px 8px;
}
.reason.warn { color: #d48806; background: #fff7e6; border-color: #ffe7ba; }
.jc-jd {
  margin-top: 9px; font-size: 12px; color: #8c8c8c; line-height: 1.7;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
  white-space: pre-line;
}
.jd-more { color: #3370ff; font-size: 12px; white-space: nowrap; }

/* 岗位详情弹窗：分区清晰，标题带色条 */
.jd-hero {
  display: flex; align-items: flex-start; justify-content: space-between; gap: 12px;
  padding: 14px 16px; border-radius: 10px;
  background: linear-gradient(135deg, #f0f6ff 0%, #f7f9fc 100%);
  margin-bottom: 14px;
}
.jd-salary { font-size: 22px; font-weight: 700; color: #e02020; margin-bottom: 8px; }
.jd-chips { display: flex; flex-wrap: wrap; gap: 6px; }
.jd-block { margin-bottom: 14px; }
.jd-sec-t {
  font-size: 13px; font-weight: 600; color: #1f2329; margin-bottom: 8px;
  padding-left: 8px; border-left: 3px solid #3370ff;
}
.jd-co { display: flex; flex-direction: column; gap: 3px; }
.jd-co b { font-size: 14px; }
.jd-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.jd-body {
  font-size: 13px; color: #444; line-height: 1.9; white-space: pre-line;
  max-height: 34vh; overflow-y: auto; padding-right: 6px;
  background: #fafafa; border-radius: 8px; padding: 12px 14px;
}
.jd-link { margin-right: auto; font-size: 13px; color: #3370ff; text-decoration: none; }
.jd-link:hover { text-decoration: underline; }
</style>
