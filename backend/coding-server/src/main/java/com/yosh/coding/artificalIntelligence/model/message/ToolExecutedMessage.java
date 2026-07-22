package com.yosh.coding.artificalIntelligence.model.message;

import com.yosh.model.enums.StreamMessageTypeEnum;
import com.yosh.coding.core.saver.PlaceholderImageUrlSanitizer;
import dev.langchain4j.service.tool.ToolExecution;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 工具执行结果消息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ToolExecutedMessage extends StreamMessage {

    private String id;

    private String name;

    private String arguments;

    private String result;

    public ToolExecutedMessage(ToolExecution toolExecution) {
        super(StreamMessageTypeEnum.TOOL_EXECUTED.getValue());
        this.id = toolExecution.request().id();
        this.name = toolExecution.request().name();
        this.arguments = "writeToFile".equals(toolExecution.request().name())
                ? PlaceholderImageUrlSanitizer.sanitize(toolExecution.request().arguments())
                : toolExecution.request().arguments();
        this.result = toolExecution.result();
    }
}
