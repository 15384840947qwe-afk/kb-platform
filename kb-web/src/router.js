import { createRouter, createWebHistory } from 'vue-router'
import Login from './views/Login.vue'
import Home from './views/Home.vue'
import Drill from './views/Drill.vue'
import Manage from './views/Manage.vue'
import Interview from './views/Interview.vue'
import Stats from './views/Stats.vue'
import Resume from './views/Resume.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: Login },
    { path: '/', component: Home },
    { path: '/drill', component: Drill },
    { path: '/manage', component: Manage },
    { path: '/interview', component: Interview },
    { path: '/stats', component: Stats },
    { path: '/resume', component: Resume }
  ]
})

// 全局路由守卫：没token一律去登录页
router.beforeEach((to, from, next) => {
  const t = localStorage.getItem('kb-token')
  if (to.path !== '/login' && !t) next('/login')
  else next()
})

export default router
