/**
 * 从可能包含 JSON 的字符串中提取显示名称
 * 兼容后端返回的 {"appName":"xxx"} 格式
 */
export const getDisplayNameFromText = (text?: string): string => {
  const value = text?.trim()
  if (!value) return ''

  if (value.startsWith('{') && value.endsWith('}')) {
    try {
      const parsed = JSON.parse(value)
      const parsedName = parsed?.appName || parsed?.title || parsed?.name
      if (typeof parsedName === 'string' && parsedName.trim()) {
        return parsedName.trim()
      }
    } catch {
      console.warn('解析应用名称失败：', value)
    }
  }

  // 正则兜底：处理截断或格式不完整的 JSON 字符串
  const appNameMatch = value.match(/"appName"\s*:\s*"([^"]+)"/)
  if (appNameMatch) return appNameMatch[1]

  const titleMatch = value.match(/"title"\s*:\s*"([^"]+)"/)
  if (titleMatch) return titleMatch[1]

  const nameMatch = value.match(/"name"\s*:\s*"([^"]+)"/)
  if (nameMatch) return nameMatch[1]

  return value
}
