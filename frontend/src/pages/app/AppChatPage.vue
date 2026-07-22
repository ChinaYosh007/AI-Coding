<template>
  <div id="appChatPage">
    <!-- 顶部栏 -->
    <div class="header-bar">
      <div class="header-left">
        <div class="app-symbol">
          <ThunderboltFilled />
        </div>
        <h1 class="app-name" :title="displayAppName">{{ displayAppName }}</h1>
        <span v-if="appInfo?.codeGenType" class="code-gen-type-tag">
          {{ formatCodeGenType(appInfo.codeGenType) }}
        </span>
        <span v-if="isModifyMode" class="mode-tag mode-modify">修改模式</span>
        <span v-else class="mode-tag mode-create">创建模式</span>
      </div>
      <div class="header-right">
        <a-button type="text" class="header-btn" @click="showAppDetail">
          <template #icon>
            <InfoCircleOutlined />
          </template>
          应用详情
        </a-button>
        <a-popconfirm
            v-if="isOwner || isAdmin"
            title="确定要删除这个应用吗？删除后相关对话和版本数据将不可恢复。"
            ok-text="删除"
            cancel-text="取消"
            ok-type="danger"
            :disabled="isGenerating || deleting"
            @confirm="deleteApp"
        >
          <a-button
              danger
              class="header-btn delete-app-btn"
              :loading="deleting"
              :disabled="isGenerating"
          >
            <template #icon>
              <DeleteOutlined />
            </template>
            删除应用
          </a-button>
        </a-popconfirm>
        <a-button
            type="primary"
            class="deploy-btn"
            @click="deployApp"
            :loading="deploying"
            :disabled="!isOwner || !selectedVersion"
        >
          <template #icon>
            <CloudUploadOutlined />
          </template>
          {{ selectedVersion ? `部署 V${selectedVersion}` : '部署上线' }}
        </a-button>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content" :class="{ generating: isGenerating }">
      <!-- 左侧面板：对话 + 输入（始终可见） -->
      <div class="step-panel">
        <div class="chat-mini">
          <!-- 生成中：紧凑进度条 -->
          <div v-if="isGenerating" class="gen-progress-bar">
            <template
              v-for="streamMsg in messages.filter(m => m.streamInfo).slice(-1)"
              :key="`stream-bar-${streamMsg.streamInfo?.updatedAt || 'pending'}`"
            >
              <div v-if="streamMsg.streamInfo" class="gen-bar-info">
                <a-spin size="small" />
                <span class="gen-bar-action">{{ streamMsg.streamInfo.currentAction }}</span>
                <strong class="gen-bar-size">{{ formatGeneratedSize(streamMsg.streamInfo.totalChars) }}</strong>
              </div>
              <div v-else class="gen-bar-info">
                <a-spin size="small" />
                <span>正在连接生成服务…</span>
              </div>
            </template>
          </div>

          <div class="panel-header">
            <span class="panel-title">
              <MessageOutlined />
              对话记录
            </span>
            <div class="chat-tools">
              <a-tooltip title="应用对话轮次">
                <span class="chat-stat-pill">
                  <BarChartOutlined />
                  {{ displayDialogRounds }} 轮
                </span>
              </a-tooltip>
              <a-button type="text" size="small" @click="exportChatMarkdown" :loading="exportingChat">
                <template #icon>
                  <FileTextOutlined />
                </template>
                导出
              </a-button>
              <a-button type="text" size="small" @click="openMemoryModal" :loading="memoryLoading">
                <template #icon>
                  <DatabaseOutlined />
                </template>
                记忆
              </a-button>
              <a-button type="text" size="small" @click="openCollaborationModal" :loading="collaborationLoading">
                <template #icon>
                  <TeamOutlined />
                </template>
                协作
              </a-button>
            </div>
          </div>
          <div class="chat-overview">
            <div v-for="item in chatOverviewItems" :key="item.label" class="chat-overview-item">
              <span class="chat-overview-label">{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
          <div class="messages-container" ref="messagesContainer">
            <div v-if="hasMoreHistory" class="load-more-container">
              <a-button type="link" @click="loadMoreHistory" :loading="loadingHistory" size="small">
                加载更多历史消息
              </a-button>
            </div>
            <div v-for="(message, index) in messages.slice(-5)" :key="index" class="message-item">
              <div v-if="message.type === 'user'" class="user-message">
                <div class="message-content">
                  <div class="message-meta">你 · {{ formatMessageTime(message.createTime) }}</div>
                  {{ message.content }}
                </div>
                <div class="message-avatar">
                  <a-avatar :src="loginUserStore.loginUser.userAvatar" />
                </div>
              </div>
              <div v-else class="ai-message">
                <div class="message-avatar">
                  <a-avatar :src="aiAvatar" />
                </div>
                <div class="message-content">
                  <div class="message-meta">AI 助手 · {{ formatMessageTime(message.createTime) }}</div>
                  <MarkdownRenderer v-if="message.content" :content="message.content" />
                  <div v-if="message.loading && message.streamInfo?.tasks?.length" class="generation-tasks">
                    <div
                        v-for="task in message.streamInfo.tasks"
                        :key="task.id"
                        class="generation-task"
                        :class="task.status"
                    >
                      <span class="task-icon">
                        <CheckCircleOutlined v-if="task.status === 'completed'" />
                        <LoadingOutlined v-else-if="task.status === 'active'" />
                        <ClockCircleOutlined v-else />
                      </span>
                      <div class="task-body">
                        <div class="task-label">{{ task.label }}</div>
                        <div v-if="task.description" class="task-desc">{{ task.description }}</div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 选中元素信息展示 -->
          <a-alert
              v-if="selectedElementInfo"
              class="selected-element-alert"
              type="info"
              closable
              @close="clearSelectedElement"
          >
            <template #message>
              <div class="selected-element-info">
                <div class="element-header">
                  <span class="element-tag">
                    选中元素：{{ selectedElementInfo.tagName.toLowerCase() }}
                  </span>
                  <span v-if="selectedElementInfo.id" class="element-id">
                    #{{ selectedElementInfo.id }}
                  </span>
                  <span v-if="selectedElementInfo.className" class="element-class">
                    .{{ selectedElementInfo.className.split(' ').join('.') }}
                  </span>
                </div>
                <div class="element-details">
                  <div v-if="selectedElementInfo.textContent" class="element-item">
                    内容: {{ selectedElementInfo.textContent.substring(0, 50) }}
                    {{ selectedElementInfo.textContent.length > 50 ? '...' : '' }}
                  </div>
                  <div v-if="selectedElementInfo.pagePath" class="element-item">
                    页面路径: {{ selectedElementInfo.pagePath }}
                  </div>
                  <div class="element-item">
                    选择器:
                    <code class="element-selector-code">{{ selectedElementInfo.selector }}</code>
                  </div>
                </div>
              </div>
            </template>
          </a-alert>

          <!-- 用户消息输入框 -->
          <div class="input-container">
            <!-- 已上传图片预览区 -->
            <div v-if="uploadedImages.length > 0" class="uploaded-images-preview">
              <div v-for="(img, idx) in uploadedImages" :key="idx" class="uploaded-image-item">
                <img :src="img.url" :alt="img.name" class="uploaded-image-thumb" />
                <button class="uploaded-image-remove" @click="removeUploadedImage(idx)">×</button>
              </div>
            </div>
            <!-- 已上传文件预览区 -->
            <div v-if="uploadedFiles.length > 0" class="uploaded-files-preview">
              <div v-for="(f, idx) in uploadedFiles" :key="idx" class="uploaded-file-item">
                <span class="file-icon">📄</span>
                <span class="file-name" :title="f.name">{{ f.name }}</span>
                <button class="uploaded-file-remove" @click="removeUploadedFile(idx)">×</button>
              </div>
            </div>
            <div class="input-wrapper">
              <!-- 隐藏的图片上传 input -->
              <input
                  ref="fileInputRef"
                  type="file"
                  accept="image/*"
                  multiple
                  style="display: none"
                  @change="handleImageUpload"
              />
              <a-tooltip v-if="!isOwner" title="无法在别人的作品下对话哦~" placement="top">
                <a-textarea
                    v-model:value="userInput"
                    :placeholder="getInputPlaceholder()"
                    :rows="4"
                    :maxlength="1000"
                    @keydown.enter.prevent="sendMessage"
                    @paste="handlePaste"
                    :disabled="!isOwner"
                />
              </a-tooltip>
              <a-textarea
                  v-else
                  v-model:value="userInput"
                  :placeholder="getInputPlaceholder()"
                  :rows="4"
                  :maxlength="1000"
                  @keydown.enter.prevent="sendMessage"
                  @paste="handlePaste"
              />
              <div class="input-actions">
                <!-- 图片上传按钮 -->
                <button
                    v-if="isOwner"
                    class="upload-btn"
                    :disabled="imageUploading || isGenerating"
                    @click="triggerFileUpload"
                    title="上传图片"
                >
                  <a-spin v-if="imageUploading" size="small" />
                  <PictureOutlined v-else />
                </button>
                <!-- 文件上传按钮 -->
                <button
                    v-if="isOwner"
                    class="upload-btn"
                    :disabled="fileUploading || isGenerating"
                    @click="triggerFileUploadInput"
                    title="上传文件"
                >
                  <a-spin v-if="fileUploading" size="small" />
                  <PaperClipOutlined v-else />
                </button>
                <button
                    class="send-btn"
                    :class="{ 'is-loading': isGenerating }"
                    :disabled="isGenerating || !isOwner"
                    @click="sendMessage"
                >
                  <a-spin v-if="isGenerating" size="small" class="send-btn-spin" />
                  <SendOutlined v-else />
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
      <!-- 中间面板：仅生成中显示文件树+代码查看 -->
      <div class="code-panel">
        <!-- 生成中：文件树 + 代码查看 -->
        <div v-if="isGenerating" class="file-explorer">
          <div class="panel-header">
            <span class="panel-title">
              <CodeOutlined />
              实时文件
            </span>
          </div>
          <template
            v-for="streamMsg in messages.filter(m => m.streamInfo).slice(-1)"
            :key="`stream-mid-${streamMsg.streamInfo?.updatedAt || 'pending'}`"
          >
            <div class="file-explorer-body">
              <aside class="file-tree">
                <div v-if="liveFiles.length === 0" class="file-tree-empty">
                  <a-spin size="small" />
                  <span>等待文件写入…</span>
                </div>
                <button
                    v-for="fileName in liveFiles"
                    :key="fileName"
                    class="file-tree-item"
                    :class="{ active: activeLiveFile === fileName }"
                    type="button"
                    @click="activeLiveFile = fileName"
                >
                  <FileTextOutlined class="file-tree-icon" />
                  <span class="file-tree-name" :title="fileName">{{ fileName }}</span>
                </button>
              </aside>
              <section class="code-viewer">
                <div class="code-viewer-header">
                  <span class="code-viewer-file">{{ currentLiveFile?.name || '选择文件查看代码' }}</span>
                </div>
                <div v-if="!currentLiveFile" class="code-viewer-placeholder">
                  <p>AI 正在生成代码，请稍候…</p>
                </div>
                <div v-else-if="!currentLiveFile.content" class="code-viewer-generating">
                  <a-spin size="small" />
                  <p>代码生成中…</p>
                </div>
                <pre v-else class="code-viewer-content"><code class="hljs" v-html="highlightedCode"></code></pre>
              </section>
            </div>
          </template>
          <div v-if="messages.filter(m => m.streamInfo).length === 0" class="file-explorer-empty">
            <a-spin size="small" />
            <span>正在连接生成服务…</span>
          </div>
        </div>

      </div>
      <!-- 右侧网页展示区域 -->
      <div class="preview-section">
        <div class="preview-header">
          <div class="browser-dots" aria-hidden="true">
            <span></span><span></span><span></span>
          </div>
          <h3 class="preview-title">实时预览</h3>
          <a-select
              v-if="appVersions.length > 0"
              v-model:value="selectedVersion"
              size="small"
              class="version-select"
              :loading="loadingVersions"
              @change="onVersionSelectChange"
          >
            <a-select-option v-for="v in appVersions" :key="v.version" :value="v.version">
              V{{ v.version }}{{ v.version === latestVersion ? ' · 最新' : '' }}
            </a-select-option>
          </a-select>
          <div class="preview-actions">
            <a-button
                v-if="previewUrl || selectedVersion || latestVersion"
                type="text"
                size="small"
                class="preview-action-btn"
                :loading="previewLoading"
                :disabled="isGenerating && !previewUrl"
                @click="loadPreview"
            >
              <template #icon>
                <ReloadOutlined />
              </template>
              {{ previewUrl ? '重新加载' : '加载预览' }}
            </a-button>
            <a-button
                v-if="selectedVersion"
                type="text"
                size="small"
                class="preview-action-btn"
                :loading="sourceLoading"
                @click="openSourceModal"
            >
              <template #icon>
                <CodeOutlined />
              </template>
              查看源码
            </a-button>
            <a-button
                v-if="isOwner && previewUrl"
                type="text"
                size="small"
                class="preview-action-btn"
                :class="{ 'edit-mode-active': isEditMode }"
                @click="toggleEditMode"
            >
              <template #icon>
                <EditOutlined />
              </template>
              {{ isEditMode ? '退出编辑' : '编辑模式' }}
            </a-button>
            <!-- 扩展项5：内联编辑 -->
            <a-button
                v-if="previewUrl && isOwner && appInfo?.codeGenType !== 'vue_project'"
                type="text"
                size="small"
                class="preview-action-btn"
                :class="{ 'edit-mode-active': inlineEditing }"
                :disabled="inlineSaving"
                @click="toggleInlineEdit"
            >
              <template #icon>
                <FormOutlined />
              </template>
              {{ inlineEditing ? '退出内联编辑' : '内联编辑' }}
            </a-button>
            <a-button
                v-if="inlineEditing"
                type="text"
                size="small"
                class="preview-action-btn"
                :loading="inlineSaving"
                @click="saveInlineEdit"
            >
              <template #icon>
                <SaveOutlined />
              </template>
              保存
            </a-button>
            <a-button
                v-if="previewUrl"
                type="text"
                size="small"
                class="preview-action-btn"
                @click="openInNewTab"
            >
              <template #icon>
                <ExportOutlined />
              </template>
              新窗口打开
            </a-button>
          </div>
        </div>
        <div class="preview-content">
          <div v-if="!previewUrl && !isGenerating" class="preview-placeholder">
            <div class="placeholder-icon">🌐</div>
            <p class="placeholder-title">等待生成</p>
            <p class="placeholder-desc">网站文件生成完成后将在这里实时展示</p>
          </div>
          <div v-else-if="isGenerating && !previewUrl" class="preview-loading">
            <a-spin size="large" />
            <p>正在生成网站…</p>
          </div>
          <iframe
              v-else
              :key="previewFrameKey"
              :src="previewUrl"
              class="preview-iframe"
              frameborder="0"
              @load="onIframeLoad"
          ></iframe>
          <div v-if="previewLoading && previewUrl" class="preview-loading-overlay">
            <a-spin />
            <span>正在加载预览页面…</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 应用详情弹窗 -->
    <AppDetailModal
        v-model:open="appDetailVisible"
        :app="appInfo"
        :show-actions="isOwner || isAdmin"
        @edit="editApp"
        @delete="deleteApp"
    />

    <!-- 部署成功弹窗 -->
    <DeploySuccessModal
        v-model:open="deployModalVisible"
        :deploy-url="deployUrl"
        @open-site="openDeployedSite"
    />

    <a-modal
        v-model:open="sourceModalVisible"
        title="版本源码"
        width="1080px"
        :footer="null"
        centered
        class="source-modal"
    >
      <div class="source-modal-toolbar">
        <span class="source-version-label">
          当前版本：V{{ selectedVersion || '-' }}
        </span>
        <div class="source-modal-actions">
          <a-button
              size="small"
              :disabled="!currentSourceFile"
              @click="copyCurrentSource"
          >
            <template #icon>
              <CopyOutlined />
            </template>
            复制当前文件
          </a-button>
          <a-button
              size="small"
              type="primary"
              :loading="downloading"
              :disabled="!isOwner || !selectedVersion"
              @click="downloadCode"
          >
            <template #icon>
              <DownloadOutlined />
            </template>
            下载源码
          </a-button>
        </div>
      </div>
      <div v-if="sourceLoading" class="source-loading">
        <a-spin />
      </div>
      <a-empty v-else-if="sourceFiles.length === 0" description="暂无源码文件" />
      <div v-else class="source-explorer">
        <aside class="source-file-panel">
          <div class="source-file-panel-title">源码文件</div>
          <button
              v-for="file in sourceFiles"
              :key="file.name"
              class="source-file-item"
              :class="{ active: file.name === activeSourceFile }"
              type="button"
              @click="activeSourceFile = file.name"
          >
            <FileTextOutlined class="source-file-icon" />
            <span class="source-file-name" :title="file.name">{{ file.name }}</span>
          </button>
        </aside>
        <section class="source-code-panel">
          <div class="source-code-header">
            <span class="source-code-file">{{ currentSourceFile?.name || '-' }}</span>
          </div>
          <pre class="source-code-view"><code>{{ currentSourceFile?.content || '' }}</code></pre>
        </section>
      </div>
    </a-modal>

    <a-modal
        v-model:open="memoryModalVisible"
        title="智能记忆管理"
        width="720px"
        :footer="null"
        centered
    >
      <a-spin :spinning="memoryLoading">
        <div class="memory-panel">
          <div class="memory-summary-card">
            <div class="memory-summary-header">
              <div>
                <div class="memory-title">对话摘要</div>
                <div class="memory-subtitle">
                  用于压缩长对话上下文，减少重复 token，并保持生成方向一致。
                </div>
              </div>
              <a-tag :color="appMemory?.enabled ? 'green' : 'default'">
                {{ appMemory?.enabled ? '已启用' : '未启用' }}
              </a-tag>
            </div>
            <div class="memory-summary-content">
              {{ appMemory?.summary || '暂无记忆摘要。点击生成后，会基于当前应用的历史对话整理项目背景、偏好和关键修改记录。' }}
            </div>
          </div>
          <div class="memory-actions">
            <a-button @click="loadMemory" :loading="memoryLoading">刷新</a-button>
            <a-button type="primary" @click="summarizeMemory" :loading="memorySaving">
              生成/更新记忆摘要
            </a-button>
          </div>
        </div>
      </a-spin>
    </a-modal>

    <a-modal
        v-model:open="collaborationModalVisible"
        title="多人协作"
        width="720px"
        :footer="null"
        centered
    >
      <div class="collaboration-panel">
        <div class="collaboration-invite">
          <a-input
              v-model:value="inviteUserAccount"
              placeholder="输入协作者账号"
              :disabled="collaborationSaving"
              @pressEnter="inviteCollaborator"
          />
          <a-button type="primary" :loading="collaborationSaving" @click="inviteCollaborator">
            <template #icon>
              <UserAddOutlined />
            </template>
            邀请
          </a-button>
        </div>
        <a-spin :spinning="collaborationLoading">
          <a-empty
              v-if="collaborators.length === 0"
              description="暂无协作者。邀请用户后，对方可以共同参与这个应用的对话。"
          />
          <div v-else class="collaborator-list">
            <div v-for="member in collaborators" :key="member.userId || member.userAccount" class="collaborator-item">
              <a-avatar :src="member.userAvatar">
                {{ (member.userName || member.userAccount || 'U').charAt(0).toUpperCase() }}
              </a-avatar>
              <div class="collaborator-main">
                <strong>{{ member.userName || member.userAccount || '未知用户' }}</strong>
                <span>{{ member.userAccount || '-' }}</span>
              </div>
              <a-tag>{{ member.role || 'collaborator' }}</a-tag>
            </div>
          </div>
        </a-spin>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, onUnmounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import hljs from 'highlight.js/lib/common'
import 'highlight.js/styles/github.css'
import { useLoginUserStore } from '@/stores/loginUser'
import {
  getAppVoById,
  deployApp as deployAppApi,
  deleteApp as deleteAppApi,
  deleteAppByAdmin,
  exportAppChatHistoryAsMarkdown,
  getAppChatHistoryMemory,
  getAppChatHistoryStats,
  getAppCollaborationMembers,
  inviteAppCollaborator,
  summarizeAppChatHistoryMemory,
} from '@/api/appController'
import { listAppVersions } from '@/api/appVersionController'
import { listAppChatHistory } from '@/api/chatHistoryController'
import { CodeGenTypeEnum, formatCodeGenType } from '@/utils/codeGenTypes'
import { getDisplayNameFromText, getFallbackAppName } from '@/utils/appNameParser'
import { formatTime } from '@/utils/time'
import request from '@/request'

import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import AppDetailModal from '@/components/AppDetailModal.vue'
import DeploySuccessModal from '@/components/DeploySuccessModal.vue'
import aiAvatar from '@/assets/aiAvatar.png'
import { API_BASE_URL, getGeneratedPreviewUrl } from '@/config/env'
import { VisualEditor, type ElementInfo } from '@/utils/visualEditor'

import {
  CloudUploadOutlined,
  SendOutlined,
  ExportOutlined,
  InfoCircleOutlined,
  DownloadOutlined,
  EditOutlined,
  MessageOutlined,
  ReloadOutlined,
  ThunderboltFilled,
  CodeOutlined,
  CopyOutlined,
  DeleteOutlined,
  BarChartOutlined,
  FileTextOutlined,
  DatabaseOutlined,
  TeamOutlined,
  UserAddOutlined,
  PictureOutlined,
  PaperClipOutlined,
  FormOutlined,
  SaveOutlined,
  CheckCircleOutlined,
  LoadingOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 应用信息
const appInfo = ref<API.AppVO>()
const appId = ref('')

// AI 应用名称自动轮询
const initialAppName = ref<string>('')
let nameRefreshTimer: number | undefined
let nameRefreshCount = 0
const NAME_REFRESH_INTERVAL = 2000
const MAX_NAME_REFRESH_COUNT = 15

// 对话相关
interface Message {
  type: 'user' | 'ai'
  content: string
  loading?: boolean
  createTime?: string
  streamInfo?: StreamInfo
}

interface GenerationTask {
  id: string
  label: string
  description?: string
  status: 'pending' | 'active' | 'completed'
}

interface StreamInfo {
  totalChars: number
  stage: number
  currentAction: string
  fileNames: string[]
  updatedAt?: string
  tasks?: GenerationTask[]
}

interface SourceFile {
  name: string
  content: string
}

interface SourceFileListResponse {
  code?: number
  data?: string[]
  message?: string
}

interface ChatStats {
  totalRounds?: number
  totalMessages?: number
  lastActiveTime?: string
}

interface AppMemory {
  enabled?: boolean
  summary?: string
}

type Collaborator = API.AppCollaborationMemberVO

const messages = ref<Message[]>([])
const userInput = ref('')
const isGenerating = ref(false)
const messagesContainer = ref<HTMLElement>()

// 对话历史相关
const loadingHistory = ref(false)
const hasMoreHistory = ref(false)
const lastCreateTime = ref<string>()
const historyLoaded = ref(false)
const chatStats = ref<ChatStats>({})
const chatStatsLoading = ref(false)
const exportingChat = ref(false)

// 预览相关
const previewUrl = ref('')
const previewReady = ref(false)
const previewLoading = ref(false)
const previewFrameKey = ref(0)
const usingDevServerPreview = ref(false)
let previewLoadTimer: number | undefined

// 部署相关
const deploying = ref(false)
const deployModalVisible = ref(false)
const deployUrl = ref('')

// 下载相关
const downloading = ref(false)
const deleting = ref(false)

// 代码版本相关
const appVersions = ref<API.AppVersion[]>([])
const loadingVersions = ref(false)
const selectedVersion = ref<number>()

// 源码查看相关
const sourceModalVisible = ref(false)
const sourceLoading = ref(false)
const sourceFiles = ref<SourceFile[]>([])
const activeSourceFile = ref('')

// 实时代码展示（生成中）
const liveCode = ref('')
const liveFiles = ref<string[]>([])
const activeLiveFile = ref('')
// Vue 模式：从 tool_executed 消息提取的 文件路径 → 文件内容 映射
const fileContentMap = ref<Map<string, string>>(new Map())
// 当前生成中的版本号（用于从后端 API 拉取已写入的文件内容）
const generatingVersion = ref<number | undefined>(undefined)
// 是否为 Vue 生成模式（有工具调用写文件），用于区分 HTML 模式
const isVueGenMode = ref(false)
// AI 实时输出文本（显示在左侧面板）
const liveAiText = ref('')

// 智能记忆相关
const memoryModalVisible = ref(false)
const memoryLoading = ref(false)
const memorySaving = ref(false)
const appMemory = ref<AppMemory>()

// 多人协作相关
const collaborationModalVisible = ref(false)
const collaborationLoading = ref(false)
const collaborationSaving = ref(false)
const collaborators = ref<Collaborator[]>([])
const inviteUserAccount = ref('')

// 可视化编辑相关
const isEditMode = ref(false)
const selectedElementInfo = ref<ElementInfo | null>(null)
const visualEditor = new VisualEditor({
  onElementSelected: (elementInfo: ElementInfo) => {
    selectedElementInfo.value = elementInfo
  },
})

// 扩展项1：创建/修改模式
const isModifyMode = computed(() => appVersions.value.length > 0)

// 扩展项4：图片上传相关
const uploadedImages = ref<{ url: string; name: string }[]>([])
const imageUploading = ref(false)
const uploadedFiles = ref<{ url: string; name: string; size: number }[]>([])
const fileUploading = ref(false)
const fileInputRef = ref<HTMLInputElement>()

// 扩展项5：内联编辑保存
const inlineEditing = ref(false)
const inlineSaving = ref(false)

// 权限相关
const isOwner = computed(() => {
  return appInfo.value?.userId === loginUserStore.loginUser.id
})

const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === 'admin'
})

const displayAppName = computed(() => {
  return getDisplayNameFromText(appInfo.value?.appName) || getFallbackAppName(appInfo.value?.initPrompt, '网站生成器')
})

const localDialogRounds = computed(() => {
  return messages.value.filter((item) => item.type === 'user').length
})

const displayDialogRounds = computed(() => {
  return chatStats.value.totalRounds ?? localDialogRounds.value
})

const latestVersion = computed(() => {
  return appVersions.value[0]?.version
})

const selectedVersionRecord = computed(() => {
  return appVersions.value.find((versionItem) => versionItem.version === selectedVersion.value)
})

const currentSourceFile = computed(() => {
  return sourceFiles.value.find((file) => file.name === activeSourceFile.value)
})

// 实时代码：优先从 fileContentMap 取内容，HTML 模式回退到 liveCode
const currentLiveFile = computed(() => {
  const name = activeLiveFile.value || liveFiles.value[0]
  if (!name) return null
  // Vue 模式：从 fileContentMap 取实际文件内容
  const mappedContent = fileContentMap.value.get(name)
  if (mappedContent) {
    return { name, content: mappedContent }
  }
  // HTML/多文件模式：aiText 就是代码
  if (!isVueGenMode.value && liveCode.value) {
    return { name, content: liveCode.value }
  }
  // Vue 模式但该文件内容尚未加载：返回空，模板显示"正在生成..."
  return { name, content: '' }
})

// 根据文件扩展名推断 highlight.js 语言
const getLanguageFromFileName = (fileName: string): string => {
  const ext = fileName.split('.').pop()?.toLowerCase() || ''
  const langMap: Record<string, string> = {
    html: 'html', htm: 'html',
    css: 'css', scss: 'scss', sass: 'sass', less: 'less',
    js: 'javascript', mjs: 'javascript', cjs: 'javascript', jsx: 'javascript',
    ts: 'typescript', tsx: 'typescript',
    vue: 'xml', json: 'json', xml: 'xml',
    md: 'markdown', yml: 'yaml', yaml: 'yaml',
    sh: 'bash', bash: 'bash',
  }
  return langMap[ext] || ''
}

// 语法高亮后的 HTML
const highlightedCode = computed(() => {
  const content = currentLiveFile.value?.content
  if (!content) return ''
  // 生成中用纯文本转义，避免 hljs 每 120ms 全量高亮导致卡顿
  if (isGenerating.value) {
    return content.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  }
  const lang = getLanguageFromFileName(currentLiveFile.value!.name)
  try {
    if (lang && hljs.getLanguage(lang)) {
      return hljs.highlight(content, { language: lang }).value
    }
    return hljs.highlightAuto(content).value
  } catch {
    return content.replace(/</g, '&lt;').replace(/>/g, '&gt;')
  }
})

// 生成中切换文件时，拉取该文件的实际内容
watch(activeLiveFile, (newFile) => {
  if (newFile && isVueGenMode.value && generatingVersion.value && !fileContentMap.value.has(newFile)) {
    fetchLiveFileContent(newFile)
  }
})

// 应用详情相关
const generationSteps = ['需求拆解', '页面结构', '样式交互', '代码整理']

const displayTotalMessages = computed(() => {
  return Math.max(chatStats.value.totalMessages ?? 0, messages.value.length)
})

const displayLastActiveTime = computed(() => {
  const latestMessageTime = messages.value[messages.value.length - 1]?.createTime
  return latestMessageTime || chatStats.value.lastActiveTime || appInfo.value?.updateTime
})

const latestGeneratedVersionLabel = computed(() => {
  return latestVersion.value ? `V${latestVersion.value}` : '待生成'
})

const memoryOverviewStatus = computed(() => {
  if (memoryLoading.value) return '读取中'
  return appMemory.value?.enabled ? '已沉淀' : '待总结'
})

const collaboratorOverviewText = computed(() => {
  if (collaborationLoading.value) return '读取中'
  return collaborators.value.length ? `${collaborators.value.length} 人` : '仅自己'
})

const chatOverviewItems = computed(() => [
  {
    label: '消息',
    value: `${displayTotalMessages.value} 条`,
  },
  {
    label: '版本',
    value: latestGeneratedVersionLabel.value,
  },
  {
    label: '记忆',
    value: memoryOverviewStatus.value,
  },
  {
    label: '协作',
    value: collaboratorOverviewText.value,
  },
  {
    label: '最近',
    value: formatMessageTime(displayLastActiveTime.value),
  },
])

const appDetailVisible = ref(false)

// 显示应用详情
const showAppDetail = () => {
  appDetailVisible.value = true
}

// 加载当前应用的代码版本
const loadAppVersions = async () => {
  if (!appId.value || loadingVersions.value) return
  loadingVersions.value = true
  try {
    const res = await listAppVersions()
    const records = Array.isArray(res.data) ? res.data : []
    appVersions.value = records
        .filter((versionItem) => String(versionItem.appId) === String(appId.value))
        .sort((a, b) => Number(b.version || 0) - Number(a.version || 0))

    if (appVersions.value.length === 0) {
      selectedVersion.value = undefined
      return
    }

    const hasSelectedVersion = appVersions.value.some(
        (versionItem) => versionItem.version === selectedVersion.value,
    )
    if (!hasSelectedVersion) {
      selectedVersion.value = appVersions.value[0].version
    }
  } catch (error) {
    console.error('加载代码版本失败：', error)
    message.error('加载代码版本失败')
  } finally {
    loadingVersions.value = false
  }
}

const selectVersion = (versionItem: API.AppVersion) => {
  if (!versionItem.version) return
  selectedVersion.value = versionItem.version
  sourceFiles.value = []
  activeSourceFile.value = ''
  updatePreview()
}

const onVersionSelectChange = (val: number) => {
  const item = appVersions.value.find((v) => v.version === val)
  if (item) selectVersion(item)
}

const getSourceStaticBaseUrl = () => {
  if (import.meta.env.DEV && API_BASE_URL === '/api') {
    return `${window.location.protocol}//${window.location.hostname}:8080/api/static`
  }
  return `${API_BASE_URL}/static`
}

const encodeSourcePath = (filePath: string) => {
  return filePath
      .split('/')
      .filter(Boolean)
      .map((pathPart) => encodeURIComponent(pathPart))
      .join('/')
}

// 生成中：从后端 API 拉取已写入磁盘的文件内容
const fetchLiveFileContent = async (filePath: string, retryCount = 0) => {
  if (!appId.value || !generatingVersion.value) return
  if (fileContentMap.value.has(filePath)) return // 已缓存
  try {
    const sourceStaticBaseUrl = getSourceStaticBaseUrl()
    const cacheKey = Date.now()
    const response = await request.get<string>(
      `${sourceStaticBaseUrl}/preview/${appId.value}/${generatingVersion.value}/${encodeSourcePath(filePath)}?t=${cacheKey}`,
      {
        responseType: 'text',
        timeout: 10000,
        transformResponse: [(data) => data],
        validateStatus: (status) => status < 500,
      },
    )
    if (response.status === 200 && response.data) {
      fileContentMap.value.set(filePath, response.data)
      fileContentMap.value = new Map(fileContentMap.value) // 触发响应式
    } else if (response.status === 404 && retryCount < 3) {
      // 文件可能还没写入磁盘，延迟重试
      setTimeout(() => fetchLiveFileContent(filePath, retryCount + 1), 800)
    }
  } catch {
    // 静默失败，不影响生成流程
  }
}

const loadSourceFiles = async () => {
  if (!appId.value || !selectedVersion.value) {
    sourceFiles.value = []
    activeSourceFile.value = ''
    return
  }

  sourceLoading.value = true
  try {
    const loadedFiles: SourceFile[] = []
    const sourceStaticBaseUrl = getSourceStaticBaseUrl()
    const cacheKey = Date.now()
    const fileListResponse = await request.get<SourceFileListResponse>(
        `${sourceStaticBaseUrl}/preview/${appId.value}/${selectedVersion.value}/files?t=${cacheKey}`,
        {
          timeout: 15000,
          validateStatus: (status) => status < 500,
        },
    )
    const sourceFilePaths = Array.isArray(fileListResponse.data?.data)
        ? fileListResponse.data.data
        : []

    for (const fileName of sourceFilePaths) {
      const response = await request.get<string>(
          `${sourceStaticBaseUrl}/preview/${appId.value}/${selectedVersion.value}/${encodeSourcePath(fileName)}?t=${cacheKey}`,
          {
            responseType: 'text',
            timeout: 15000,
            transformResponse: [(data) => data],
            validateStatus: (status) => status < 500,
          },
      )

      if (response.status !== 200) {
        continue
      }

      loadedFiles.push({
        name: fileName,
        content: response.data,
      })
    }

    sourceFiles.value = loadedFiles
    activeSourceFile.value = loadedFiles[0]?.name || ''

    if (loadedFiles.length === 0) {
      message.warning('未找到当前版本源码文件')
    }
  } catch (error) {
    console.error('加载源码失败：', error)
    message.error('加载源码失败，请重试')
  } finally {
    sourceLoading.value = false
  }
}

const openSourceModal = async () => {
  sourceModalVisible.value = true
  await loadSourceFiles()
}

const copyCurrentSource = async () => {
  if (!currentSourceFile.value) return
  try {
    await navigator.clipboard.writeText(currentSourceFile.value.content)
    message.success('源码已复制')
  } catch (error) {
    console.error('复制源码失败：', error)
    message.error('复制失败，请手动选择复制')
  }
}

const downloadBlob = (blob: Blob, fileName: string) => {
  const downloadUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = downloadUrl
  link.download = fileName
  link.click()
  URL.revokeObjectURL(downloadUrl)
}

const buildCurrentMarkdown = () => {
  const lines = [
    `# ${displayAppName.value} 对话记录`,
    '',
    `- 应用 ID：${appId.value || '-'}`,
    `- 对话轮次：${displayDialogRounds.value}`,
    `- 导出时间：${formatTime(new Date().toISOString())}`,
    '',
    '---',
    '',
  ]

  messages.value.forEach((item, index) => {
    const role = item.type === 'user' ? '用户' : 'AI'
    lines.push(`## ${index + 1}. ${role}`)
    if (item.createTime) {
      lines.push(`> ${formatTime(item.createTime)}`)
      lines.push('')
    }
    lines.push(item.content || '')
    lines.push('')
  })

  return lines.join('\n')
}

const exportChatMarkdown = async () => {
  if (!appId.value || exportingChat.value) return

  exportingChat.value = true
  try {
    const res = await exportAppChatHistoryAsMarkdown(appId.value, {
      timeout: 30000,
      validateStatus: (status: number) => status < 500,
    })
    if (res.status === 200 && res.data.code === 0 && res.data.data) {
      downloadBlob(
          new Blob([res.data.data], { type: 'text/markdown;charset=utf-8' }),
          `${displayAppName.value}-对话记录.md`,
      )
      message.success('对话记录已导出')
      return
    }

    const markdown = buildCurrentMarkdown()
    downloadBlob(new Blob([markdown], { type: 'text/markdown;charset=utf-8' }), `${displayAppName.value}-当前对话.md`)
    message.warning('后端导出接口未就绪，已导出当前已加载的对话')
  } catch (error) {
    console.warn('后端导出接口不可用，使用前端兜底导出：', error)
    const markdown = buildCurrentMarkdown()
    downloadBlob(new Blob([markdown], { type: 'text/markdown;charset=utf-8' }), `${displayAppName.value}-当前对话.md`)
    message.warning('已导出当前已加载的对话')
  } finally {
    exportingChat.value = false
  }
}

const loadChatStats = async () => {
  if (!appId.value || chatStatsLoading.value) return

  chatStatsLoading.value = true
  try {
    const res = await getAppChatHistoryStats(appId.value, {
      timeout: 10000,
      validateStatus: (status: number) => status < 500,
    })
    if (res.status === 200 && res.data.code === 0 && typeof res.data.data === 'number') {
      const totalMessages = res.data.data
      chatStats.value = {
        totalRounds: Math.ceil(totalMessages / 2),
        totalMessages,
        lastActiveTime: messages.value[messages.value.length - 1]?.createTime,
      }
      return
    }
  } catch (error) {
    console.warn('对话统计接口不可用，使用前端本地统计：', error)
  } finally {
    chatStatsLoading.value = false
  }

  chatStats.value = {
    totalRounds: localDialogRounds.value,
    totalMessages: messages.value.length,
    lastActiveTime: messages.value[messages.value.length - 1]?.createTime,
  }
}

const openMemoryModal = async () => {
  memoryModalVisible.value = true
  await loadMemory()
}

const loadMemory = async () => {
  if (!appId.value || memoryLoading.value) return

  memoryLoading.value = true
  try {
    const res = await getAppChatHistoryMemory(appId.value, {
      timeout: 10000,
      validateStatus: (status: number) => status < 500,
    })
    if (res.status === 200 && res.data.code === 0) {
      const summary = res.data.data || ''
      appMemory.value = {
        enabled: Boolean(summary),
        summary,
      }
      return
    }
    appMemory.value = {}
  } catch (error) {
    console.warn('加载智能记忆失败：', error)
    appMemory.value = {}
  } finally {
    memoryLoading.value = false
  }
}

const summarizeMemory = async () => {
  if (!appId.value || !selectedVersion.value || memorySaving.value) return

  memorySaving.value = true
  try {
    const res = await summarizeAppChatHistoryMemory(appId.value, selectedVersion.value!)
    if (res.data.code === 0) {
      await loadMemory()
      message.success('记忆摘要已更新')
    } else {
      message.error('更新记忆失败：' + res.data.message)
    }
  } catch (error) {
    console.error('更新记忆失败：', error)
    message.error('后端记忆接口未就绪')
  } finally {
    memorySaving.value = false
  }
}

const openCollaborationModal = async () => {
  collaborationModalVisible.value = true
  await loadCollaborators()
}

const loadCollaborators = async () => {
  if (!appId.value || collaborationLoading.value) return

  collaborationLoading.value = true
  try {
    const res = await getAppCollaborationMembers(appId.value, {
      timeout: 10000,
      validateStatus: (status: number) => status < 500,
    })
    if (res.status === 200 && res.data.code === 0) {
      collaborators.value = res.data.data || []
      return
    }
    collaborators.value = []
  } catch (error) {
    console.warn('加载协作者失败：', error)
    collaborators.value = []
  } finally {
    collaborationLoading.value = false
  }
}

const inviteCollaborator = async () => {
  if (!appId.value || collaborationSaving.value) return
  const userAccount = inviteUserAccount.value.trim()
  if (!userAccount) {
    message.warning('请输入协作者账号')
    return
  }

  collaborationSaving.value = true
  try {
    const res = await inviteAppCollaborator(appId.value, {
      appId: appId.value,
      userAccount,
    })
    if (res.data.code === 0) {
      inviteUserAccount.value = ''
      message.success('已发送协作邀请')
      await loadCollaborators()
    } else {
      message.error('邀请失败：' + res.data.message)
    }
  } catch (error) {
    console.error('邀请协作者失败：', error)
    message.error('后端协作接口未就绪')
  } finally {
    collaborationSaving.value = false
  }
}

const formatMessageTime = (time?: string) => {
  if (!time) return '刚刚'
  const formatted = formatTime(time)
  return formatted || '刚刚'
}

// 从 AI 文本流中提取指定代码块（html/css/js）
const extractCodeBlock = (text: string, fileName: string): string => {
  if (!text) return ''
  const lang = fileName.endsWith('.css') ? 'css'
    : fileName.endsWith('.js') ? 'javascript'
    : 'html'
  const regex = new RegExp('```' + lang + '\\s*\\n([\\s\\S]*?)```', 'i')
  const match = text.match(regex)
  if (match) return match[1].trim()
  // 如果是 html 且没找到代码块，直接返回原文（可能就是 HTML）
  if (lang === 'html' && text.includes('<') && text.includes('>')) return text.trim()
  return ''
}

const formatGeneratedSize = (totalChars: number) => {
  if (totalChars >= 1000) {
    return `${(totalChars / 1000).toFixed(1)}k 字符`
  }
  return `${totalChars} 字符`
}

const getStreamStage = (totalChars: number, fileCount: number) => {
  if (fileCount >= 2 || totalChars > 30000) return 3
  if (fileCount >= 1 || totalChars > 12000) return 2
  if (totalChars > 3000) return 1
  return 0
}

const generationStatusMessages = [
  [
    '已收到需求，正在拆解页面结构和内容模块。',
    '正在理解你的描述，规划导航、主体内容和预览入口。',
    '正在整理页面文案与演示数据，让内容更完整。',
    '正在准备生成文件，右侧预览会在服务就绪后自动更新。',
  ],
  [
    '正在生成页面骨架，导航、内容区和页脚会一起整理。',
    '正在补充核心模块与页面结构，请稍等。',
    '正在把需求转成可运行的前端代码。',
    '正在组织页面区块顺序，避免内容显得单薄。',
  ],
  [
    '页面主体已经生成，正在完善样式和交互细节。',
    '正在优化按钮、卡片和响应式布局。',
    '正在补充预览体验，等待开发服务热加载完成。',
    '正在检查资源路径和页面状态，确保右侧可以加载。',
  ],
  [
    '正在完成最后的代码整理，后端会启动预览服务。',
    '正在等待 npm run dev 完成，服务就绪后将自动加载。',
    '正在收尾生成文件和热加载地址。',
    '即将刷新右侧预览，你也可以稍后手动重新加载。',
  ],
]

const getRotatingMessage = (messages: string[], tick: number) => {
  return messages[tick % messages.length]
}

// 根据项目类型构建生成任务步骤
const buildGenerationTasks = (
  codeGenType: string | undefined,
  resourceCollectionStatus: string,
  aiTextLength: number,
  pendingFile: string,
  writtenFiles: string[],
  devServerUrl: string,
  completed: boolean,
  isModification: boolean,
): GenerationTask[] => {
  const isVue = codeGenType === 'vue_project'
  const isMultiFile = codeGenType === 'multi_file'

  const hasResources = !isModification && !!resourceCollectionStatus
  const hasCode = aiTextLength > 0 || writtenFiles.length > 0
  const hasFiles = writtenFiles.length > 0
  const hasDevServer = !!devServerUrl

  const baseTasks: GenerationTask[] = []
  if (!isModification) {
    baseTasks.push({
      id: 'resource_collection',
      label: '资源收集',
      description: '并行搜集图片、插画和 Logo',
      status: hasResources ? 'active' : 'pending',
    })
  }
  baseTasks.push({
    id: 'code_generation',
    label: isModification ? '修改文件' : isVue ? '生成文件' : isMultiFile ? '生成多文件' : '生成代码',
    description: isModification
      ? '读取现有代码并按要求增量修改'
      : isVue
        ? '正在写入 Vue 组件与配置文件'
        : isMultiFile
          ? '正在写入 HTML/CSS/JS'
          : '正在生成单文件页面',
    status: 'pending',
  })

  if (isVue) {
    baseTasks.push({
      id: 'dev_server',
      label: '启动开发服务器',
      description: '执行 npm run dev 并返回预览地址',
      status: 'pending',
    })
  }
  baseTasks.push({
    id: 'preview_ready',
    label: '预览就绪',
    description: isVue ? '启动开发服务器并加载右侧预览' : '右侧预览加载完成',
    status: 'pending',
  })

  // 推进状态
  const resourceTask = baseTasks.find(task => task.id === 'resource_collection')
  const generationTask = baseTasks.find(task => task.id === 'code_generation')
  const devServerTask = baseTasks.find(task => task.id === 'dev_server')
  const previewTask = baseTasks.find(task => task.id === 'preview_ready')

  if (resourceTask && !hasResources && (hasCode || completed)) {
    resourceTask.status = 'completed'
  }

  if (generationTask && (hasCode || completed)) {
    generationTask.status = 'completed'
  } else if (!hasResources && !completed) {
    if (generationTask) generationTask.status = 'active'
  }

  if (isVue) {
    if (devServerTask) {
      if (hasFiles && !hasDevServer && !completed) {
        devServerTask.status = 'active'
      } else if (hasDevServer || completed) {
        devServerTask.status = 'completed'
      }
    }
    if (previewTask && (hasDevServer || completed)) previewTask.status = 'completed'
  } else {
    if (previewTask) {
      if (completed) {
        previewTask.status = 'completed'
      } else if (hasCode) {
        previewTask.status = 'active'
      }
    }
  }

  return baseTasks
}

const buildGenerationStatusMessage = (
  aiText: string,
  pendingFile: string,
  writtenFiles: string[],
  devServerUrl: string,
  tick: number,
  completed = false,
) => {
  if (completed) {
    const summary = writtenFiles.length > 0 ? `本次已生成 ${writtenFiles.length} 个文件。` : '本次生成已完成。'
    return devServerUrl
      ? `${summary}\n\n预览服务已启动，右侧已切换到最新热加载页面。你可以继续描述修改需求。`
      : `${summary}\n\n暂未收到热加载地址，已使用静态预览兜底。你可以点击右侧“重新加载”刷新页面。`
  }

  const stage = getStreamStage(aiText.length, writtenFiles.length)
  const lines = [getRotatingMessage(generationStatusMessages[stage], tick)]
  if (pendingFile) {
    lines.push(`正在处理：\`${pendingFile}\``)
  } else if (writtenFiles.length > 0) {
    lines.push(`已完成文件：${writtenFiles.length} 个`)
  }
  if (writtenFiles.length > 0) {
    lines.push(`最近完成：${writtenFiles.slice(-3).map((fileName) => `\`${fileName}\``).join('、')}`)
  }
  lines.push(
    devServerUrl
      ? '开发服务器已就绪，右侧预览正在加载最新页面。'
      : '后端完成 npm run dev 并返回地址后，右侧会自动加载。',
  )
  return lines.join('\n\n')
}

const extractGeneratedFileName = (chunk: string) => {
  const fileMatch =
      chunk.match(/写入文件\s+([^\s\n]+)/) ||
      chunk.match(/write(?:\s+to)?\s+file\s+([^\s\n]+)/i) ||
      chunk.match(/relative(?:File)?Path["']?\s*[:=]\s*["']([^"']+)/i)
  return fileMatch?.[1]?.replace(/[，,。.;；]+$/, '') || ''
}

const parseBackendTextMessage = (text: string) => {
  if (text.includes('[选择工具] 写入文件')) {
    return { type: 'tool_request', data: text }
  }
  if (text.includes('[工具调用] 写入文件')) {
    return {
      type: 'tool_executed',
      data: text,
      filePath: extractGeneratedFileName(text),
    }
  }
  return null
}

const createStreamInfo = (): StreamInfo => ({
  totalChars: 0,
  stage: 0,
  currentAction: '已连接生成流，正在理解需求',
  fileNames: [],
  updatedAt: new Date().toISOString(),
})

const normalizeAiHistoryContent = (content?: string) => {
  if (!content) return ''
  const codeBlockCount = (content.match(/```/g) || []).length
  const looksLikeGeneratedCode =
      content.length > 12000 ||
      codeBlockCount >= 2 ||
      /```(?:html|css|js|javascript)/i.test(content)
  if (!looksLikeGeneratedCode) {
    return content
  }
  return '网站代码已生成完成，右侧可查看预览。你可以继续提出修改需求。'
}

// 加载对话历史
const loadChatHistory = async (isLoadMore = false) => {
  if (!appId.value || loadingHistory.value) return
  loadingHistory.value = true
  try {
    const params: API.listAppChatHistoryParams = {
      appId: appId.value,
      pageSize: 10,
    }
    // 如果是加载更多，传递最后一条消息的创建时间作为游标
    if (isLoadMore && lastCreateTime.value) {
      params.lastCreateTime = lastCreateTime.value
    }
    const res = await listAppChatHistory(params)
    if (res.data.code === 0 && res.data.data) {
      const chatHistories = res.data.data.records || []
      if (chatHistories.length > 0) {
        // 将对话历史转换为消息格式，并按时间正序排列（老消息在前）
        const historyMessages: Message[] = chatHistories
            .map((chat) => ({
              type: (chat.messageType === 'user' ? 'user' : 'ai') as 'user' | 'ai',
              content:
                  chat.messageType === 'user'
                      ? chat.message || ''
                      : normalizeAiHistoryContent(chat.message),
              createTime: chat.createTime,
            }))
            .reverse() // 反转数组，让老消息在前
        if (isLoadMore) {
          // 加载更多时，将历史消息添加到开头
          messages.value.unshift(...historyMessages)
        } else {
          // 初始加载，直接设置消息列表
          messages.value = historyMessages
        }
        // 更新游标
        lastCreateTime.value = chatHistories[chatHistories.length - 1]?.createTime
        // 检查是否还有更多历史
        hasMoreHistory.value = chatHistories.length === 10
      } else {
        hasMoreHistory.value = false
      }
      historyLoaded.value = true
      await loadChatStats()
    }
  } catch (error) {
    console.error('加载对话历史失败：', error)
    message.error('加载对话历史失败')
  } finally {
    loadingHistory.value = false
  }
}

// 加载更多历史消息
const loadMoreHistory = async () => {
  await loadChatHistory(true)
}

// 启动应用名称轮询，AI 异步生成名称后自动刷新
const startAppNameRefresh = () => {
  if (!appInfo.value?.id) return
  if (nameRefreshTimer !== undefined) return

  initialAppName.value = appInfo.value?.appName || ''
  nameRefreshCount = 0

  const refresh = async () => {
    try {
      const res = await getAppVoById({ id: appInfo.value?.id || appId.value })
      if (res.data.code === 0 && res.data.data) {
        const newAppName = res.data.data.appName
        if (newAppName && newAppName !== initialAppName.value) {
          appInfo.value = { ...appInfo.value, ...res.data.data }
          clearNameRefreshTimer()
          return
        }
      }
    } catch (error) {
      console.error('刷新应用名称失败：', error)
    }

    nameRefreshCount++
    if (nameRefreshCount >= MAX_NAME_REFRESH_COUNT) {
      clearNameRefreshTimer()
      return
    }

    nameRefreshTimer = window.setTimeout(refresh, NAME_REFRESH_INTERVAL)
  }

  nameRefreshTimer = window.setTimeout(refresh, NAME_REFRESH_INTERVAL)
}

// 清理应用名称轮询计时器
const clearNameRefreshTimer = () => {
  if (nameRefreshTimer !== undefined) {
    window.clearTimeout(nameRefreshTimer)
    nameRefreshTimer = undefined
  }
}

// 获取应用信息
const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error('应用ID不存在')
    router.push('/')
    return
  }

  appId.value = id

  try {
    const res = await getAppVoById({ id })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data

      // 启动名称轮询，等待 AI 异步生成应用名称
      startAppNameRefresh()

      // 先加载对话历史
      await loadChatHistory()
      await loadAppVersions()
      void loadMemory()
      void loadCollaborators()
      // 如果有至少2条对话记录，展示对应的网站
      // 如果当前已经是后端返回的 dev server 热加载预览，不要覆盖成静态预览
      if (messages.value.length >= 2 && !usingDevServerPreview.value) {
        updatePreview()
      }
      // 检查是否需要自动发送初始提示词
      // 只有在是自己的应用且没有对话历史时才自动发送
      if (
          appInfo.value.initPrompt &&
          isOwner.value &&
          messages.value.length === 0 &&
          historyLoaded.value
      ) {
        await sendInitialMessage(appInfo.value.initPrompt)
      }
    } else {
      message.error('获取应用信息失败')
      router.push('/')
    }
  } catch (error) {
    console.error('获取应用信息失败：', error)
    message.error('获取应用信息失败')
    router.push('/')
  }
}

// 发送初始消息
const sendInitialMessage = async (prompt: string) => {
  // 添加用户消息
  messages.value.push({
    type: 'user',
    content: prompt,
    createTime: new Date().toISOString(),
  })

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
    createTime: new Date().toISOString(),
    streamInfo: createStreamInfo(),
  })

  await nextTick()
  scrollToBottom()

  // 开始生成
  isGenerating.value = true
  await generateCode(prompt, aiMessageIndex)
}

// 发送消息
const sendMessage = async () => {
  if (!userInput.value.trim() || isGenerating.value) {
    return
  }

  let message = userInput.value.trim()
  // 将上传的图片 URL 拼接到消息中
  if (uploadedImages.value.length > 0) {
    const imageUrls = uploadedImages.value.map(img => img.url)
    message += `\n\n参考图片：\n${imageUrls.map(url => `- ${url}`).join('\n')}`
    uploadedImages.value = []
  }
  // 将上传的文件 URL 拼接到消息中
  if (uploadedFiles.value.length > 0) {
    const fileUrls = uploadedFiles.value.map(f => `- [${f.name}](${f.url})`)
    message += `\n\n参考文件：\n${fileUrls.join('\n')}`
    uploadedFiles.value = []
  }
  // 如果有选中的元素，将元素信息添加到提示词中
  if (selectedElementInfo.value) {
    let elementContext = `\n\n选中元素信息：`
    if (selectedElementInfo.value.pagePath) {
      elementContext += `\n- 页面路径: ${selectedElementInfo.value.pagePath}`
    }
    elementContext += `\n- 标签: ${selectedElementInfo.value.tagName.toLowerCase()}\n- 选择器: ${selectedElementInfo.value.selector}`
    if (selectedElementInfo.value.textContent) {
      elementContext += `\n- 当前内容: ${selectedElementInfo.value.textContent.substring(0, 100)}`
    }
    message += elementContext
  }
  userInput.value = ''
  // 添加用户消息（包含元素信息）
  messages.value.push({
    type: 'user',
    content: message,
    createTime: new Date().toISOString(),
  })

  // 发送消息后，清除选中元素并退出编辑模式
  if (selectedElementInfo.value) {
    clearSelectedElement()
    if (isEditMode.value) {
      toggleEditMode()
    }
  }

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
    createTime: new Date().toISOString(),
    streamInfo: createStreamInfo(),
  })

  await nextTick()
  scrollToBottom()

  // 开始生成
  isGenerating.value = true
  await generateCode(message, aiMessageIndex)
}

// 从工具参数中提取文件路径和内容
const extractFileFromArgs = (args: unknown): { filePath: string; content: string } => {
  if (!args) return { filePath: '', content: '' }
  try {
    const obj = typeof args === 'string' ? JSON.parse(args) : args
    return {
      filePath: obj?.relativePath || obj?.relativeFilePath || '',
      content: typeof obj?.content === 'string' ? obj.content : '',
    }
  } catch {
    return { filePath: '', content: '' }
  }
}

// 解析 SSE 消息，支持结构化 JSON（type 字段分发）和纯文本向后兼容
const parseSseMessage = (raw: string): { type: string; data: string; filePath?: string; fileContent?: string; url?: string } => {
  const text = raw?.trim()
  if (!text) return { type: 'unknown', data: '' }

  const backendTextMessage = parseBackendTextMessage(text)
  if (backendTextMessage) {
    return backendTextMessage
  }

  // 尝试解析为 JSON
  if (text.startsWith('{')) {
    try {
      const parsed = JSON.parse(text)

      // 结构化消息：带 type 字段
      if (parsed.type) {
        switch (parsed.type) {
          case 'ai_response':
            return { type: 'ai_response', data: parsed.data ?? '' }

          case 'tool_request': {
            const { filePath, content } = extractFileFromArgs(parsed.arguments)
            return { type: 'tool_request', data: text, filePath, fileContent: content }
          }

          case 'tool_executed': {
            const { filePath, content } = extractFileFromArgs(parsed.arguments)
            return { type: 'tool_executed', data: text, filePath, fileContent: content }
          }

          case 'dev_server':
            return { type: 'dev_server', data: '', url: parsed.url }

          default:
            return { type: parsed.type, data: parsed.data ?? text }
        }
      }

      // 非结构化 JSON（旧格式 {"d": "..."} 不应该到这里，但兜底）
      return { type: 'text', data: text }
    } catch {
      return { type: 'text', data: text }
    }
  }

  return { type: 'text', data: text }
}

// 生成代码 - 使用 EventSource 处理流式响应
const generateCode = async (userMessage: string, aiMessageIndex: number) => {
  let eventSource: EventSource | null = null
  let streamCompleted = false
  let renderTimer: number | undefined
  let statusTimer: number | undefined
  let generatingMessageTick = 0

  // 当前轮次的跟踪状态
  let aiText = ''
  const writtenFiles: string[] = []
  let pendingFile = ''
  let devServerUrl = ''
  let resourceCollectionStatus = ''

  // 重置实时代码展示
  liveCode.value = ''
  liveFiles.value = []
  activeLiveFile.value = ''
  fileContentMap.value = new Map()
  generatingVersion.value = undefined
  isVueGenMode.value = false
  liveAiText.value = ''

  // 计算预期的新版本号：后端创建版本 = 当前最新版本 + 1
  // 后端在收到 SSE 请求时才创建版本，这里提前算好避免时序问题
  const preGenVersion = appVersions.value[0]?.version
  const isModification = !!preGenVersion
  if (preGenVersion) {
    generatingVersion.value = preGenVersion + 1
  } else {
    generatingVersion.value = 1
  }

  // 标记是否已刷新过版本列表（收到第一条 SSE 消息后刷新一次以获取准确版本号）
  let versionListRefreshed = false

  try {
    const baseURL = request.defaults.baseURL || API_BASE_URL
    const params = new URLSearchParams({
      appId: appId.value || '',
      message: userMessage,
    })
    const url = `${baseURL}/app/chat/gen/code?${params}`

    eventSource = new EventSource(url, {
      withCredentials: true,
    })

    // 刷新 UI 内容
    const flushContent = () => {
      const msg = messages.value[aiMessageIndex]
      if (!msg) return

      // 更新 streamInfo（文件列表 + 进度阶段 + 任务步骤）
      const codeGenType = appInfo.value?.codeGenType
      msg.streamInfo = {
        totalChars: aiText.length,
        stage: getStreamStage(aiText.length, writtenFiles.length),
        currentAction: resourceCollectionStatus || (pendingFile
          ? `正在写入 ${pendingFile}`
          : writtenFiles.length > 0
            ? `已写入 ${writtenFiles.length} 个文件`
            : '正在生成代码'),
        fileNames: writtenFiles.slice(-4),
        updatedAt: new Date().toISOString(),
        tasks: buildGenerationTasks(
          codeGenType,
          resourceCollectionStatus,
          aiText.length,
          pendingFile,
          writtenFiles,
          devServerUrl,
          false,
          isModification,
        ),
      }

      // 实时代码：更新 liveCode 和 liveFiles 供中间面板显示
      liveAiText.value = aiText
      if (writtenFiles.length > 0) {
        // Vue 模式：文件列表来自 tool_executed，实际内容在 fileContentMap 中
        isVueGenMode.value = true
        liveFiles.value = writtenFiles
        // liveCode 仅作为 AI 状态文本（非文件内容），actual content 由 currentLiveFile 从 fileContentMap 获取
        liveCode.value = aiText
      } else if (aiText.length > 100) {
        // HTML/多文件模式：AI 文本流就是代码，创建虚拟文件
        liveCode.value = aiText
        const codeGenType = appInfo.value?.codeGenType
        if (codeGenType === 'multi_file') {
          liveFiles.value = ['index.html', 'style.css', 'script.js'].filter(f => extractCodeBlock(aiText, f))
          if (liveFiles.value.length === 0) liveFiles.value = ['index.html']
        } else {
          liveFiles.value = ['index.html']
        }
        // 按 activeLiveFile 提取对应代码块
        if (activeLiveFile.value && liveFiles.value.includes(activeLiveFile.value)) {
          liveCode.value = extractCodeBlock(aiText, activeLiveFile.value) || aiText
        }
      }

      const generationStatus = buildGenerationStatusMessage(
        aiText,
        pendingFile,
        writtenFiles,
        devServerUrl,
        generatingMessageTick,
      )
      msg.content = resourceCollectionStatus
        ? `⏳ ${resourceCollectionStatus}\n\n${generationStatus}`
        : generationStatus
      msg.loading = !streamCompleted
      scrollToBottom()
      renderTimer = undefined
    }

    const startStatusTicker = () => {
      flushContent()
      statusTimer = window.setInterval(() => {
        if (streamCompleted || !isGenerating.value) return
        generatingMessageTick += 1
        flushContent()
      }, 2200)
    }

    const stopStatusTicker = () => {
      if (statusTimer !== undefined) {
        window.clearInterval(statusTimer)
        statusTimer = undefined
      }
    }

    const scheduleFlush = () => {
      if (renderTimer !== undefined) return
      renderTimer = window.setTimeout(flushContent, 300)
    }

    startStatusTicker()

    const finishGeneration = () => {
      if (streamCompleted) return

      streamCompleted = true
      stopStatusTicker()
      if (renderTimer !== undefined) {
        window.clearTimeout(renderTimer)
        renderTimer = undefined
      }

      // 最终刷新一次内容
      const msg = messages.value[aiMessageIndex]
      if (msg) {
        msg.content = buildGenerationStatusMessage(aiText, '', writtenFiles, devServerUrl, generatingMessageTick, true)
        msg.loading = false
        if (msg.streamInfo) {
          msg.streamInfo.stage = generationSteps.length - 1
          msg.streamInfo.currentAction = '生成完成'
          msg.streamInfo.updatedAt = new Date().toISOString()
          msg.streamInfo.tasks = buildGenerationTasks(
            appInfo.value?.codeGenType,
            resourceCollectionStatus,
            aiText.length,
            '',
            writtenFiles,
            devServerUrl,
            true,
            isModification,
          )
        }
      }

      isGenerating.value = false
      isVueGenMode.value = false
      generatingVersion.value = undefined
      eventSource?.close()

      // 刷新应用信息和版本列表
      setTimeout(async () => {
        await fetchAppInfo()
        // 如果没收到 dev_server，用静态预览兜底
        if (!devServerUrl) {
          updatePreview()
        }
      }, 1000)
    }

    const handleBusinessError = (rawData: string) => {
      let errorMessage = rawData || '生成过程中出现错误'
      try {
        const errorData = JSON.parse(rawData)
        console.error('SSE业务错误事件:', errorData)
        errorMessage = errorData.message || errorData.data || errorMessage
      } catch (parseError) {
        console.error('解析SSE业务错误事件失败:', parseError, '原始数据:', rawData)
      }

      streamCompleted = true
      stopStatusTicker()
      if (renderTimer !== undefined) {
        window.clearTimeout(renderTimer)
        renderTimer = undefined
      }
      isGenerating.value = false
      isVueGenMode.value = false
      generatingVersion.value = undefined
      eventSource?.close()
      messages.value[aiMessageIndex].content = `❌ ${errorMessage}`
      messages.value[aiMessageIndex].loading = false
      message.error(errorMessage)
    }

    // ===== SSE 消息处理 =====
    eventSource.onmessage = function (event) {
      if (streamCompleted) return

      // 收到第一条消息后，后台刷新版本列表以获取准确版本号
      if (!versionListRefreshed) {
        versionListRefreshed = true
        loadAppVersions().then(() => {
          const actualVersion = appVersions.value[0]?.version
          if (actualVersion && actualVersion !== generatingVersion.value) {
            generatingVersion.value = actualVersion
            // 版本号更新后，重新拉取已写入文件的内容
            for (const f of writtenFiles) {
              if (!fileContentMap.value.has(f)) {
                fetchLiveFileContent(f)
              }
            }
          }
        }).catch(() => {})
      }

      // 流结束标记
      if (event.data?.trim() === 'done' || event.data?.trim() === '[DONE]') {
        finishGeneration()
        return
      }

      // 第一层：SSE 控制器包装 {"d": "..."}
      let chunk = event.data
      try {
        const wrapper = JSON.parse(event.data)
        if (wrapper.d !== undefined) {
          chunk = String(wrapper.d)
        }
      } catch {
        // event.data 本身就是内容
      }

      if (!chunk) return

      // 第二层：解析结构化消息
      const parsed = parseSseMessage(chunk)

      switch (parsed.type) {
        case 'resource_collection_progress':
          resourceCollectionStatus = parsed.data
          scheduleFlush()
          break

        case 'ai_response':
          // AI 文本回复 — 追加到文本
          resourceCollectionStatus = ''
          aiText += parsed.data
          scheduleFlush()
          break

        case 'tool_request':
          // AI 开始写文件
          resourceCollectionStatus = ''
          if (parsed.filePath) {
            pendingFile = parsed.filePath
          }
          // tool_request 也可能携带 content（取决于后端是否发送）
          if (parsed.filePath && parsed.fileContent) {
            fileContentMap.value.set(parsed.filePath, parsed.fileContent)
            fileContentMap.value = new Map(fileContentMap.value)
          }
          scheduleFlush()
          break

        case 'tool_executed': {
          // 文件写入完成
          resourceCollectionStatus = ''
          const filePath = parsed.filePath
          if (filePath) {
            if (!writtenFiles.includes(filePath)) {
              writtenFiles.push(filePath)
            }
            if (pendingFile === filePath) {
              pendingFile = ''
            }
            // 提取文件内容存入 map
            if (parsed.fileContent) {
              fileContentMap.value.set(filePath, parsed.fileContent)
              fileContentMap.value = new Map(fileContentMap.value)
            }
            // 工具事件携带的是模型请求参数；回读落盘文件才能展示服务端清洗后的真实代码。
            fileContentMap.value.delete(filePath)
            fileContentMap.value = new Map(fileContentMap.value)
            fetchLiveFileContent(filePath)
          }
          scheduleFlush()
          break
        }

        case 'dev_server':
          // 开发服务器已启动 — 立刻更新预览 iframe
          if (parsed.url) {
            devServerUrl = parsed.url
            previewUrl.value = parsed.url
            previewLoading.value = true
            previewReady.value = true
            usingDevServerPreview.value = true
            previewFrameKey.value += 1
            if (previewLoadTimer !== undefined) {
              window.clearTimeout(previewLoadTimer)
            }
            previewLoadTimer = window.setTimeout(() => {
              previewLoading.value = false
              previewLoadTimer = undefined
            }, 12000)
          }
          scheduleFlush()
          break

        default:
          // 纯文本（向后兼容：后端未结构化时，整个 chunk 当文本处理）
          aiText += chunk
          // 兼容旧版：从文本中正则提取文件名
          const legacyFile = extractGeneratedFileName(chunk)
          if (legacyFile && !writtenFiles.includes(legacyFile)) {
            writtenFiles.push(legacyFile)
            // Vue 模式：后端已将文件写入磁盘，从 API 拉取内容显示在代码面板
            fetchLiveFileContent(legacyFile)
          }
          scheduleFlush()
          break
      }
    }

    eventSource.addEventListener('done', function () {
      finishGeneration()
    })

    eventSource.addEventListener('business-error', function (event: MessageEvent) {
      if (streamCompleted) return
      handleBusinessError(event.data)
    })

    eventSource.onerror = function (event) {
      if (streamCompleted || !isGenerating.value) return

      const errorEvent = event as MessageEvent
      if (typeof errorEvent.data === 'string' && errorEvent.data) {
        handleBusinessError(errorEvent.data)
        return
      }

      if (devServerUrl) {
        finishGeneration()
        return
      }
      if (aiText || writtenFiles.length > 0) {
        const msg = messages.value[aiMessageIndex]
        if (msg) {
          msg.content = [
            buildGenerationStatusMessage(aiText, pendingFile, writtenFiles, devServerUrl, generatingMessageTick),
            '连接正在等待后端继续返回预览地址，请不要刷新页面。',
          ].join('\n\n')
          msg.loading = true
        }
        return
      }

      eventSource?.close()
      stopStatusTicker()
      handleError(new Error('SSE连接错误'), aiMessageIndex)
    }
  } catch (error) {
    if (statusTimer !== undefined) {
      window.clearInterval(statusTimer)
    }
    if (renderTimer !== undefined) {
      window.clearTimeout(renderTimer)
    }
    console.error('创建 EventSource 失败:', error)
    handleError(error, aiMessageIndex)
  }
}

// 错误处理函数
const handleError = (error: unknown, aiMessageIndex: number) => {
  console.error('生成代码失败：', error)
  messages.value[aiMessageIndex].content = '抱歉，生成过程中出现了错误，请重试。'
  messages.value[aiMessageIndex].loading = false
  message.error('生成失败，请重试')
  isGenerating.value = false
  isVueGenMode.value = false
  generatingVersion.value = undefined
}

// 更新预览
const updatePreview = () => {
  if (!appId.value) {
    previewReady.value = false
    return
  }
  const version = selectedVersion.value || latestVersion.value
  if (!version) {
    previewReady.value = false
    return
  }
  const codeGenType = selectedVersionRecord.value?.codeGenType || appInfo.value?.codeGenType || CodeGenTypeEnum.HTML
  previewUrl.value = getGeneratedPreviewUrl(appId.value, version, codeGenType)
  usingDevServerPreview.value = false
  previewReady.value = true
}

const loadPreview = () => {
  if (previewLoading.value) return
  if (!previewUrl.value) {
    updatePreview()
  }
  if (!previewUrl.value) {
    message.warning('暂无可加载的预览地址')
    return
  }
  previewLoading.value = true
  previewReady.value = false
  previewFrameKey.value += 1
  if (previewLoadTimer !== undefined) {
    window.clearTimeout(previewLoadTimer)
  }
  previewLoadTimer = window.setTimeout(() => {
    previewLoading.value = false
    previewLoadTimer = undefined
  }, 12000)
}

// 滚动到底部
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 下载代码
const downloadCode = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }
  if (!selectedVersion.value) {
    message.warning('请先选择一个代码版本')
    return
  }
  downloading.value = true
  try {
    const API_BASE_URL = request.defaults.baseURL || ''
    const url = `${API_BASE_URL}/app/download/${appId.value}?version=${selectedVersion.value}`
    const response = await fetch(url, {
      method: 'GET',
      credentials: 'include',
    })
    if (!response.ok) {
      throw new Error(`下载失败: ${response.status}`)
    }
    // 获取文件名
    const contentDisposition = response.headers.get('Content-Disposition')
    const fileName =
        contentDisposition?.match(/filename="(.+)"/)?.[1] ||
        `app-${appId.value}-v${selectedVersion.value}.zip`
    // 下载文件
    const blob = await response.blob()
    const downloadUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = fileName
    link.click()
    // 清理
    URL.revokeObjectURL(downloadUrl)
    message.success(`V${selectedVersion.value} 代码下载成功`)
  } catch (error) {
    console.error('下载失败：', error)
    message.error('下载失败，请重试')
  } finally {
    downloading.value = false
  }
}

// 部署应用
const deployApp = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }
  if (!selectedVersion.value) {
    message.warning('请先生成并选择一个代码版本')
    return
  }

  deploying.value = true
  try {
    const res = await deployAppApi({
      appId: appId.value,
      version: selectedVersion.value,
    })

    if (res.data.code === 0 && res.data.data) {
      deployUrl.value = res.data.data
      deployModalVisible.value = true
      message.success(`V${selectedVersion.value} 部署成功`)
      await fetchAppInfo()
    } else {
      message.error('部署失败：' + res.data.message)
    }
  } catch (error) {
    console.error('部署失败：', error)
    message.error('部署失败，请重试')
  } finally {
    deploying.value = false
  }
}

// 在新窗口打开预览
const openInNewTab = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

// 打开部署的网站
const openDeployedSite = () => {
  if (deployUrl.value) {
    window.open(deployUrl.value, '_blank')
  }
}

// iframe加载完成
const onIframeLoad = () => {
  previewReady.value = true
  previewLoading.value = false
  if (previewLoadTimer !== undefined) {
    window.clearTimeout(previewLoadTimer)
    previewLoadTimer = undefined
  }
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
  if (iframe) {
    visualEditor.init(iframe)
    visualEditor.onIframeLoad()
  }
}

// 编辑应用
const editApp = () => {
  if (appInfo.value?.id) {
    router.push(`/app/edit/${appInfo.value.id}`)
  }
}

// 删除应用
const deleteApp = async () => {
  if (!appInfo.value?.id || deleting.value) return

  deleting.value = true
  try {
    const deleteApi = isAdmin.value && !isOwner.value ? deleteAppByAdmin : deleteAppApi
    const res = await deleteApi({ id: appInfo.value.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      appDetailVisible.value = false
      router.push('/')
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    console.error('删除失败：', error)
    message.error('删除失败')
  } finally {
    deleting.value = false
  }
}

// 可视化编辑相关函数
const toggleEditMode = () => {
  // 检查 iframe 是否已经加载
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
  if (!iframe) {
    message.warning('请等待页面加载完成')
    return
  }
  // 确保 visualEditor 已初始化
  if (!previewReady.value) {
    message.warning('请等待页面加载完成')
    return
  }
  const newEditMode = visualEditor.toggleEditMode()
  isEditMode.value = newEditMode
}

const clearSelectedElement = () => {
  selectedElementInfo.value = null
  visualEditor.clearSelection()
}

const getInputPlaceholder = () => {
  if (selectedElementInfo.value) {
    return `正在编辑 ${selectedElementInfo.value.tagName.toLowerCase()} 元素，描述您想要的修改...`
  }
  const isModifyMode = appVersions.value.length > 0
  if (isModifyMode) {
    return '描述你想要的修改，比如「把首页标题改成XX」「添加一个轮播图」'
  }
  return '请描述你想生成的网站，越详细效果越好哦'
}

// 扩展项4：图片上传
const handleImageUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = target.files
  if (!files || files.length === 0) return

  imageUploading.value = true
  try {
    for (const file of Array.from(files)) {
      if (!file.type.startsWith('image/')) {
        message.warning(`${file.name} 不是图片文件`)
        continue
      }
      if (file.size > 5 * 1024 * 1024) {
        message.warning(`${file.name} 超过 5MB 限制`)
        continue
      }
      const formData = new FormData()
      formData.append('file', file)
      const response = await request.post('/file/upload-image', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 30000,
      })
      // 后端返回 BaseResponse { code, data, message }，data 字段是 URL 字符串
      const baseResp = response?.data
      const imageUrl = baseResp?.data
      if (imageUrl && typeof imageUrl === 'string') {
        uploadedImages.value.push({ url: imageUrl, name: file.name })
        message.success(`图片上传成功：${file.name}`)
      } else {
        message.error(`图片上传失败：${baseResp?.message || '返回数据异常'}`)
      }
    }
  } catch (error) {
    console.error('图片上传失败', error)
    message.error('图片上传失败')
  } finally {
    imageUploading.value = false
    if (target) target.value = ''
  }
}

const removeUploadedImage = (index: number) => {
  uploadedImages.value.splice(index, 1)
}

const removeUploadedFile = (index: number) => {
  uploadedFiles.value.splice(index, 1)
}

const triggerFileUploadInput = () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.multiple = true
  input.onchange = (e) => handleFileUpload(e)
  input.click()
}

const handleFileUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = target.files
  if (!files || files.length === 0) return

  fileUploading.value = true
  try {
    for (const file of Array.from(files)) {
      if (file.size > 10 * 1024 * 1024) {
        message.warning(`${file.name} 超过 10MB 限制`)
        continue
      }
      const formData = new FormData()
      formData.append('file', file)
      const response = await request.post('/file/upload-file', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 60000,
      })
      const baseResp = response?.data
      const fileUrl = baseResp?.data
      if (fileUrl && typeof fileUrl === 'string') {
        uploadedFiles.value.push({ url: fileUrl, name: file.name, size: file.size })
        message.success(`文件上传成功：${file.name}`)
      } else {
        message.error(`文件上传失败：${baseResp?.message || '返回数据异常'}`)
      }
    }
  } catch (error) {
    console.error('文件上传失败', error)
    message.error('文件上传失败')
  } finally {
    fileUploading.value = false
    if (target) target.value = ''
  }
}

const handlePaste = async (event: ClipboardEvent) => {
  const items = event.clipboardData?.items
  if (!items) return
  const files: File[] = []
  for (const item of Array.from(items)) {
    if (item.kind === 'file') {
      const file = item.getAsFile()
      if (file) files.push(file)
    }
  }
  if (files.length === 0) return
  event.preventDefault()
  for (const file of files) {
    if (file.type.startsWith('image/')) {
      // 图片走图片上传
      if (file.size > 5 * 1024 * 1024) {
        message.warning(`${file.name} 超过 5MB 限制`)
        continue
      }
      const formData = new FormData()
      formData.append('file', file)
      imageUploading.value = true
      try {
        const response = await request.post('/file/upload-image', formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
          timeout: 30000,
        })
        const baseResp = response?.data
        const imageUrl = baseResp?.data
        if (imageUrl && typeof imageUrl === 'string') {
          uploadedImages.value.push({ url: imageUrl, name: file.name })
          message.success(`图片上传成功：${file.name}`)
        }
      } catch {
        message.error(`图片上传失败：${file.name}`)
      } finally {
        imageUploading.value = false
      }
    } else {
      // 非图片走通用文件上传
      if (file.size > 10 * 1024 * 1024) {
        message.warning(`${file.name} 超过 10MB 限制`)
        continue
      }
      const formData = new FormData()
      formData.append('file', file)
      fileUploading.value = true
      try {
        const response = await request.post('/file/upload-file', formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
          timeout: 60000,
        })
        const baseResp = response?.data
        const fileUrl = baseResp?.data
        if (fileUrl && typeof fileUrl === 'string') {
          uploadedFiles.value.push({ url: fileUrl, name: file.name, size: file.size })
          message.success(`文件上传成功：${file.name}`)
        }
      } catch {
        message.error(`文件上传失败：${file.name}`)
      } finally {
        fileUploading.value = false
      }
    }
  }
}

const triggerFileUpload = () => {
  fileInputRef.value?.click()
}

// 扩展项5：内联编辑 —— 注入 contentEditable 到 iframe
const toggleInlineEdit = () => {
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
  if (!iframe) {
    message.warning('请等待页面加载完成')
    return
  }
  try {
    const doc = iframe.contentDocument
    if (!doc) return
    if (!inlineEditing.value) {
      doc.body.contentEditable = 'true'
      doc.body.style.outline = '2px dashed #0ea5e9'
      inlineEditing.value = true
      message.info('已开启内联编辑，直接在页面上修改文字内容')
    } else {
      doc.body.contentEditable = 'false'
      doc.body.style.outline = ''
      inlineEditing.value = false
    }
  } catch {
    message.error('无法编辑此页面，可能是跨域限制')
  }
}

// 扩展项5：保存内联编辑到后端
const saveInlineEdit = async () => {
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
  if (!iframe || !selectedVersion.value) {
    message.warning('没有可保存的版本')
    return
  }
  inlineSaving.value = true
  try {
    const doc = iframe.contentDocument
    if (!doc) throw new Error('无法访问页面内容')
    const html = '<!DOCTYPE html>\n' + doc.documentElement.outerHTML
    const baseURL = request.defaults.baseURL || API_BASE_URL
    await request.post(`${baseURL}/app/${appId.value}/version/${selectedVersion.value}/save-file`, {
      filePath: 'index.html',
      content: html,
    }, { timeout: 15000 })
    message.success('修改已保存')
    // 退出编辑模式
    doc.body.contentEditable = 'false'
    doc.body.style.outline = ''
    inlineEditing.value = false
    // 刷新预览
    previewFrameKey.value++
    loadPreview()
  } catch (error) {
    console.error('保存失败', error)
    message.error('保存失败，后端可能尚未实现此接口')
  } finally {
    inlineSaving.value = false
  }
}

// 页面加载时获取应用信息
onMounted(() => {
  fetchAppInfo()

  // 监听 iframe 消息
  window.addEventListener('message', (event) => {
    visualEditor.handleIframeMessage(event)
  })
})

// 清理资源
onUnmounted(() => {
  if (previewLoadTimer !== undefined) {
    window.clearTimeout(previewLoadTimer)
  }
  clearNameRefreshTimer()
  // EventSource 会在组件卸载时自动清理
})
</script>

<style scoped>
#appChatPage {
  height: calc(100vh - 64px);
  display: flex;
  flex-direction: column;
  padding: 16px 20px 20px;
  background: var(--bg-page);
}

/* ===== 顶部栏 ===== */
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 4px 14px;
  gap: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.app-symbol {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  background: var(--gradient-brand);
  box-shadow: var(--shadow-brand);
}

.app-name {
  margin: 0;
  min-width: 0;
  max-width: min(48vw, 620px);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--text-1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.code-gen-type-tag {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 500;
  color: var(--brand-600);
  background: rgba(14, 165, 233, 0.08);
  border: 1px solid rgba(14, 165, 233, 0.22);
  padding: 3px 10px;
  border-radius: var(--radius-full);
}

/* 扩展项1：创建/修改模式标签 */
.mode-tag {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: var(--radius-full);
  white-space: nowrap;
}

.mode-create {
  color: #15803d;
  background: rgba(34, 197, 94, 0.1);
  border: 1px solid rgba(34, 197, 94, 0.3);
}

.mode-modify {
  color: #b45309;
  background: rgba(245, 158, 11, 0.1);
  border: 1px solid rgba(245, 158, 11, 0.3);
}

.header-right {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.header-btn {
  color: var(--text-2);
}

.delete-app-btn {
  background: #fff;
}

.delete-app-btn:hover {
  background: #fff5f5;
}

.deploy-btn {
  background: var(--gradient-brand);
  border: none;
  font-weight: 600;
  box-shadow: var(--shadow-brand);
  transition:
    transform 0.2s var(--ease-out),
    box-shadow 0.2s var(--ease-out);
}

.deploy-btn:hover {
  background: var(--gradient-brand-hover);
  transform: translateY(-1px);
  box-shadow: 0 10px 28px rgba(14, 165, 233, 0.24);
}

/* ===== 主要内容区域 ===== */
.main-content {
  flex: 1;
  display: flex;
  gap: 12px;
  overflow: hidden;
  min-height: 0;
}

/* 生成中：隐藏预览面板，代码面板占 7 份 */
.main-content.generating .step-panel {
  flex: 3;
}
.main-content.generating .code-panel {
  display: flex !important;
  flex: 7;
}
.main-content.generating .preview-section {
  display: none !important;
}

/* 非生成中：隐藏代码面板，预览占 7 份 */
.main-content:not(.generating) .code-panel {
  display: none;
}
.main-content:not(.generating) .step-panel {
  flex: 3;
}
.main-content:not(.generating) .preview-section {
  flex: 7;
}

/* ===== 左侧对话区域 ===== */
.chat-section {
  flex: 2;
  display: flex;
  flex-direction: column;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  min-width: 0;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
  background: var(--surface-2);
}

.panel-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-2);
}

.chat-tools {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.chat-stat-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 28px;
  padding: 0 9px;
  border: 1px solid rgba(14, 165, 233, 0.2);
  border-radius: var(--radius-full);
  background: rgba(14, 165, 233, 0.07);
  color: var(--brand-600);
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.chat-overview {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
  padding: 10px 12px;
  border-bottom: 1px solid var(--border);
  background:
    linear-gradient(135deg, rgba(14, 165, 233, 0.08), rgba(34, 197, 94, 0.06)),
    var(--surface);
}

.chat-overview-item {
  min-width: 0;
  padding: 8px 10px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
}

.chat-overview-label {
  display: block;
  margin-bottom: 3px;
  color: var(--text-3);
  font-size: 11px;
  line-height: 1.2;
}

.chat-overview-item strong {
  display: block;
  min-width: 0;
  overflow: hidden;
  color: var(--text-1);
  font-size: 13px;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.messages-container {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  scroll-behavior: smooth;
  min-height: 0;
  background:
    linear-gradient(180deg, rgba(248, 250, 252, 0.8) 0%, rgba(255, 255, 255, 0) 120px),
    var(--surface);
}

.message-item {
  margin-bottom: 16px;
  animation: fade-up 0.3s var(--ease-out) both;
}

.user-message {
  display: flex;
  justify-content: flex-end;
  align-items: flex-start;
  gap: 10px;
}

.ai-message {
  display: flex;
  justify-content: flex-start;
  align-items: flex-start;
  gap: 10px;
}

.message-content {
  max-width: 78%;
  padding: 12px 16px;
  border-radius: 16px;
  line-height: 1.6;
  word-wrap: break-word;
  font-size: 14px;
}

.message-meta {
  margin-bottom: 6px;
  color: rgba(100, 116, 139, 0.86);
  font-size: 11px;
  line-height: 1.3;
}

.user-message .message-content {
  background: var(--gradient-brand);
  color: #fff;
  border-bottom-right-radius: 6px;
  box-shadow: 0 6px 18px rgba(14, 165, 233, 0.18);
}

.ai-message .message-content {
  background: var(--surface);
  border: 1px solid var(--border);
  color: var(--text-1);
  border-bottom-left-radius: 6px;
  padding: 10px 14px;
  box-shadow: var(--shadow-xs);
}

.user-message .message-meta {
  color: rgba(255, 255, 255, 0.78);
}

.generation-activity {
  margin-top: 10px;
  padding: 10px;
  border: 1px solid rgba(14, 165, 233, 0.18);
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.08), rgba(20, 184, 166, 0.06));
}

.generation-activity-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: var(--text-2);
  font-size: 12px;
}

.generation-activity-header span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.generation-activity-header strong {
  flex-shrink: 0;
  color: var(--brand-600);
  font-size: 12px;
}

.generation-steps {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
  margin-top: 10px;
}

.generation-step {
  min-width: 0;
  padding: 5px 6px;
  border-radius: 6px;
  background: rgba(148, 163, 184, 0.12);
  color: var(--text-3);
  font-size: 11px;
  line-height: 1.2;
  text-align: center;
  white-space: nowrap;
}

.generation-step.done {
  background: rgba(34, 197, 94, 0.12);
  color: #15803d;
}

.generation-step.active {
  background: rgba(14, 165, 233, 0.16);
  color: var(--brand-600);
  font-weight: 700;
}

.generation-files {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 10px;
}

.generation-files span {
  max-width: 160px;
  overflow: hidden;
  padding: 4px 7px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.78);
  color: var(--text-2);
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 11px;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.generation-tasks {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
  padding: 12px;
  border-radius: 10px;
  background: rgba(248, 250, 252, 0.8);
  border: 1px solid rgba(148, 163, 184, 0.14);
}

.generation-task {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid transparent;
  transition: all 0.2s var(--ease-out);
}

.generation-task.pending {
  color: var(--text-3);
  opacity: 0.72;
}

.generation-task.active {
  border-color: rgba(14, 165, 233, 0.22);
  background: rgba(14, 165, 233, 0.06);
  color: var(--text-1);
}

.generation-task.completed {
  color: var(--text-2);
}

.generation-task .task-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  font-size: 14px;
  margin-top: 1px;
}

.generation-task.pending .task-icon {
  color: var(--text-3);
}

.generation-task.active .task-icon {
  color: var(--brand-600);
}

.generation-task.completed .task-icon {
  color: #16a34a;
}

.generation-task .task-body {
  min-width: 0;
  flex: 1;
}

.generation-task .task-label {
  font-size: 13px;
  font-weight: 600;
  line-height: 1.4;
}

.generation-task .task-desc {
  font-size: 12px;
  line-height: 1.4;
  margin-top: 2px;
  opacity: 0.86;
}

.message-avatar {
  flex-shrink: 0;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text-3);
  font-size: 13px;
  padding: 2px 0;
}

/* 打字指示动画 */
.typing-dots {
  display: inline-flex;
  gap: 4px;
}

.typing-dots i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--brand-500);
  animation: typing-bounce 1.2s ease-in-out infinite;
}

.typing-dots i:nth-child(2) {
  animation-delay: 0.15s;
}

.typing-dots i:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes typing-bounce {
  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.5;
  }
  30% {
    transform: translateY(-4px);
    opacity: 1;
  }
}

/* 加载更多按钮 */
.load-more-container {
  text-align: center;
  padding: 8px 0;
  margin-bottom: 16px;
}

/* ===== 输入区域 ===== */
.input-container {
  padding: 12px 16px 16px;
  border-top: 1px solid var(--border);
  background: var(--surface);
}

.input-wrapper {
  position: relative;
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-md);
  background: var(--surface);
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}

.input-wrapper:focus-within {
  border-color: var(--brand-500);
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.12);
}

.input-wrapper :deep(.ant-input) {
  border: none !important;
  box-shadow: none !important;
  background: transparent;
  padding: 12px 100px 12px 14px;
  resize: none;
}

.input-actions {
  position: absolute;
  bottom: 10px;
  right: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 扩展项4：图片上传按钮 */
.upload-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid var(--border);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-2);
  font-size: 16px;
  background: var(--surface);
  transition: all 0.2s;
}

.upload-btn:hover:not(:disabled) {
  color: var(--brand-600);
  border-color: var(--brand-500);
  transform: translateY(-1px);
}

.upload-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

/* 扩展项4：已上传图片预览区 */
.uploaded-images-preview {
  display: flex;
  gap: 8px;
  padding: 8px 14px 0;
  flex-shrink: 0;
  overflow-x: auto;
}

.uploaded-image-item {
  position: relative;
  width: 60px;
  height: 60px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--border);
  flex-shrink: 0;
}

.uploaded-image-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.uploaded-image-remove {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 12px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.uploaded-image-remove:hover {
  background: rgba(220, 38, 38, 0.85);
}

.uploaded-files-preview {
  display: flex;
  gap: 8px;
  padding: 8px 14px 0;
  flex-shrink: 0;
  overflow-x: auto;
}

.uploaded-file-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--bg-1);
  flex-shrink: 0;
  font-size: 13px;
}

.uploaded-file-item .file-icon {
  font-size: 16px;
}

.uploaded-file-item .file-name {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.uploaded-file-remove {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.1);
  color: var(--text-2);
  font-size: 12px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.uploaded-file-remove:hover {
  background: rgba(220, 38, 38, 0.85);
  color: #fff;
}

.send-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
  background: var(--gradient-brand);
  box-shadow: 0 4px 14px rgba(14, 165, 233, 0.22);
  transition:
    transform 0.2s var(--ease-out),
    box-shadow 0.2s var(--ease-out),
    opacity 0.2s;
}

.send-btn:hover:not(:disabled) {
  background: var(--gradient-brand-hover);
  transform: translateY(-1px) scale(1.05);
}

.send-btn:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.send-btn-spin :deep(.ant-spin-dot-item) {
  background-color: #fff;
}

/* ===== 代码版本区域 ===== */
.version-section {
  flex: 1.2;
  display: flex;
  flex-direction: column;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  min-width: 260px;
}

.version-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
  overflow-y: auto;
  min-height: 0;
}

.version-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.version-item {
  width: 100%;
  text-align: left;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  background: var(--surface);
  color: var(--text-1);
  cursor: pointer;
  transition:
    border-color 0.2s,
    box-shadow 0.2s,
    transform 0.2s;
}

.version-item:hover {
  border-color: rgba(14, 165, 233, 0.32);
  box-shadow: var(--shadow-xs);
  transform: translateY(-1px);
}

.version-item.active {
  border-color: var(--brand-500);
  background: rgba(14, 165, 233, 0.06);
}

.version-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
}

.version-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-1);
}

.version-meta,
.version-time {
  font-size: 12px;
  color: var(--text-3);
}

.version-message {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.5;
  color: var(--text-2);
  word-break: break-word;
}

.source-modal-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--surface-2);
}

.source-modal-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.source-version-label {
  color: var(--text-2);
  font-size: 13px;
  font-weight: 600;
}

.source-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 220px;
}

.source-explorer {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  min-height: 420px;
  max-height: 62vh;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: #ffffff;
}

.source-file-panel {
  min-width: 0;
  overflow: auto;
  border-right: 1px solid var(--border);
  background: var(--surface-2);
}

.source-file-panel-title {
  position: sticky;
  top: 0;
  z-index: 1;
  padding: 10px 12px;
  border-bottom: 1px solid var(--border);
  background: var(--surface-2);
  color: var(--text-2);
  font-size: 12px;
  font-weight: 700;
}

.source-file-item {
  display: flex;
  align-items: center;
  width: 100%;
  min-height: 36px;
  padding: 8px 12px;
  border: 0;
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
  background: transparent;
  color: var(--text-2);
  font-size: 12px;
  line-height: 1.4;
  text-align: left;
  cursor: pointer;
}

.source-file-item:hover {
  background: rgba(22, 119, 255, 0.08);
  color: var(--primary);
}

.source-file-item.active {
  background: #ffffff;
  color: var(--primary);
  font-weight: 700;
}

.source-file-icon {
  flex: 0 0 auto;
  margin-right: 8px;
  font-size: 14px;
}

.source-file-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.source-code-panel {
  display: flex;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
}

.source-code-header {
  display: flex;
  align-items: center;
  min-height: 38px;
  padding: 0 14px;
  border-bottom: 1px solid var(--border);
  background: #ffffff;
}

.source-code-file {
  min-width: 0;
  overflow: hidden;
  color: var(--text-1);
  font-size: 13px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.source-code-view {
  flex: 1;
  min-height: 0;
  margin: 0;
  padding: 14px;
  overflow: auto;
  background: #ffffff;
  color: #1f2937;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre;
}

.source-code-view code {
  color: inherit;
}

.memory-panel,
.collaboration-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.memory-summary-card {
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--surface);
  overflow: hidden;
}

.memory-summary-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
  border-bottom: 1px solid var(--border);
  background: var(--surface-2);
}

.memory-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-1);
}

.memory-subtitle {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--text-3);
}

.memory-summary-content {
  min-height: 110px;
  padding: 14px;
  color: var(--text-2);
  line-height: 1.7;
  white-space: pre-wrap;
}

.memory-actions,
.collaboration-invite {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.collaboration-invite {
  justify-content: stretch;
}

.collaboration-invite .ant-input {
  flex: 1;
}

.collaborator-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.collaborator-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--surface);
}

.collaborator-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.collaborator-main strong,
.collaborator-main span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.collaborator-main span {
  color: var(--text-3);
  font-size: 12px;
}

/* ===== 右侧预览区域 ===== */
.preview-section {
  flex: 7;
  display: flex;
  flex-direction: column;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  min-width: 0;
}

.preview-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  border-bottom: 1px solid var(--border);
  background: var(--surface-2);
}

.version-select {
  width: 160px;
  margin-left: auto;
}

/* 浏览器窗口三色圆点 */
.browser-dots {
  display: inline-flex;
  gap: 6px;
  flex-shrink: 0;
}

.browser-dots span {
  width: 11px;
  height: 11px;
  border-radius: 50%;
}

.browser-dots span:nth-child(1) {
  background: #ff5f57;
}

.browser-dots span:nth-child(2) {
  background: #febc2e;
}

.browser-dots span:nth-child(3) {
  background: #28c840;
}

.preview-title {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-2);
  flex: 1;
}

.preview-actions {
  display: flex;
  gap: 4px;
}

.preview-action-btn {
  color: var(--text-2);
  font-size: 13px;
}

.preview-action-btn.edit-mode-active {
  color: #16a34a;
  background: rgba(34, 197, 94, 0.1);
}

.preview-content {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: #fff;
}

.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-3);
  background:
    radial-gradient(circle at 30% 20%, rgba(14, 165, 233, 0.05), transparent 55%),
    radial-gradient(circle at 70% 80%, rgba(20, 184, 166, 0.05), transparent 55%);
}

.placeholder-icon {
  font-size: 52px;
  margin-bottom: 16px;
  opacity: 0.9;
}

.placeholder-title {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-2);
}

.placeholder-desc {
  margin: 0;
  font-size: 13px;
  color: var(--text-3);
}

.preview-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-2);
}

.preview-loading p {
  margin-top: 16px;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.preview-loading-overlay {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--text-2);
  font-size: 13px;
  backdrop-filter: blur(3px);
}

/* 选中元素提示 */
.selected-element-alert {
  margin: 0 16px 8px;
  border-radius: var(--radius-md);
}

.selected-element-info {
  line-height: 1.5;
}

.element-header {
  margin-bottom: 6px;
}

.element-tag {
  font-family: var(--font-mono);
  font-size: 13px;
  font-weight: 600;
  color: var(--brand-600);
}

.element-id {
  color: #16a34a;
  margin-left: 4px;
}

.element-class {
  color: #d97706;
  margin-left: 4px;
}

.element-item {
  margin-bottom: 3px;
  font-size: 13px;
  color: var(--text-2);
}

.element-selector-code {
  font-family: var(--font-mono);
  background: var(--surface-2);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  color: #dc2626;
  border: 1px solid var(--border);
}

/* ===== 响应式 ===== */
@media (max-width: 1024px) {
  #appChatPage {
    height: auto;
  }

  .main-content {
    flex-direction: column;
  }

  .chat-section,
  .version-section,
  .preview-section {
    flex: none;
    height: 60vh;
  }

  .version-section {
    min-width: 0;
    height: auto;
    max-height: 420px;
  }
}

@media (max-width: 768px) {
  #appChatPage {
    padding: 12px;
  }

  .header-bar {
    flex-wrap: wrap;
  }

  .app-name {
    font-size: 15px;
  }

  .message-content {
    max-width: 88%;
  }

  .chat-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .generation-steps {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .source-modal-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .source-modal-actions {
    justify-content: flex-start;
  }

  .source-explorer {
    grid-template-columns: 1fr;
    grid-template-rows: auto minmax(320px, 1fr);
    max-height: 70vh;
  }

  .source-file-panel {
    display: flex;
    max-height: 132px;
    border-right: 0;
    border-bottom: 1px solid var(--border);
  }

  .source-file-panel-title {
    display: none;
  }

  .source-file-item {
    flex: 0 0 180px;
    border-right: 1px solid rgba(148, 163, 184, 0.16);
    border-bottom: 0;
  }
}

/* ===== 左侧步骤面板 ===== */
.step-panel {
  flex: 3;
  display: flex;
  flex-direction: column;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  min-width: 0;
}

/* ===== 中间代码面板 ===== */
.code-panel {
  flex: 7;
  display: flex;
  flex-direction: column;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  min-width: 0;
}

/* ===== 步骤进度区域 ===== */
.step-progress-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.step-current-action {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 16px;
  border-bottom: 1px solid var(--border);
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.08), rgba(20, 184, 166, 0.06));
  color: var(--text-2);
  font-size: 12px;
}

.step-current-action span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.step-current-action strong {
  flex-shrink: 0;
  color: var(--brand-600);
  font-size: 12px;
}

.generation-steps-vertical {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 12px 16px;
  overflow-y: auto;
}

.generation-step-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px 8px 12px;
  border-left: 3px solid rgba(148, 163, 184, 0.2);
  border-radius: 0 6px 6px 0;
  color: var(--text-3);
  font-size: 13px;
  transition:
    border-color 0.2s,
    background 0.2s,
    color 0.2s;
}

.generation-step-item.done {
  border-left-color: #22c55e;
  color: #15803d;
}

.generation-step-item.active {
  border-left-color: var(--brand-500);
  background: rgba(14, 165, 233, 0.06);
  color: var(--brand-600);
  font-weight: 600;
}

.step-index {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  background: rgba(148, 163, 184, 0.15);
  color: var(--text-3);
}

.generation-step-item.done .step-index {
  background: rgba(34, 197, 94, 0.16);
  color: #15803d;
}

.generation-step-item.active .step-index {
  background: rgba(14, 165, 233, 0.18);
  color: var(--brand-600);
}

.step-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.step-waiting {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  color: var(--text-3);
  font-size: 13px;
}

/* 生成中：紧凑进度条 */
.gen-progress-bar {
  flex-shrink: 0;
  padding: 8px 16px;
  background: linear-gradient(90deg, rgba(14, 165, 233, 0.06), rgba(14, 165, 233, 0.02));
  border-bottom: 1px solid var(--border);
}

.gen-bar-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-2);
}

.gen-bar-action {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gen-bar-size {
  flex-shrink: 0;
  color: var(--brand-600);
  font-size: 11px;
}

/* AI 实时输出日志 */
.ai-output-log {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border-top: 1px solid var(--border);
  overflow: hidden;
}

.ai-output-header {
  padding: 8px 16px;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--text-3);
  background: var(--surface-2);
}

.ai-output-text {
  flex: 1;
  min-height: 0;
  margin: 0;
  padding: 10px 16px;
  overflow-y: auto;
  color: var(--text-2);
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 11.5px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

/* ===== 精简对话容器 ===== */
.chat-mini {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

/* ===== 文件浏览器（生成中中间面板） ===== */
.file-explorer {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.file-explorer-body {
  flex: 1;
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr);
  overflow: hidden;
  min-height: 0;
}

.file-tree {
  min-width: 0;
  overflow-y: auto;
  border-right: 1px solid var(--border);
  background: var(--surface-2);
}

.file-tree-item {
  display: flex;
  align-items: center;
  width: 100%;
  min-height: 34px;
  padding: 8px 12px;
  border: 0;
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
  background: transparent;
  color: var(--text-2);
  font-size: 12px;
  line-height: 1.4;
  text-align: left;
  cursor: pointer;
  transition:
    background 0.2s,
    color 0.2s;
}

.file-tree-item:hover {
  background: rgba(14, 165, 233, 0.08);
  color: var(--brand-600);
}

.file-tree-item.active {
  background: var(--surface);
  color: var(--brand-600);
  font-weight: 700;
}

.file-tree-icon {
  flex: 0 0 auto;
  margin-right: 8px;
  font-size: 14px;
}

.file-tree-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-tree-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px 12px;
  color: var(--text-3);
  font-size: 12px;
}

.code-viewer {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.code-viewer-header {
  display: flex;
  align-items: center;
  min-height: 38px;
  padding: 0 14px;
  border-bottom: 1px solid var(--border);
  background: var(--surface-2);
}

.code-viewer-file {
  min-width: 0;
  overflow: hidden;
  color: var(--text-1);
  font-size: 13px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.code-viewer-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-3);
  font-size: 13px;
}

.code-viewer-placeholder p {
  margin: 0;
}

.code-viewer-generating {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--text-3);
  font-size: 13px;
}

.code-viewer-generating p {
  margin: 0;
}

.code-viewer-content {
  flex: 1;
  min-height: 0;
  margin: 0;
  padding: 14px;
  overflow: auto;
  background: #ffffff;
  color: #24292e;
  font-family: 'Fira Code', Consolas, Monaco, 'Courier New', monospace;
  font-size: 12.5px;
  line-height: 1.65;
  white-space: pre;
  tab-size: 2;
}

.code-viewer-content code {
  color: inherit;
  background: none;
  padding: 0;
  text-shadow: none;
}

.file-explorer-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--text-3);
  font-size: 13px;
}

/* ===== 新增面板响应式 ===== */
@media (max-width: 1024px) {
  .step-panel,
  .code-panel {
    flex: none;
    height: 60vh;
  }

  .code-panel {
    min-width: 0;
    height: auto;
    max-height: 420px;
  }

  .file-explorer-body {
    grid-template-columns: 1fr;
    grid-template-rows: auto minmax(320px, 1fr);
  }

  .file-tree {
    display: flex;
    max-height: 132px;
    border-right: 0;
    border-bottom: 1px solid var(--border);
    overflow-x: auto;
  }

  .file-tree-item {
    flex: 0 0 180px;
    border-right: 1px solid rgba(148, 163, 184, 0.16);
    border-bottom: 0;
  }
}
</style>
