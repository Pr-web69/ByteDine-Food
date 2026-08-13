<template>
  <div class="home">
    <header class="home-header"><div class="header-inner">
      <div class="brand" @click="$router.push('/home')"><img src="@/assets/logo-icon.png" class="brand-logo" alt="ByteBites" /><span class="brand-name">字节智能餐饮</span></div>
      <div class="header-search"><el-input v-model="keyword" placeholder="搜索菜品..." prefix-icon="Search" class="search-input" clearable @keyup.enter="onSearch" /></div>
      <div class="header-actions">
        <router-link to="/cart" class="cart-btn"><el-icon :size="22"><ShoppingCart /></el-icon><span class="cart-badge" v-if="cartCount > 0">{{ cartCount }}</span></router-link>
        <router-link to="/profile" class="profile-btn" v-if="token"><el-icon :size="20"><UserFilled /></el-icon></router-link>
        <router-link to="/login" class="login-link" v-else>登录</router-link>
      </div></div></header><div class="header-placeholder"></div>

    <section class="home-section" v-if="shopClosed">
      <div class="closed-banner">
        <el-icon :size="22"><InfoFilled /></el-icon>
        <span>商家已打烊，暂不接单，明日再来吧~</span>
      </div>
    </section>

    <section class="home-section" v-if="!loading"><div class="banner-wrap" @mouseenter="pauseBanner" @mouseleave="resumeBanner">
      <div class="banner-track" :style="{ transform: `translateX(-${bannerIdx * 100}%)` }"><div class="banner-slide" v-for="i in 5" :key="i">
        <img
            :src="`/images/banner/banner-${i}.jpg`"
            :class="['banner-img', i===1 ? 'banner-img-first' : '']"
            @error="e => e.target.src = fallbackBanner[i-1]"

        />

        <div class="banner-text"><span class="banner-tag">{{ bannerTitles[i-1] }}</span>
        <span class="banner-sub">{{ ['热辣鲜香 即刻开吃','你的口味 AI 最懂','同学们都在点','吃出好身材'][i-1] }}</span></div>
      </div></div><div class="banner-dots"><span v-for="i in 5" :key="i" class="dot" :class="{ on: bannerIdx === i-1 }" @click="bannerIdx = i-1" /></div>
    </div></section>

    <section class="home-section" v-if="categories.length && !loading"><div class="cate-nav">
      <div class="cate-item" :class="{ active: activeCate === null }" @click="activeCate = null"><div class="cate-icon cate-icon--all"><span class="cate-emoji">🍽️</span></div><span class="cate-name">全部</span></div>
      <div class="cate-item" v-for="(c, idx) in categories" :key="c.id" :class="['cate-theme-' + (idx % 8), { active: activeCate === c.id }]" @click="activeCate = c.id"><div class="cate-icon"><span class="cate-emoji">{{ cateEmojis[idx] || '🍴' }}</span></div><span class="cate-name">{{ c.name }}</span></div>
    </div></section>

    <section class="home-section" v-if="hotDishes.length && !loading && activeCate === null">
      <div class="section-head"><div class="section-title-wrap"><span class="section-icon hot-icon">🔥</span><span class="section-title">热销推荐</span></div><span class="section-more" @click="activeCate = null">全部菜品 →</span></div>
      <div class="hot-grid"><div class="hot-card" v-for="(d, idx) in hotDishes.slice(0, 6)" :key="d.id" @click="openDetail(d)">
        <div class="hot-img-wrap"><img :src="d.image || fallbackImg" :alt="d.name" class="hot-img" @error="e => e.target.src = fallbackImg" />
        <span class="hot-rank" :class="'rank-' + (idx + 1)" v-if="idx < 3">{{ idx + 1 }}</span>
        <span class="card-badge badge-hot" v-if="d.label">{{ d.label }}</span></div>
        <div class="hot-info"><div class="hot-name">{{ d.name }}</div>
        <div class="hot-meta" v-if="d.tasteTag || d.sceneTag"><span class="meta-tag" v-if="d.tasteTag">{{ d.tasteTag }}</span><span class="meta-tag" v-if="d.sceneTag">{{ d.sceneTag }}</span></div>
        <div class="hot-row"><div class="hot-price-block"><span class="hot-price">¥{{ Number(d.price).toFixed(2) }}</span>
        <span class="hot-origin" v-if="d.originalPrice && d.originalPrice > d.price">¥{{ Number(d.originalPrice).toFixed(2) }}</span></div>
        <span class="hot-sales">月售 {{ d.sales || 0 }}</span></div></div></div></div></section>

    <section class="home-section" v-if="!loading && activeCate === null" @click="$router.push('/ai')"><div class="ai-banner">
      <div class="ai-banner-left"><AiRobotIcon size="md" /><div class="ai-banner-text"><div class="ai-banner-title">AI 智能点餐助手</div>
      <div class="ai-banner-desc">告诉我你的口味偏好，我来为你搭配</div></div></div>
      <el-button type="primary" size="small" round class="ai-banner-btn">立即体验</el-button></div></section>

    <section class="home-section dish-section" v-loading="loading" element-loading-text="加载中...">
      <div class="section-head" v-if="!loading"><div class="section-title-wrap"><span class="section-icon dish-icon">🍜</span>
      <span class="section-title">{{ activeCate ? categories.find(c => c.id === activeCate)?.['name'] || '全部菜品' : '全部菜品' }}</span></div>
      <span class="section-count" v-if="displayDishes.length">共 {{ displayDishes.length }} 道</span></div>
      <div class="dish-grid" v-if="loading"><div class="sk-card" v-for="i in 6" :key="i"><div class="sk-img" /><div class="sk-line w60" /><div class="sk-line w40" /></div></div>
      <div class="dish-grid" v-else-if="displayDishes.length">
        <div class="dish-card" v-for="d in displayDishes" :key="d.id" @click="openDetail(d)"><div class="dish-img-wrap">
        <img :src="d.image || fallbackImg" :alt="d.name" class="dish-img" @error="e => e.target.src = fallbackImg" />
        <span class="card-badge badge-hot" v-if="d.label === '热门' || d.label === '热卖'">{{ d.label }}</span>
        <span class="card-badge badge-new" v-else-if="d.label === '新品'">{{ d.label }}</span>
        <span class="card-badge badge-rec" v-else-if="d.label === '推荐'">{{ d.label }}</span>
        <span class="card-badge badge-hot" v-else-if="d.isHot">热销</span></div>
        <div class="dish-info"><div class="dish-name">{{ d.name }}</div>
        <div class="dish-stats" v-if="d.rating || d.sales"><span class="dish-rating" v-if="d.rating"><span class="star">★</span> {{ Number(d.rating).toFixed(1) }}</span><span class="dish-sales" v-if="d.sales">月售 {{ d.sales }}</span></div>
        <div class="dish-tags" v-if="d.tasteTag || d.sceneTag || (d.nutritionTag && d.nutritionTag.includes('减脂'))"><span class="dtag" v-if="d.tasteTag">{{ d.tasteTag }}</span><span class="dtag" v-if="d.sceneTag">{{ d.sceneTag }}</span><span class="dtag dtag-green" v-if="d.nutritionTag && d.nutritionTag.includes('减脂')">减脂</span></div>
        <div class="dish-price-row"><span class="dish-price">¥{{ Number(d.price).toFixed(2) }}</span><span class="dish-origin" v-if="d.originalPrice && d.originalPrice > d.price">¥{{ Number(d.originalPrice).toFixed(2) }}</span></div>
        </div></div></div>
      <el-empty v-else description="暂无菜品" :image-size="80" />
    </section>
    <DishDetailDialog v-model:visible="dialogVisible" :dish="currentDish" @added="cartCount++" />
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ShoppingCart, Grid, Food, UserFilled, InfoFilled } from '@element-plus/icons-vue'
import { getHotDishes, listAllDishes } from '@/api/dish'
import { addToCart } from '@/api/cart'
import { getBusinessStatus } from '@/api/merchant'
import request from '@/utils/request'
import fallbackImg from '@/assets/logo-icon.png'
import DishDetailDialog from '@/components/DishDetailDialog.vue'
import AiRobotIcon from '@/components/AiRobotIcon.vue'

const keyword = ref(''); const categories = ref([]); const hotDishes = ref([])
const displayDishes = ref([]); const activeCate = ref(null); const loading = ref(true)
const bannerIdx = ref(0); const cartCount = ref(parseInt(localStorage.getItem('cartCount') || '0'))
const token = ref(localStorage.getItem('token')); const dialogVisible = ref(false)
const shopClosed = ref(false)

// cartCount persistence
const persistCartCount = (count) => { localStorage.setItem('cartCount', String(count)) }
const currentDish = ref(null)

const bannerTitles = ['校园美食 · 即刻送达', 'AI 智能推荐', '热销 TOP10' ]



const fallbackBanner = [
  'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="400" height="150"%3E%3Cdefs%3E%3ClinearGradient id="g" x1="0%25" y1="0%25" x2="100%25" y2="100%25"%3E%3Cstop offset="0%25" stop-color="%232563eb"/%3E%3Cstop offset="100%25" stop-color="%2360a5fa"/%3E%3C/linearGradient%3E%3C/defs%3E%3Crect fill="url(%23g)" width="400" height="150"/%3E%3Ctext fill="white" x="200" y="85" text-anchor="middle" font-size="18" font-weight="600"%3E校园美食 即刻送达%3C/text%3E%3C/svg%3E',
  'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="400" height="150"%3E%3Cdefs%3E%3ClinearGradient id="g" x1="0%25" y1="0%25" x2="100%25" y2="100%25"%3E%3Cstop offset="0%25" stop-color="%233b82f6"/%3E%3Cstop offset="100%25" stop-color="%232dd4bf"/%3E%3C/linearGradient%3E%3C/defs%3E%3Crect fill="url(%23g)" width="400" height="150"/%3E%3Ctext fill="white" x="200" y="85" text-anchor="middle" font-size="18" font-weight="600"%3EAI 智能推荐%3C/text%3E%3C/svg%3E',
  'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="400" height="150"%3E%3Cdefs%3E%3ClinearGradient id="g" x1="0%25" y1="0%25" x2="100%25" y2="100%25"%3E%3Cstop offset="0%25" stop-color="%23f97316"/%3E%3Cstop offset="100%25" stop-color="%23fbbf24"/%3E%3C/linearGradient%3E%3C/defs%3E%3Crect fill="url(%23g)" width="400" height="150"/%3E%3Ctext fill="white" x="200" y="85" text-anchor="middle" font-size="18" font-weight="600"%3E热销 TOP10%3C/text%3E%3C/svg%3E',
  'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="400" height="150"%3E%3Cdefs%3E%3ClinearGradient id="g" x1="0%25" y1="0%25" x2="100%25" y2="100%25"%3E%3Cstop offset="0%25" stop-color="%2310b981"/%3E%3Cstop offset="100%25" stop-color="%2334d399"/%3E%3C/linearGradient%3E%3C/defs%3E%3Crect fill="url(%23g)" width="400" height="150"/%3E%3Ctext fill="white" x="200" y="85" text-anchor="middle" font-size="18" font-weight="600"%3E健康轻食专区%3C/text%3E%3C/svg%3E',
  'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="400" height="150"%3E%3Cdefs%3E%3ClinearGradient id="g" x1="0%25" y1="0%25" x2="100%25" y2="100%25"%3E%3Cstop offset="0%25" stop-color="%236366f1"/%3E%3Cstop offset="100%25" stop-color="%238b5cf6"/%3E%3C/linearGradient%3E%3C/defs%3E%3Crect fill="url(%23g)" width="400" height="150"/%3E%3Ctext fill="white" x="200" y="85" text-anchor="middle" font-size="18" font-weight="600"%3E夜宵好味道%3C/text%3E%3C/svg%3E',
]

const cateColors = [{ iconBg: '#FFF7ED', iconColor: '#F97316' }, { iconBg: '#ECFDF5', iconColor: '#10B981' }, { iconBg: '#FEF2F2', iconColor: '#EF4444' }, { iconBg: '#EFF6FF', iconColor: '#2563EB' }, { iconBg: '#FFFBEB', iconColor: '#F59E0B' }, { iconBg: '#F5F3FF', iconColor: '#8B5CF6' }, { iconBg: '#F0FDFA', iconColor: '#14B8A6' }, { iconBg: '#FFF1F2', iconColor: '#FB7185' }]

const cateEmojis = ['⭐','🍚','🍜','🍗','🥤','🥗','🌶️','🍰']

let autoTimer = null
const startBanner = () => { autoTimer = setInterval(() => { bannerIdx.value = (bannerIdx.value + 1) % 5 }, 4000) }
const pauseBanner = () => { clearInterval(autoTimer) }
const resumeBanner = () => { pauseBanner(); startBanner() }
onMounted(() => { startBanner() })
onUnmounted(() => { pauseBanner() })

watch(activeCate, async (cid) => {
  loading.value = true
  try { displayDishes.value = await listAllDishes(cid || undefined) }
  catch { displayDishes.value = [] }
  finally { loading.value = false }
})

onMounted(async () => {
  try {
    const [c, all, h, bs] = await Promise.all([
      request.get('/category/list')
        .then(list => (list || []).map((item, idx) => ({ ...item, ...cateColors[idx % cateColors.length] })))
        .catch(() => []),
      listAllDishes(null).catch(() => []),
      getHotDishes().catch(() => []),
      getBusinessStatus().catch(() => 1)
    ])
    categories.value = c; displayDishes.value = all; hotDishes.value = h?.slice(0, 8) || []
    shopClosed.value = bs === 0
  } catch (e) { console.error('首页数据加载失败:', e) } finally { loading.value = false }
})

const onSearch = () => {
  if (!keyword.value.trim()) return
  const kw = keyword.value.trim().toLowerCase()
  const list = activeCate.value ? displayDishes.value : hotDishes.value.length ? hotDishes.value : displayDishes.value
  const found = list.filter(d => d.name?.toLowerCase().includes(kw))
  if (found.length) { displayDishes.value = found }
  else { ElMessage.info('未找到相关菜品') }
}

const openDetail = (dish) => {
  if (dish.hasSpec && dish.specGroups?.length) { currentDish.value = dish; dialogVisible.value = true }
  else { onAddCart(dish) }
}

const onAddCart = async (dish) => {
  if (!localStorage.getItem('token')) { ElMessage.warning('请先登录'); return }
  try { await addToCart(dish.id, 1); cartCount.value += 1; persistCartCount(cartCount.value); ElMessage.success('已添加「' + dish.name + '」') }
  catch (e) { console.error('加入购物车失败:', e) } }
</script>

<style scoped>
.home { --home-primary: #2563EB; --home-accent: #F97316; --home-danger: #EF4444; --home-success: #10B981;
  --home-bg: #F5F6FA; --home-card: #FFFFFF; --home-text: #1E293B; --home-text-sub: #64748B;
  --home-text-light: #94A3B8; --home-border: #E2E8F0; --home-radius-lg: 16px; --home-radius-md: 12px;
  --home-radius-sm: 8px; --home-shadow-sm: 0 1px 3px rgba(0,0,0,0.04),0 1px 2px rgba(0,0,0,0.03);
  --home-shadow-md: 0 4px 12px rgba(0,0,0,0.06),0 1px 4px rgba(0,0,0,0.04);
  --home-shadow-lg: 0 8px 24px rgba(0,0,0,0.08),0 2px 8px rgba(0,0,0,0.04);
  min-height: 100vh; background: linear-gradient(180deg, #F0F4FF 0%, #F5F6FA 120px, #F5F6FA 100%);
  padding-bottom: 80px; }
@media (max-width: 767px) { .home { padding-bottom: calc(56px + env(safe-area-inset-bottom, 0px) + 20px); } }
.home-header { position: fixed; top: 0; left: 0; right: 0; height: 52px; padding-top: env(safe-area-inset-top, 6px);
  background: rgba(255,255,255,0.92); backdrop-filter: blur(20px); -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(226,232,240,0.5); z-index: 100; box-shadow: 0 1px 4px rgba(0,0,0,0.04); }
.header-inner { max-width: 1200px; margin: 0 auto; height: 100%; display: flex; align-items: center;
  justify-content: space-between; padding: 0 16px; }
.brand { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.brand-logo { width: 26px; height: 26px; border-radius: 6px; }
.brand-name { font-size: 14px; font-weight: 700; color: var(--home-text); }
.header-search { flex: 1; max-width: 320px; margin: 0 16px; }
.search-input :deep(.el-input__wrapper) { height: 34px; border-radius: 20px;
  box-shadow: 0 0 0 1.5px rgba(37,99,235,0.15) inset; background: #F0F4FF; transition: all 0.25s ease; }
.search-input :deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1.5px var(--home-primary) inset; }
.search-input :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 2px var(--home-primary) inset,0 0 0 4px rgba(37,99,235,0.1); background: #FFFFFF; }
.header-actions { display: flex; align-items: center; gap: 8px; }
.cart-btn { position: relative; color: #475569; text-decoration: none; display: flex; align-items: center; padding: 6px; border-radius: 10px; transition: all 0.2s; }
.cart-btn:hover { color: var(--home-primary); background: rgba(37,99,235,0.06); }
.profile-btn { width: 30px; height: 30px; border-radius: 50%; background: rgba(37,99,235,0.06); color: var(--home-primary); display: flex; align-items: center; justify-content: center; text-decoration: none; transition: all 0.2s; margin-left: 2px; }
.profile-btn:hover { background: var(--home-primary); color: #fff; }
.login-link { font-size: 13px; color: var(--home-primary); text-decoration: none; font-weight: 600; padding: 4px 12px; border-radius: 16px; border: 1.5px solid var(--home-primary); transition: all 0.2s; }
.login-link:hover { background: var(--home-primary); color: #fff; }
.cart-badge { position: absolute; top: 2px; right: 2px; min-width: 16px; height: 16px; padding: 0 4px; border-radius: 8px; background: var(--home-danger); color: #fff; font-size: 10px; font-weight: 600; display: flex; align-items: center; justify-content: center; }
.header-placeholder { height: 52px; }
.home-section { padding: 0 16px; margin-bottom: 24px; }
.closed-banner {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px; border-radius: 10px;
  background: #FEF3C7; color: #92400E; font-size: 13px;
  border: 1px solid #FDE68A;
}
.closed-banner .el-icon { flex-shrink: 0; }
.section-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; padding-bottom: 10px; border-bottom: 1px solid rgba(37,99,235,0.06); }
.section-title-wrap { display: flex; align-items: center; gap: 8px; }
.section-icon { font-size: 16px; line-height: 1; }
.hot-icon { color: var(--home-danger); }
.dish-icon { color: var(--home-primary); }
.section-title { font-size: 16px; font-weight: 700; color: var(--home-text); }
.section-more { font-size: 12px; color: var(--home-text-sub); cursor: pointer; transition: color 0.2s; }
.section-more:hover { color: var(--home-primary); }
.section-count { font-size: 12px; color: var(--home-text-light); }

/* Banner */
.banner-wrap { position: relative; border-radius: var(--home-radius-lg); overflow: hidden; box-shadow: var(--home-shadow-md); }
.banner-track { display: flex; transition: transform 0.45s cubic-bezier(0.25,0.46,0.45,0.94); height: 160px; }
.banner-slide { flex: 0 0 100%; position: relative; height: 100%; }
.banner-img { width: 100%; height: 100%; object-fit: cover; }
:deep(.banner-img-first) {
  object-position: 50% 80%;
}
.banner-text { position: absolute; bottom: 16px; left: 20px; color:#FFFFFF; display: flex; flex-direction: column; gap: 2px; }
.banner-tag { font-size: 16px; font-weight: 600; letter-spacing: 1px; text-shadow: 0 2px 12px rgba(0,0,0,0.35); }
.banner-sub { font-size: 16px; font-weight: 400; opacity: 0.85; text-shadow: 0 1px 4px rgba(0,0,0,0.3); }
.banner-dots { position: absolute; bottom: 12px; right: 16px; display: flex; gap: 6px; }
.dot { width: 6px; height: 6px; border-radius: 3px; background: rgba(255,255,255,0.5); cursor: pointer; transition: all 0.3s cubic-bezier(0.25,0.46,0.45,0.94); }
.dot.on { width: 20px; background: #fff; }

/* Category Nav */
.cate-nav { display: flex; gap: 10px; overflow-x: auto; padding: 2px 0 8px; scrollbar-width: none; position: sticky; top: 52px; z-index: 50; background: transparent; }
.cate-nav::-webkit-scrollbar { display: none; }
.cate-item { flex-shrink: 0; display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 10px 12px; border-radius: var(--home-radius-md); background: var(--home-card); border: 1.5px solid transparent; cursor: pointer; transition: all 0.25s; box-shadow: var(--home-shadow-sm); min-width: 68px; }
.cate-item:hover { transform: translateY(-2px); box-shadow: var(--home-shadow-md); }
.cate-item.active { border-color: var(--home-primary); background: linear-gradient(135deg, #EFF6FF 0%, #F0F9FF 100%); transform: translateY(-1px); box-shadow: var(--home-shadow-md); }
.cate-icon { width: 40px; height: 40px; border-radius: 14px; display: flex; align-items: center; justify-content: center; background: #F1F5F9; transition: all 0.25s ease; }
.cate-item.active .cate-icon { transform: scale(1.08); }
.cate-emoji { font-size: 20px; line-height: 1; }
.cate-icon--all { background: linear-gradient(135deg, #EFF6FF, #DBEAFE); }
.cate-name { font-size: 11px; color: var(--home-text-sub); font-weight: 500; }
.cate-item.active .cate-name { color: var(--home-primary); font-weight: 700; }
.cate-theme-0 .cate-icon { background: #FFF7ED; }
.cate-theme-1 .cate-icon { background: #ECFDF5; }
.cate-theme-2 .cate-icon { background: #FEF2F2; }
.cate-theme-3 .cate-icon { background: #FFFBEB; }
.cate-theme-4 .cate-icon { background: #EFF6FF; }
.cate-theme-5 .cate-icon { background: #F5F3FF; }
.cate-theme-6 .cate-icon { background: #FFF1F2; }
.cate-theme-7 .cate-icon { background: #FEF9EF; }

/* Hot Deals */
.hot-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.hot-card { background: var(--home-card); border-radius: var(--home-radius-md); overflow: hidden; box-shadow: var(--home-shadow-sm); cursor: pointer; transition: all 0.25s ease; border: 1px solid transparent; }
.hot-card:hover { transform: translateY(-4px); box-shadow: var(--home-shadow-lg); border-color: rgba(37,99,235,0.06); }
.hot-card:active { transform: scale(0.97); }
.hot-img-wrap { position: relative; aspect-ratio: 4 / 3; overflow: hidden; background: #F1F5F9; }
.hot-img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.35s ease; }
.hot-card:hover .hot-img { transform: scale(1.06); }
.hot-rank { position: absolute; top: 8px; left: 8px; width: 22px; height: 22px; border-radius: 8px; color: #fff; font-size: 11px; font-weight: 800; display: flex; align-items: center; justify-content: center; box-shadow: 0 2px 6px rgba(0,0,0,0.2); z-index: 2; }
.hot-rank.rank-1 { background: linear-gradient(135deg, #FFD700, #FF8C00); }
.hot-rank.rank-2 { background: linear-gradient(135deg, #94A3B8, #64748B); }
.hot-rank.rank-3 { background: linear-gradient(135deg, #CD7F32, #8B4513); }
.hot-info { padding: 10px 12px 12px; }
.hot-name { font-size: 13px; font-weight: 600; color: var(--home-text); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-bottom: 4px; }
.hot-meta { display: flex; gap: 4px; margin-bottom: 6px; }
.meta-tag { font-size: 10px; padding: 1px 6px; border-radius: 4px; background: #F1F5F9; color: var(--home-text-sub); font-weight: 500; }
.hot-row { display: flex; justify-content: space-between; align-items: center; }
.hot-price-block { display: flex; align-items: baseline; gap: 4px; }
.hot-price { font-size: 16px; font-weight: 800; color: var(--home-accent); }
.hot-origin { font-size: 11px; color: var(--home-text-light); text-decoration: line-through; }
.hot-sales { font-size: 11px; color: var(--home-text-light); }

/* AI */
.ai-banner { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 16px 18px; background: linear-gradient(135deg, #EEF2FF 0%, #F0F9FF 40%, #ECFDF5 100%); border: 1px solid rgba(37,99,235,0.08); border-radius: var(--home-radius-lg); cursor: pointer; transition: all 0.3s; box-shadow: var(--home-shadow-sm); }
.ai-banner:hover { border-color: rgba(37,99,235,0.18); box-shadow: var(--home-shadow-md); transform: translateY(-2px); }
.ai-banner-left { display: flex; align-items: center; gap: 12px; }
.ai-banner-text { display: flex; flex-direction: column; gap: 2px; }
.ai-banner-title { font-size: 15px; font-weight: 700; color: var(--home-text); }
.ai-banner-desc { font-size: 12px; color: var(--home-text-sub); }
.ai-banner-btn { flex-shrink: 0; font-size: 12px; }

/* Skeleton */
.dish-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.sk-card { background: var(--home-card); border-radius: var(--home-radius-md); overflow: hidden; box-shadow: var(--home-shadow-sm); }
.sk-img { height: 140px; background: linear-gradient(90deg, #E2E8F0 25%, #F1F5F9 50%, #E2E8F0 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; }
.sk-line { height: 14px; background: #E2E8F0; border-radius: 4px; margin: 10px 12px; }
.sk-line.w60 { width: 60%; }
.sk-line.w40 { width: 40%; margin-top: 4px; }
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

/* Dish Cards */
.dish-section { min-height: 300px; }
.dish-card { background: var(--home-card); border-radius: var(--home-radius-md); overflow: hidden; box-shadow: var(--home-shadow-sm); cursor: pointer; transition: all 0.3s ease; border: 1px solid transparent; display: flex; flex-direction: column; }
.dish-card:hover { transform: translateY(-4px); box-shadow: var(--home-shadow-lg); border-color: rgba(37,99,235,0.06); }
.dish-card:active { transform: scale(0.98); }
.dish-img-wrap { position: relative; aspect-ratio: 4 / 3; overflow: hidden; background: linear-gradient(135deg, #F1F5F9, #E2E8F0); flex-shrink: 0; }
.dish-img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.35s ease; }
.dish-card:hover .dish-img { transform: scale(1.05); }
.card-badge { position: absolute; top: 8px; left: 8px; z-index: 2; padding: 3px 8px; border-radius: 6px; font-size: 10px; font-weight: 700; color: #fff; box-shadow: 0 2px 6px rgba(0,0,0,0.15); letter-spacing: 0.5px; }
.badge-hot { background: linear-gradient(135deg, #EF4444, #F97316); }
.badge-new { background: linear-gradient(135deg, #8B5CF6, #A78BFA); }
.badge-rec { background: linear-gradient(135deg, #10B981, #34D399); }
.dish-info { padding: 10px 12px 12px; flex: 1; display: flex; flex-direction: column; gap: 4px; }
.dish-name { font-size: 14px; font-weight: 600; color: var(--home-text); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; line-height: 1.3; }
.dish-stats { display: flex; align-items: center; gap: 8px; }
.dish-rating { font-size: 11px; color: var(--home-accent); font-weight: 600; display: flex; align-items: center; gap: 2px; }
.dish-rating .star { color: #F59E0B; font-size: 12px; }
.dish-sales { font-size: 11px; color: var(--home-text-light); }
.dish-tags { display: flex; gap: 4px; flex-wrap: wrap; }
.dtag { font-size: 10px; padding: 1px 6px; border-radius: 4px; background: #F1F5F9; color: var(--home-text-sub); font-weight: 500; }
.dtag-green { background: #ECFDF5; color: #059669; }
.dish-price-row { display: flex; align-items: baseline; gap: 4px; margin-top: 2px; }
.dish-price { font-size: 16px; font-weight: 800; color: var(--home-accent); }
.dish-origin { font-size: 11px; color: var(--home-text-light); text-decoration: line-through; }

/* Responsive */
@media (min-width: 768px) { .home-section { padding: 0 24px; } .header-inner { padding: 0 24px; } .banner-track { height: 200px; } .hot-grid { grid-template-columns: repeat(3, 1fr); gap: 16px; } .dish-grid { grid-template-columns: repeat(3, 1fr); gap: 16px; } }
@media (min-width: 1024px) { .dish-grid { grid-template-columns: repeat(4, 1fr); } }
</style>

