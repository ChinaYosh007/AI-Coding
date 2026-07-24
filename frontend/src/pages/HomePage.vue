<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowUpOutlined, DownOutlined } from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { addApp, listMyAppVoByPage, listGoodAppVoByPage } from '@/api/appController'
import { getDeployUrl } from '@/config/env'
import { CODE_GEN_TYPE_OPTIONS, CodeGenTypeEnum } from '@/utils/codeGenTypes'
import AppCard from '@/components/AppCard.vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 用户提示词
const userPrompt = ref('')
const creating = ref(false)
const selectedCodeGenType = ref(CodeGenTypeEnum.AUTO)

const selectedCodeGenTypeLabel = computed(() => {
  return (
    CODE_GEN_TYPE_OPTIONS.find((option) => option.value === selectedCodeGenType.value)?.label ||
    '生成模式'
  )
})

const handleCodeGenTypeSelect = ({ key }: { key: string | number }) => {
  selectedCodeGenType.value = key as CodeGenTypeEnum
}

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

// 平台统计数据（用于"敬请期待"动态展示）
const platformStats = reactive({
  totalApps: 0,
  loading: false,
})

// 快捷模板
const promptTemplates = [
  {
    icon: '📝',
    label: '个人博客与技术专栏',
    prompt:
      '架构并构建一个符合现代响应式规范的个人博客与技术专栏平台。参考掘金与知乎专栏的技术排版标准。规格要求：1. 顶部固定导航栏（集成 Logo、分类检索、全局搜索框与主题切换）；2. 首页配置精选文章轮播大图及双列响应式卡片流（呈现封面图、标题、摘要、分类标签及阅读统计）；3. 文章详情组件支持 Markdown 实时渲染、目录树锚点定位与平滑跳转；4. 侧边栏构建作者名片、个人简介及动态热门标签云；5. 底部集成友情链接与版权声明。整体采用高留白极简设计，主色调为高雅蓝灰系，完全适配全端响应式。图片资源规范：优先使用系统提供的标准资源路径；若缺少图片，使用 CSS 矢量渐变背景占位，禁止引用第三方未校验的外部占位图。',
  },
  {
    icon: '🏢',
    label: '企业级品牌官网',
    prompt:
      '设计并构建高规格企业级官网及品牌展示平台，参考腾讯与阿里云官网的视觉动效标准。规格要求：1. 顶部全透玻璃拟态导航栏，页面向下滚动时触发高斯模糊吸顶效果；2. 首屏全宽 Hero 大图搭配核心标语及明确的行动召唤（CTA）按钮；3. 核心产品/服务矩阵采用三列高阶卡片响应式布局；4. 数据指标区配备动态数字递增与里程碑呈现；5. 品牌合作伙伴与客户案例 Logo 墙；6. 团队介绍区采用圆形头像卡片；7. 深色高密底栏集成多维导航、联系方式及社交图标。主色调采用深蓝极光渐变，支持响应式布局。资源规范：图片资源优先使用系统提供的真实 URL，无资源时采用 CSS 渐变色块兜底。',
  },
  {
    icon: '🛒',
    label: '全功能电商商城',
    prompt:
      '构建一个精致高可用在线电商平台，借鉴美团、京东与饿了么的前端交互布局。规格要求：1. 居中全局搜索栏与横向可滑动商品分类导航；2. 首屏配置营销活动轮播 Banner 与限时秒杀倒计时模块；3. 商品展现区采用双列瀑布流网格，展示商品主图、品名、实时价格、划线原价与评分星级；4. 商品详情组件包含画廊预览、多规格 SKU 选择、用户评价列表与关联推荐；5. 悬浮购物车图标配置实时数量徽标；6. 底部服务保障体系说明与标准支付渠道图标。整体配色活泼，以活力橙为主色调，全端响应式适配。资源规范：图片必须引用系统提供的合法 URL，禁止请求外部 picsum 等测试地址。',
  },
  {
    icon: '🎨',
    label: '设计师作品集平台',
    prompt:
      '制作一个极具视效冲击力的设计师/创作者作品集展示平台，参考 Behance 与 Dribbble 的展现风范。规格要求：1. 首屏全屏 Hero 区展示创作者名片、个人定位一句话说明，搭配微质感渐变背景；2. 作品画廊采用瀑布流网格，支持 Hover 悬停微动效与遮罩信息展开；3. 作品详情组件包含高清大图组图轮播与右侧项目元数据卡片（客户、完成时间、技术栈、设计理念）；4. 极简固定导航与经验时间线展示；5. 响应式联系表单与社交链接。暗色沉浸主题，强调作品视觉张力。资源规范：必须使用系统合规图片或 CSS 渐变作为封面兜底。',
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
      codeGenType: selectedCodeGenType.value,
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
      platformStats.totalApps = featuredAppsPage.total
    }
  } catch (error) {
    console.error('加载精选应用失败：', error)
  }
}

// 查看对话
const viewChat = (appId: string | undefined) => {
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
          AI 代码母体 · 全栈零代码生成平台
        </div>
        <h1 class="hero-title">
          一句话，<span class="hero-title-gradient">构建并上线完整 Web 应用</span>
        </h1>
        <p class="hero-description">
          输入业务需求，AI 智能架构 DOM 结构、CSS 布局与 JS 交互逻辑，支持一键实时编译与部署
        </p>

        <!-- 核心工程特性卡片 -->
        <div class="engine-portals">
          <div class="portal-card active">
            <div class="portal-icon">⚡</div>
            <div class="portal-info">
              <h3>智能架构推导</h3>
              <p>自动根据需求推导单页、多文件或完整 Vue 工程结构</p>
            </div>
            <span class="portal-tag">多模式支持</span>
          </div>
          <div class="portal-card active">
            <div class="portal-icon">🖥️</div>
            <div class="portal-info">
              <h3>沙箱实时热编译</h3>
              <p>支持在线代码预览、可视化元素调试与一键部署上线</p>
            </div>
            <span class="portal-tag accent">实时沙箱</span>
          </div>
        </div>
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
            <div class="composer-left">
              <a-dropdown :trigger="['click']" placement="topLeft">
                <button class="composer-type-button" type="button">
                  <span>{{ selectedCodeGenTypeLabel }}</span>
                  <DownOutlined />
                </button>
                <template #overlay>
                  <a-menu :selectedKeys="[selectedCodeGenType]" @click="handleCodeGenTypeSelect">
                    <a-menu-item
                      v-for="option in CODE_GEN_TYPE_OPTIONS"
                      :key="option.value"
                    >
                      <div class="type-menu-item">
                        <span class="type-menu-label">
                          {{ option.label }}
                          <a-tag v-if="option.value === 'auto'" color="blue" :bordered="false" style="margin-left: 4px; font-size: 11px;">推荐</a-tag>
                        </span>
                        <small class="type-menu-desc">{{ option.desc }}</small>
                      </div>
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
              <span class="composer-hint">Enter 发送 · Shift + Enter 换行</span>
            </div>
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
          <p class="empty-desc">
            {{ platformStats.totalApps > 0
              ? `平台已有 ${platformStats.totalApps} 个作品被精选，快来创作你的优秀应用吧`
              : '敬请期待更多优秀作品，你也可以成为第一个被精选的创作者'
            }}
          </p>
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
  margin: 0 0 32px;
}

/* ===== 双大核心入口卡片 ===== */
.engine-portals {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  max-width: 860px;
  margin: 0 auto 12px;
  text-align: left;
}

@media (max-width: 640px) {
  .engine-portals {
    grid-template-columns: 1fr;
  }
}

.portal-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 22px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-sm);
  transition: all 0.3s var(--ease-out);
  cursor: pointer;
  overflow: hidden;
}

.portal-card:hover {
  transform: translateY(-4px);
  border-color: rgba(14, 165, 233, 0.4);
  box-shadow: var(--shadow-md);
}

.portal-card.active {
  border-color: rgba(14, 165, 233, 0.3);
  background: linear-gradient(135deg, var(--surface) 0%, rgba(14, 165, 233, 0.04) 100%);
}

.portal-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: rgba(14, 165, 233, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.portal-info h3 {
  font-size: 16px;
  font-weight: 700;
  margin: 0 0 4px;
  color: var(--text-1);
}

.portal-info p {
  font-size: 12.5px;
  color: var(--text-2);
  margin: 0;
  line-height: 1.45;
}

.portal-tag {
  position: absolute;
  top: 12px;
  right: 14px;
  font-size: 11px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: var(--radius-full);
  background: var(--surface-2);
  color: var(--text-3);
}

.portal-tag.accent {
  background: var(--gradient-brand);
  color: #fff;
  box-shadow: 0 2px 8px rgba(14, 165, 233, 0.25);
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
  gap: 12px;
}

.composer-left {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 12px;
}

.composer-type-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  max-width: 160px;
  padding: 0 11px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.78);
  color: var(--text-2);
  font-size: 13px;
  line-height: 1;
  cursor: pointer;
  box-shadow: var(--shadow-xs);
  transition:
    border-color 0.2s var(--ease-out),
    color 0.2s var(--ease-out),
    background 0.2s var(--ease-out);
}

.composer-type-button:hover {
  border-color: rgba(99, 102, 241, 0.42);
  background: rgba(255, 255, 255, 0.95);
  color: var(--brand-600);
}

.composer-type-button span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.type-menu-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 2px 0;
}

.type-menu-label {
  display: flex;
  align-items: center;
  font-size: 13px;
  font-weight: 500;
}

.type-menu-desc {
  font-size: 11px;
  color: var(--text-3, #94a3b8);
  white-space: nowrap;
}

.composer-hint {
  min-width: 0;
  overflow: hidden;
  font-size: 12px;
  color: var(--text-3);
  text-overflow: ellipsis;
  white-space: nowrap;
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

  .composer-footer {
    align-items: flex-end;
  }

  .composer-left {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
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
