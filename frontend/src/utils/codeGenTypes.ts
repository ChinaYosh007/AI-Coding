/**
 * 代码生成类型枚举
 */
export enum CodeGenTypeEnum {
  AUTO = 'auto',
  HTML = 'html',
  MULTI_FILE = 'multi_file',
  VUE_PROJECT = 'vue_project',
}

/**
 * 代码生成类型配置
 */
export const CODE_GEN_TYPE_CONFIG = {
  [CodeGenTypeEnum.AUTO]: {
    label: '智能自动模式',
    value: CodeGenTypeEnum.AUTO,
    desc: 'AI 根据需求自动选择，简单页面无需 npm，复杂应用用 Vue',
  },
  [CodeGenTypeEnum.HTML]: {
    label: '原生 HTML 模式',
    value: CodeGenTypeEnum.HTML,
    desc: '单文件 HTML，适合简单页面',
  },
  [CodeGenTypeEnum.MULTI_FILE]: {
    label: '原生多文件模式',
    value: CodeGenTypeEnum.MULTI_FILE,
    desc: 'HTML + CSS + JS 三文件分离',
  },
  [CodeGenTypeEnum.VUE_PROJECT]: {
    label: 'Vue 项目模式',
    value: CodeGenTypeEnum.VUE_PROJECT,
    desc: '完整 Vue 工程，适合复杂应用',
  },
} as const

/**
 * 代码生成类型选项（用于下拉选择，自动模式在前）
 */
export const CODE_GEN_TYPE_OPTIONS = Object.values(CODE_GEN_TYPE_CONFIG).map((config) => ({
  label: config.label,
  value: config.value,
  desc: config.desc,
}))

/**
 * 格式化代码生成类型
 */
export const formatCodeGenType = (type: string | undefined): string => {
  if (!type) return '未知类型'
  const config = CODE_GEN_TYPE_CONFIG[type as CodeGenTypeEnum]
  return config ? config.label : type
}

export const getAllCodeGenTypes = () => {
  return Object.values(CodeGenTypeEnum)
}

export const isValidCodeGenType = (type: string): type is CodeGenTypeEnum => {
  return Object.values(CodeGenTypeEnum).includes(type as CodeGenTypeEnum)
}
