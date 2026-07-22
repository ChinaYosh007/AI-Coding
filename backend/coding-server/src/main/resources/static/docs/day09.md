# 扩展思路 TODO —— 基于当前后端架构分析

> 以下分析基于 `D:\project\ai-coding\backend` 的实际代码结构，对照文章中的 5 个扩展方向逐一评估可行性和实施路径。

---

## 当前架构概要

| 模块 | 关键类 | 职责 |
|------|--------|------|
| AI 服务接口 | `AiCodeGeneratorService` | LangChain4j 接口，定义了 HTML/MultiFile/Vue 三种生成方法 |
| 编排中心 | `AiCodeGeneratorFacade` | 按 CodeGenTypeEnum 构建 AI 服务、管理模板拷贝/npm 安装/TokenStream→Flux 转换 |
| 流式处理 | `StreamHandlerExecutor` → `JsonMessageStreamHandler` / `SimpleTextStreamHandler` | 按 type 路由，Vue 走 JSON 消息解析，HTML 走纯文本透传 |
| 工具系统 | `WriteToFile`（唯一存活的 @Tool） | AI 调用写文件，上限 25 次，自动跳过预配置文件 |
| 版本管理 | `AppServiceImpl.reserveAppVersion()` | Redis 分布式锁 + DB 重试，生成前预留版本号 |
| SSE 端点 | `AppController.chatToGenCode()` | 返回 `Flux<ServerSentEvent<String>>`，格式 `{"d":"chunk"}` |
| OSS | Alibaba Cloud OSS 已配置 | 当前仅用于截图存储 |

已删除的工具类（源码不存在，仅 `.class` 残留）：`ReadFile`、`ModifyFile`、`DeleteFIle`。

---

## 扩展项 1：创建和修改分离

**文章原意：** 区分创建和修改应用的提示词和 AI Service，只传递需要的工具，提高准确度、减少工具调用幻觉。

**可行性：高 —— 架构天然支持，改动量适中**

当前 `AiCodeGeneratorFacade.createAiCodeGeneratorService()` 对所有操作（首次创建 + 后续修改）使用相同的 prompt（`vue-multi-file-html.txt`）和相同的工具集（仅 `WriteToFile`）。问题在于：

- **创建场景**：AI 需要一次性生成所有文件，`WriteToFile` 足够。
- **修改场景**：AI 需要先读取现有文件内容，再针对性修改。当前 AI 只能靠 chat memory 中的历史信息"猜"文件内容，容易产生幻觉。

### TODO

- [ ] **1.1 新增修改模式判断逻辑**：在 `AppServiceImpl.chatToGenCode()` 中，根据 `appVersions` 数量判断是"首次创建"还是"后续修改"，传入 `isModify` 标志。
- [ ] **1.2 编写修改专用 Prompt**：创建 `vue-modify-file.md`，指示 AI 先用 `ReadFile` 读取目标文件，再用 `ModifyFile` 精准替换内容，而非全量重写。
- [ ] **1.3 恢复 `ReadFile` 工具**：从 `target/classes` 的 `.class` 反编译或重写源码，注册为 LangChain4j `@Tool`，让 AI 能读取已有文件内容。需在 `AiCodeGeneratorFacade.createAiCodeGeneratorService()` 的工具列表中添加，仅修改模式启用。
- [ ] **1.4 恢复 `ModifyFile` 工具**：同理，重写 `ModifyFile`，支持 `oldContent`→`newContent` 的精准替换，避免全量覆写大文件。
- [ ] **1.5 AI Service 分离**：在 `AiCodeGeneratorService` 接口中新增 `generateVueCodeModifyStream()` 方法，使用修改专用 prompt，绑定 `ReadFile` + `ModifyFile` + `WriteToFile` 三个工具。`AiCodeGeneratorFacade` 根据 `isModify` 标志选择调用哪个方法。
- [ ] **1.6 `StreamHandlerExecutor` 适配**：新增 `VUE_PROJECT_MODIFY` 类型或在现有 `VUE_PROJECT` 分支中根据 `isModify` 标志切换 handler。

---

## 扩展项 2：优化工具流式输出（不推荐）

**文章原意：** 让工具调用时流式输出参数（比如 `WriteFile` 时实时流式输出 `content` 字段的代码内容），而非等工具调用完成后一次性返回。文章明确标注"不推荐"。

**可行性：低收益 —— 当前架构已有替代方案，且文章本身不推荐**

当前 `processTokenStream()` 已经将 LangChain4j 的 `onPartialResponse` / `onToolExecuted` 回调转换为 `Flux<String>`，前端能实时收到 `ToolRequestMessage`（含 `id`、`name`、`arguments`）和 `ToolExecutedMessage`。前端已通过后端 API（`/api/static/preview/{appId}/{version}/{path}`）在文件写入后主动拉取文件内容实现了实时展示。

真正的"工具参数流式输出"需要 LangChain4j 框架层面支持 `onPartialToolArguments` 回调，目前框架不支持。

### TODO

- [ ] ~~暂不实施~~ —— 等待 LangChain4j 框架优化工具调用事件后再考虑。当前方案（文件写入后前端主动拉取）已满足体验需求。

---

## 扩展项 3：优化代码展示效果

**文章原意：** 用 Tab 切换多文件展示，而非区域输出。

**可行性：已在前端完成 —— 无需后端改动**

前一轮会话中已完成：`AppChatPage.vue` 实现了文件树 + 代码查看器 + highlight.js 语法高亮 + 白色背景主题。生成中实时展示文件树和代码内容，空闲态隐藏代码面板、预览区占满。

### TODO

- [x] ~~已完成~~ —— 前端已实现 Tab 式文件展示和语法高亮。

---

## 扩展项 4：支持多媒体上传

**文章原意：** 用户上传图片 → 后端返回图片 URL → 作为 Prompt 输入给 AI。

**可行性：高 —— OSS 已配置，只需新增上传端点**

当前 Alibaba Cloud OSS 已在 `ScreenshotUtil` 中用于截图上传，配置和凭证已就绪。只需新增一个图片上传 API，复用 OSS 上传逻辑。

### TODO

- [ ] **4.1 新增 `FileUploadController`**：暴露 `POST /api/file/upload-image` 端点，接收 `MultipartFile`，调用 OSS 上传，返回图片 URL。
- [ ] **4.2 复用 `ScreenshotUtil` 的 OSS 上传逻辑**：抽取 OSS 上载方法为公共工具类（如 `OssUtil.uploadFile(MultipartFile)`），返回公网可访问 URL。
- [ ] **4.3 前端上传交互**：在 `AppChatPage.vue` 的输入框区域添加图片上传按钮，上传后将 URL 插入到用户消息中（如 `请基于这张图片设计：{url}`）。
- [ ] **4.4（可选）Prompt 增强**：在 `vue-multi-file-html.md` 中增加说明，告知 AI 可以使用用户提供的图片 URL 作为素材。

---

## 扩展项 5：优化代码编辑效果

**文章原意：** 用 `contentEditable=true` 让用户直接在预览页面内编辑内容，类似 Z.ai 的编辑效果。

**可行性：中 —— 前端已有编辑模式框架，后端需新增保存端点**

当前前端 `AppChatPage.vue` 已有 `isEditMode` / `toggleEditMode` 的 UI 框架（按钮已存在但功能可能未完全实现）。要实现真正的 inline editing，需要：

1. 前端：iframe 加载后注入 `contentEditable=true` 到页面元素
2. 后端：新增端点接收用户编辑后的文件内容并保存回版本目录

### TODO

- [ ] **5.1 后端新增文件保存端点**：在 `AppController` 中新增 `POST /api/app/{appId}/version/{version}/save-file`，接收 `filePath` + `content`，写入对应版本的源文件目录。需校验用户权限（仅 owner）。
- [ ] **5.2 前端 iframe 注入编辑能力**：在 `isEditMode = true` 时，通过 `iframe.contentDocument.body.contentEditable = 'true'` 开启编辑，或注入一段 JS 脚本为指定元素添加 `contentEditable`。
- [ ] **5.3 编辑后保存回传**：用户编辑完成退出编辑模式时，从 iframe 中提取修改后的 HTML，调用 5.1 的端点保存到版本目录。
- [ ] **5.4（可选）支持局部修改回写**：对于 Vue 项目，直接编辑 iframe 中编译后的 DOM 无法直接映射回 `.vue` 源码。此方案更适用于 HTML/MULTI_FILE 模式（源码即 DOM）。Vue 模式需另考虑。

---

## 优先级建议

| 优先级 | 扩展项 | 理由 |
|--------|--------|------|
| P0 | 扩展项 4：多媒体上传 | 改动最小（OSS 已就绪），用户体验提升明显 |
