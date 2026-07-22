package com.yosh.coding.agent.subagent.resource;

import com.yosh.coding.agent.model.image.query.ResourceCollectionResult;
import com.yosh.coding.artificalIntelligence.guardrail.PromptSafetyInputGuardrail;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.guardrail.InputGuardrails;

public interface ResourceCollectionAgent {

    @SystemMessage(fromResource = "prompt/resource-collection-subagent-system-prompt.md")
    @InputGuardrails(PromptSafetyInputGuardrail.class)
    ResourceCollectionResult collectResources(@UserMessage String userPrompt);
}
