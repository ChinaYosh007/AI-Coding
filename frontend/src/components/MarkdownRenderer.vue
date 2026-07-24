<template>
  <div class="markdown-content" v-html="renderedMarkdown"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'

// 引入高亮度 GitHub Dark 主题样式
import 'highlight.js/styles/github-dark.css'

interface Props {
  content: string
}

const props = defineProps<Props>()

// 配置 markdown-it 实例
const md: MarkdownIt = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
})

// 自定义代码块渲染器（将自然语言描述与代码块清晰剖析隔离，带亮色高对比度 IDE 头部与复制功能）
md.renderer.rules.fence = (tokens, idx) => {
  const token = tokens[idx]
  const lang = token.info.trim() || 'code'
  const code = token.content

  let highlighted = ''
  if (lang && hljs.getLanguage(lang)) {
    try {
      highlighted = hljs.highlight(code, { language: lang, ignoreIllegals: true }).value
    } catch {
      highlighted = md.utils.escapeHtml(code)
    }
  } else {
    highlighted = md.utils.escapeHtml(code)
  }

  const langLabel = lang.toUpperCase()
  const encodedCode = encodeURIComponent(code)

  return `
    <div class="code-block-wrapper">
      <div class="code-block-header">
        <span class="code-lang-tag">
          <span class="lang-dot"></span>
          ${langLabel}
        </span>
        <button
          type="button"
          class="code-copy-action"
          onclick="navigator.clipboard.writeText(decodeURIComponent('${encodedCode}')).then(() => {
            const btn = this;
            const originalText = btn.innerText;
            btn.innerText = '✓ 已复制';
            btn.classList.add('copied');
            setTimeout(() => { btn.innerText = originalText; btn.classList.remove('copied'); }, 2000);
          })"
        >
          📋 复制代码
        </button>
      </div>
      <pre class="code-block-pre"><code class="hljs ${lang}">${highlighted}</code></pre>
    </div>
  `
}

// 计算渲染后的 Markdown
const renderedMarkdown = computed(() => {
  if (!props.content) return ''
  return md.render(props.content)
})
</script>

<style scoped>
.markdown-content {
  line-height: 1.65;
  color: var(--text-1);
  word-wrap: break-word;
  font-size: 14px;
}

/* 全局 Markdown 文本样式 */
.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3),
.markdown-content :deep(h4) {
  margin: 1.2em 0 0.5em 0;
  font-weight: 700;
  color: var(--text-1);
  line-height: 1.3;
}

.markdown-content :deep(h1) {
  font-size: 1.4em;
  border-bottom: 1px solid var(--border);
  padding-bottom: 0.3em;
}

.markdown-content :deep(h2) {
  font-size: 1.25em;
  border-bottom: 1px solid var(--border);
  padding-bottom: 0.3em;
}

.markdown-content :deep(p) {
  margin: 0.7em 0;
  line-height: 1.65;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin: 0.6em 0;
  padding-left: 1.4em;
}

.markdown-content :deep(li) {
  margin: 0.25em 0;
}

.markdown-content :deep(blockquote) {
  margin: 1em 0;
  padding: 0.6em 1em;
  border-left: 4px solid var(--brand-500);
  background-color: var(--surface-2);
  color: var(--text-2);
  border-radius: var(--radius-sm);
}

.markdown-content :deep(code:not(.hljs)) {
  background-color: rgba(14, 165, 233, 0.08);
  color: var(--brand-600);
  padding: 0.2em 0.45em;
  border-radius: 4px;
  font-family: var(--font-mono);
  font-size: 0.88em;
  border: 1px solid rgba(14, 165, 233, 0.18);
}

/* 独立剖析的代码块卡片 (高亮度 Dark IDE 风格) */
.markdown-content :deep(.code-block-wrapper) {
  margin: 14px 0;
  border-radius: var(--radius-md);
  overflow: hidden;
  background: #0d1117;
  border: 1px solid #30363d;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.markdown-content :deep(.code-block-header) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px;
  background: #161b22;
  border-bottom: 1px solid #30363d;
}

.markdown-content :deep(.code-lang-tag) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 700;
  color: #58a6ff;
  letter-spacing: 0.5px;
  font-family: var(--font-mono);
}

.markdown-content :deep(.lang-dot) {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #38bdf8;
  box-shadow: 0 0 6px #38bdf8;
}

.markdown-content :deep(.code-copy-action) {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: #c9d1d9;
  border-radius: 5px;
  font-size: 11.5px;
  font-weight: 600;
  padding: 3px 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.markdown-content :deep(.code-copy-action:hover) {
  background: rgba(255, 255, 255, 0.18);
  color: #ffffff;
}

.markdown-content :deep(.code-copy-action.copied) {
  background: rgba(34, 197, 94, 0.2);
  color: #4ade80;
  border-color: rgba(34, 197, 94, 0.4);
}

.markdown-content :deep(.code-block-pre) {
  margin: 0;
  padding: 14px;
  background: #0d1117;
  overflow-x: auto;
}

.markdown-content :deep(.code-block-pre code) {
  background: transparent !important;
  color: #e6edf3;
  font-family: var(--font-mono);
  font-size: 12.5px;
  line-height: 1.6;
  white-space: pre;
  tab-size: 2;
}

/* 高亮度 HLJS 代码节点增强 */
.markdown-content :deep(.hljs-keyword) {
  color: #ff7b72 !important;
  font-weight: 700;
}

.markdown-content :deep(.hljs-string) {
  color: #a5d6ff !important;
}

.markdown-content :deep(.hljs-comment) {
  color: #8b949e !important;
  font-style: italic;
}

.markdown-content :deep(.hljs-number) {
  color: #79c0ff !important;
}

.markdown-content :deep(.hljs-function) {
  color: #d2a8ff !important;
}

.markdown-content :deep(.hljs-tag) {
  color: #7ee787 !important;
}

.markdown-content :deep(.hljs-attr) {
  color: #79c0ff !important;
}

.markdown-content :deep(.hljs-title) {
  color: #d2a8ff !important;
  font-weight: 700;
}
</style>
