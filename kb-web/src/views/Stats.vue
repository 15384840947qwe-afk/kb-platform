<template>
  <div class="stats-page">
    <header class="stats-top">
      <a class="quit" @click="$router.push('/')">×</a>
      <span class="stats-title">成长看板</span>
      <span class="stats-sub">刷题与面试的数据足迹</span>
    </header>

    <!-- 完全没数据：引导而不是三张空图 -->
    <div v-if="empty" class="stats-empty">
      <p>还没有数据，先去<a @click="$router.push('/drill')">刷题</a>或<a @click="$router.push('/interview')">面试</a>一场吧</p>
    </div>

    <div v-else class="stats-content">
      <!-- 概览卡：从已有数据汇总，不进新接口 -->
      <div class="stats-overview">
        <div v-for="o in overview" :key="o.label" class="ov-card">
          <div class="ov-num">{{ o.value }}<small>{{ o.unit }}</small></div>
          <div class="ov-label">{{ o.label }}</div>
        </div>
      </div>

      <div class="stats-grid">
      <section class="stats-card">
        <h3>分科目正确率</h3>
        <div v-if="!data.perCategory.length" class="chart-empty">刷几道题就有科目数据了</div>
        <div v-else ref="catChart" class="chart"></div>
      </section>

      <section class="stats-card">
        <h3>近 7 天刷题趋势</h3>
        <div v-if="!recent7HasData" class="chart-empty">最近 7 天还没刷题，今天来一组？</div>
        <div v-else ref="weekChart" class="chart"></div>
      </section>

      <section class="stats-card wide">
        <h3>面试分数趋势（最近 20 场）</h3>
        <div v-if="!data.scores.length" class="chart-empty">还没面过试，去体验一场模拟面试吧</div>
        <div v-else ref="scoreChart" class="chart"></div>
      </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import http from '../api/http.js'

const data = ref({ perCategory: [], recent7: [], scores: [] })
const catChart = ref(null)
const weekChart = ref(null)
const scoreChart = ref(null)
const charts = []

const empty = computed(() =>
  !data.value.perCategory.length && !data.value.recent7.some(d => d.total > 0) && !data.value.scores.length)
const recent7HasData = computed(() => data.value.recent7.some(d => d.total > 0))

/** 概览指标：全部由 dashboard 已有数据汇总，不新增接口 */
const overview = computed(() => {
  const total = data.value.perCategory.reduce((s, x) => s + x.total, 0)
  const correct = data.value.perCategory.reduce((s, x) => s + x.correct, 0)
  const scores = data.value.scores
  const avg = scores.length ? Math.round(scores.reduce((s, x) => s + x.score, 0) / scores.length) : null
  return [
    { label: '累计刷题', value: total, unit: '题' },
    { label: '总体正确率', value: total ? Math.round(correct / total * 100) : 0, unit: '%' },
    { label: '模拟面试', value: scores.length, unit: '场' },
    { label: '面试平均分', value: avg ?? '—', unit: avg != null ? '分' : '' }
  ]
})

onMounted(async () => {
  data.value = await http.get('/drill/dashboard')
  await nextTick()
  renderAll()
  window.addEventListener('resize', resizeAll)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeAll)
  charts.forEach(c => c.dispose())
})

function resizeAll() { charts.forEach(c => c.resize()) }

const GREEN = '#00b96b'
const BASE = {
  grid: { left: 40, right: 16, top: 30, bottom: 28 },
  tooltip: { trigger: 'axis' }
}

function renderAll() {
  // 分科目正确率：柱状图，柱上带题量提示
  if (data.value.perCategory.length && catChart.value) {
    const c = echarts.init(catChart.value)
    c.setOption({
      ...BASE,
      xAxis: { type: 'category', data: data.value.perCategory.map(x => x.category) },
      yAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
      series: [{
        type: 'bar', barMaxWidth: 36,
        data: data.value.perCategory.map(x => x.rate),
        itemStyle: { color: GREEN, borderRadius: [6, 6, 0, 0] },
        label: { show: true, position: 'top', formatter: '{c}%' }
      }],
      tooltip: {
        trigger: 'axis',
        formatter: p => {
          const x = data.value.perCategory[p[0].dataIndex]
          return `${x.category}<br/>正确率 ${x.rate}%（${x.correct}/${x.total}）`
        }
      }
    })
    charts.push(c)
  }

  // 近7天：总量与正确数双折线
  if (recent7HasData.value && weekChart.value) {
    const c = echarts.init(weekChart.value)
    c.setOption({
      ...BASE,
      legend: { data: ['刷题量', '答对数'], top: 0 },
      xAxis: { type: 'category', data: data.value.recent7.map(d => d.date.slice(5)) },
      yAxis: { type: 'value', minInterval: 1 },
      series: [
        { name: '刷题量', type: 'line', smooth: true, data: data.value.recent7.map(d => d.total),
          itemStyle: { color: GREEN }, areaStyle: { opacity: 0.08 } },
        { name: '答对数', type: 'line', smooth: true, data: data.value.recent7.map(d => d.correct),
          itemStyle: { color: '#409eff' } }
      ]
    })
    charts.push(c)
  }

  // 面试分数：折线，tooltip带科目
  if (data.value.scores.length && scoreChart.value) {
    const c = echarts.init(scoreChart.value)
    c.setOption({
      ...BASE,
      xAxis: {
        type: 'category',
        data: data.value.scores.map(s => (s.createTime || '').replace('T', ' ').slice(5, 16))
      },
      yAxis: { type: 'value', min: 0, max: 100 },
      series: [{
        type: 'line', smooth: true, data: data.value.scores.map(s => s.score),
        itemStyle: { color: GREEN },
        areaStyle: { opacity: 0.08 },
        markLine: { silent: true, symbol: 'none', data: [{ yAxis: 60 }],
          lineStyle: { color: '#b88230', type: 'dashed' }, label: { formatter: '及格线' } }
      }],
      tooltip: {
        trigger: 'axis',
        formatter: p => {
          const s = data.value.scores[p[0].dataIndex]
          return `${s.category} · ${s.score} 分`
        }
      }
    })
    charts.push(c)
  }
}
</script>

<style scoped>
.stats-page { min-height: 100vh; background: var(--kb-bg); }
.stats-top {
  display: flex; align-items: center; gap: 12px;
  max-width: 960px; margin: 0 auto; padding: 14px 16px;
}
.quit {
  font-size: 18px; color: var(--kb-ink-3); cursor: pointer; line-height: 1;
  width: 30px; height: 30px; display: flex; align-items: center; justify-content: center;
  border-radius: 8px; transition: var(--kb-trans);
}
.quit:hover { background: var(--kb-side-hover); color: var(--kb-ink); }
.stats-title { font-weight: 700; font-size: 17px; letter-spacing: 1px; }
.stats-sub { font-size: 13px; color: var(--kb-ink-3); }

.stats-empty { text-align: center; padding: 80px 20px; color: var(--kb-ink-3); }
.stats-empty a { color: var(--el-color-primary-dark-2); cursor: pointer; margin: 0 4px; font-weight: 500; }

.stats-content { max-width: 960px; margin: 0 auto; padding: 4px 16px 24px; }
/* 概览卡：顶部四个指标，左侧品牌竖条 */
.stats-overview {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 14px;
}
.ov-card {
  background: #fff; border: 1px solid var(--kb-line); border-radius: var(--kb-radius);
  padding: 16px 18px;
  box-shadow: var(--kb-shadow-sm);
  position: relative;
  overflow: hidden;
}
.ov-card::before {
  content: ""; position: absolute; left: 0; top: 14px; bottom: 14px;
  width: 3px; border-radius: 2px; background: var(--kb-brand-grad);
}
.ov-num { font-size: 26px; font-weight: 700; color: var(--kb-ink); line-height: 1.2; }
.ov-num small { font-size: 12px; font-weight: 500; color: var(--kb-ink-3); margin-left: 3px; }
.ov-label { font-size: 12px; color: var(--kb-ink-3); margin-top: 4px; }

.stats-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 14px;
}
.stats-card {
  background: #fff; border: 1px solid var(--kb-line); border-radius: var(--kb-radius);
  padding: 18px;
  box-shadow: var(--kb-shadow-sm);
  transition: var(--kb-trans);
}
.stats-card:hover { box-shadow: var(--kb-shadow-md); transform: translateY(-2px); }
.stats-card.wide { grid-column: 1 / -1; }
.stats-card h3 { margin: 0 0 10px; font-size: 14px; color: var(--kb-ink); font-weight: 600; }
.chart { height: 260px; }
.chart-empty {
  height: 260px; display: flex; align-items: center; justify-content: center;
  color: var(--kb-ink-3); font-size: 13px;
}

@media (max-width: 768px) {
  .stats-grid { grid-template-columns: 1fr; }
  .stats-overview { grid-template-columns: 1fr 1fr; }
}
</style>
