<template>
  <div class="job-page">
    <header class="job-top">
      <h2>岗位管理</h2>
      <div class="job-ops">
        <el-select v-model="filterStatus" placeholder="全部状态" clearable style="width: 130px" @change="reload">
          <el-option label="待审核" :value="0" />
          <el-option label="已上架" :value="1" />
          <el-option label="已驳回" :value="2" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜岗位名/公司名" clearable style="width: 200px"
                  @keyup.enter="reload" @clear="reload" />
        <el-button @click="reload">查询</el-button>
        <el-button type="primary" @click="openForm(null)">手动录入</el-button>
        <el-button link @click="$router.push('/')">返回首页</el-button>
      </div>
    </header>

    <el-table :data="list" v-loading="loading" :header-cell-style="{ background: '#fafafa', color: '#595959' }">
      <el-table-column label="岗位" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="job-title">{{ row.title }}
            <el-tag v-if="row.requireJson" size="small" type="success" effect="plain" class="parsed-tag">已AI解析</el-tag>
          </div>
          <div class="job-company">{{ row.company || '—' }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="city" label="城市" width="80" />
      <el-table-column prop="salary" label="薪资" width="110" show-overflow-tooltip />
      <el-table-column label="要求" width="110">
        <template #default="{ row }">{{ row.experience || '—' }} / {{ row.education || '不限' }}</template>
      </el-table-column>
      <el-table-column label="来源" width="80">
        <template #default="{ row }">
          <el-tag size="small" :type="row.source === 'BOSS' ? 'info' : 'warning'" effect="plain">
            {{ row.source === 'BOSS' ? '爬虫' : '手动' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag size="small" :type="statusType[row.status]">{{ statusLabel[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="入库时间" width="140">
        <template #default="{ row }">{{ fmt(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button link @click="openDetail(row)">详情</el-button>
          <el-button link :loading="parsingId === row.id" @click="parse(row)">AI解析</el-button>
          <el-button link :loading="recommendingId === row.id" @click="recommend(row)">AI出题</el-button>
          <el-button v-if="row.status !== 1" link type="success" @click="audit(row, true)">通过</el-button>
          <el-button v-if="row.status !== 2" link type="warning" @click="audit(row, false)">驳回</el-button>
          <el-button link @click="openForm(row)">编辑</el-button>
          <el-button link type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="job-pager" v-model:current-page="page" :page-size="size" :total="total"
                   layout="prev, pager, next, total" @current-change="load" />

    <!-- JD详情 -->
    <el-dialog v-model="showDetail" :title="detail?.title" width="640px">
      <template v-if="detail">
        <div class="detail-meta">
          <span>{{ detail.company || '—' }}</span><span>{{ detail.city }}</span>
          <span>{{ detail.salary }}</span><span>{{ detail.experience }} / {{ detail.education || '不限' }}</span>
          <el-tag size="small" :type="statusType[detail.status]">{{ statusLabel[detail.status] }}</el-tag>
        </div>
        <div v-if="skills.length" class="detail-block">
          <h4>技能标签</h4>
          <el-tag v-for="s in skills" :key="s" size="small" effect="plain" class="skill-tag">{{ s }}</el-tag>
        </div>
        <div v-if="requireInfo" class="detail-block">
          <h4>AI解析结果</h4>
          <div class="req-card">
            <p><span class="req-k">技能要求</span>
              <template v-if="requireInfo.skills?.length">
                <el-tag v-for="s in requireInfo.skills" :key="s" size="small" effect="plain" class="skill-tag">{{ s }}</el-tag>
              </template>
              <span v-else class="req-empty">未提炼到</span>
            </p>
            <p><span class="req-k">经验 / 学历</span>
              {{ requireInfo.minExpYears ? requireInfo.minExpYears + '年以上' : '不限年限' }}
              · {{ requireInfo.education || '不限学历' }}
            </p>
            <p><span class="req-k">考察关键词</span>
              <template v-if="requireInfo.keywords?.length">
                <el-tag v-for="k in requireInfo.keywords" :key="k" size="small" type="warning" effect="plain" class="skill-tag">{{ k }}</el-tag>
              </template>
              <span v-else class="req-empty">未提炼到</span>
            </p>
            <!-- 兑底：AI返回了约定外的字段也能看见，不会白解析 -->
            <details v-if="extraKeys.length" class="req-raw">
              <summary>其他字段</summary>
              <pre>{{ extraJson }}</pre>
            </details>
          </div>
          <p class="req-tip">此结果是AI出题、简历智能推荐的依据；不满意可改完JD后重新点“AI解析”覆盖</p>
        </div>
        <div v-else class="detail-block">
          <h4>AI解析结果</h4>
          <p class="req-empty">还没解析：点列表里的“AI解析”，会从JD里提炼技能/经验/学历/考察关键词</p>
        </div>
        <div class="detail-block">
          <h4>JD原文</h4>
          <pre class="jd-text">{{ detail.jdText || '（无）' }}</pre>
        </div>
        <a v-if="detail.jobUrl" :href="detail.jobUrl" target="_blank" class="jd-link">原始链接 ↗</a>
      </template>
    </el-dialog>

    <!-- 录入/编辑表单 -->
    <el-dialog v-model="showForm" :title="form.id ? '编辑岗位' : '手动录入岗位'" width="640px">
      <el-form label-position="top">
        <el-form-item label="岗位名" required>
          <el-input v-model="form.title" placeholder="如：Java开发工程师" />
        </el-form-item>
        <el-form-item label="公司">
          <el-input v-model="form.company" />
        </el-form-item>
        <el-form-item label="城市">
          <el-input v-model="form.city" placeholder="如：北京" />
        </el-form-item>
        <el-form-item label="薪资（原文）">
          <el-input v-model="form.salary" placeholder="如：15-25K·14薪" />
        </el-form-item>
        <el-form-item label="经验要求">
          <el-input v-model="form.experience" placeholder="如：3-5年" />
        </el-form-item>
        <el-form-item label="学历要求">
          <el-input v-model="form.education" placeholder="如：本科" />
        </el-form-item>
        <el-form-item label="技能标签（每行一个）">
          <el-input v-model="skillsText" type="textarea" :rows="3" placeholder="Java&#10;Spring Boot" />
        </el-form-item>
        <el-form-item label="JD职责描述">
          <el-input v-model="form.jdText" type="textarea" :rows="6" />
        </el-form-item>
        <el-form-item label="原始链接（可选）">
          <el-input v-model="form.jobUrl" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showForm = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- AI出题：按岗位结构化需求生成的面试简答题，自动入题库 -->
    <el-dialog v-model="showRecommend" :title="`「${recommendFor?.title}」AI出题`" width="560px">
      <ol v-if="questions.length" class="q-list">
        <li v-for="(q, i) in questions" :key="i">{{ q }}</li>
      </ol>
      <div v-else class="q-empty">暂无出题结果</div>
      <p class="q-tip">题目已按分类「{{ recommendFor?.title }}」存入题库，重复出题同题干不会重复入库；可在刷题/模拟面试里练</p>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http.js'

const router = useRouter()
const statusLabel = { 0: '待审核', 1: '已上架', 2: '已驳回' }
const statusType = { 0: 'warning', 1: 'success', 2: 'danger' }

const list = ref([])
const loading = ref(false)
const filterStatus = ref(null)
const keyword = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)

onMounted(() => {
  // 岗位管理只给管理员；误入弹回首页
  const user = JSON.parse(localStorage.getItem('kb-user') || '{"role":""}')
  if (user.role !== 'ADMIN') {
    ElMessage.warning('岗位管理仅管理员可用')
    router.replace('/')
    return
  }
  load()
})

async function load() {
  loading.value = true
  try {
    const params = new URLSearchParams({ page: page.value, size: size.value })
    if (filterStatus.value !== null && filterStatus.value !== '') params.set('status', filterStatus.value)
    if (keyword.value.trim()) params.set('keyword', keyword.value.trim())
    const data = await http.get('/job/page?' + params)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function reload() {
  page.value = 1
  load()
}

const fmt = t => (t || '').replace('T', ' ').slice(0, 16)

// ===== 详情 =====
const showDetail = ref(false)
const detail = ref(null)
const skills = computed(() => {
  try { return JSON.parse(detail.value?.skillsJson || '[]') } catch { return [] }
})
const requireInfo = computed(() => {
  try { return JSON.parse(detail.value?.requireJson || 'null') } catch { return null }
})
// 约定四个字段之外的额外字段：AI偶尔会多吐东西，兑底展示不丢信息
const REQ_KEYS = ['skills', 'minExpYears', 'education', 'keywords']
const extraKeys = computed(() =>
  requireInfo.value ? Object.keys(requireInfo.value).filter(k => !REQ_KEYS.includes(k)) : [])
const extraJson = computed(() => {
  if (!requireInfo.value || !extraKeys.value.length) return ''
  const o = {}
  extraKeys.value.forEach(k => { o[k] = requireInfo.value[k] })
  return JSON.stringify(o, null, 2)
})

function openDetail(row) {
  detail.value = row
  showDetail.value = true
}

// ===== 录入/编辑 =====
const showForm = ref(false)
const form = ref({})
const skillsText = ref('')

function openForm(row) {
  if (row) {
    form.value = { ...row }
    try { skillsText.value = (JSON.parse(row.skillsJson || '[]') || []).join('\n') } catch { skillsText.value = '' }
  } else {
    form.value = {}
    skillsText.value = ''
  }
  showForm.value = true
}

async function save() {
  if (!form.value.title?.trim()) {
    ElMessage.warning('岗位名不能为空')
    return
  }
  form.value.skillsJson = JSON.stringify(skillsText.value.split('\n').map(s => s.trim()).filter(Boolean))
  if (form.value.id) {
    await http.put(`/job/${form.value.id}`, form.value)
  } else {
    await http.post('/job', form.value)
  }
  ElMessage.success('已保存')
  showForm.value = false
  load()
}

async function del(row) {
  try {
    await ElMessageBox.confirm(`删除岗位「${row.title}」？`, '删除', { type: 'warning' })
  } catch { return }
  await http.delete(`/job/${row.id}`)
  ElMessage.success('已删除')
  load()
}

// ===== AI解析 / 审核 / AI出题 =====
const parsingId = ref(null)
async function parse(row) {
  parsingId.value = row.id
  try {
    // 后端返回解析后的结构化需求Map，同步到行上，详情弹窗立即能看到
    const requireInfo = await http.post(`/job/${row.id}/parse`)
    row.requireJson = JSON.stringify(requireInfo)
    ElMessage.success('AI解析完成，结果已保存')
    if (showDetail.value && detail.value?.id === row.id) detail.value = { ...row }
  } finally {
    parsingId.value = null
  }
}

async function audit(row, ok) {
  await http.post(`/job/${row.id}/${ok ? 'approve' : 'reject'}`)
  ElMessage.success(ok ? '已通过上架' : '已驳回')
  load()
}

const showRecommend = ref(false)
const recommendFor = ref(null)
const questions = ref([])
const recommendingId = ref(null)
async function recommend(row) {
  recommendingId.value = row.id
  try {
    // save=true：管理员出题顺便入题库（同题干去重）；用户端生成练习不带此参数不写库
    questions.value = await http.post(`/job/${row.id}/recommend?save=true`)
    recommendFor.value = row
    showRecommend.value = true
    ElMessage.success(`已生成 ${questions.value.length} 道题并加入题库（重复题自动跳过）`)
  } finally {
    recommendingId.value = null
  }
}
</script>

<style scoped>
.job-page {
  min-height: 100vh;
  background: var(--kb-bg);
  padding: 20px;
}
.job-top {
  max-width: 1200px;
  margin: 0 auto 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.job-top h2 { margin: 0; font-size: 22px; font-weight: 700; letter-spacing: 1px; }
.job-ops { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.job-page :deep(.el-table) {
  max-width: 1200px;
  margin: 0 auto;
  border-radius: var(--kb-radius);
  overflow: hidden;
  box-shadow: var(--kb-shadow-sm);
  border: 1px solid var(--kb-line);
}
.job-pager { max-width: 1200px; margin: 14px auto 0; justify-content: flex-end; display: flex; }
.job-title { font-weight: 600; }
.parsed-tag { margin-left: 6px; vertical-align: 1px; }
.job-company { font-size: 12px; color: #8c8c8c; }

.detail-meta { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; color: #595959; margin-bottom: 12px; }
.req-card {
  background: #fafafa;
  border: 1px solid var(--kb-line);
  border-radius: 8px;
  padding: 10px 12px;
}
.req-card p { margin: 4px 0; }
.req-k { color: #8c8c8c; font-size: 12px; margin-right: 8px; }
.req-empty { color: #bfbfbf; }
.req-tip { font-size: 12px; color: #8c8c8c; margin-top: 6px; }
.req-raw summary { cursor: pointer; font-size: 12px; color: #8c8c8c; }
.req-raw pre {
  font-size: 12px; background: #fff; border: 1px solid var(--kb-line);
  border-radius: 6px; padding: 8px; white-space: pre-wrap; word-break: break-word;
}
.detail-block { margin-bottom: 14px; }
.detail-block h4 { margin: 0 0 6px; font-size: 13px; color: #8c8c8c; }
.detail-block p { margin: 2px 0; }
.skill-tag { margin: 0 6px 6px 0; }
.jd-text {
  white-space: pre-wrap;
  word-break: break-word;
  background: #fafafa;
  border: 1px solid var(--kb-line);
  border-radius: 8px;
  padding: 12px;
  font-size: 13px;
  max-height: 260px;
  overflow-y: auto;
  font-family: inherit;
}
.jd-link { font-size: 13px; color: var(--kb-primary, #3370ff); }

.q-list { margin: 0; padding-left: 22px; line-height: 2; }
.q-empty { color: #8c8c8c; text-align: center; padding: 12px 0; }
.q-tip { font-size: 12px; color: #8c8c8c; margin: 12px 0 0; }
</style>
