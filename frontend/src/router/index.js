import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../views/Dashboard.vue'
import DataImport from '../views/DataImport.vue'
import RiskPrediction from '../views/RiskPrediction.vue'
import History from '../views/History.vue'
import CompanyManagement from '../views/CompanyManagement.vue'
import Login from '../views/Login.vue'
import Settings from '../views/Settings.vue'
import Evaluation from '../views/Evaluation.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/',
    name: 'Dashboard',
    component: Dashboard,
    meta: { requiresAuth: true }
  },
  {
    path: '/evaluation',
    name: 'Evaluation',
    component: Evaluation,
    meta: { requiresAuth: true }
  },
  {
    path: '/companies',
    name: 'CompanyManagement',
    component: CompanyManagement,
    meta: { requiresAuth: true }
  },
  {
    path: '/import',
    name: 'DataImport',
    component: DataImport,
    meta: { requiresAuth: true }
  },
  {
    path: '/predict',
    name: 'RiskPrediction',
    component: RiskPrediction,
    meta: { requiresAuth: true }
  },
  {
    path: '/history',
    name: 'History',
    component: History,
    meta: { requiresAuth: true }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: Settings,
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：检查是否登录
router.beforeEach((to, from, next) => {
  const user = localStorage.getItem('user')
  if (to.meta.requiresAuth && !user) {
    next('/login')
  } else {
    next()
  }
})

export default router
