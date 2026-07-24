package com.yosh.coding.core.handle;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yosh.coding.artificalIntelligence.model.message.DevServerMessage;
import com.yosh.coding.artificalIntelligence.skill.BaseTool;
import com.yosh.coding.artificalIntelligence.skill.DeleteFile;
import com.yosh.coding.artificalIntelligence.skill.ExitTool;
import com.yosh.coding.artificalIntelligence.skill.ModifyFile;
import com.yosh.coding.artificalIntelligence.skill.ReadFile;
import com.yosh.coding.artificalIntelligence.skill.ReadProjectDir;
import com.yosh.coding.artificalIntelligence.skill.WriteToFile;
import com.yosh.coding.core.builder.BuilderVueCommand;
import com.yosh.coding.service.AppVersionService;
import com.yosh.coding.service.ChatHistoryService;
import com.yosh.model.constants.AppConstant;
import com.yosh.model.entity.AppVersion;
import com.yosh.model.enums.MessageTypeEnum;
import com.yosh.model.enums.StreamMessageTypeEnum;
import com.yosh.model.vo.LoginUserVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * JSON 消息流处理器
 * 处理 VUE_PROJECT 类型的复杂流式响应，包含工具调用信息
 */
@Slf4j
@Component
public class JsonMessageStreamHandler {
    @Resource
    private BuilderVueCommand builderVueCommand;
    @Resource
    private AppVersionService appVersionService;

    /**
     * 处理 TokenStream（VUE_PROJECT）
     * 解析 JSON 消息并重组为完整的响应格式
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param loginUser          登录用户
     * @return 处理后的流
     */
    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               long appId, long version,
                               LoginUserVO loginUser,
                               com.yosh.model.enums.CodeGenTypeEnum codeGenType) {
        // 收集数据用于生成后端记忆格式
        StringBuilder chatHistoryStringBuilder = new StringBuilder();
        // 用于跟踪已经见过的工具ID，判断是否是第一次调用
        Set<String> seenToolIds = new HashSet<>();
        Map<String, BaseTool> tools = createToolMap(appId, version);
        return originFlux
                .map(chunk -> {
                    // 解析每个 JSON 消息块
                    return handleJsonMessageChunk(chunk, chatHistoryStringBuilder, seenToolIds, tools);
                })
                .filter(StrUtil::isNotEmpty) // 过滤空字串
                .doOnComplete(() -> {
                    // 流结束后保存聊天记录（轻量操作，不阻塞）
                    try {
                        String aiResponse = chatHistoryStringBuilder.toString();
                        chatHistoryService.addChatHistory(appId, loginUser.getId(), aiResponse, MessageTypeEnum.AI.getValue());
                        updateAppVersionResponse(appId, version, aiResponse);
                    } catch (Exception e) {
                        log.error("保存聊天记录失败: {}", e.getMessage(), e);
                    }
                })
                // 在流末尾追加开发服务器 URL
                .concatWith(Mono.fromCallable(() -> {
                    try {
                        if (codeGenType != com.yosh.model.enums.CodeGenTypeEnum.VUE_PROJECT) {
                            return "";
                        }
                        String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator
                                + AppConstant.VUE_PREFIX + appId + "_" + version;
                        File projectDir = new File(projectPath);

                        // 确保依赖已安装
                        File nodeModules = new File(projectDir, "node_modules");
                        if (!nodeModules.exists()) {
                            log.info("node_modules 不存在，先执行 npm install: {}", projectPath);
                            boolean ok = builderVueCommand.executeNpmInstallOnly(projectDir);
                            if (!ok) {
                                log.error("npm install 失败, appId={}", appId);
                                chatHistoryService.addChatHistory(appId, loginUser.getId(),
                                    "[系统提示] npm install 失败，请检查依赖配置。", MessageTypeEnum.AI.getValue());
                                return "";
                            }
                        }

                        // 启动开发服务器
                        int port = 5173 + (int)(appId % 100);
                        String url = builderVueCommand.startDevServer(projectDir, appId);
                        if (url != null) {
                            log.info("Vue 开发服务器已启动, url={}", url);
                            DevServerMessage msg = new DevServerMessage(url);
                            return JSONUtil.toJsonStr(msg);
                        } else {
                            log.error("开发服务器启动失败, appId={}", appId);
                            chatHistoryService.addChatHistory(appId, loginUser.getId(),
                                "[系统提示] 开发服务器启动失败，请重试。", MessageTypeEnum.AI.getValue());
                            return "";
                        }
                    } catch (Exception e) {
                        log.error("启动开发服务器异常: {}", e.getMessage(), e);
                        return "";
                    }
                })
                .filter(StrUtil::isNotEmpty))
                .doOnError(error -> {
                    // 如果AI回复失败，也要记录错误消息
                    String errorMessage = "AI回复失败: "
                            + AiGenerationErrorMessageResolver.resolve(error);
                    chatHistoryService.addChatHistory(appId, loginUser.getId(), errorMessage, MessageTypeEnum.AI.getValue());
                });
    }

    /**
     * 解析并收集 TokenStream 数据
     */
    private String handleJsonMessageChunk(String chunk,
                                          StringBuilder chatHistoryStringBuilder,
                                          Set<String> seenToolIds,
                                          Map<String, BaseTool> tools) {
        if (StrUtil.isBlank(chunk)) {
            return "";
        }
        String trimmedChunk = chunk.trim();
        if (!trimmedChunk.startsWith("{")) {
            chatHistoryStringBuilder.append(chunk);
            return chunk;
        }
        // 解析 JSON
        JSONObject messageJson;
        try {
            messageJson = JSONUtil.parseObj(trimmedChunk);
        } catch (Exception e) {
            chatHistoryStringBuilder.append(chunk);
            return chunk;
        }
        if ("resource_collection_progress".equals(messageJson.getStr("type"))) {
            return chunk;
        }
        StreamMessageTypeEnum typeEnum = StreamMessageTypeEnum.getEnumByValue(messageJson.getStr("type"));
        if (typeEnum == null) {
            chatHistoryStringBuilder.append(chunk);
            return chunk;
        }
        switch (typeEnum) {
            case AI_RESPONSE -> {
                String data = messageJson.getStr("data", "");
                // 直接拼接响应
                chatHistoryStringBuilder.append(data);
                return data;
            }
            case TOOL_REQUEST -> {
                String toolId = messageJson.getStr("id");
                String toolName = messageJson.getStr("name");
                // 检查是否是第一次看到这个工具 ID
                if (StrUtil.isNotBlank(toolId) && seenToolIds.add(toolId)) {
                    // 第一次调用这个工具，记录 ID 并返回工具信息
                    // 返回格式化的工具调用信息
                    BaseTool tool = tools.get(toolName);
                    if (tool == null) {
                        log.warn("Unknown tool request: id={}, name={}", toolId, toolName);
                        return String.format("\n\n[选择工具] %s\n\n", StrUtil.blankToDefault(toolName, "未知工具"));
                    }
                    return tool.generateToolRequestResponse();
                } else {
                    // 不是第一次调用这个工具，直接返回空
                    return "";
                }
            }
            case TOOL_EXECUTED -> {
                String toolId = messageJson.getStr("id");
                String toolName = messageJson.getStr("name");
                JSONObject arguments = parseToolArguments(messageJson.get("arguments"), toolId, toolName);
                // 根据工具名称获取工具实例并生成相应的结果格式
                BaseTool tool = tools.get(toolName);
                String result = generateToolExecutedResult(tool, toolName, arguments);
                // 输出前端和要持久化的内容
                String output = String.format("\n\n%s\n\n", result);
                chatHistoryStringBuilder.append(output);
                return output;
            }

            default -> {
                log.error("不支持的消息类型: {}", typeEnum);
                return "";
            }
        }
    }

    private Map<String, BaseTool> createToolMap(long appId, long version) {
        return List.<BaseTool>of(
                        new WriteToFile(appId, version),
                        new DeleteFile(appId, version),
                        new ModifyFile(appId, version),
                        new ReadFile(appId, version),
                        new ReadProjectDir(appId, version),
                        new ExitTool()
                ).stream()
                .collect(Collectors.toUnmodifiableMap(BaseTool::getToolName, Function.identity()));
    }

    private JSONObject parseToolArguments(Object rawArguments, String toolId, String toolName) {
        if (rawArguments instanceof JSONObject jsonObject) {
            return jsonObject;
        }
        if (rawArguments == null || StrUtil.isBlank(rawArguments.toString())) {
            log.warn("Tool arguments are empty: id={}, name={}", toolId, toolName);
            return new JSONObject();
        }
        try {
            return JSONUtil.parseObj(rawArguments.toString());
        } catch (Exception e) {
            log.warn("Tool arguments are not a valid JSON object: id={}, name={}", toolId, toolName);
            return new JSONObject();
        }
    }

    private String generateToolExecutedResult(BaseTool tool, String toolName, JSONObject arguments) {
        if (tool == null) {
            log.warn("Unknown executed tool: name={}", toolName);
            return String.format("[工具调用] %s", StrUtil.blankToDefault(toolName, "未知工具"));
        }
        try {
            return tool.generateToolExecutedResult(arguments);
        } catch (Exception e) {
            log.warn("Failed to format tool execution result: name={}", toolName, e);
            return String.format("[工具调用] %s（参数解析失败）", tool.getDisplayName());
        }
    }

    private void updateAppVersionResponse(long appId, long version, String aiResponse) {
        AppVersion appVersion = appVersionService.getByAppIdAndVersion(appId, version);
        if (appVersion == null) {
            log.warn("app version not found when saving ai response. appId={}, version={}", appId, version);
            return;
        }
        appVersion.setAiResponse(aiResponse);
        appVersionService.updateById(appVersion);
    }
}
