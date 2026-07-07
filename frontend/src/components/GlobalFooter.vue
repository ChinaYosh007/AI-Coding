<template>
  <footer class="site-footer">
    <div class="footer-inner">
      <div class="footer-brand">
        <img src="@/assets/logo.png" alt="Logo" class="footer-logo" />
        <span class="footer-name">AI Coding</span>
      </div>
      <p class="footer-slogan">一句话，把想法变成能上线的应用</p>
      <p class="footer-copyright">© {{ new Date().getFullYear() }} AI Coding · All rights reserved</p>
    </div>
  </footer>
</template>

<script setup lang="ts">
// 无需额外的响应式数据
</script>

<style scoped>
.site-footer {
  border-top: 1px solid var(--border);
  background: var(--surface);
  padding: 40px 24px 32px;
}

.footer-inner {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.footer-brand {
  display: flex;
  align-items: center;
  gap: 8px;
}

.footer-logo {
  width: 24px;
  height: 24px;
  border-radius: 7px;
}

.footer-name {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.01em;
  background: var(--gradient-text);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.footer-slogan {
  margin: 0;
  font-size: 13px;
  color: var(--text-2);
}

.footer-copyright {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--text-3);
}
</style>
