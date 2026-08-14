<template>
  <div class="login-wrap">
    <!-- 左侧品牌区：渐变底 + 产品主张 -->
    <div class="brand-side">
      <div class="deco deco-1"></div>
      <div class="deco deco-2"></div>
      <div class="brand-inner">
        <div class="brand-logo">
          <span class="badge">KB</span>
          <span class="brand-name">KB 知识库</span>
        </div>
        <h1 class="brand-title">沉淀知识<br />让每个岗位更专业</h1>
        <p class="brand-sub">面向一切知识型岗位的团队知识库与求职工作台</p>
        <ul class="brand-points">
          <li>文档协作与 AI 文档问答，知识即查即用</li>
          <li>AI 刷题批改、模拟面试，流式实时反馈</li>
          <li>成长看板 + 简历助手，从学习到求职闭环</li>
        </ul>
      </div>
      <div class="brand-foot">KB · Knowledge Base</div>
    </div>

    <!-- 右侧表单区 -->
    <div class="form-side">
      <div class="login-card">
        <div class="logo">KB</div>
        <h2 class="title">欢迎回来</h2>
        <p class="subtitle">登录你的团队知识仓库</p>
        <el-form :model="form">
          <el-form-item>
            <el-input v-model="form.username" placeholder="用户名" size="large" clearable />
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              size="large"
              show-password
              @keyup.enter="doLogin"
            />
          </el-form-item>
          <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="doLogin">
            登 录
          </el-button>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http.js'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

async function doLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const data = await http.post('/auth/login', form)
    localStorage.setItem('kb-token', data.token)
    localStorage.setItem('kb-user', JSON.stringify({ nickname: data.nickname, role: data.role }))
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  height: 100vh;
  display: flex;
  background: var(--kb-bg);
}

/* 左侧品牌区 */
.brand-side {
  flex: 1.15;
  position: relative;
  overflow: hidden;
  background: linear-gradient(150deg, #00c97b 0%, #009a5b 55%, #00713f 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 64px 72px;
}
/* 装饰圆：低透明度增加层次，不抢内容 */
.deco {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, .07);
}
.deco-1 { width: 420px; height: 420px; right: -120px; top: -140px; }
.deco-2 { width: 300px; height: 300px; left: -100px; bottom: -110px; background: rgba(255, 255, 255, .05); }

.brand-inner { position: relative; max-width: 480px; }
.brand-logo { display: flex; align-items: center; gap: 10px; margin-bottom: 48px; }
.badge {
  width: 40px; height: 40px; border-radius: 10px; background: rgba(255, 255, 255, .16);
  border: 1px solid rgba(255, 255, 255, .3);
  display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 15px; letter-spacing: 1px;
}
.brand-name { font-size: 17px; font-weight: 600; letter-spacing: 1px; }
.brand-title { font-size: 38px; line-height: 1.35; font-weight: 700; margin: 0 0 14px; letter-spacing: 2px; }
.brand-sub { font-size: 15px; opacity: .85; margin: 0 0 44px; }
.brand-points { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 16px; }
.brand-points li {
  font-size: 14px; opacity: .92; padding-left: 26px; position: relative; line-height: 1.6;
}
/* 绿色底上的对勾圆点 */
.brand-points li::before {
  content: ""; position: absolute; left: 0; top: 4px;
  width: 16px; height: 16px; border-radius: 50%;
  background: rgba(255, 255, 255, .22);
}
.brand-points li::after {
  content: ""; position: absolute; left: 5px; top: 9px;
  width: 7px; height: 4px; border-left: 2px solid #fff; border-bottom: 2px solid #fff;
  transform: rotate(-45deg);
}
.brand-foot {
  position: absolute; left: 72px; bottom: 32px;
  font-size: 12px; opacity: .55; letter-spacing: 1px;
}

/* 右侧表单区 */
.form-side {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.login-card {
  width: 380px;
  background: #fff;
  border: 1px solid var(--kb-line);
  border-radius: 18px;
  box-shadow: var(--kb-shadow-lg);
  padding: 48px 40px 36px;
  text-align: center;
}
.logo {
  width: 56px;
  height: 56px;
  margin: 0 auto 16px;
  border-radius: 14px;
  background: var(--kb-brand-grad);
  box-shadow: 0 6px 16px rgba(0, 185, 107, .32);
  color: #fff;
  font-size: 21px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
.title { margin: 0; font-size: 22px; font-weight: 700; }
.subtitle { margin: 8px 0 30px; color: var(--kb-ink-3); font-size: 13px; }
.login-btn { width: 100%; }
.login-card :deep(.el-form-item) { margin-bottom: 20px; }

/* 窄屏：藏掉品牌区只留表单 */
@media (max-width: 860px) {
  .brand-side { display: none; }
}
</style>
