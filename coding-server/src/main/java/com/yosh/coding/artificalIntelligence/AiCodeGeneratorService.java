package com.yosh.coding.artificalIntelligence;

import com.yosh.coding.artificalIntelligence.model.HtmlCodeResult;
import com.yosh.coding.artificalIntelligence.model.MultiFileCodeResult;
import dev.langchain4j.service.SystemMessage;
import reactor.core.publisher.Flux;

public interface AiCodeGeneratorService {

    @SystemMessage(fromResource = "prompt/code-one-file-html.txt")
    HtmlCodeResult generateHtmlCode(String userMessage);


    @SystemMessage(fromResource = "prompt/code-multi-file-html.txt")
    MultiFileCodeResult generateMultiFileCode(String userMessage);
    /**
     * 生成 HTML 代码（流式）
     *
     * @param userMessage 用户消息
     * @return 生成的代码结果
     */
    @SystemMessage(fromResource = "prompt/code-one-file-html.txt")
    Flux<String> generateHtmlCodeStream(String userMessage);

    /**
     * 生成多文件代码（流式）
     *
     * @param userMessage 用户消息
     * @return 生成的代码结果
     */
    @SystemMessage(fromResource = "prompt/code-multi-file-html.txt")
    Flux<String> generateMultiFileCodeStream(String userMessage);

}
