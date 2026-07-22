package com.yosh.coding.agent.subagent.resource;

import com.yosh.coding.agent.model.image.query.ImageResource;
import com.yosh.coding.agent.model.image.query.ResourceCollectionResult;
import com.yosh.coding.agent.skills.ImageSearchSkill;
import com.yosh.coding.agent.skills.LogoGeneratorSkill;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
public class ParallelResourceCollectionTool {

    private static final int CONTENT_IMAGE_COUNT = 40;
    private static final int ILLUSTRATION_COUNT = 9;
    private static final int MAX_RETURNED_RESOURCES = 50;
    private static final long RESOURCE_TASK_TIMEOUT_SECONDS = 15;

    @Resource
    private ImageSearchSkill imageSearchSkill;

    @Resource
    private LogoGeneratorSkill logoGeneratorSkill;

    @Resource(name = "resourceCollectionExecutor")
    private Executor resourceCollectionExecutor;

    @Tool("Collects up to 50 website visual resources in parallel. It returns only real tool results.")
    public ResourceCollectionResult collectVisualResources(
            @P("The original website requirement") String userPrompt) {

        CompletableFuture<TaskResult> contentImages = executeAsync(
                "content images",
                () -> imageSearchSkill.searchImages(userPrompt, CONTENT_IMAGE_COUNT));
        CompletableFuture<TaskResult> illustrations = executeAsync(
                "illustrations",
                () -> imageSearchSkill.searchIllustrations(userPrompt, ILLUSTRATION_COUNT));
        CompletableFuture<TaskResult> logos = executeAsync(
                "logo",
                () -> logoGeneratorSkill.generateLogos(userPrompt));

        CompletableFuture.allOf(contentImages, illustrations, logos).join();

        List<TaskResult> taskResults = List.of(
                contentImages.join(),
                illustrations.join(),
                logos.join());

        List<ImageResource> resources = mergeResources(taskResults);
        List<String> warnings = taskResults.stream()
                .map(TaskResult::warning)
                .filter(warning -> warning != null && !warning.isBlank())
                .toList();

        return new ResourceCollectionResult(
                resources,
                buildUsageSuggestions(resources),
                warnings);
    }

    private CompletableFuture<TaskResult> executeAsync(
            String taskName,
            Supplier<List<ImageResource>> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<ImageResource> resources = task.get();
                if (resources == null || resources.isEmpty()) {
                    String warning = taskName + " collection returned no resources";
                    log.warn(warning);
                    return new TaskResult(List.of(), warning);
                }
                return new TaskResult(resources, null);
            } catch (Exception e) {
                String warning = taskName + " collection failed: " + conciseMessage(e);
                log.warn(warning);
                return new TaskResult(List.of(), warning);
            }
        }, resourceCollectionExecutor).completeOnTimeout(
                new TaskResult(List.of(), taskName + " collection timed out"),
                RESOURCE_TASK_TIMEOUT_SECONDS,
                TimeUnit.SECONDS);
    }

    private String conciseMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        String normalized = message.replaceAll("\\s+", " ");
        return normalized.length() > 240 ? normalized.substring(0, 240) : normalized;
    }

    private List<ImageResource> mergeResources(Collection<TaskResult> taskResults) {
        Map<String, ImageResource> resourcesByUrl = new LinkedHashMap<>();
        taskResults.stream()
                .map(TaskResult::resources)
                .flatMap(Collection::stream)
                .filter(resource -> resource != null && resource.getImageUrl() != null && !resource.getImageUrl().isBlank())
                .forEach(resource -> resourcesByUrl.putIfAbsent(resource.getImageUrl(), resource));

        return resourcesByUrl.values().stream()
                .sorted(Comparator.comparing(resource -> resource.getImageCategory() == null ? "" : resource.getImageCategory().name()))
                .limit(MAX_RETURNED_RESOURCES)
                .toList();
    }

    private List<String> buildUsageSuggestions(List<ImageResource> resources) {
        List<String> suggestions = new ArrayList<>();
        resources.forEach(resource -> {
            if (resource.getImageCategory() != null) {
                suggestions.add("Use " + resource.getImageCategory().getValue()
                        + " resource for " + resource.getDescription());
            }
        });
        return suggestions;
    }

    private record TaskResult(List<ImageResource> resources, String warning) {
    }
}
