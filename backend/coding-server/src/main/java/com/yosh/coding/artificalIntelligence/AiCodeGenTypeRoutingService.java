package com.yosh.coding.artificalIntelligence;

import com.yosh.coding.artificalIntelligence.model.message.CodeGenTypeResult;
import dev.langchain4j.service.SystemMessage;

/**
 * AI代码生成类型智能路由服务
 * 使用结构化输出返回JSON结果，由调用方解析为枚举
 *
 * @author yupi
 */
public interface AiCodeGenTypeRoutingService {

    /**
     * 根据用户需求智能选择代码生成类型
     *
     * @param userPrompt 用户输入的需求描述
     * @return 推荐的代码生成类型结果
     */
    @SystemMessage(fromResource = "prompt/generater-code-type-router.md")
    CodeGenTypeResult routeCodeGenType(String userPrompt);
}
