package com.yosh.coding.artificalIntelligence.config;

import dev.langchain4j.http.client.spring.restclient.SpringRestClient;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestClient;

@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.streaming-chat-model")
@Data
public class StreamingChatModelConfig {

    private String baseUrl;

    private String apiKey;

    private String modelName;

    private Integer maxTokens;

    private Double temperature;

    private boolean logRequests;

    private boolean logResponses;

    private boolean thinkingEnabled;

    @Bean
    @Scope("prototype")
    public StreamingChatModel streamingChatModelPrototype() {
        var builder = OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .logRequests(logRequests)
                .logResponses(logResponses);
        if (modelName != null && modelName.startsWith("deepseek-v4")) {
            var restClientBuilder = RestClient.builder()
                    .requestInterceptor(new DeepSeekThinkingInterceptor(thinkingEnabled));
            builder.httpClientBuilder(SpringRestClient.builder().restClientBuilder(restClientBuilder));
        }
        return builder.build();
    }
}
