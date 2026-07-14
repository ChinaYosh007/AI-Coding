package com.yosh.coding.core;

import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yosh.coding.agent.util.SpringContextUtil;
import com.yosh.coding.artificalIntelligence.AiCodeGeneratorService;
import com.yosh.coding.artificalIntelligence.model.HtmlCodeResult;
import com.yosh.coding.artificalIntelligence.model.MultiFileCodeResult;
import com.yosh.coding.artificalIntelligence.model.message.AiResponseMessage;
import com.yosh.coding.artificalIntelligence.model.message.ToolExecutedMessage;
import com.yosh.coding.artificalIntelligence.skill.*;
import com.yosh.coding.core.builder.VueProjectInitializer;
import com.yosh.coding.core.parser.CodeParserExcutor;
import com.yosh.coding.core.saver.CodeFilleSaveExecutor;
import com.yosh.coding.service.ChatHistoryService;
import com.yosh.exception.BusinessException;
import com.yosh.exception.ErrorCode;
import com.yosh.model.constants.AppConstant;
import com.yosh.model.enums.CodeGenTypeEnum;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecution;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.io.File;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 代码生成外观类，组合生成和保存功�?
 */
@Slf4j
@Service
public class AiCodeGeneratorFacade {
    private static final int MAX_VUE_TOOL_INVOCATIONS = 25;
    private static final Duration VUE_STREAM_TIMEOUT = Duration.ofMinutes(15);

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;
    @Autowired
    private ObjectProvider<ChatHistoryService> chatHistoryServiceProvider;
    @Resource(name = "openAiStreamingChatModel")
    private StreamingChatModel openAiStreamingChatModel;
    @Resource
    private VueProjectInitializer vueProjectInitializer;
    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;
    /**
     * 缓存内部
     * @param appId
     * @param version
     * @param type
     * @return
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
//            .expireAfterWrite(Duration.ofMinutes(30))  -->强制删除
            .expireAfterAccess(Duration.ofMinutes(30))
            .removalListener((key, value, cause) -> log.info("Cache removed key: {} with value: {}", key, value))
            .build();

    /**
     * 预加载tools
     * @param appId
     * @param version
     * @return
     */
    @Deprecated
    private List<Object> loadSkill(long appId, long version) {
        return List.of(
                new WriteToFile(appId, version),
                new DeleteFile(appId, version),
                new ModifyFile(appId, version),
                new ReadFile(appId, version),
                new ReadProjectDir(appId, version)
        );
    }

    private Map<ToolSpecification, ToolExecutor> loadSanitizedSkills(long appId, long version) {
        Map<ToolSpecification, ToolExecutor> tools = new LinkedHashMap<>();
        for (Object tool : loadSkill(appId, version)) {
            for (ToolSpecification specification : ToolSpecifications.toolSpecificationsFrom(tool)) {
                tools.put(specification, (request, memoryId) -> {
                    ToolExecutionRequest sanitizedRequest = ToolExecutionRequest.builder()
                            .id(request.id())
                            .name(request.name())
                            .arguments(ToolArgumentsJsonSanitizer.sanitize(request.arguments()))
                            .build();
                    return new DefaultToolExecutor(tool, sanitizedRequest).execute(sanitizedRequest, memoryId);
                });
            }
        }
        return tools;
    }

    public AiCodeGeneratorService createAiCodeGeneratorService(long appId, long version,CodeGenTypeEnum type, boolean isModify) {
        // 清理旧缓存
        redisChatMemoryStore.deleteMessages(appId);
        MessageWindowChatMemory messageWindowChatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .maxMessages(30)
                .chatMemoryStore(redisChatMemoryStore)
                .build();
        chatHistoryServiceProvider.getObject().loadHistoryMessage(messageWindowChatMemory, appId, 30L);

      return  switch (type){
            case VUE_PROJECT -> {
                var bean = SpringContextUtil.getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
                var toolCallingChatModel = SpringContextUtil.getBean("toolCallingChatModelPrototype", ChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(toolCallingChatModel)
                        .streamingChatModel(bean)
                        .tools(this.loadSanitizedSkills(appId, version))
                        .chatMemoryProvider(id -> messageWindowChatMemory)
                        .hallucinatedToolNameStrategy((request) -> dev.langchain4j.data.message.ToolExecutionResultMessage.from(request, "error: there no tool called " + request.name()))
                        .maxSequentialToolsInvocations(MAX_VUE_TOOL_INVOCATIONS)
                        .build();


            }
            case HTML, MULTI_FILE -> {

                var bean = SpringContextUtil.getBean("streamingChatModelPrototype", StreamingChatModel.class);
                var toolCallingChatModel = isModify
                        ? SpringContextUtil.getBean("toolCallingChatModelPrototype", ChatModel.class)
                        : chatModel;
                var builder = AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(toolCallingChatModel)
                        .streamingChatModel(bean)
                        .chatMemoryProvider(id -> messageWindowChatMemory)
                        .hallucinatedToolNameStrategy((request) -> dev.langchain4j.data.message.ToolExecutionResultMessage.from(request, "error: there no tool called " + request.name()))
                        .maxSequentialToolsInvocations(MAX_VUE_TOOL_INVOCATIONS);
                if (isModify) {
                    builder.tools(this.loadSanitizedSkills(appId, version));
                }
                yield builder.build();

            }
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的生成类型:" + type.getValue());
        };

    }



    public  AiCodeGeneratorService getAiCodeGeneratorService(Long appId,Long version){
        return this.getAiCodeGeneratorService(appId, version, CodeGenTypeEnum.HTML, false);
    }
    public  AiCodeGeneratorService getAiCodeGeneratorService(Long appId, Long version, CodeGenTypeEnum type, boolean isModify) {
        String cacheKey = buildCacheKey(appId, version, type, isModify);
        return serviceCache.get(cacheKey, k -> createAiCodeGeneratorService(appId, version, type, isModify));
    }
    public String buildCacheKey(Long appId, Long version, CodeGenTypeEnum type, boolean isModify) {
        return appId + ":" + version + ":" + type + ":" + isModify;
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
        return Flux.<String>create(sink -> {
            tokenStream.onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        try {
                            ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                            sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                        } catch (Exception e) {
                            log.error("处理工具执行结果失败: {}, 错误: {}", toolExecution.request().name(), e.getMessage());
                            // 发送一个错误消息而不是让流崩溃
                            AiResponseMessage errorMsg = new AiResponseMessage("\n\n[工具调用失败] " + e.getMessage() + "\n\n");
                            sink.next(JSONUtil.toJsonStr(errorMsg));
                        }
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        sink.complete();
                    })
                    .onError((Throwable error) -> {
                        log.error("AI 响应流错误: {}", error.getMessage(), error);
                        Throwable cause = error;
                        while (cause != null) {
                            if (cause instanceof com.fasterxml.jackson.core.JsonParseException) {
                                AiResponseMessage errorMsg = new AiResponseMessage("\n\n[AI 响应格式错误] 工具调用参数解析失败，请重试。\n\n");
                                sink.next(JSONUtil.toJsonStr(errorMsg));
                                sink.complete();
                                return;
                            }
                            cause = cause.getCause();
                        }
                        if (error.getMessage() != null && 
                            (error.getMessage().contains("JsonParseException") 
                             || error.getMessage().contains("JsonEOFException"))) {
                            AiResponseMessage errorMsg = new AiResponseMessage("\n\n[AI 响应格式错误] 工具调用参数解析失败，请重试。\n\n");
                            sink.next(JSONUtil.toJsonStr(errorMsg));
                            sink.complete();
                        } else {
                            sink.error(error);
                        }
                    })
                    .start();
        }).timeout(VUE_STREAM_TIMEOUT);
    }

    private Flux<String> processToolCallResponse(java.util.function.Supplier<String> responseSupplier) {
        return Flux.defer(() -> {
            String response = responseSupplier.get();
            if (response == null || response.isBlank()) {
                return Flux.empty();
            }
            return Flux.just(JSONUtil.toJsonStr(new AiResponseMessage(response)));
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
        AiCodeGeneratorService service = getAiCodeGeneratorService(appId, version, codeGenTypeEnum, false);
        return switch (codeGenTypeEnum) {
            case HTML -> {

                HtmlCodeResult file = service.generateHtmlCode(appId,userMessage);
                yield CodeFilleSaveExecutor.saveFile(file,CodeGenTypeEnum.HTML,appId, version);

            }
            case MULTI_FILE -> {
                MultiFileCodeResult file = service.generateMultiFileCode(appId,userMessage);
                yield CodeFilleSaveExecutor.saveFile(file,CodeGenTypeEnum.MULTI_FILE,appId,version);
            }
            default -> {
                String errorMessage = "不支持的生成类型:" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId, Long version, boolean isModify) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }

        AiCodeGeneratorService service = getAiCodeGeneratorService(appId, version, codeGenTypeEnum, isModify);
        //记忆存入redis

        return switch (codeGenTypeEnum) {
            case VUE_PROJECT -> {
                String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + AppConstant.VUE_PREFIX + appId + "_" + version;

                // 阶段一：同步复制模板文件（<1 秒），确保 AI 生成代码前模板已就位
                // 同步避免了与 WriteToFile 工具的竞态条件（初始化需删除旧目录再重建）
                if (!isModify) {
                    try {
                        log.info("开始复制 Vue 模板: {}", projectPath);
                        boolean copied = vueProjectInitializer.copyTemplate(projectPath);
                        if (!copied) {
                            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Vue 模板复制失败");
                        }
                        log.info("Vue 模板复制完成: {}", projectPath);
                    } catch (BusinessException e) {
                        throw e;
                    } catch (Exception e) {
                        log.error("Vue 模板复制失败: {}", e.getMessage(), e);
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Vue 模板复制失败: " + e.getMessage());
                    }
                }

                yield processToolCallResponse(() -> isModify
                        ? service.generateVueCodeModify(appId, userMessage)
                        : service.generateVueCode(appId, userMessage));
            }
            case HTML -> {
                if (isModify) {
                    yield processToolCallResponse(() -> service.generateHtmlCodeModify(appId, userMessage));
                } else {
                    Flux<String> flux = service.generateHtmlCodeStream(appId,userMessage);
                    yield processCodeStream(flux,CodeGenTypeEnum.HTML,appId,version);
                }
            }
            case MULTI_FILE -> {
                if (isModify) {
                    yield processToolCallResponse(() -> service.generateMultiFileCodeModify(appId, userMessage));
                } else {
                    Flux<String> flux = service.generateMultiFileCodeStream(appId,userMessage);
                    yield processCodeStream(flux,CodeGenTypeEnum.MULTI_FILE,appId,version);
                }
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


    public void clearAppMemory(Long appId, Long version,CodeGenTypeEnum type) {

        redisChatMemoryStore.deleteMessages(appId);
        //清理 caffeine
        String key = buildCacheKey(appId, version, type, false);
        serviceCache.invalidate(key);
    }
}
