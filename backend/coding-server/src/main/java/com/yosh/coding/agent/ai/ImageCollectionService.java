package com.yosh.coding.agent.ai;

import com.yosh.coding.agent.model.image.query.ResourceCollectionResult;
import com.yosh.coding.artificalIntelligence.guardrail.PromptSafetyInputGuardrail;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.guardrail.InputGuardrails;

public interface ImageCollectionService {


    @SystemMessage(fromResource = "prompt/resource-collection-system-prompt.md")
    @InputGuardrails(PromptSafetyInputGuardrail.class)
    ResourceCollectionResult collectResources(@UserMessage String userPrompt);

    @SystemMessage(fromResource = "prompt/image-collection-system-prompt.md")
    String searchImages(@UserMessage String userPrompt);
}
