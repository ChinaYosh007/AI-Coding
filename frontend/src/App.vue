<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'

dayjs.locale('zh-cn')

// Ant Design Vue 全局主题定制，与 global.css 中的设计变量保持一致
const themeConfig = {
  token: {
    colorPrimary: '#0ea5e9',
    colorInfo: '#0ea5e9',
    colorLink: '#0284c7',
    colorTextBase: '#0f172a',
    colorBgLayout: '#f6f7fb',
    borderRadius: 10,
    controlHeight: 36,
    fontFamily:
      "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Helvetica Neue', Arial, sans-serif",
    boxShadow:
      '0 1px 3px rgba(15, 23, 42, 0.06), 0 4px 12px rgba(15, 23, 42, 0.04)',
  },
  components: {
    Button: {
      controlHeight: 36,
      fontWeight: 500,
    },
    Modal: {
      borderRadiusLG: 16,
    },
    Card: {
      borderRadiusLG: 16,
    },
  },
}
</script>

<template>
  <a-config-provider :locale="zhCN" :theme="themeConfig">
    <BasicLayout />
  </a-config-provider>
</template>
