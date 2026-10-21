import { createRouter, createWebHistory } from 'vue-router'
import { ROLE_KEY, TOKEN_KEY } from '@/api/request'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true, title: '登录' }
    },
    {
      path: '/',
      component: () => import('@/components/AppLayout.vue'),
      children: [
        {
          path: 'selection',
          name: 'selection',
          component: () => import('@/views/SelectionView.vue'),
          meta: { role: 'STUDENT', title: '选课大厅' }
        },
        {
          path: 'timetable',
          name: 'timetable',
          component: () => import('@/views/TimetableView.vue'),
          meta: { role: 'STUDENT', title: '我的课表' }
        },
        {
          path: 'plan',
          name: 'plan',
          component: () => import('@/views/PlanView.vue'),
          meta: { role: 'STUDENT', title: '培养计划' }
        },
        {
          path: 'admin/dashboard',
          name: 'admin-dashboard',
          component: () => import('@/views/admin/DashboardView.vue'),
          meta: { role: 'ADMIN', title: '数据看板' }
        },
        {
          path: 'admin/rounds',
          name: 'admin-rounds',
          component: () => import('@/views/admin/RoundsView.vue'),
          meta: { role: 'ADMIN', title: '轮次配置' }
        },
        {
          path: 'admin/classes',
          name: 'admin-classes',
          component: () => import('@/views/admin/ClassesView.vue'),
          meta: { role: 'ADMIN', title: '教学班管理' }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: () => (localStorage.getItem(ROLE_KEY) === 'ADMIN' ? '/admin/dashboard' : '/selection')
    }
  ]
})

router.beforeEach((to) => {
  const token = localStorage.getItem(TOKEN_KEY)
  const role = localStorage.getItem(ROLE_KEY)

  if (to.meta.public) {
    if (token) {
      return role === 'ADMIN' ? '/admin/dashboard' : '/selection'
    }
    return true
  }
  if (!token) {
    return '/login'
  }
  if (to.meta.role && to.meta.role !== role) {
    return role === 'ADMIN' ? '/admin/dashboard' : '/selection'
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 上理选课` : '上理选课 · 教学演示系统'
})

export default router
