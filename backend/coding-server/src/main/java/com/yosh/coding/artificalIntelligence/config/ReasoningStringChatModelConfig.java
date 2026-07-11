package com.yosh.coding.artificalIntelligence.config;

import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "language.open-ai.string.chat-model")
@Data
public class ReasoningStringChatModelConfig {
    private String baseUrl;
    private String apiKey;
    private  String model = "qwen-turbo";
    private final int maxTokens = 8192;

    /**
     * vue
     * @return
     */
    @Bean(name = "reasoningStreamingChatModel")
    @ConditionalOnProperty(prefix = "language.open-ai.string.chat-model", name = "enabled", havingValue = "true")
    public OpenAiStreamingChatModel reasoningStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .maxTokens(maxTokens)
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
