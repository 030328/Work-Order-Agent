import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('../views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue')
      },
      {
        path: 'workorder/create',
        name: 'CreateWorkOrder',
        component: () => import('../views/CreateWorkOrder.vue')
      },
      {
        path: 'department-pool',
        name: 'DepartmentPool',
        component: () => import('../views/DepartmentPool.vue')
      },
      {
        path: 'workorder/:id',
        name: 'WorkOrderDetail',
        component: () => import('../views/WorkOrderDetail.vue')
      },
      {
        path: 'chat',
        name: 'Chat',
        component: () => import('../views/Chat.vue')
      },
      {
        path: 'knowledge',
        name: 'Knowledge',
        component: () => import('../views/Knowledge.vue')
      },
      {
        path: 'ai/analyze',
        name: 'AiAnalyze',
        component: () => import('../views/AiAnalyze.vue')
      },
      {
        path: 'users',
        name: 'UserManagement',
        component: () => import('../views/UserManagement.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
