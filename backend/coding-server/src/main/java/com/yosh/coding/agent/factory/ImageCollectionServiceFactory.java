package com.yosh.coding.agent.factory;

import com.yosh.coding.agent.ai.ImageCollectionService;
import com.yosh.coding.agent.skills.ImageSearchSkill;
import com.yosh.coding.agent.skills.LogoGeneratorSkill;
import com.yosh.coding.agent.skills.MermaidDiagramSkill;
import com.yosh.coding.agent.skills.UndrawIllustrationSkill;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ImageCollectionServiceFactory {

    @Value("${AGENT_URL}")
    private String baseUrl;

    @Value("${API_KEY}")
    private String apiKey;

    @Value("${CHAT_MODEL}")
    private String modelName;

    @Resource
    private ImageSearchSkill imageSearchSkill;

    @Resource
    private LogoGeneratorSkill logoGeneratorSkill;
    @Resource
    private MermaidDiagramSkill mermaidDiagramSkill;
    @Resource
    private UndrawIllustrationSkill undrawIllustrationSkill;

    @Bean
    public ImageCollectionService imageCollectionService() {
        OpenAiChatModel toolCallChatModel = OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(8192)
                .logRequests(true)
                .logResponses(true)
                .maxRetries(3)
                .build();

        return AiServices.builder(ImageCollectionService.class)
                .chatModel(toolCallChatModel)
                .tools(imageSearchSkill,logoGeneratorSkill, mermaidDiagramSkill, undrawIllustrationSkill)
                .maxSequentialToolsInvocations(10)
                .build();
    }
}
