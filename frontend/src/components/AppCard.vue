<template>
  <div class="app-card" :class="{ 'app-card--featured': featured }">
    <!-- 精选徽标 -->
    <div v-if="featured" class="featured-ribbon">
      <StarFilled />
      精选
    </div>

    <div class="app-preview">
      <img v-if="app.cover" :src="app.cover" :alt="displayAppName" loading="lazy" />
      <div v-else class="app-placeholder">
        <div class="app-placeholder-icon">
          {{ displayAppName.charAt(0).toUpperCase() }}
        </div>
        <div class="app-placeholder-text">
          <span>{{ displayAppName }}</span>
          <small>{{ featured ? '精选作品' : '我的应用' }}</small>
        </div>
      </div>
      <div class="app-overlay">
        <button class="overlay-btn overlay-btn--primary" @click="handleViewChat">
          <MessageOutlined />
          查看对话
        </button>
        <button v-if="app.deployKey" class="overlay-btn" @click="handleViewWork">
          <GlobalOutlined />
          查看作品
        </button>
      </div>
    </div>

    <div class="app-info">
      <a-avatar class="app-avatar" :src="app.user?.userAvatar" :size="38">
        {{ displayAuthorName.charAt(0).toUpperCase() }}
      </a-avatar>
      <div class="app-meta">
        <h3 class="app-title" :title="displayAppName">{{ displayAppName }}</h3>
        <p class="app-author">
          {{ displayAuthorName }}
        </p>
      </div>
      <span v-if="app.deployKey" class="app-status" title="已部署上线"></span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { MessageOutlined, GlobalOutlined, StarFilled } from '@ant-design/icons-vue'

interface Props {
  app: API.AppVO
  featured?: boolean
}

interface Emits {
  (e: 'view-chat', appId: string | undefined): void
  (e: 'view-work', app: API.AppVO): void
}

const props = withDefaults(defineProps<Props>(), {
  featured: false,
})

const emit = defineEmits<Emits>()

const getDisplayNameFromText = (text?: string) => {
  const value = text?.trim()
  if (!value) return ''

  if (value.startsWith('{') && value.endsWith('}')) {
    try {
      const parsed = JSON.parse(value)
      const parsedName = parsed?.appName || parsed?.title || parsed?.name
      if (typeof parsedName === 'string' && parsedName.trim()) {
        return parsedName.trim()
      }
    } catch (error) {
      console.warn('解析应用名称失败：', error)
    }
  }

  return value
}

const displayAppName = computed(() => {
  return getDisplayNameFromText(props.app.appName) || getDisplayNameFromText(props.app.initPrompt) || '未命名应用'
})

const displayAuthorName = computed(() => {
  return props.app.user?.userName || props.app.user?.userAccount || (props.featured ? '官方' : '未知用户')
})

const handleViewChat = () => {
  emit('view-chat', props.app.id)
}

const handleViewWork = () => {
  emit('view-work', props.app)
}
</script>

<style scoped>
.app-card {
  position: relative;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  transition:
    transform 0.3s var(--ease-out),
    box-shadow 0.3s var(--ease-out),
    border-color 0.3s;
}

.app-card:hover {
  transform: translateY(-6px);
  border-color: rgba(14, 165, 233, 0.28);
  box-shadow: var(--shadow-md);
}

/* 精选徽标 */
.featured-ribbon {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 3;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  font-weight: 600;
  color: #92400e;
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  border: 1px solid rgba(217, 119, 6, 0.35);
  padding: 4px 10px;
  border-radius: var(--radius-full);
  box-shadow: 0 2px 8px rgba(217, 119, 6, 0.2);
}

/* 封面预览 */
.app-preview {
  position: relative;
  aspect-ratio: 16 / 10;
  background: var(--surface-2);
  overflow: hidden;
}

.app-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s var(--ease-out);
}

.app-card:hover .app-preview img {
  transform: scale(1.05);
}

.app-placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 22px;
  text-align: center;
  background:
    radial-gradient(circle at 30% 30%, rgba(14, 165, 233, 0.1), transparent 60%),
    radial-gradient(circle at 70% 70%, rgba(20, 184, 166, 0.08), transparent 60%),
    var(--surface-2);
}

.app-placeholder-icon {
  width: 54px;
  height: 54px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  background: var(--gradient-brand);
  color: #fff;
  font-size: 22px;
  font-weight: 800;
  box-shadow: var(--shadow-brand);
}

.app-placeholder-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 100%;
}

.app-placeholder-text span {
  color: var(--text-1);
  font-size: 15px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-placeholder-text small {
  color: var(--text-3);
  font-size: 12px;
}

/* 悬浮操作层 */
.app-overlay {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(4px);
  opacity: 0;
  transition: opacity 0.25s ease;
}

.app-card:hover .app-overlay {
  opacity: 1;
}

.overlay-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 16px;
  font-size: 13px;
  font-weight: 600;
  border-radius: var(--radius-full);
  border: 1px solid rgba(255, 255, 255, 0.4);
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  cursor: pointer;
  backdrop-filter: blur(8px);
  transform: translateY(6px);
  transition:
    transform 0.25s var(--ease-out),
    background 0.2s;
}

.app-card:hover .overlay-btn {
  transform: translateY(0);
}

.overlay-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.overlay-btn--primary {
  background: var(--gradient-brand);
  border-color: transparent;
  box-shadow: var(--shadow-brand);
}

.overlay-btn--primary:hover {
  background: var(--gradient-brand-hover);
}

/* 信息栏 */
.app-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
}

.app-avatar {
  flex-shrink: 0;
  background: var(--gradient-brand);
  color: #fff;
  font-weight: 600;
}

.app-meta {
  flex: 1;
  min-width: 0;
}

.app-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 3px;
  color: var(--text-1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-author {
  font-size: 13px;
  color: var(--text-3);
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 已部署状态点 */
.app-status {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.18);
}
</style>
