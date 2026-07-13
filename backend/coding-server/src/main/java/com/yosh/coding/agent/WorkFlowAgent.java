package com.yosh.coding.agent;

import com.yosh.coding.agent.node.ImageCollectionNode;
import com.yosh.coding.agent.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class WorkFlowAgent {
    static AsyncNodeAction<MessagesState<String>> makeNode(
            String nodeName
    ) {
        return node_async(state -> {
            WorkflowContext context =
                    WorkflowContext.getContext(state);

            log.info("执行节点：{}", nodeName);

            context.setCurrentStep(nodeName);
            return WorkflowContext.saveContext(context);
        });
    }
    public static CompiledGraph<MessagesState<String>>
    createWorkflow() throws GraphStateException {

        return new MessagesStateGraph<String>()
                .addNode(
                        "image_collector",
                        ImageCollectionNode.create()
                )
                .addEdge(START, "image_collector")
                .addEdge("image_collector", END)
                .compile();
    }
    public static WorkflowContext executeWorkflow(
            String originalPrompt
    ) throws GraphStateException {

        // 1. 创建并编译工作流
        CompiledGraph<MessagesState<String>> workflow =
                createWorkflow();

        // 2. 创建工作流的初始数据
        WorkflowContext initialContext =
                WorkflowContext.builder()
                        .originalPrompt(originalPrompt)
                        .currentStep("初始化")
                        .build();

        // 3. 把 WorkflowContext 放入初始状态
        Map<String, Object> initialState = Map.of(
                WorkflowContext.WORKFLOW_CONTEXT_KEY,
                initialContext
        );

        // 4. 保存最后一次执行得到的 Context
        WorkflowContext result = initialContext;

        // 5. 执行工作流
        for (NodeOutput<MessagesState<String>> step
                : workflow.stream(initialState)) {

            WorkflowContext currentContext =
                    WorkflowContext.getContext(step.state());

            if (currentContext != null) {
                result = currentContext;
                log.info(
                        "当前步骤：{}",
                        currentContext.getCurrentStep()
                );
            }
        }

        // 6. 返回执行结束后的状态
        return result;
    }
}
