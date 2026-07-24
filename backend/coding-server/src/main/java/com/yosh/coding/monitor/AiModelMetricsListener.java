package com.yosh.coding.monitor;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
@Component
public class AiModelMetricsListener implements ChatModelListener {

    private static final String REQUEST_START_TIME = "requestStartTimestamp";
    private static final String RESPONSE_CONTEXT_KEY = "responseContext";

    @Resource
    private AiModelMetricsCollector aiModelMetricsCollector;
    @Override
    public void onRequest(ChatModelRequestContext context) {

        context.attributes().put(REQUEST_START_TIME, Instant.now());
        MonitorContext res = MonitorContextHolder.getContext();
        context.attributes().put(RESPONSE_CONTEXT_KEY, res);
        String model = context.chatRequest().modelName();
        if (res != null) {
            aiModelMetricsCollector.recordRequest(res.getUserId(), res.getAppId(),res.getVersion(), model, MontiorEnum.STARTED);
        }
    }
    @Override
    public void onResponse(ChatModelResponseContext context) {
        Map<Object, Object> attributes = context.attributes();
        MonitorContext data = (MonitorContext) attributes.get(RESPONSE_CONTEXT_KEY);
        String model = context.chatResponse().modelName();
        if (data != null) {
            aiModelMetricsCollector.recordRequest(data.getUserId(), data.getAppId(),data.getVersion(), model, MontiorEnum.SUCCESS);
            recordResponseTime(attributes, data.getUserId(), data.getAppId(), data.getVersion(), model);
            recordTokenUsage(context, data.getUserId(), data.getAppId(), data.getVersion(), model);
        }
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        // 从监控上下文中获取信息
        MonitorContext context = MonitorContextHolder.getContext();
        // 获取模型名称和错误类型
        String modelName = errorContext.chatRequest().modelName();
        String errorMessage = errorContext.error().getMessage();
        if (context != null) {
            // 记录失败请求
            aiModelMetricsCollector.recordRequest(context.getUserId(), context.getAppId(), context.getVersion(), modelName, MontiorEnum.ERROR);
            aiModelMetricsCollector.recordError(context.getUserId(), context.getAppId(), context.getVersion(), modelName, errorMessage);
            // 记录响应时间（即使是错误响应）
            Map<Object, Object> attributes = errorContext.attributes();
            recordResponseTime(attributes, context.getUserId(), context.getAppId(), context.getVersion(), modelName);
        }
    }

    /**
     * 记录响应时间
     */
    private void recordResponseTime(Map<Object, Object> attributes, String userId, String appId, String version, String modelName) {
        Instant startTime = (Instant) attributes.get(REQUEST_START_TIME);
        Duration responseTime = Duration.between(startTime, Instant.now());
        aiModelMetricsCollector.recordResponseTime(userId, appId, version, modelName, responseTime);

    }

    /**
     * 记录Token使用情况
     */
    private void recordTokenUsage(ChatModelResponseContext responseContext, String userId, String appId,String version, String modelName) {
        TokenUsage tokenUsage = responseContext.chatResponse().metadata().tokenUsage();
        if (tokenUsage != null) {
            aiModelMetricsCollector.recordTokenUsage(userId, appId, version, modelName, "input", tokenUsage.inputTokenCount());
            aiModelMetricsCollector.recordTokenUsage(userId, appId, version, modelName, "output", tokenUsage.outputTokenCount());
            aiModelMetricsCollector.recordTokenUsage(userId, appId, version, modelName, "total", tokenUsage.totalTokenCount());
        }
    }
}
