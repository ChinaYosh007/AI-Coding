<template>
  <a-modal v-model:open="visible" :footer="null" width="520px" :closable="true" centered>
    <div class="deploy-success">
      <div class="success-icon">
        <CheckCircleOutlined />
      </div>
      <h3>网站部署成功 🎉</h3>
      <p>你的网站已经上线，可以通过以下链接访问：</p>
      <div class="deploy-url">
        <a-input :value="deployUrl" readonly size="large">
          <template #suffix>
            <a-button type="text" size="small" @click="handleCopyUrl">
              <CopyOutlined />
            </a-button>
          </template>
        </a-input>
      </div>
      <div class="deploy-actions">
        <a-button type="primary" size="large" class="visit-btn" @click="handleOpenSite">
          访问网站
        </a-button>
        <a-button size="large" @click="handleClose">关闭</a-button>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { message } from 'ant-design-vue'
import { CheckCircleOutlined, CopyOutlined } from '@ant-design/icons-vue'

interface Props {
  open: boolean
  deployUrl: string
}

interface Emits {
  (e: 'update:open', value: boolean): void
  (e: 'open-site'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value),
})

const handleCopyUrl = async () => {
  try {
    await navigator.clipboard.writeText(props.deployUrl)
    message.success('链接已复制到剪贴板')
  } catch (error) {
    console.error('复制失败：', error)
    message.error('复制失败')
  }
}

const handleOpenSite = () => {
  emit('open-site')
}

const handleClose = () => {
  visible.value = false
}
</script>

<style scoped>
.deploy-success {
  text-align: center;
  padding: 24px 8px 8px;
}

.success-icon {
  margin-bottom: 16px;
  font-size: 52px;
  color: #22c55e;
  filter: drop-shadow(0 6px 16px rgba(34, 197, 94, 0.35));
}

.deploy-success h3 {
  margin: 0 0 10px;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--text-1);
}

.deploy-success p {
  margin: 0 0 20px;
  color: var(--text-2);
  font-size: 14px;
}

.visit-btn {
  background: var(--gradient-brand);
  border: none;
  font-weight: 600;
  box-shadow: var(--shadow-brand);
}

.visit-btn:hover {
  background: var(--gradient-brand-hover);
}

.deploy-url {
  margin-bottom: 24px;
}

.deploy-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>
