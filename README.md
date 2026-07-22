# AI-Coding

AI-Coding 是一个由 Vue 3 前端与 Spring Boot 后端组成的 AI 应用生成项目。用户可创建应用、通过 SSE 对话生成代码、预览和部署应用，并管理应用版本、聊天记录与协作成员。

## 目录说明

```text
ai-coding/
├── frontend/                  # Vue 3 + TypeScript + Vite 前端
├── backend/                   # Java 21 + Spring Boot 多模块后端
│   ├── coding-common/         # 公共模型、DTO、工具与统一响应
│   └── coding-server/         # Controller、Service、Mapper 与启动模块
├── doc/                       # 项目开发、接口与资料整理文档
└── README.md                  # 本文档
```

## 开始前必须配置 `.env`

后端不会把数据库、Redis、模型服务和 OSS 的密钥写入代码。启动前必须在 `backend/coding-server/.env` 中填写实际配置：

```powershell
Copy-Item backend/coding-server/.env.example backend/coding-server/.env
```

前端本地环境建议使用 `frontend/.env.local` 覆盖环境变量：

```powershell
Copy-Item frontend/.env.example frontend/.env.local
```

`.env` 和 `.env.local` 均被 Git 忽略。请勿提交、截图或分享其中的密码、API Key、OSS 密钥等敏感值。

## 本地启动

1. 先按[开发指南](doc/开发指南.md)完成 MySQL、Redis 和 `.env` 配置。
2. 在 `backend` 目录启动后端：`./mvnw.cmd -q -pl coding-server spring-boot:run`。
3. 在 `frontend` 目录安装依赖并启动：`npm install`、`npm run dev`。
4. 打开 Vite 终端输出的本地地址；健康检查地址为 `http://localhost:8080/api/health`。

## 本地部署（Nginx）

应用部署阶段依赖本机 Nginx。后端部署接口会将生成物发布为 6 位 `deployKey`，并返回 `http://localhost/{deployKey}/`；Nginx 负责将该路径反向代理到后端的 `/api/static/{deployKey}/`。

部署前必须完成以下事项：

1. 后端已启动在 `127.0.0.1:8080`，且 `.env` 已配置完成。
2. 前端已执行 `npm run build`。
3. Nginx 已配置前端静态目录、`/api/` 反向代理及 `/{deployKey}/` 反向代理，并通过 `nginx -t`。
4. 先启动或重载 Nginx，再在页面中执行“部署”；后端部署过程会访问部署 URL 生成截图。

完整 Nginx 配置与验证步骤见[部署指南](doc/部署指南.md)。

## 文档

- [开发指南](doc/开发指南.md)：环境要求、`.env`、数据库初始化、构建与启动流程。
- [部署指南](doc/部署指南.md)：本地 Nginx、前端静态站点、API/SSE 与已部署应用的反向代理。
- [接口文档](doc/接口文档.md)：接口基址、认证方式、响应结构、常用请求示例与接口目录。
- [资源整理说明](doc/资源整理说明.md)：后端资源目录的保留与迁移规则。
- [数据库脚本](doc/sql/README.md)：初始化 SQL 的文档副本与导入说明。

## 常用命令

```powershell
# 后端编译与测试（在 backend 目录）
./mvnw.cmd -q clean compile
./mvnw.cmd -q test

# 前端类型检查并构建（在 frontend 目录）
npm run build

# 根据后端 OpenAPI 更新前端客户端（后端启动后，在 frontend 目录）
npm run openapi2ts
```
