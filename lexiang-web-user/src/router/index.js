import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // ===== 登录 / 注册（独立页面，无外层布局） =====
    { path: '/login', name: 'login', component: () => import('@/views/user/Login.vue') },
    { path: '/register', name: 'register', component: () => import('@/views/user/Register.vue') },
    { path: '/admin/login', name: 'merchantLogin', component: () => import('@/views/admin/MerchantLogin.vue') },

    // ===== 用户端 =====
    {
      path: '/',
      component: () => import('@/layouts/UserLayout.vue'),
      children: [
        { path: '', redirect: '/home' },
        { path: 'home', name: 'home', component: () => import('@/views/user/Home.vue') },
        { path: 'ai', name: 'ai', component: () => import('@/views/user/Ai.vue') },
        { path: 'cart', name: 'cart', component: () => import('@/views/user/Cart.vue'), meta: { requireAuth: true } },
        { path: 'order', name: 'userOrder', component: () => import('@/views/user/OrderList.vue'), meta: { requireAuth: true } },
        { path: 'order/paySuccess', name: 'paySuccess', component: () => import('@/views/user/PaySuccess.vue'), meta: { requireAuth: true } },
        { path: 'order/:id', name: 'userOrderDetail', component: () => import('@/views/user/OrderDetail.vue'), meta: { requireAuth: true } },
        { path: 'checkout', name: 'checkout', component: () => import('@/views/user/Checkout.vue'), meta: { requireAuth: true } },
        { path: 'address', name: 'address', component: () => import('@/views/user/Address.vue'), meta: { requireAuth: true } },
        { path: 'profile', name: 'profile', component: () => import('@/views/user/Profile.vue'), meta: { requireAuth: true } },
      ]
    },

    // ===== 商家后台 =====
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      redirect: '/admin/dashboard',
      children: [
        { path: 'dashboard', name: 'dashboard', component: () => import('@/views/admin/Dashboard.vue'), meta: { title: '工作台', requireAdmin: true } },
        { path: 'statistics', name: 'adminStatistics', component: () => import('@/views/admin/Statistics.vue'), meta: { title: '数据统计', requireAdmin: true } },
        { path: 'order', name: 'adminOrder', component: () => import('@/views/admin/OrderManage.vue'), meta: { title: '订单管理', requireAdmin: true } },
        { path: 'dish', name: 'adminDish', component: () => import('@/views/admin/DishManage.vue'), meta: { title: '菜品管理', requireAdmin: true } },
        { path: 'category', name: 'adminCategory', component: () => import('@/views/admin/CategoryManage.vue'), meta: { title: '分类管理', requireAdmin: true } },
        { path: 'spec', name: 'adminSpec', component: () => import('@/views/admin/SpecManage.vue'), meta: { title: '规格管理', requireAdmin: true } },
        { path: 'banner', name: 'adminBanner', component: () => import('@/views/admin/BannerManage.vue'), meta: { title: '轮播图管理', requireAdmin: true } },
        { path: 'staff', name: 'adminStaff', component: () => import('@/views/admin/EmployeeManage.vue'), meta: { title: '员工管理', requireAdmin: true } },
        { path: 'ai', name: 'adminAi', component: () => import('@/views/admin/AiService.vue'), meta: { title: 'AI 智能客服', requireAdmin: true } },
        { path: 'setmeal', redirect: '/admin/dish' },
        // 商家端订单详情使用独立组件，不与用户端复用
        { path: 'order/:id', name: 'adminOrderDetail', component: () => import('@/views/admin/OrderDetail.vue'), meta: { title: '订单详情', requireAdmin: true } },
      ]
    },

    // ===== 404 兜底 =====
    { path: '/:pathMatch(.*)*', name: 'notFound', component: () => import('@/views/user/NotFound.vue') }
  ]
})

/**
 * 全局路由守卫
 *
 * 【安全规则】
 * 1. 用户端 token (localStorage.token) 和商家端 token (localStorage.adminToken) 完全隔离
 * 2. 商家端路由（/admin/* 除 /admin/login）必须携带 adminToken，否则跳商家登录页
 * 3. 用户端受保护路由（meta.requireAuth）必须携带 token，否则跳用户登录页
 * 4. 首页、登录页始终放行
 * 5. token 失效时统一跳转对应登录页
 */
router.beforeEach((to, from, next) => {
  document.title = to.meta?.title || '字节餐饮'
  const token = localStorage.getItem('token')
  const adminToken = localStorage.getItem('adminToken')

  // 首页始终放行
  if (to.path === '/' || to.path === '/home') {
    return next()
  }

  // 已登录用户/商家不允许回到登录页
  if (token && to.path === '/login') return next('/home')
  if (adminToken && to.path === '/admin/login') return next('/admin/dashboard')

  // 商家端路由：必须携带 adminToken
  if (to.path.startsWith('/admin') && to.path !== '/admin/login') {
    if (!adminToken) {
      return next('/admin/login')
    }
    return next()
  }

  // 用户端受保护路由：必须携带 token
  if (to.meta?.requireAuth && !token) {
    return next('/login')
  }

  next()
})

export default router

/**
 * 路由错误处理：懒加载组件失败时显示 404 非白屏
 */
router.onError((error) => {
  console.error('路由加载失败:', error)
  const pattern = /Failed to fetch dynamically imported module|Failed to load module script|Importing a module script failed/i
  if (pattern.test(error.message)) {
    // 试硬刷新
    window.location.href = window.location.href
  }
})
