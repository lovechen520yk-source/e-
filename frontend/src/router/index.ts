import { createRouter, createWebHashHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/records'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录', noAuth: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { title: '注册', noAuth: true }
  },
  {
    path: '/records',
    name: 'RecordList',
    component: () => import('@/views/RecordList.vue'),
    meta: { title: '工时记录' }
  },
  {
    path: '/records/add',
    name: 'RecordAdd',
    component: () => import('@/views/RecordForm.vue'),
    meta: { title: '新增记录' }
  },
  {
    path: '/records/edit/:id',
    name: 'RecordEdit',
    component: () => import('@/views/RecordForm.vue'),
    meta: { title: '编辑记录' }
  },
  {
    path: '/records/monthly-detail/:yearMonth',
    name: 'MonthlyDetail',
    component: () => import('@/views/MonthlyDetail.vue'),
    meta: { title: '月度详情' }
  },
  {
    path: '/stats',
    name: 'Stats',
    component: () => import('@/views/StatsView.vue'),
    meta: { title: '数据统计' }
  },
  {
    path: '/config',
    name: 'Config',
    component: () => import('@/views/ConfigView.vue'),
    meta: { title: '系统配置' }
  },
  {
    path: '/config/edit',
    name: 'ConfigEdit',
    component: () => import('@/views/ProfileEdit.vue'),
    meta: { title: '个人信息' }
  },
  {
    path: '/export',
    name: 'Export',
    component: () => import('@/views/ExportView.vue'),
    meta: { title: '数据导出' }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 路由守卫：未登录时跳转到登录页
router.beforeEach((to, _from, next) => {
  // 不需要登录的页面放行
  if (to.meta.noAuth) {
    next()
    return
  }

  // 检查是否已登录（localStorage 中是否有 token）
  const token = localStorage.getItem('token')
  if (!token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  next()
})

export default router
