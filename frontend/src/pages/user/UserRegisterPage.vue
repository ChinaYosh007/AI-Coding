<template>
  <div id="userRegisterPage">
    <!-- 氛围背景 -->
    <div class="auth-ambient" aria-hidden="true">
      <div class="auth-orb auth-orb--left"></div>
      <div class="auth-orb auth-orb--right"></div>
    </div>

    <div class="auth-card">
      <div class="auth-brand">
        <img src="@/assets/logo.png" alt="Logo" class="auth-logo" />
      </div>
      <h2 class="title">创建账号</h2>
      <div class="desc">不写一行代码，生成完整应用</div>
      <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
        <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
          <a-input v-model:value="formState.userAccount" placeholder="请输入账号" size="large">
            <template #prefix>
              <UserOutlined class="input-icon" />
            </template>
          </a-input>
        </a-form-item>
        <a-form-item
          name="userPassword"
          :rules="[
            { required: true, message: '请输入密码' },
            { min: 8, message: '密码不能小于 8 位' },
          ]"
        >
          <a-input-password
            v-model:value="formState.userPassword"
            placeholder="请输入密码（至少 8 位）"
            size="large"
          >
            <template #prefix>
              <LockOutlined class="input-icon" />
            </template>
          </a-input-password>
        </a-form-item>
        <a-form-item
          name="checkPassword"
          :rules="[
            { required: true, message: '请确认密码' },
            { min: 8, message: '密码不能小于 8 位' },
            { validator: validateCheckPassword },
          ]"
        >
          <a-input-password
            v-model:value="formState.checkPassword"
            placeholder="请再次输入密码"
            size="large"
          >
            <template #prefix>
              <SafetyOutlined class="input-icon" />
            </template>
          </a-input-password>
        </a-form-item>
        <div class="tips">
          已有账号？
          <RouterLink to="/user/login">去登录</RouterLink>
        </div>
        <a-form-item>
          <a-button type="primary" html-type="submit" size="large" class="submit-btn" block>
            注 册
          </a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { userRegister } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import { reactive } from 'vue'
import { UserOutlined, LockOutlined, SafetyOutlined } from '@ant-design/icons-vue'

const router = useRouter()

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

/**
 * 验证确认密码
 * @param rule
 * @param value
 * @param callback
 */
const validateCheckPassword = (rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value && value !== formState.userPassword) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: API.UserRegisterRequest) => {
  const res = await userRegister(values)
  // 注册成功，跳转到登录页面
  if (res.data.code === 0) {
    message.success('注册成功')
    router.push({
      path: '/user/login',
      replace: true,
    })
  } else {
    message.error('注册失败，' + res.data.message)
  }
}
</script>

<style scoped>
#userRegisterPage {
  position: relative;
  min-height: calc(100vh - 64px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 16px;
  overflow: hidden;
}

/* 氛围背景 */
.auth-ambient {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.auth-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.45;
  animation: float-slow 12s ease-in-out infinite;
}

.auth-orb--left {
  width: 420px;
  height: 420px;
  top: -80px;
  left: -120px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.5), transparent 70%);
}

.auth-orb--right {
  width: 380px;
  height: 380px;
  bottom: -100px;
  right: -100px;
  background: radial-gradient(circle, rgba(168, 85, 247, 0.4), transparent 70%);
  animation-delay: -6s;
}

/* 卡片 */
.auth-card {
  position: relative;
  width: 100%;
  max-width: 420px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  padding: 40px 36px 24px;
  animation: fade-up 0.5s var(--ease-out) both;
}

.auth-brand {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.auth-logo {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  box-shadow: var(--shadow-md);
}

.title {
  text-align: center;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--text-1);
  margin: 0 0 8px;
}

.desc {
  text-align: center;
  color: var(--text-3);
  font-size: 13px;
  margin-bottom: 28px;
}

.input-icon {
  color: var(--text-3);
}

.tips {
  text-align: right;
  color: var(--text-3);
  font-size: 13px;
  margin-bottom: 16px;
}

.submit-btn {
  height: 44px;
  font-weight: 600;
  font-size: 15px;
  background: var(--gradient-brand);
  border: none;
  box-shadow: var(--shadow-brand);
  transition:
    transform 0.2s var(--ease-out),
    box-shadow 0.2s var(--ease-out);
}

.submit-btn:hover {
  background: var(--gradient-brand-hover);
  transform: translateY(-1px);
  box-shadow: 0 12px 30px rgba(99, 102, 241, 0.42);
}
</style>
