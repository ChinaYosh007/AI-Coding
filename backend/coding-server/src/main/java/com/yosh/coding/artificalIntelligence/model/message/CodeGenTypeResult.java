package com.yosh.coding.artificalIntelligence.model.message;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
@Description("代码生成类型路由结果")
public class CodeGenTypeResult {

    @Description("代码生成类型枚举名称：HTML、MULTI_FILE、VUE_PROJECT")
    private String codeGenType;
}
