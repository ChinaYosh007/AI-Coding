package com.yosh.coding.agent.node;

import com.yosh.coding.agent.state.WorkflowContext;
import com.yosh.coding.agent.util.SpringContextUtil;
import com.yosh.coding.artificalIntelligence.config.AiCodeGenTypeRoutingServiceFactory;
import com.yosh.coding.artificalIntelligence.model.message.CodeGenTypeResult;
import com.yosh.model.enums.CodeGenTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class RouterNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 智能路由");

            CodeGenTypeEnum generationType;
            try {
                // 获取AI路由服务
                AiCodeGenTypeRoutingServiceFactory routingServiceFactory = SpringContextUtil.getBean(AiCodeGenTypeRoutingServiceFactory.class);
                // 根据原始提示词进行智能路由
                CodeGenTypeResult result = routingServiceFactory.createAiCodeGenTypeRoutingService()
                        .routeCodeGenType(context.getOriginalPrompt());
                generationType = CodeGenTypeEnum.valueOf(result.getCodeGenType());
                log.info("AI智能路由完成，选择类型: {} ({})", generationType.getValue(), generationType.getText());
            } catch (Exception e) {
                log.error("AI智能路由失败，使用默认HTML类型: {}", e.getMessage());
                generationType = CodeGenTypeEnum.HTML;
            }

            // 更新状态
            context.setCurrentStep("智能路由");
            context.setGenerationType(generationType);
            return WorkflowContext.saveContext(context);
        });
    }
}
