import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { requiresGuest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { requiresGuest: true }
  },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/goods',
    children: [
      {
        path: 'goods',
        name: 'Goods',
        component: () => import('../views/GoodsList.vue')
      },
      {
        path: 'seckill',
        name: 'Seckill',
        component: () => import('../views/SeckillList.vue')
      },
      {
        path: 'auction',
        name: 'Auction',
        component: () => import('../views/AuctionList.vue')
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'user-center',
        name: 'UserCenter',
        component: () => import('../views/UserCenter.vue'),
        meta: { requiresAuth: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  if (to.matched.some(record => record.meta.requiresAuth) && !userStore.isLoggedIn) {
    next('/login')
  } else if (to.matched.some(record => record.meta.requiresGuest) && userStore.isLoggedIn) {
    next('/')
  } else {
    next()
  }
})

export default router
