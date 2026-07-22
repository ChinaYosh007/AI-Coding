package com.yosh.coding.core;

import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yosh.coding.agent.model.image.query.ImageResource;
import com.yosh.coding.agent.model.image.query.ResourceCollectionResult;
import com.yosh.coding.agent.subagent.resource.ResourceCollectionAgent;
import com.yosh.coding.agent.util.SpringContextUtil;
import com.yosh.coding.artificalIntelligence.AiCodeGeneratorService;
import com.yosh.coding.artificalIntelligence.guardrail.RetryOutputGuardrail;
import com.yosh.coding.artificalIntelligence.model.HtmlCodeResult;
import com.yosh.coding.artificalIntelligence.model.MultiFileCodeResult;
import com.yosh.coding.artificalIntelligence.model.message.AiResponseMessage;
import com.yosh.coding.artificalIntelligence.model.message.ResourceCollectionProgressMessage;
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
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.GuardrailException;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecution;
import dev.langchain4j.service.tool.ToolExecutor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.net.SocketException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

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
    @Resource
    private ObjectProvider<ChatHistoryService> chatHistoryServiceProvider;
    @Resource(name = "openAiStreamingChatModel")
    private StreamingChatModel openAiStreamingChatModel;
    @Resource
    private VueProjectInitializer vueProjectInitializer;
    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;
    @Resource(name = "resourceCollectionAgent")
    private ResourceCollectionAgent resourceCollectionAgent;
    @Resource
    private ResourcePromptAssembler resourcePromptAssembler;
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
     * 预加载tools,这里把图片相关操作也进行预加载，这样对于图片处理会比较方便一点
     * @param appId
     * @param version
     * @return
     */

    private List<Object> loadSkill(long appId, long version, List<String> resourceUrls) {
        return List.of(
                new WriteToFile(appId, version, resourceUrls),
                new DeleteFile(appId, version),
                new ModifyFile(appId, version),
                new ReadFile(appId, version),
                new ReadProjectDir(appId, version),
                new ExitTool()
        );
    }

    private Map<ToolSpecification, ToolExecutor> loadSanitizedSkills(long appId, long version, List<String> resourceUrls) {
        Map<ToolSpecification, ToolExecutor> tools = new LinkedHashMap<>();
        for (Object tool : loadSkill(appId, version, resourceUrls)) {
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
        return createAiCodeGeneratorService(appId, version, type, isModify, List.of());
    }

    private AiCodeGeneratorService createAiCodeGeneratorService(
            long appId, long version, CodeGenTypeEnum type, boolean isModify, List<String> resourceUrls) {
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
                        .tools(this.loadSanitizedSkills(appId, version, resourceUrls))
                        .chatMemoryProvider(id -> messageWindowChatMemory)
                        .hallucinatedToolNameStrategy((request) -> ToolExecutionResultMessage.from(request, "error: there no tool called " + request.name()))
                        .maxSequentialToolsInvocations(MAX_VUE_TOOL_INVOCATIONS)
                        .outputGuardrails(new RetryOutputGuardrail())
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
                        .hallucinatedToolNameStrategy((request) -> ToolExecutionResultMessage.from(request, "error: there no tool called " + request.name()))
                        .maxSequentialToolsInvocations(MAX_VUE_TOOL_INVOCATIONS);
                if (isModify) {
                    builder.tools(this.loadSanitizedSkills(appId, version, resourceUrls));
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
        return getAiCodeGeneratorService(appId, version, type, isModify, List.of());
    }

    private AiCodeGeneratorService getAiCodeGeneratorService(
            Long appId, Long version, CodeGenTypeEnum type, boolean isModify, List<String> resourceUrls) {
        String cacheKey = buildCacheKey(appId, version, type, isModify) + ":" + resourceUrls.hashCode();
        return serviceCache.get(cacheKey, k -> createAiCodeGeneratorService(appId, version, type, isModify, resourceUrls));
    }
    public String buildCacheKey(Long appId, Long version, CodeGenTypeEnum type, boolean isModify) {
        return appId + ":" + version + ":" + type + ":" + isModify;
    }

    public Flux<String> processCodeStream(Flux<String> flux,CodeGenTypeEnum type,Long appId,Long version){
        return processCodeStream(flux, type, appId, version, List.of());
    }

    private Flux<String> processCodeStream(
            Flux<String> flux, CodeGenTypeEnum type, Long appId, Long version, List<String> resourceUrls) {
        if(type == null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        StringBuilder str = new StringBuilder();
        return flux.doOnNext(str::append)
                // 保存是响应成功完成的一部分。concatWith 中的异常会继续向 SSE 下游传播，
                // 避免 doFinally 在流已经完成后抛错却仍向前端发送 done。
                .concatWith(Flux.defer(() -> {
                    parseAndSaveCode(str.toString(), type, appId, version, resourceUrls);
                    return Flux.empty();
                }))
                .doOnCancel(() -> log.warn("Stream cancelled, skipping save"));
    }

    private void parseAndSaveCode(
            String content, CodeGenTypeEnum type, Long appId, Long version, List<String> resourceUrls) {
        try {
            log.info("Code content length: {}", content.length());
            Object parsedCode = CodeParserExcutor.executeCode(content, type);
            log.info("Code parsed successfully: {}", parsedCode.getClass().getSimpleName());
            File file = CodeFilleSaveExecutor.saveFile(parsedCode, type, appId, version, resourceUrls);
            log.info("save file success: {}", file.getAbsolutePath());
        } catch (BusinessException e) {
            log.warn("generated code validation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("save file failed: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码生成结果保存失败: " + e.getMessage());
        }
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
    Flux<String> generateAndSaveCodeStreamInternal(
            String userMessage,
            CodeGenTypeEnum codeGenTypeEnum,
            Long appId,
            Long version,
            boolean isModify,
            List<String> resourceUrls) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }

        AiCodeGeneratorService service = getAiCodeGeneratorService(
                appId, version, codeGenTypeEnum, isModify, resourceUrls);
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

                TokenStream tokenStream = isModify ? service.generateVueCodeModifyStream(appId, userMessage)
                        : service.generateVueCodeStream(appId, userMessage);
                yield processTokenStream(tokenStream);
            }
            case HTML -> {
                if (isModify) {
                    yield processToolCallResponse(() -> service.generateHtmlCodeModify(appId, userMessage));
                } else {
                    Flux<String> flux = retryInitialConnectionFailure(
                            () -> service.generateHtmlCodeStream(appId, userMessage));
                    yield processCodeStream(flux,CodeGenTypeEnum.HTML,appId,version,resourceUrls);
                }
            }
            case MULTI_FILE -> {
                if (isModify) {
                    yield processToolCallResponse(() -> service.generateMultiFileCodeModify(appId, userMessage));
                } else {
                    Flux<String> flux = retryInitialConnectionFailure(
                            () -> service.generateMultiFileCodeStream(appId, userMessage));
                    yield processCodeStream(flux,CodeGenTypeEnum.MULTI_FILE,appId,version,resourceUrls);
                }
            }
            default -> {
                String errorMessage = "不支持的生成类型:" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };

    }

    Flux<String> retryInitialConnectionFailure(Supplier<Flux<String>> streamSupplier) {
        return Flux.defer(() -> {
            AtomicBoolean responseStarted = new AtomicBoolean(false);
            return Flux.defer(streamSupplier)
                    .doOnNext(ignored -> responseStarted.set(true))
                    .onErrorResume(error -> {
                        if (!responseStarted.get() && isTransientConnectionFailure(error)) {
                            log.warn("AI 流在首个响应片段前断开，将重试一次: {}", conciseMessage(error));
                            return Flux.defer(streamSupplier);
                        }
                        return Flux.error(error);
                    });
        });
    }

    private boolean isTransientConnectionFailure(Throwable error) {
        Throwable cause = error;
        while (cause != null) {
            if (cause instanceof ResourceAccessException || cause instanceof SocketException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    public Flux<String> generateAndSaveCodeStream(
            String userMessage,
            CodeGenTypeEnum codeGenTypeEnum,
            Long appId,
            Long version,
            boolean isModify) {

        // 修改模式已经复制了上一版本，并通过读写文件工具做增量修改，
        // 不需要重新搜集资源，也不能把 isModify 错误地降级为首次生成。
        if (isModify) {
            log.info("修改模式跳过资源收集: appId={}, version={}", appId, version);
            return generateAndSaveCodeStreamInternal(
                    userMessage, codeGenTypeEnum, appId, version, true, List.of());
        }

        return Flux.concat(
                Flux.just(resourceCollectionProgress("正在并行搜集图片、插画和 Logo…")),
                Mono.fromCallable(() -> resourceCollectionAgent.collectResources(userMessage))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(result -> new ResourceCollectionOutcome(result, false))
                        // 降级边界只能覆盖资源收集本身，不能吞掉后续代码生成流的异常。
                        .onErrorResume(error -> {
                            if (error instanceof GuardrailException) {
                                return Mono.error(error);
                            }
                            log.warn("资源收集失败，使用原始提示词生成: {}", conciseMessage(error));
                            return Mono.just(new ResourceCollectionOutcome(null, true));
                        })
                        .flatMapMany(outcome -> {
                    if (outcome.collectionFailed()) {
                        return Flux.concat(
                                Flux.just(resourceCollectionProgress("资源服务暂时不可用，将继续生成代码…")),
                                generateAndSaveCodeStreamInternal(
                                        userMessage, codeGenTypeEnum, appId, version, false, List.of()));
                    }
                    ResourceCollectionResult result = outcome.result();
                    int resourceCount = result == null || result.getResources() == null
                            ? 0 : result.getResources().size();
                    int warningCount = result == null || result.getWarnings() == null
                            ? 0 : result.getWarnings().size();
                    List<String> resourceUrls = extractResourceUrls(result);
                    String prompt = resourcePromptAssembler.assemble(userMessage, result);
                    String warningText = warningCount > 0
                            ? "，" + warningCount + " 个资源来源已自动降级"
                            : "";
                    return Flux.concat(
                            Flux.just(resourceCollectionProgress(
                                    "资源收集完成，已找到 " + resourceCount + " 个可用资源"
                                            + warningText + "，开始生成代码…")),
                            generateAndSaveCodeStreamInternal(
                                    prompt, codeGenTypeEnum, appId, version, false, resourceUrls));
                }));
    }

    private String conciseMessage(Throwable error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            return error.getClass().getSimpleName();
        }
        String normalized = message.replaceAll("\\s+", " ");
        return normalized.length() > 240 ? normalized.substring(0, 240) : normalized;
    }

    private record ResourceCollectionOutcome(ResourceCollectionResult result, boolean collectionFailed) {
    }

    private List<String> extractResourceUrls(ResourceCollectionResult result) {
        if (result == null || result.getResources() == null) {
            return List.of();
        }
        return result.getResources().stream()
                .map(ImageResource::getImageUrl)
                .filter(url -> url != null && url.startsWith("http"))
                .distinct()
                .toList();
    }

    private String resourceCollectionProgress(String message) {
        return JSONUtil.toJsonStr(new ResourceCollectionProgressMessage(message));
    }

    public String generateAppName(String initPrompt) {
        String systemPrompt = """
                Generate a concise Chinese application title based on the user's requirement.
                You must output one valid JSON object and nothing else.
                The JSON format must be exactly: {"appName":"示例应用"}
                The appName value must contain 2 to 16 Chinese characters and must not contain HTML, source code, Markdown, quotes, or explanations.
                """;
        for (int attempt = 0; attempt < 2; attempt++) {
            String response = chatModel.chat(
                            SystemMessage.from(systemPrompt),
                            UserMessage.from(initPrompt + "\nReturn the result in JSON format."))
                    .aiMessage()
                    .text();
            if (response != null && !response.isBlank()) {
                return response;
            }
        }
        return null;
    }

    public String summarizeAppChatHistoryMemory(String markdown, Long appId) {
        return aiCodeGeneratorService.summarizeAppChatHistoryMemory(markdown, appId);
    }

    public void clearAppMemory(Long appId, Long version,CodeGenTypeEnum type) {

        redisChatMemoryStore.deleteMessages(appId);
        //清理 caffeine
        String key = buildCacheKey(appId, version, type, false);
        serviceCache.invalidate(key);
    }
}
