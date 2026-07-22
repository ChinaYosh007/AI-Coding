# Day 10 后端待实现接口清单

> 本文档通过阅读前端项目 `frontend/src` 下所有 API 调用、页面组件内联请求，与后端 `backend/coding-server` 已有 Controller 逐一对比后生成。
> 标注了后端缺失或前后端不一致的接口，每个条目附带接口规格说明，供后端实现时参考。
>
> **更新：2026-07-13 — 全部 4 项采用方案 A（改前端）修复完成。**

---

## 一、接口总览

| 编号 | 接口 | HTTP | 路径 | 状态 | 所属模块 |
|------|------|------|------|------|----------|
| 1 | 删除对话历史 | POST | `/chatHistory/delete` | **已修复（前端本地移除）** | 对话管理 |
| 2 | 图片上传 | POST | `/upload` | **已修复（方案 A 改前端）** | 应用对话 |
| 3 | 健康检查 | GET | `/health/` | **已修复（方案 A 改前端）** | 系统健康 |
| 4 | 总结智能记忆 | POST | `/app/{appId}/memory/summarize` | **已修复（方案 A 改前端）** | 应用对话 |

---

## 二、接口详细规格

### 1. 删除对话历史 — 后端完全缺失 ✅ 已修复（前端本地移除）

**修复内容：** 修改 `ChatManagePage.vue` 的 `deleteMessage` 函数，改为从前端 `data` 数组中过滤掉对应 id 的记录并更新 `total` 计数，不再调用不存在的后端接口。刷新页面后记录会恢复（后端未持久化删除），如需持久化删除仍需后端补充接口。

**问题说明**

前端 `src/pages/admin/ChatManagePage.vue` 第 197-208 行定义了 `deleteMessage` 函数，UI 上每条对话记录有"删除"按钮（第 71 行 `<a-popconfirm>`），但函数体内只有注释"需要后端提供删除对话历史的接口"，并未真正发起请求。后端 `ChatHistoryController` 只有 `listAppChatHistory` 和 `listAllChatHistoryByPageForAdmin` 两个查询接口，没有任何删除端点。

**前端调用位置**

- 文件：`src/pages/admin/ChatManagePage.vue`，第 197 行
- 触发：管理员在对话管理页面点击"删除"按钮并确认

**接口规格**

```
POST /api/chatHistory/delete
Content-Type: application/json
```

**请求参数（Body）**

```json
{
  "id": 123
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | number | 是 | 对话记录 ID |

**期望响应**

```json
{
  "code": 0,
  "data": true,
  "message": "ok"
}
```

**权限要求**

- 管理员（admin 角色）

**后端实现建议**

- 在 `ChatHistoryController` 中新增 `@PostMapping("/delete")` 方法
- 参数使用 `@RequestBody DeleteRequest deleteRequest`（复用 `coding-common` 中已有的 `DeleteRequest`）
- 权限校验：仅管理员可调用（使用 `@AuthCheck(mustRole = ADMIN)` 注解）
- Service 层：在 `ChatHistoryService` 中新增 `boolean deleteChatHistory(long id)` 方法，校验记录存在性后执行逻辑删除（更新 `isDelete` 字段）

---

### 2. 图片上传 — 前后端路径不匹配 ✅ 已修复（方案 A 改前端）

**修复内容：** 将 `AppChatPage.vue` 中 `request.post(baseURL + '/upload', ...)` 改为 `request.post(baseURL + '/file/upload-image', ...)`，对齐后端 `UploadController` 的实际路径。

**问题说明**

前端 `src/pages/app/AppChatPage.vue` 第 2294 行通过 axios 实例直接调用 `request.post(baseURL + '/upload', formData)`，请求路径为 `/upload`。而后端 `UploadController` 的类级前缀是 `@RequestMapping("/file")`，方法路径是 `@PostMapping("/upload-image")`，完整路径为 `/api/file/upload-image`。前端请求会命中 404。

**前端调用位置**

- 文件：`src/pages/app/AppChatPage.vue`，第 2294 行
- 触发：用户在对话输入框中粘贴/选择图片后自动上传

**前端实际请求**

```
POST /api/upload
Content-Type: multipart/form-data
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File (multipart) | 是 | 用户选择的图片文件 |

**后端已有实现**

```
POST /api/file/upload-image
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | MultipartFile | 是 | 图片文件（@RequestPart("file")） |

**期望响应**

```json
{
  "code": 0,
  "data": "https://xxx.oss-cn-xxx.aliyuncs.com/xxx.png",
  "message": "ok"
}
```

**修复方案（二选一）**

- **方案 A（改前端）**：将 `AppChatPage.vue` 第 2294 行的 `'/upload'` 改为 `'/file/upload-image'`，对齐后端已有路径
- **方案 B（改后端）**：在 `UploadController` 中新增 `@PostMapping("/upload")` 方法（或修改类级前缀为空），使后端也能响应 `/upload` 路径

---

### 3. 健康检查 — 前后端路径不匹配（尾斜杠） ✅ 已修复（方案 A 改前端）

**修复内容：** 将 `healthController.ts` 中的 `'/health/'` 改为 `'/health'`，去除尾斜杠以匹配后端 `TestController` 的 `@GetMapping("/health")` 路径。

**问题说明**

前端 `src/api/healthController.ts` 第 7 行调用 `request('/health/', ...)`，路径带尾斜杠。后端 `TestController` 中方法注解为 `@GetMapping("/health")`（无尾斜杠）。Spring Boot 3.x 默认 `setUseTrailingSlashMatch(false)`，`/health/` 不会匹配 `/health`，会返回 404。

**前端调用位置**

- 文件：`src/api/healthController.ts`，第 6-11 行

**前端实际请求**

```
GET /api/health/
```

**后端已有实现**

```
GET /api/health
```

**期望响应**

```json
{
  "code": 0,
  "data": "ok",
  "message": "ok"
}
```

**修复方案（二选一）**

- **方案 A（改前端）**：将 `healthController.ts` 中的 `'/health/'` 改为 `'/health'`
- **方案 B（改后端）**：在 `TestController` 的 `@GetMapping` 中添加尾斜杠路径 `@GetMapping("/health/")`，或配置 `WebMvcConfigurer` 启用 `setUseTrailingSlashMatch(true)`

---

### 4. 总结智能记忆 — 参数定义不一致 ✅ 已修复（方案 A 改前端）

**修复内容：** 修改 `appController.ts` 中 `summarizeAppChatHistoryMemory` 函数签名，增加 `version: number` 参数并通过 `params` 传递给后端。同时在 `AppChatPage.vue` 调用处传入 `selectedVersion.value`，并增加空值前置检查。

**问题说明**

前端 `src/api/appController.ts` 第 141-149 行定义的 `summarizeAppChatHistoryMemory` 函数只接收 `appId` 一个参数，生成的请求不携带 `version` 查询参数。但后端 `AppController` 中 `summarizeAppChatHistoryMemory` 方法签名包含 `@RequestParam Long version`，该参数为必填。前端调用时未传 `version`，后端会因缺少必填参数返回 400 Bad Request。

**前端调用位置**

- 文件：`src/api/appController.ts`，第 141-149 行（API 定义）
- 文件：`src/pages/app/AppChatPage.vue`，第 1217 行（实际调用：`summarizeAppChatHistoryMemory(appId.value)`）

**前端实际请求**

```
POST /api/app/{appId}/memory/summarize
```

无查询参数。

**后端已有实现**

```
POST /api/app/{appId}/memory/summarize?version={version}
```

| 参数 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| appId | Path | Long | 是 | 应用 ID |
| version | Query | Long | 是 | 代码版本号 |

**期望响应**

```json
{
  "code": 0,
  "data": null,
  "message": "ok"
}
```

**修复方案（二选一）**

- **方案 A（改前端）**：修改 `summarizeAppChatHistoryMemory` 函数签名，增加 `version` 参数，并在 `AppChatPage.vue` 调用处传入当前版本号（`selectedVersion.value`）。之后重新执行 `npm run openapi2ts` 重新生成 API 代码
- **方案 B（改后端）**：将 `@RequestParam Long version` 改为 `@RequestParam(required = false) Long version`，在 Service 层根据 `appId` 自动获取最新版本号

---

## 三、附录：后端已正确实现的接口（共 36 个）

以下接口前后端已对齐，无需修改：

**用户模块（11 个）**

| 接口 | 方法 | 路径 | 状态 |
|------|------|------|------|
| 用户注册 | POST | `/user/register` | OK |
| 用户登录 | POST | `/user/login` | OK |
| 获取登录用户 | GET | `/user/get/login` | OK |
| 退出登录 | POST | `/user/logout` | OK |
| 添加用户 | POST | `/user/add` | OK |
| 删除用户 | POST | `/user/delete` | OK |
| 获取用户 | GET | `/user/get` | OK |
| 获取用户 VO | GET | `/user/get/vo` | OK |
| 分页查询用户 | POST | `/user/list/page/vo` | OK |
| 更新用户 | POST | `/user/update` | OK |
| 更新个人资料 | POST | `/user/update/my` | OK |

**应用模块（20 个）**

| 接口 | 方法 | 路径 | 状态 |
|------|------|------|------|
| 创建应用 | POST | `/app/add` | OK |
| 更新应用 | POST | `/app/update` | OK |
| 删除应用 | POST | `/app/delete` | OK |
| 获取应用 VO | GET | `/app/get/vo` | OK |
| 我的应用分页 | POST | `/app/my/list/page/vo` | OK |
| 精选应用分页 | POST | `/app/good/list/page/vo` | OK |
| 管理员删除应用 | POST | `/app/admin/delete` | OK |
| 管理员更新应用 | POST | `/app/admin/update` | OK |
| 管理员分页查询 | POST | `/app/admin/list/page/vo` | OK |
| 管理员获取应用 | GET | `/app/admin/get/vo` | OK |
| AI 生成代码 (SSE) | GET | `/app/chat/gen/code` | OK |
| 部署应用 | POST | `/app/deploy` | OK |
| 对话统计 | GET | `/app/{appId}/stats` | OK |
| 导出对话 Markdown | GET | `/app/{appId}/export/markdown` | OK |
| 获取智能记忆 | GET | `/app/{appId}/memory` | OK |
| 协作成员列表 | GET | `/app/{appId}/collaboration/members` | OK |
| 邀请协作者 | POST | `/app/{appId}/collaboration/invite` | OK |
| 下载代码 | GET | `/app/download/{appId}` | OK |
| 保存内联编辑 | POST | `/app/{appId}/version/{version}/save-file` | OK |

**应用版本模块（2 个）**

| 接口 | 方法 | 路径 | 状态 |
|------|------|------|------|
| 版本列表 | GET | `/appVersion/list` | OK |
| 版本详情 | GET | `/appVersion/getInfo/{id}` | OK |

**对话历史模块（2 个）**

| 接口 | 方法 | 路径 | 状态 |
|------|------|------|------|
| 应用对话历史 | GET | `/chatHistory/app/{appId}` | OK |
| 管理员对话历史 | POST | `/chatHistory/admin/list/page/vo` | OK |

**静态资源模块（3 个）**

| 接口 | 方法 | 路径 | 状态 |
|------|------|------|------|
| 部署资源服务 | GET | `/static/{deployKey}/**` | OK |
| 预览文件列表 | GET | `/static/preview/{appId}/{version}/files` | OK |
| 预览文件内容 | GET | `/static/preview/{appId}/{version}/**` | OK |

---

## 四、后端额外接口（前端未使用）

后端已实现但前端当前未调用的接口：

| 接口 | 方法 | 路径 | 备注 |
|------|------|------|------|
| 修改密码 | POST | `/user/update/password` | 前端未接入修改密码功能 |
| 保存版本 | POST | `/appVersion/save` | 前端未直接调用 |
| 删除版本 | DELETE | `/appVersion/remove/{id}` | 前端未直接调用 |
| 更新版本 | PUT | `/appVersion/update` | 前端未直接调用 |
| 版本分页 | GET | `/appVersion/page` | 前端未直接调用 |
