package com.yosh.coding.core;

import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mybatisflex.core.paginate.Page;
import com.yosh.coding.artificalIntelligence.AiCodeGeneratorService;
import com.yosh.coding.artificalIntelligence.model.HtmlCodeResult;
import com.yosh.coding.artificalIntelligence.model.MultiFileCodeResult;
import com.yosh.coding.artificalIntelligence.model.message.AiResponseMessage;
import com.yosh.coding.artificalIntelligence.model.message.ToolExecutedMessage;
import com.yosh.coding.artificalIntelligence.model.message.ToolRequestMessage;
import com.yosh.coding.artificalIntelligence.skill.WriteToFile;
import com.yosh.coding.core.parser.CodeParserExcutor;
import com.yosh.coding.core.saver.CodeFilleSaveExecutor;
import com.yosh.coding.service.ChatHistoryService;
import com.yosh.exception.BusinessException;
import com.yosh.exception.ErrorCode;
import com.yosh.model.entity.ChatHistory;
import com.yosh.model.enums.CodeGenTypeEnum;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.io.File;
import java.time.Duration;

/**
 * AI 代码生成外观类，组合生成和保存功�?
 */
@Slf4j
@Service
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;
    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    private OpenAiStreamingChatModel openAiStreamingChatModel;
    @Autowired
    private ChatModel chatModel;

    /**
     * 缓存内部�?
     * @param appId
     * @param version
     * @param type
     * @return
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(30))
            .removalListener((key, value, cause) -> log.info("Cache removed key: {} with value: {}", key, value))
            .build();
    public AiCodeGeneratorService createAiCodeGeneratorService(long appId, long version,CodeGenTypeEnum type) {
        MessageWindowChatMemory messageWindowChatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .maxMessages(20)
                .chatMemoryStore(redisChatMemoryStore)
                .build();
        chatHistoryService.loadHistoryMessage(messageWindowChatMemory, appId, 20L);

      return  switch (type){
            case VUE_PROJECT -> {
                AiCodeGeneratorService build = AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(openAiStreamingChatModel)
                        .tools(new WriteToFile())
                        .chatMemoryProvider(id -> messageWindowChatMemory)
                        .hallucinatedToolNameStrategy((request) -> ToolExecutionResultMessage.from(request,
                                "error: there no tool called " + request.name()))
                        .build();
                yield build;
            }
            case HTML, MULTI_FILE -> {
                AiCodeGeneratorService build = AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(openAiStreamingChatModel)
                        .chatMemory(messageWindowChatMemory)
                        .build();
                yield build;

            }
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的生成类型:" + type.getValue());
        };

    }
    public  AiCodeGeneratorService getAiCodeGeneratorService(Long appId,Long version){
        return this.getAiCodeGeneratorService(appId, version, CodeGenTypeEnum.HTML);
    }
    public  AiCodeGeneratorService getAiCodeGeneratorService(Long appId, Long version, CodeGenTypeEnum type) {
        String cacheKey = buildCacheKey(appId, version, type);
        return serviceCache.get(cacheKey, k -> createAiCodeGeneratorService(appId, version, type));
    }
    public String buildCacheKey(Long appId, Long version, CodeGenTypeEnum type) {
        return appId + ":" + version + ":" + type;
    }
    public Flux<String> processCodeStream(Flux<String> flux,CodeGenTypeEnum type,Long appId,Long version){
        if(type == null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        StringBuilder str = new StringBuilder();
        return flux.doOnNext(str::append)
                .doFinally(signalType -> {
                    // 无论流是完成、错误还是取消，都执行保存
                    if (!signalType.equals(SignalType.CANCEL)) {
                        try{
                            String content = str.toString();
                            log.info("Code content length: {}", content.length());
                            Object exec = CodeParserExcutor.executeCode(content,type);
                            log.info("Code parsed successfully: {}", exec.getClass().getSimpleName());
                            File file = CodeFilleSaveExecutor.saveFile(exec,type,appId,version);
                            log.info("save file success: {}", file.getAbsolutePath());
                        }catch (Exception e){
                            log.error("save file failed: {}", e.getMessage(), e);
                            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to save code: " + e.getMessage());
                        }
                    } else {
                        log.warn("Stream cancelled, skipping save");
                    }
                });
    }
    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     *
     * @param tokenStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream) {
        return Flux.create(sink -> {
            tokenStream.onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    //todo 处理工具调用请求
//                    .onToolExecutionRequest((toolExecutionRequest) -> {
//                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
//                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
//                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        sink.complete();
                    })
                    .onError((Throwable error) -> {
                        error.printStackTrace();
                        sink.error(error);
                    })
                    .start();
        });
    }
    /**
     * 统一入口：根据类型生成并保存代码
     *
     * @param userMessage     用户提示�?
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目�?
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum,Long appId,Long version) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {

                HtmlCodeResult file = aiCodeGeneratorService.generateHtmlCode(appId,userMessage);
                yield CodeFilleSaveExecutor.saveFile(file,CodeGenTypeEnum.HTML,appId, version);

            }
            case MULTI_FILE -> {
                MultiFileCodeResult file = aiCodeGeneratorService.generateMultiFileCode(appId,userMessage);
                yield CodeFilleSaveExecutor.saveFile(file,CodeGenTypeEnum.MULTI_FILE,appId,version);
            }
            default -> {
                String errorMessage = "不支持的生成类型:" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum,Long appId,Long version) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }

        return switch (codeGenTypeEnum) {
            case VUE_PROJECT -> {
                Flux<String> flux = aiCodeGeneratorService.generateVueCodeStream(userMessage, appId, version);
                yield processCodeStream(flux, CodeGenTypeEnum.VUE_PROJECT, appId, version);
            }
            case HTML -> {
                Flux<String> flux = aiCodeGeneratorService.generateHtmlCodeStream(appId,userMessage);
                yield processCodeStream(flux,CodeGenTypeEnum.HTML,appId,version);
            }
            case MULTI_FILE -> {
                Flux<String> flux = aiCodeGeneratorService.generateMultiFileCodeStream(appId,userMessage);
                yield processCodeStream(flux,CodeGenTypeEnum.MULTI_FILE,appId,version);
            }
            default -> {
                String errorMessage = "不支持的生成类型:" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    public String generateAppName(String initPrompt) {
        return aiCodeGeneratorService.generateAppName(initPrompt);
    }

    public String summarizeAppChatHistoryMemory(String markdown, Long appId) {
        return aiCodeGeneratorService.summarizeAppChatHistoryMemory(markdown, appId);
    }

    public String getAppChatHistoryMemory(Long appId) {
        return aiCodeGeneratorService.getAppChatHistoryMemory(appId);
    }



}
