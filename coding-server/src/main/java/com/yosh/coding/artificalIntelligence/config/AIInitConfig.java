package com.yosh.coding.artificalIntelligence.config;

import com.yosh.coding.artificalIntelligence.AiCodeGeneratorService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIInitConfig {
    @Resource
    private ChatModel dskChatModel;
    @Resource
    private StreamingChatModel dskStreamChatModel;
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService(){
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(dskChatModel)
                .streamingChatModel(dskStreamChatModel)
                .build();
    }

}
