package com.yosh.coding.core.handle;

import com.yosh.coding.service.AppVersionService;
import com.yosh.coding.service.ChatHistoryService;
import com.yosh.model.entity.AppVersion;
import com.yosh.model.enums.MessageTypeEnum;
import com.yosh.model.vo.LoginUserVO;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * 简单文本流处理器
 * 处理 HTML 和 MULTI_FILE 类型的流式响应
 */
@Slf4j
public class SimpleTextStreamHandler {

    private final AppVersionService appVersionService;

    public SimpleTextStreamHandler(AppVersionService appVersionService) {
        this.appVersionService = appVersionService;
    }

    /**
     * 处理传统流（HTML, MULTI_FILE）
     * 直接收集完整的文本响应
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param loginUser          登录用户
     * @return 处理后的流
     */
    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               long appId,long version,
                               LoginUserVO loginUser) {
        StringBuilder aiResponseBuilder = new StringBuilder();
        return originFlux
                .map(chunk -> {
                    if (chunk.contains("\"type\":\"resource_collection_progress\"")) {
                        return chunk;
                    }
                    // 收集AI响应内容
                    aiResponseBuilder.append(chunk);
                    return chunk;
                })
                .doOnComplete(() -> {
                    // 流式响应完成后，添加AI消息到对话历史
                    String aiResponse = aiResponseBuilder.toString();
                    chatHistoryService.addChatHistory(appId,loginUser.getId(), aiResponse,MessageTypeEnum.AI.getValue());
                    updateAppVersionResponse(appId, version, aiResponse);
                })
                .doOnError(error -> {
                    // 如果AI回复失败，也要记录错误消息
                    String errorMessage = "AI回复失败: " + error.getMessage();
                    chatHistoryService.addChatHistory(appId,loginUser.getId(), errorMessage,MessageTypeEnum.AI.getValue());
                });
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
