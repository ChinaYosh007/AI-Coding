package com.yosh.coding.agent.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface ImageCollectionService {


    @SystemMessage(fromResource = "prompt/image-collection-system-prompt.md")
    String searchImages(@UserMessage String userPrompt);
}
