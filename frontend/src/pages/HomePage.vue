<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowUpOutlined } from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { addApp, listMyAppVoByPage, listGoodAppVoByPage } from '@/api/appController'
import { getDeployUrl } from '@/config/env'
import AppCard from '@/components/AppCard.vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 用户提示词
const userPrompt = ref('')
const creating = ref(false)

// 我的应用数据
const myApps = ref<API.AppVO[]>([])
const myAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0,
})

// 精选应用数据
const featuredApps = ref<API.AppVO[]>([])
const featuredAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0,
})

// 快捷模板
const promptTemplates = [
  {
    icon: '📝',
    label: '个人博客网站',
    prompt:
      '创建一个现代化的个人博客网站，包含文章列表、详情页、分类标签、搜索功能、评论系统和个人简介页面。采用简洁的设计风格，支持响应式布局，文章支持Markdown格式，首页展示最新文章和热门推荐。',
  },
  {
    icon: '🏢',
    label: '企业官网',
    prompt:
      '设计一个专业的企业官网，包含公司介绍、产品服务展示、新闻资讯、联系我们等页面。采用商务风格的设计，包含轮播图、产品展示卡片、团队介绍、客户案例展示，支持多语言切换和在线客服功能。',
  },
  {
    icon: '🛒',
    label: '在线商城',
    prompt:
      '构建一个功能完整的在线商城，包含商品展示、购物车、用户注册登录、订单管理、支付结算等功能。设计现代化的商品卡片布局，支持商品搜索筛选、用户评价、优惠券系统和会员积分功能。',
  },
  {
    icon: '🎨',
    label: '作品展示网站',
    prompt:
      '制作一个精美的作品展示网站，适合设计师、摄影师、艺术家等创作者。包含作品画廊、项目详情页、个人简历、联系方式等模块。采用瀑布流或网格布局展示作品，支持图片放大预览和作品分类筛选。',
  },
]

// 设置提示词
const setPrompt = (prompt: string) => {
  userPrompt.value = prompt
}

// 创建应用
const createApp = async () => {
  if (!userPrompt.value.trim()) {
    message.warning('请输入应用描述')
    return
  }

  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    await router.push('/user/login')
    return
  }

  creating.value = true
  try {
    const res = await addApp({
      initPrompt: userPrompt.value.trim(),
    })

    if (res.data.code === 0 && res.data.data) {
      message.success('应用创建成功')
      // 跳转到对话页面，确保ID是字符串类型
      const appId = String(res.data.data)
      await router.push(`/app/chat/${appId}`)
    } else {
      message.error('创建失败：' + res.data.message)
    }
  } catch (error) {
    console.error('创建应用失败：', error)
    message.error('创建失败，请重试')
  } finally {
    creating.value = false
  }
}

// 加载我的应用
const loadMyApps = async () => {
  if (!loginUserStore.loginUser.id) {
    return
  }

  try {
    const res = await listMyAppVoByPage({
      pageNum: myAppsPage.current,
      pageSize: myAppsPage.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    })

    if (res.data.code === 0 && res.data.data) {
      myApps.value = res.data.data.records || []
      myAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载我的应用失败：', error)
  }
}

// 加载精选应用
const loadFeaturedApps = async () => {
  try {
    const res = await listGoodAppVoByPage({
      pageNum: featuredAppsPage.current,
      pageSize: featuredAppsPage.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    })

    if (res.data.code === 0 && res.data.data) {
      featuredApps.value = res.data.data.records || []
      featuredAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载精选应用失败：', error)
  }
}

// 查看对话
const viewChat = (appId: string | number | undefined) => {
  if (appId) {
    router.push(`/app/chat/${appId}?view=1`)
  }
}

// 查看作品
const viewWork = (app: API.AppVO) => {
  if (app.deployKey) {
    const url = getDeployUrl(app.deployKey)
    window.open(url, '_blank')
  }
}

// 页面加载时获取数据
onMounted(() => {
  loadMyApps()
  loadFeaturedApps()

  // 鼠标跟随光效
  const handleMouseMove = (e: MouseEvent) => {
    const { clientX, clientY } = e
    const { innerWidth, innerHeight } = window
    const x = (clientX / innerWidth) * 100
    const y = (clientY / innerHeight) * 100

    document.documentElement.style.setProperty('--mouse-x', `${x}%`)
    document.documentElement.style.setProperty('--mouse-y', `${y}%`)
  }

  document.addEventListener('mousemove', handleMouseMove)
})
</script>

<template>
  <div id="homePage">
    <!-- 氛围背景 -->
    <div class="ambient" aria-hidden="true">
      <div class="ambient-grid"></div>
      <div class="ambient-orb ambient-orb--indigo"></div>
      <div class="ambient-orb ambient-orb--violet"></div>
      <div class="ambient-orb ambient-orb--cyan"></div>
      <div class="ambient-spotlight"></div>
    </div>

    <div class="container">
      <!-- 英雄区 -->
      <section class="hero-section">
        <div class="hero-badge">
          <span class="hero-badge-dot"></span>
          AI 驱动 · 零代码 · 分钟级上线
        </div>
        <h1 class="hero-title">
          一句话，<span class="hero-title-gradient">生成完整应用</span>
        </h1>
        <p class="hero-description">
          描述你的想法，AI 帮你实时生成、预览并一键部署网站应用
        </p>
      </section>

      <!-- AI 输入台 -->
      <section class="composer">
        <div class="composer-glow" aria-hidden="true"></div>
        <div class="composer-card">
          <a-textarea
            v-model:value="userPrompt"
            placeholder="描述你想创建的应用，例如：帮我创建一个个人博客网站…"
            :rows="4"
            :maxlength="1000"
            class="composer-input"
            @keydown.enter.exact.prevent="createApp"
          />
          <div class="composer-footer">
            <span class="composer-hint">Enter 发送 · Shift + Enter 换行</span>
            <div class="composer-right">
              <span class="composer-count">{{ userPrompt.length }} / 1000</span>
              <button
                class="composer-send"
                :class="{ 'is-loading': creating }"
                :disabled="creating"
                @click="createApp"
              >
                <a-spin v-if="creating" size="small" class="composer-send-spin" />
                <ArrowUpOutlined v-else />
              </button>
            </div>
          </div>
        </div>
      </section>

      <!-- 快捷模板 -->
      <section class="quick-actions">
        <button
          v-for="tpl in promptTemplates"
          :key="tpl.label"
          class="quick-chip"
          @click="setPrompt(tpl.prompt)"
        >
          <span class="quick-chip-icon">{{ tpl.icon }}</span>
          {{ tpl.label }}
        </button>
      </section>

      <!-- 我的作品 -->
      <section class="section">
        <div class="section-header">
          <div>
            <h2 class="section-title">我的作品</h2>
            <p class="section-subtitle">你创建的应用都会出现在这里</p>
          </div>
        </div>
        <div v-if="myApps.length" class="app-grid">
          <AppCard
            v-for="app in myApps"
            :key="app.id"
            :app="app"
            @view-chat="viewChat"
            @view-work="viewWork"
          />
        </div>
        <div v-else class="empty-state">
          <div class="empty-icon">🚀</div>
          <p class="empty-title">
            {{ loginUserStore.loginUser.id ? '还没有作品' : '登录后开始创作' }}
          </p>
          <p class="empty-desc">
            {{
              loginUserStore.loginUser.id
                ? '在上方输入一句话，创建你的第一个应用吧'
                : '登录账号即可用 AI 生成属于你的应用'
            }}
          </p>
        </div>
        <div v-if="myAppsPage.total > myAppsPage.pageSize" class="pagination-wrapper">
          <a-pagination
            v-model:current="myAppsPage.current"
            v-model:page-size="myAppsPage.pageSize"
            :total="myAppsPage.total"
            :show-size-changer="false"
            :show-total="(total: number) => `共 ${total} 个应用`"
            @change="loadMyApps"
          />
        </div>
      </section>

      <!-- 精选案例 -->
      <section class="section">
        <div class="section-header">
          <div>
            <h2 class="section-title">精选案例</h2>
            <p class="section-subtitle">看看大家都在用 AI 创造什么</p>
          </div>
        </div>
        <div v-if="featuredApps.length" class="app-grid">
          <AppCard
            v-for="app in featuredApps"
            :key="app.id"
            :app="app"
            :featured="true"
            @view-chat="viewChat"
            @view-work="viewWork"
          />
        </div>
        <div v-else class="empty-state">
          <div class="empty-icon">✨</div>
          <p class="empty-title">暂无精选案例</p>
          <p class="empty-desc">敬请期待更多优秀作品</p>
        </div>
        <div v-if="featuredAppsPage.total > featuredAppsPage.pageSize" class="pagination-wrapper">
          <a-pagination
            v-model:current="featuredAppsPage.current"
            v-model:page-size="featuredAppsPage.pageSize"
            :total="featuredAppsPage.total"
            :show-size-changer="false"
            :show-total="(total: number) => `共 ${total} 个案例`"
            @change="loadFeaturedApps"
          />
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
#homePage {
  position: relative;
  width: 100%;
  min-height: 100vh;
  overflow: hidden;
}

/* ===== 氛围背景 ===== */
.ambient {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}

.ambient-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(99, 102, 241, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(99, 102, 241, 0.06) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: radial-gradient(ellipse 90% 60% at 50% 0%, #000 40%, transparent 100%);
  -webkit-mask-image: radial-gradient(ellipse 90% 60% at 50% 0%, #000 40%, transparent 100%);
}

.ambient-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  opacity: 0.5;
  animation: float-slow 14s ease-in-out infinite;
}

.ambient-orb--indigo {
  width: 480px;
  height: 480px;
  top: -160px;
  left: -120px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.5), transparent 70%);
}

.ambient-orb--violet {
  width: 420px;
  height: 420px;
  top: -100px;
  right: -100px;
  background: radial-gradient(circle, rgba(168, 85, 247, 0.4), transparent 70%);
  animation-delay: -5s;
}

.ambient-orb--cyan {
  width: 360px;
  height: 360px;
  top: 380px;
  left: 55%;
  background: radial-gradient(circle, rgba(34, 211, 238, 0.28), transparent 70%);
  animation-delay: -9s;
}

.ambient-spotlight {
  position: absolute;
  inset: 0;
  background: radial-gradient(
    560px circle at var(--mouse-x, 50%) var(--mouse-y, 30%),
    rgba(99, 102, 241, 0.08),
    transparent 70%
  );
}

.container {
  position: relative;
  z-index: 1;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px 40px;
  width: 100%;
}

/* ===== 英雄区 ===== */
.hero-section {
  text-align: center;
  padding: 88px 0 48px;
  animation: fade-up 0.7s var(--ease-out) both;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 500;
  color: var(--brand-600);
  background: rgba(99, 102, 241, 0.08);
  border: 1px solid rgba(99, 102, 241, 0.22);
  border-radius: var(--radius-full);
  padding: 7px 16px;
  margin-bottom: 28px;
  backdrop-filter: blur(8px);
}

.hero-badge-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--brand-500);
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.18);
  animation: pulse-dot 2s ease-in-out infinite;
}

@keyframes pulse-dot {
  0%,
  100% {
    box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.2);
  }
  50% {
    box-shadow: 0 0 0 6px rgba(99, 102, 241, 0.08);
  }
}

.hero-title {
  font-size: clamp(38px, 6vw, 64px);
  font-weight: 800;
  letter-spacing: -0.03em;
  line-height: 1.15;
  margin: 0 0 20px;
  color: var(--text-1);
}

.hero-title-gradient {
  background: var(--gradient-text);
  background-size: 200% auto;
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: shimmer 6s ease-in-out infinite;
}

@keyframes shimmer {
  0%,
  100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

.hero-description {
  font-size: 18px;
  color: var(--text-2);
  margin: 0;
}

/* ===== AI 输入台 ===== */
.composer {
  position: relative;
  max-width: 760px;
  margin: 0 auto 28px;
  animation: fade-up 0.7s var(--ease-out) 0.1s both;
}

.composer-glow {
  position: absolute;
  inset: -2px;
  border-radius: 22px;
  background: var(--gradient-brand);
  opacity: 0.35;
  filter: blur(18px);
  transition: opacity 0.3s;
}

.composer:focus-within .composer-glow {
  opacity: 0.55;
}

.composer-card {
  position: relative;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  padding: 8px 8px 10px;
}

.composer-input {
  border: none !important;
  box-shadow: none !important;
  background: transparent !important;
  font-size: 16px;
  line-height: 1.7;
  padding: 14px 16px 4px;
  resize: none;
}

.composer-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 8px 2px 16px;
}

.composer-hint {
  font-size: 12px;
  color: var(--text-3);
}

.composer-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.composer-count {
  font-size: 12px;
  color: var(--text-3);
  font-variant-numeric: tabular-nums;
}

.composer-send {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  background: var(--gradient-brand);
  box-shadow: var(--shadow-brand);
  transition:
    transform 0.2s var(--ease-out),
    box-shadow 0.2s var(--ease-out);
}

.composer-send:hover:not(:disabled) {
  background: var(--gradient-brand-hover);
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 12px 30px rgba(99, 102, 241, 0.45);
}

.composer-send:disabled {
  cursor: not-allowed;
  opacity: 0.75;
}

.composer-send-spin :deep(.ant-spin-dot-item) {
  background-color: #fff;
}

/* ===== 快捷模板 ===== */
.quick-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
  margin-bottom: 88px;
  animation: fade-up 0.7s var(--ease-out) 0.2s both;
}

.quick-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 9px 18px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-2);
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  cursor: pointer;
  backdrop-filter: blur(10px);
  box-shadow: var(--shadow-xs);
  transition:
    transform 0.2s var(--ease-out),
    box-shadow 0.2s var(--ease-out),
    border-color 0.2s,
    color 0.2s;
}

.quick-chip:hover {
  transform: translateY(-2px);
  color: var(--brand-600);
  border-color: rgba(99, 102, 241, 0.45);
  box-shadow: 0 8px 22px rgba(99, 102, 241, 0.16);
}

.quick-chip-icon {
  font-size: 15px;
}

/* ===== 内容区 ===== */
.section {
  margin-bottom: 72px;
}

.section-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 28px;
}

.section-title {
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--text-1);
  margin: 0 0 6px;
}

.section-subtitle {
  font-size: 14px;
  color: var(--text-3);
  margin: 0;
}

.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 56px 24px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px dashed var(--border-strong);
  border-radius: var(--radius-lg);
}

.empty-icon {
  font-size: 40px;
  margin-bottom: 12px;
}

.empty-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-1);
  margin: 0 0 6px;
}

.empty-desc {
  font-size: 14px;
  color: var(--text-3);
  margin: 0;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .hero-section {
    padding: 56px 0 36px;
  }

  .hero-description {
    font-size: 15px;
  }

  .quick-actions {
    margin-bottom: 56px;
  }

  .app-grid {
    grid-template-columns: 1fr;
  }

  .section {
    margin-bottom: 48px;
  }
}
</style>
