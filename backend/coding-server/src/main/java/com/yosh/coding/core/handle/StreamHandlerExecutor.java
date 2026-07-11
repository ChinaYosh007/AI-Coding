package com.yosh.coding.core.handle;

import com.yosh.coding.service.AppVersionService;
import com.yosh.coding.service.ChatHistoryService;
import com.yosh.model.enums.CodeGenTypeEnum;
import com.yosh.model.vo.LoginUserVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import static com.yosh.model.enums.CodeGenTypeEnum.*;

/**
 * 流处理器执行器
 * 根据代码生成类型创建合适的流处理器：
 * 1. 传统的 Flux<String> 流（HTML、MULTI_FILE） -> SimpleTextStreamHandler
 * 2. TokenStream 格式的复杂流（VUE_PROJECT） -> JsonMessageStreamHandler
 */
@Slf4j
@Component
public class StreamHandlerExecutor {

    @Resource
    private JsonMessageStreamHandler jsonMessageStreamHandler;
    @Resource
    private AppVersionService appVersionService;

    /**
     * 创建流处理器并处理聊天历史记录
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param loginUser          登录用户
     * @param codeGenType        代码生成类型
     * @return 处理后的流
     */
    public Flux<String> doExecute(Flux<String> originFlux,
                                  ChatHistoryService chatHistoryService,
                                  long appId, long version,
                                  LoginUserVO loginUser, CodeGenTypeEnum codeGenType, boolean isModify) {
        if (codeGenType == CodeGenTypeEnum.VUE_PROJECT) {
            return jsonMessageStreamHandler.handle(originFlux, chatHistoryService, appId, version, loginUser, codeGenType);
        } else if (codeGenType == CodeGenTypeEnum.AUTO) {
            return null;
        } else {
            // HTML, MULTI_FILE
            if (isModify) {
                return jsonMessageStreamHandler.handle(originFlux, chatHistoryService, appId, version, loginUser, codeGenType);
            } else {
                return new SimpleTextStreamHandler(appVersionService)
                        .handle(originFlux, chatHistoryService, appId, version, loginUser);
            }
        }
    }
}
