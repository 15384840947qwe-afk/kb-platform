<template>
  <div class="manage-page">
    <header class="manage-top">
      <h2>题库管理</h2>
      <div class="manage-ops">
        <el-select v-model="filterCat" placeholder="全部科目" clearable style="width: 150px" @change="load">
          <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
        </el-select>
        <el-button type="primary" @click="openForm(null)">新建题目</el-button>
        <el-button link @click="$router.push('/drill')">返回刷题</el-button>
      </div>
    </header>

    <el-table :data="list" :header-cell-style="{ background: '#fafafa', color: '#595959' }">
      <el-table-column prop="category" label="科目" width="110" />
      <el-table-column prop="type" label="类型" width="80">
        <template #default="{ row }">{{ typeLabel[row.type] }}</template>
      </el-table-column>
      <el-table-column prop="stem" label="题干" show-overflow-tooltip />
      <el-table-column label="操作" width="130">
        <template #default="{ row }">
          <el-button link @click="openForm(row)">编辑</el-button>
          <el-button link type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 录入/编辑表单 -->
    <el-dialog v-model="showForm" :title="form.id ? '编辑题目' : '新建题目'" width="640px">
      <el-form label-position="top">
        <el-form-item label="科目">
          <el-input v-model="form.category" placeholder="如：MySQL" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="单选" value="SINGLE" />
            <el-option label="多选" value="MULTI" />
            <el-option label="填空" value="FILL" />
            <el-option label="简答" value="SHORT" />
          </el-select>
        </el-form-item>
        <el-form-item label="题干">
          <el-input v-model="form.stem" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item v-if="isChoice" label="选项（每行一个，按A/B/C/D顺序）">
          <el-input v-model="optionsText" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item :label="answerLabel">
          <el-input v-model="form.answer" :placeholder="answerPlaceholder" />
        </el-form-item>
        <el-form-item label="解析">
          <el-input v-model="form.explanation" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="关联教材文档id（可选）">
          <el-input v-model.number="form.relatedDocId" placeholder="留空不关联，刷题答错可跳去看教材" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showForm = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http.js'

const router = useRouter()
const typeLabel = { SINGLE: '单选', MULTI: '多选', FILL: '填空', SHORT: '简答' }

const categories = ref([])
const list = ref([])
const filterCat = ref('')
const showForm = ref(false)
const optionsText = ref('')
const form = ref({})

onMounted(async () => {
  // 管理页只给管理员；成员误入弹回首页
  const user = JSON.parse(localStorage.getItem('kb-user') || '{"role":""}')
  if (user.role !== 'ADMIN') {
    ElMessage.warning('题库管理仅管理员可用')
    router.replace('/')
    return
  }
  categories.value = await http.get('/drill/categories')
  load()
})

async function load() {
  list.value = await http.get('/question/list' + (filterCat.value ? `?category=${encodeURIComponent(filterCat.value)}` : ''))
}

const isChoice = computed(() => form.value.type === 'SINGLE' || form.value.type === 'MULTI')
const answerLabel = computed(() => {
  if (form.value.type === 'SINGLE') return '正确答案（字母）'
  if (form.value.type === 'MULTI') return '正确答案（字母连写，如AC）'
  if (form.value.type === 'FILL') return '正确答案'
  return '参考答案'
})
const answerPlaceholder = computed(() => {
  if (form.value.type === 'SINGLE') return 'B'
  if (form.value.type === 'MULTI') return 'AC'
  return ''
})

function openForm(row) {
  if (row) {
    form.value = { ...row }
    try { optionsText.value = (JSON.parse(row.options || '[]') || []).join('\n') } catch { optionsText.value = '' }
  } else {
    form.value = { type: 'SINGLE', category: filterCat.value || '' }
    optionsText.value = ''
  }
  showForm.value = true
}

async function save() {
  if (isChoice.value) {
    // 选项每行一个 -> JSON数组
    form.value.options = JSON.stringify(optionsText.value.split('\n').map(s => s.trim()).filter(Boolean))
  }
  if (!form.value.relatedDocId) form.value.relatedDocId = null
  if (form.value.id) {
    await http.put(`/question/${form.value.id}`, form.value)
  } else {
    await http.post('/question', form.value)
  }
  ElMessage.success('已保存')
  showForm.value = false
  load()
}

async function del(row) {
  try {
    await ElMessageBox.confirm(`删除题目「${row.stem.slice(0, 20)}…」？`, '删除', { type: 'warning' })
  } catch { return }
  await http.delete(`/question/${row.id}`)
  ElMessage.success('已删除')
  load()
}
</script>

<style scoped>
.manage-page {
  min-height: 100vh;
  background: var(--kb-bg);
  padding: 20px;
}
.manage-top {
  max-width: 960px;
  margin: 0 auto 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.manage-top h2 { margin: 0; font-size: 22px; font-weight: 700; letter-spacing: 1px; }
.manage-ops { display: flex; align-items: center; gap: 10px; }
.manage-page :deep(.el-table) {
  max-width: 960px;
  margin: 0 auto;
  border-radius: var(--kb-radius);
  overflow: hidden;
  box-shadow: var(--kb-shadow-sm);
  border: 1px solid var(--kb-line);
}
</style>
