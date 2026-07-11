package com.yosh.coding.artificalIntelligence;

import com.yosh.coding.artificalIntelligence.model.HtmlCodeResult;
import com.yosh.coding.artificalIntelligence.model.MultiFileCodeResult;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

public interface AiCodeGeneratorService {



    @SystemMessage(fromResource = "prompt/code-one-file-html.txt")
    HtmlCodeResult generateHtmlCode(@MemoryId Long memoryId, @UserMessage String userMessage);


    @SystemMessage(fromResource = "prompt/code-multi-file-html.txt")
    MultiFileCodeResult generateMultiFileCode(@MemoryId Long memoryId, @UserMessage String userMessage);
    /**
     * 生成 HTML 代码（流式）
     *
     * @param userMessage 用户消息
     * @return 生成的代码结果
     */
    @SystemMessage(fromResource = "prompt/code-one-file-html.txt")
    Flux<String> generateHtmlCodeStream(@MemoryId Long memoryId, @UserMessage String userMessage);

    /**
     * 生成多文件代码（流式）
     *
     * @param userMessage 用户消息
     * @return 生成的代码结果
     */
    @SystemMessage(fromResource = "prompt/code-multi-file-html.txt")
    Flux<String> generateMultiFileCodeStream(@MemoryId Long memoryId, @UserMessage String userMessage);

    @SystemMessage("根据初始化提示生成6个字应用名称，以JSON格式返回，例如：{\"appName\": \"示例应用\"}")
    String generateAppName(String initPrompt);
    @SystemMessage("总结应用聊天历史记录，以JSON格式返回，例如：{\"summary\": \"用户询问了如何创建一个TODO应用，开发人员提供了详细步骤和代码示例。\"}")
    String summarizeAppChatHistoryMemory(String markdown, Long appId);
    @SystemMessage("根据应用ID获取聊天历史记录，以JSON格式返回，例如：{\"chatHistory\": \"用户询问了如何创建一个TODO应用，开发人员提供了详细步骤和代码示例。\"}")
    String getAppChatHistoryMemory(Long appId);

    @SystemMessage(fromResource = "prompt/vue-multi-file-html.txt")
    TokenStream generateVueCodeStream(@MemoryId Long appId, @UserMessage String userMessage);
}
