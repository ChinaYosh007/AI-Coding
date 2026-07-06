package com.yosh.coding.artificalIntelligence.config;

import com.yosh.coding.artificalIntelligence.AiCodeGeneratorService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIInitConfig {
    @Resource(name = "openAiChatModel")
    private ChatModel dskChatModel;
    @Resource(name = "openAiStreamingChatModel")
    private StreamingChatModel dskStreamChatModel;
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService(){
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(dskChatModel)
                .streamingChatModel(dskStreamChatModel)
                .chatMemoryProvider(id -> MessageWindowChatMemory.builder()
                                .id(id)
                        .chatMemoryStore(redisChatMemoryStore)
                        .maxMessages(20)
                        .build()
                )
                .build();
    }
    /**
     * 默认提供一�?Bean
     */


}
