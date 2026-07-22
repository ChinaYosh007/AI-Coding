package com.yosh.coding.agent.subagent.resource;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ResourceCollectionAgentFactory {

    @Resource
    @Lazy
    private ParallelResourceCollectionTool parallelResourceCollectionTool;

    @Bean(name = "resourceCollectionExecutor", destroyMethod = "close")
    public ExecutorService resourceCollectionExecutor() {
        return Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("resource-collection-", 0).factory());
    }

    @Bean(name = "resourceCollectionAgent")
    public ResourceCollectionAgent resourceCollectionAgent() {
        // 资源流程是固定的：无需先让模型决定是否调用工具，避免一次额外的模型请求成为超时点。
        // 保留 ResourceCollectionAgent 作为独立子代理边界，内部直接调用并行资源工具即可。
        return parallelResourceCollectionTool::collectVisualResources;
    }
}
