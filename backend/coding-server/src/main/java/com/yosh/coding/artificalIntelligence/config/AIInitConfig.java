package com.yosh.coding.artificalIntelligence.config;

import com.yosh.coding.artificalIntelligence.AiCodeGeneratorService;
import com.yosh.coding.service.ChatHistoryService;
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
    @Resource
    private ChatHistoryService chatHistoryService;

    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService(){
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(dskChatModel)
                .streamingChatModel(dskStreamChatModel)
                .chatMemoryProvider(id -> {
                    MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                            .id(id)
                            .chatMemoryStore(redisChatMemoryStore)
                            .maxMessages(30)
                            .build();
                    // 从数据库加载历史到记忆窗口，确保默认 bean 也能访问对话历史
                    try {
                        chatHistoryService.loadHistoryMessage(memory, (Long) id, 30L);
                    } catch (Exception e) {
                        // 加载历史失败不影响正常使用
                    }
                    return memory;
                })
                .build();
    }
}