/**
 * 从可能包含 JSON 的字符串中提取显示名称
 * 兼容后端返回的 {"appName":"xxx"} 格式
 */
export const getDisplayNameFromText = (text?: string): string => {
  const value = text?.trim()
  if (!value) return ''

  const isInvalidName = (name: string) =>
    /<!doctype|<html|<script|"code"\s*:|\b(?:function|const|import)\b/i.test(name)

  if (value.startsWith('{') && value.endsWith('}')) {
    try {
      const parsed = JSON.parse(value)
      const parsedName = parsed?.appName || parsed?.title || parsed?.name
      if (typeof parsedName === 'string' && parsedName.trim() && !isInvalidName(parsedName)) {
        return parsedName.trim()
      }
      return ''
    } catch {
      console.warn('解析应用名称失败：', value)
    }
  }

  // 正则兜底：处理截断或格式不完整的 JSON 字符串
  const appNameMatch = value.match(/"appName"\s*:\s*"([^"]+)"/)
  if (appNameMatch && !isInvalidName(appNameMatch[1])) return appNameMatch[1]

  const titleMatch = value.match(/"title"\s*:\s*"([^"]+)"/)
  if (titleMatch && !isInvalidName(titleMatch[1])) return titleMatch[1]

  const nameMatch = value.match(/"name"\s*:\s*"([^"]+)"/)
  if (nameMatch && !isInvalidName(nameMatch[1])) return nameMatch[1]

  return isInvalidName(value) ? '' : value
}

/**
 * 历史应用可能保存了无效的模型输出；从初始化需求生成一个安全、可读的临时标题。
 */
export const getFallbackAppName = (initPrompt?: string, fallback = '未命名应用'): string => {
  const value = initPrompt?.replace(/\s+/g, ' ').trim()
  if (!value || /<!doctype|<html|<script|\b(?:function|const|import)\b/i.test(value)) {
    return fallback
  }

  const candidate = value
    .replace(/^(?:请帮我|帮我|请|设计|开发|创建|制作|生成|做一个|做个)(?:一个|一套)?/, '')
    .split(/[，,。；;：:]/, 1)[0]
    .trim()

  return candidate ? candidate.slice(0, 16) : fallback
}
