<template>
  <!-- 动态虚拟面试官：半写实插画风，眨眼/眼神/嘴型/头部全部由随机时序驱动，动作不机械重复 -->
  <svg class="iv-avatar" viewBox="0 0 300 360">
    <defs>
      <!-- 皮肤渐变，做出立体感 -->
      <radialGradient id="skin" cx="50%" cy="42%" r="70%">
        <stop offset="0%" :stop-color="c.skinHi" />
        <stop offset="100%" :stop-color="c.skinLo" />
      </radialGradient>
      <linearGradient id="blazer" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" :stop-color="c.suitHi" />
        <stop offset="100%" :stop-color="c.suitLo" />
      </linearGradient>
      <linearGradient id="hair" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" :stop-color="c.hairHi" />
        <stop offset="100%" :stop-color="c.hairLo" />
      </linearGradient>
    </defs>

    <!-- 身体：呼吸起伏由CSS循环，偶尔耸肩由JS触发 -->
    <g class="body" :style="{ transform: bodyT }">
      <!-- 西装 -->
      <path d="M52 360 Q58 268 118 252 L150 272 L182 252 Q242 268 248 360 Z" fill="url(#blazer)" />
      <!-- 左右翻领 -->
      <path d="M118 252 L150 272 L127 296 L104 264 Z" :fill="c.suitLo" />
      <path d="M182 252 L150 272 L173 296 L196 264 Z" :fill="c.suitLo" />
      <!-- 衬衫 -->
      <path d="M131 259 L150 272 L169 259 L165 305 L135 305 Z" fill="#f7f9fb" />
      <!-- 领带 / 丝巾 -->
      <path v-if="variant === 'm'" d="M150 272 L141 285 L150 322 L159 285 Z" fill="#00a868" />
      <path v-else d="M138 262 Q150 276 162 262 Q150 288 138 262 Z" fill="#d98c5f" />
    </g>

    <!-- 头部：整体可轻摆/侧倾，颈部跟着动 -->
    <g class="head" :style="{ transform: headT }">
      <!-- 颈 -->
      <rect x="132" y="218" width="36" height="46" rx="16" :fill="c.skinLo" />
      <!-- 后层头发（女：披肩） -->
      <path v-if="variant === 'f'"
        d="M88 150 Q78 60 150 52 Q222 60 212 150 L216 250 Q206 262 196 250 L196 150 L104 150 L104 250 Q94 262 84 250 Z"
        fill="url(#hair)" />
      <!-- 耳 -->
      <ellipse cx="92" cy="182" rx="11" ry="16" :fill="c.skinLo" />
      <ellipse cx="208" cy="182" rx="11" ry="16" :fill="c.skinLo" />
      <!-- 脸 -->
      <ellipse cx="150" cy="178" rx="60" ry="72" fill="url(#skin)" />
      <!-- 前层头发/刘海 -->
      <path v-if="variant === 'f'"
        d="M90 168 Q86 66 150 58 Q214 66 210 168 Q208 118 178 106 Q158 98 150 100 Q142 98 122 106 Q92 118 90 168 Z"
        fill="url(#hair)" />
      <path v-else
        d="M92 160 Q90 70 150 62 Q210 70 208 160 Q206 118 176 108 Q154 100 150 102 Q146 100 124 108 Q94 118 92 160 Z"
        fill="url(#hair)" />

      <!-- 眉毛：说话/惊讶时上挑 -->
      <path class="brow" :style="{ transform: `translateY(${browY}px)` }"
        d="M112 150 Q126 143 140 150" :stroke="c.hairLo" stroke-width="5" fill="none" stroke-linecap="round" />
      <path class="brow" :style="{ transform: `translateY(${browY}px)` }"
        d="M160 150 Q174 143 188 150" :stroke="c.hairLo" stroke-width="5" fill="none" stroke-linecap="round" />

      <!-- 左眼：blink负责眨眼(scaleY)，iris负责眼神移动(translate) -->
      <g class="eye" :style="{ transform: `scaleY(${blink})` }">
        <ellipse cx="126" cy="172" rx="13" ry="9" fill="#fff" />
        <g class="iris" :style="{ transform: `translate(${gaze.x}px, ${gaze.y}px)` }">
          <circle cx="126" cy="172" r="6.5" :fill="c.iris" />
          <circle cx="126" cy="172" r="3" fill="#1c1c1c" />
          <circle cx="128" cy="169.5" r="1.6" fill="#fff" />
        </g>
        <path d="M113 168 Q126 160 139 168" :stroke="c.hairLo" stroke-width="2.5" fill="none" stroke-linecap="round" />
      </g>
      <!-- 右眼 -->
      <g class="eye" :style="{ transform: `scaleY(${blink})` }">
        <ellipse cx="174" cy="172" rx="13" ry="9" fill="#fff" />
        <g class="iris" :style="{ transform: `translate(${gaze.x}px, ${gaze.y}px)` }">
          <circle cx="174" cy="172" r="6.5" :fill="c.iris" />
          <circle cx="174" cy="172" r="3" fill="#1c1c1c" />
          <circle cx="176" cy="169.5" r="1.6" fill="#fff" />
        </g>
        <path d="M161 168 Q174 160 187 168" :stroke="c.hairLo" stroke-width="2.5" fill="none" stroke-linecap="round" />
      </g>

      <!-- 眼镜（男） -->
      <g v-if="variant === 'm'" :stroke="c.glass" stroke-width="3" fill="rgba(255,255,255,0.12)">
        <rect x="108" y="160" width="36" height="26" rx="9" />
        <rect x="156" y="160" width="36" height="26" rx="9" />
        <line x1="144" y1="171" x2="156" y2="171" />
        <line x1="108" y1="171" x2="95" y2="167" />
        <line x1="192" y1="171" x2="205" y2="167" />
      </g>

      <!-- 鼻 -->
      <path d="M150 186 Q146 202 152 205" :stroke="c.nose" stroke-width="3.5" fill="none" stroke-linecap="round" />
      <!-- 腮红 -->
      <ellipse cx="112" cy="202" rx="11" ry="6" fill="rgba(240,128,96,0.20)" />
      <ellipse cx="188" cy="202" rx="11" ry="6" fill="rgba(240,128,96,0.20)" />
      <!-- 嘴：说话时JS随机切换嘴型 -->
      <path class="mouth" :d="mouthD" :fill="c.mouth" />
      <path v-if="mouthOpen" class="teeth" :d="teethD" fill="#fff" opacity="0.9" />
    </g>
  </svg>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'

const props = defineProps({
  speaking: { type: Boolean, default: false },   // 面试官正在朗读
  listening: { type: Boolean, default: false },   // 正在听用户说话
  thinking: { type: Boolean, default: false },    // AI评判思考中
  variant: { type: String, default: 'f' }         // m男 f女
})

// 两套配色：女=米色西装深棕发，男=藏蓝西装黑发文
const c = computed(() => props.variant === 'm'
  ? { skinHi: '#f3d3ac', skinLo: '#e6ba8d', suitHi: '#3c5266', suitLo: '#2e4053',
      hairHi: '#2c2c30', hairLo: '#1d1d21', iris: '#5a4632', nose: '#d9a97c',
      mouth: '#8a4a42', glass: '#3c3c40' }
  : { skinHi: '#f8dcc0', skinLo: '#eec49d', suitHi: '#dcc5a4', suitLo: '#c8ae8a',
      hairHi: '#4a3527', hairLo: '#33241a', iris: '#5a4632', nose: '#e0ab80',
      mouth: '#b3543f', glass: '#3c3c40' })

// ===== 可驱动的动画状态 =====
const blink = ref(1)            // 眼睛纵向缩放=眨眼
const gaze = ref({ x: 0, y: 0 })// 眼神偏移
const browY = ref(0)            // 眉毛高度
const headT = ref('rotate(0deg) translate(0px,0px)') // 头部姿态
const bodyT = ref('translateY(0px)')
const mouthD = ref(MOUTH.smile) // 当前嘴型
const mouthOpen = ref(false)
const teethD = ref('')

// 嘴型库（同一坐标系，可随机切换模拟说话）
const MOUTH = {
  smile: 'M128 224 Q150 238 172 224 Q150 231 128 224 Z',
  small: 'M134 223 Q150 221 166 223 Q150 236 134 223 Z',
  open:  'M134 220 Q150 216 166 220 Q150 243 134 220 Z',
  wide:  'M131 218 Q150 212 169 218 Q150 250 131 218 Z',
  o:     'M141 219 Q150 215 159 219 Q161 231 150 235 Q139 231 141 219 Z'
}
const TALK = [MOUTH.small, MOUTH.open, MOUTH.wide, MOUTH.o]
const TEETH = {
  [MOUTH.open]: 'M136 221 Q150 218 164 221 L164 226 Q150 224 136 226 Z',
  [MOUTH.wide]: 'M134 219 Q150 215 166 219 L166 225 Q150 222 134 225 Z'
}

// ===== 随机工具：灵动的核心是"不规律" =====
const rand = (a, b) => a + Math.random() * (b - a)
const pick = arr => arr[Math.floor(Math.random() * arr.length)]
let timers = []
function later(fn, ms) { const t = setTimeout(fn, ms); timers.push(t); return t }
function clearAll() { timers.forEach(clearTimeout); timers = [] }

// 眨眼：随机2.5~6秒一次，偶尔连眨两下
function scheduleBlink() {
  later(() => {
    blink.value = 0.08
    later(() => {
      blink.value = 1
      if (Math.random() < 0.18) {          // 18%概率双眨
        later(() => { blink.value = 0.08; later(() => { blink.value = 1 }, 90) }, 140)
      }
    }, 110)
    scheduleBlink()
  }, rand(2500, 6000))
}

// 眼神：大部分时间看你(镜头)，偶尔瞟向别处/低头看材料
function scheduleGaze() {
  later(() => {
    const r = Math.random()
    if (props.listening) {
      gaze.value = { x: 0, y: 0 }           // 听你说话时锁定眼神交流
    } else if (props.thinking) {
      gaze.value = { x: pick([-5, 5]), y: -5 } // 思考时眼神上飘
    } else if (r < 0.6) {
      gaze.value = { x: 0, y: 0 }
    } else if (r < 0.82) {
      gaze.value = { x: pick([-6, 6]), y: pick([-2, 2]) }
    } else {
      gaze.value = { x: pick([-3, 3]), y: 6 } // 低头
    }
    scheduleGaze()
  }, rand(1400, 3400))
}

// 头部：缓慢摆向随机角度，停顿，再换方向——像真人微调坐姿
function scheduleHead() {
  later(() => {
    if (props.listening) {
      // 聆听时点头：轻幅快速
      headT.value = `rotate(${rand(-1, 1).toFixed(1)}deg) translate(${rand(-1, 1).toFixed(1)}px, ${rand(1, 3).toFixed(1)}px)`
      later(() => { headT.value = 'rotate(0deg) translate(0px,0px)' }, 320)
    } else {
      headT.value = `rotate(${rand(-3, 3).toFixed(1)}deg) translate(${rand(-3, 3).toFixed(1)}px, ${rand(-2, 2).toFixed(1)}px)`
    }
    scheduleHead()
  }, props.listening ? rand(600, 900) : rand(2000, 4200))
}

// 眉毛：说话时偶尔上扬，强调语气
function scheduleBrow() {
  later(() => {
    if (props.speaking && Math.random() < 0.5) {
      browY.value = -3
      later(() => { browY.value = 0 }, 380)
    }
    scheduleBrow()
  }, rand(1200, 2600))
}

// 说话嘴型：随机切换+随机时长，模拟真实语速起伏
let talkTimer = null
function talkLoop() {
  if (!props.speaking) return
  const d = pick(TALK)
  mouthD.value = d
  mouthOpen.value = !!(d === MOUTH.open || d === MOUTH.wide)
  teethD.value = TEETH[d] || ''
  talkTimer = later(talkLoop, rand(90, 170))
}
watch(() => props.speaking, on => {
  if (on) { talkLoop() }
  else {
    if (talkTimer) clearTimeout(talkTimer)
    mouthD.value = MOUTH.smile
    mouthOpen.value = false
  }
})
// 思考时嘴抿住
watch(() => props.thinking, on => {
  if (!props.speaking) mouthD.value = on ? MOUTH.small : MOUTH.smile
})

onMounted(() => {
  scheduleBlink()
  scheduleGaze()
  scheduleHead()
  scheduleBrow()
})
onUnmounted(() => {
  clearAll()
  if (talkTimer) clearTimeout(talkTimer)
})
</script>

<style scoped>
.iv-avatar { width: 100%; height: 100%; display: block; }
/* 所有可动部位用柔和过渡，JS只负责给随机目标值，动画由CSS平滑插值——动作自然不僵硬 */
.head { transform-origin: 150px 250px; transition: transform 1.1s cubic-bezier(0.37, 0, 0.63, 1); }
.body { transform-origin: 150px 360px; transition: transform 0.9s ease; animation: breathe 4.5s ease-in-out infinite; }
.eye { transform-origin: center; transform-box: fill-box; transition: transform 0.09s ease; }
.iris { transform-box: fill-box; transform-origin: center; transition: transform 0.5s cubic-bezier(0.33, 1, 0.68, 1); }
.brow { transform-box: fill-box; transform-origin: center; transition: transform 0.3s ease; }
.mouth { transition: d 0.08s linear; }
@keyframes breathe { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-3px); } }
</style>
