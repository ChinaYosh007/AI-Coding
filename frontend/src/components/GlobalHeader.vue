<template>
  <header class="site-header">
    <div class="header-inner">
      <!-- 左侧：Logo 和品牌 -->
      <RouterLink to="/" class="brand">
        <img class="brand-logo" src="@/assets/logo.png" alt="Logo" />
        <span class="brand-name">AI Coding</span>
        <span class="brand-badge">Beta</span>
      </RouterLink>

      <!-- 中间：导航菜单 -->
      <nav class="header-nav">
        <a-menu
          v-model:selectedKeys="selectedKeys"
          mode="horizontal"
          :items="menuItems"
          @click="handleMenuClick"
        />
      </nav>

      <!-- 右侧：用户操作区域 -->
      <div class="header-actions">
        <template v-if="loginUserStore.loginUser.id">
          <a-dropdown placement="bottomRight" :arrow="false">
            <button class="user-chip" type="button" :title="displayUserTitle">
              <a-avatar :size="30" :src="loginUserStore.loginUser.userAvatar">
                {{ displayUserName.charAt(0).toUpperCase() }}
              </a-avatar>
              <span class="user-chip-name">{{ displayUserName }}</span>
              <DownOutlined class="user-chip-arrow" />
            </button>
            <template #overlay>
              <a-menu class="user-menu">
                <div class="user-menu-profile">
                  <div class="user-menu-name">{{ displayUserName }}</div>
                  <div v-if="loginUserStore.loginUser.userAccount" class="user-menu-account">
                    {{ loginUserStore.loginUser.userAccount }}
                  </div>
                  <a-tag class="user-menu-role" color="blue">{{ displayUserRole }}</a-tag>
                </div>
                <a-menu-item key="profile" @click="openProfileModal">
                  <UserOutlined />
                  编辑资料
                </a-menu-item>
                <a-menu-item key="logout" @click="doLogout">
                  <LogoutOutlined />
                  退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </template>
        <template v-else>
          <a-button type="text" class="btn-ghost" @click="router.push('/user/register')">
            注册
          </a-button>
          <a-button type="primary" class="btn-cta" @click="router.push('/user/login')">
            登录
          </a-button>
        </template>
      </div>
    </div>
  </header>

  <a-modal
    v-model:open="profileModalVisible"
    title="编辑个人资料"
    ok-text="保存"
    cancel-text="取消"
    :confirm-loading="profileSaving"
    @ok="saveProfile"
  >
    <a-form layout="vertical" class="profile-form">
      <a-form-item label="账号" required>
        <a-input v-model:value="profileForm.userAccount" placeholder="请输入账号" :maxlength="30" />
      </a-form-item>
      <a-form-item label="权限">
        <a-input :value="displayUserRole" disabled />
      </a-form-item>
      <a-form-item label="昵称" required>
        <a-input v-model:value="profileForm.userName" placeholder="请输入昵称" :maxlength="30" />
      </a-form-item>
      <a-form-item label="新密码">
        <a-input-password
          v-model:value="profileForm.userPassword"
          placeholder="不修改密码请留空，至少 8 位"
          autocomplete="new-password"
        />
      </a-form-item>
      <a-form-item label="确认新密码">
        <a-input-password
          v-model:value="profileForm.checkPassword"
          placeholder="再次输入新密码"
          autocomplete="new-password"
        />
      </a-form-item>
      <a-form-item label="头像">
        <a-upload
          :show-upload-list="false"
          :before-upload="handleAvatarBeforeUpload"
          :custom-request="handleAvatarUpload"
          accept="image/*"
        >
          <div class="avatar-upload-trigger">
            <a-spin :spinning="avatarUploading">
              <a-avatar :size="48" :src="profileForm.userAvatar">
                {{ profileForm.userName?.charAt(0)?.toUpperCase() || 'U' }}
              </a-avatar>
            </a-spin>
            <span class="avatar-upload-hint">点击上传头像</span>
          </div>
        </a-upload>
      </a-form-item>
      <a-form-item label="个人简介">
        <a-textarea
          v-model:value="profileForm.userProfile"
          placeholder="写一点个人介绍"
          :rows="3"
          :maxlength="120"
          show-count
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, h, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { type MenuProps, message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { updateMyUser, userLogout } from '@/api/userController.ts'
import request from '@/request'
import {
  LogoutOutlined,
  HomeOutlined,
  TeamOutlined,
  AppstoreOutlined,
  MessageOutlined,
  DownOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'

const loginUserStore = useLoginUserStore()
const router = useRouter()
const profileModalVisible = ref(false)
const profileSaving = ref(false)
const profileForm = reactive<API.UserUpdateMyRequest & { checkPassword?: string }>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
  userName: '',
  userAvatar: '',
  userProfile: '',
})

const displayUserName = computed(() => {
  return loginUserStore.loginUser.userName || loginUserStore.loginUser.userAccount || '用户'
})

const displayUserRole = computed(() => {
  return loginUserStore.loginUser.userRole === 'admin' ? '管理员' : '普通用户'
})

const displayUserTitle = computed(() => {
  const account = loginUserStore.loginUser.userAccount
  return account ? `${displayUserName.value}（${account}）` : displayUserName.value
})

const avatarUploading = ref(false)

const handleAvatarBeforeUpload = (file: File) => {
  if (!file.type.startsWith('image/')) {
    message.error('请上传图片文件')
    return false
  }
  if (file.size / 1024 / 1024 > 5) {
    message.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

const handleAvatarUpload = async (options: { file: File; onSuccess?: Function; onError?: Function }) => {
  const { file, onSuccess, onError } = options
  avatarUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await request.post('/file/upload-image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 30000,
    })
    if (res.data.code === 0) {
      profileForm.userAvatar = res.data.data
      message.success('头像上传成功')
      onSuccess?.(res.data.data)
    } else {
      message.error('上传失败，' + res.data.message)
      onError?.(res.data.message)
    }
  } catch (error) {
    console.error('头像上传失败：', error)
    message.error('上传失败，请重试')
    onError?.(error)
  } finally {
    avatarUploading.value = false
  }
}

const openProfileModal = () => {
  profileForm.userAccount = loginUserStore.loginUser.userAccount || ''
  profileForm.userPassword = ''
  profileForm.checkPassword = ''
  profileForm.userName = loginUserStore.loginUser.userName || ''
  profileForm.userAvatar = loginUserStore.loginUser.userAvatar || ''
  profileForm.userProfile = loginUserStore.loginUser.userProfile || ''
  profileModalVisible.value = true
}

const saveProfile = async () => {
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    return
  }
  if (!profileForm.userAccount?.trim()) {
    message.warning('账号不能为空')
    return
  }
  if (!profileForm.userName?.trim()) {
    message.warning('昵称不能为空')
    return
  }
  if (profileForm.userPassword && profileForm.userPassword.length < 8) {
    message.warning('新密码不能小于 8 位')
    return
  }
  if (profileForm.userPassword !== profileForm.checkPassword) {
    message.warning('两次输入的新密码不一致')
    return
  }

  profileSaving.value = true
  try {
    const payload: API.UserUpdateMyRequest = {
      userAccount: profileForm.userAccount.trim(),
      userName: profileForm.userName.trim(),
      userAvatar: profileForm.userAvatar?.trim(),
      userProfile: profileForm.userProfile?.trim(),
    }
    if (profileForm.userPassword) {
      payload.userPassword = profileForm.userPassword
    }
    const res = await updateMyUser(payload)
    if (res.data.code === 0) {
      await loginUserStore.fetchLoginUser()
      profileModalVisible.value = false
      message.success('资料已更新')
    } else {
      message.error('保存失败，' + res.data.message)
    }
  } catch (error) {
    console.error('保存用户资料失败：', error)
    message.error('保存失败，请重试')
  } finally {
    profileSaving.value = false
  }
}

// 当前选中菜单
const selectedKeys = ref<string[]>(['/'])
// 监听路由变化，更新当前选中菜单
router.afterEach((to) => {
  selectedKeys.value = [to.path]
})

// 菜单配置项
const originItems = [
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: '/admin/userManage',
    icon: () => h(TeamOutlined),
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/admin/appManage',
    icon: () => h(AppstoreOutlined),
    label: '应用管理',
    title: '应用管理',
  },
  {
    key: '/admin/chatManage',
    icon: () => h(MessageOutlined),
    label: '对话管理',
    title: '对话管理',
  },
]

// 过滤菜单项
const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    const menuKey = menu?.key as string
    if (menuKey?.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}

// 展示在菜单的路由数组
const menuItems = computed<MenuProps['items']>(() => filterMenus(originItems))

// 处理菜单点击
const handleMenuClick: MenuProps['onClick'] = (e) => {
  const key = e.key as string
  selectedKeys.value = [key]
  // 跳转到对应页面
  if (key.startsWith('/')) {
    router.push(key)
  }
}

// 退出登录
const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
.site-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(16px) saturate(1.6);
  -webkit-backdrop-filter: blur(16px) saturate(1.6);
  border-bottom: 1px solid var(--border);
}

.header-inner {
  max-width: 1400px;
  margin: 0 auto;
  height: 64px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 24px;
}

/* 品牌区 */
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  text-decoration: none;
}

.brand-logo {
  height: 34px;
  width: 34px;
  border-radius: 10px;
  box-shadow: var(--shadow-xs);
  transition: transform 0.3s var(--ease-out);
}

.brand:hover .brand-logo {
  transform: rotate(-8deg) scale(1.06);
}

.brand-name {
  font-size: 19px;
  font-weight: 800;
  letter-spacing: -0.02em;
  background: var(--gradient-text);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  white-space: nowrap;
}

.brand-badge {
  font-size: 10px;
  font-weight: 600;
  line-height: 1;
  color: var(--brand-600);
  background: rgba(14, 165, 233, 0.1);
  border: 1px solid rgba(14, 165, 233, 0.24);
  padding: 3px 7px;
  border-radius: var(--radius-full);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

/* 导航 */
.header-nav {
  flex: 1;
  min-width: 0;
}

.header-nav :deep(.ant-menu-horizontal) {
  background: transparent;
  border-bottom: none !important;
  line-height: 62px;
  font-weight: 500;
}

.header-nav :deep(.ant-menu-horizontal > .ant-menu-item::after) {
  border-bottom-width: 3px !important;
  border-radius: 3px;
}

/* 用户操作区 */
.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.btn-ghost {
  color: var(--text-2);
  font-weight: 500;
}

.btn-cta {
  background: var(--gradient-brand);
  border: none;
  box-shadow: var(--shadow-brand);
  font-weight: 600;
  padding-inline: 20px;
  transition:
    transform 0.2s var(--ease-out),
    box-shadow 0.2s var(--ease-out);
}

.btn-cta:hover {
  background: var(--gradient-brand-hover);
  transform: translateY(-1px);
  box-shadow: 0 10px 28px rgba(14, 165, 233, 0.24);
}

/* 用户胶囊 */
.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 12px 5px 6px;
  min-width: 190px;
  max-width: 280px;
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  background: var(--surface);
  cursor: pointer;
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}

.user-chip:hover {
  border-color: rgba(14, 165, 233, 0.36);
  box-shadow: 0 4px 14px rgba(14, 165, 233, 0.12);
}

.user-chip-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-1);
  flex: 1;
  min-width: 0;
  max-width: 190px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: left;
}

.user-chip-arrow {
  font-size: 10px;
  color: var(--text-3);
}

.user-menu {
  min-width: 240px;
}

.user-menu-profile {
  padding: 12px 14px 10px;
  border-bottom: 1px solid var(--border);
}

.user-menu-name {
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
  color: var(--text-1);
  word-break: break-word;
}

.user-menu-account {
  margin-top: 3px;
  font-size: 12px;
  color: var(--text-3);
  word-break: break-all;
}

.user-menu-role {
  margin-top: 8px;
}

.profile-form :deep(.ant-form-item) {
  margin-bottom: 16px;
}

.avatar-upload-trigger {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px;
  border: 1px dashed var(--border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: border-color 0.2s;
}

.avatar-upload-trigger:hover {
  border-color: rgba(14, 165, 233, 0.48);
}

.avatar-upload-hint {
  font-size: 13px;
  color: var(--text-3);
}

@media (max-width: 768px) {
  .header-inner {
    padding: 0 16px;
    gap: 12px;
  }

  .brand-name,
  .brand-badge {
    display: none;
  }

  .user-chip-name {
    display: none;
  }

  .user-chip {
    min-width: 0;
    padding-right: 8px;
  }
}
</style>
