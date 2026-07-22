package com.yosh.coding.agent.subagent.resource;

import com.yosh.coding.agent.model.image.enums.ImageCategoryEnum;
import com.yosh.coding.agent.model.image.query.ImageResource;
import com.yosh.coding.agent.model.image.query.ResourceCollectionResult;
import com.yosh.coding.agent.skills.ImageSearchSkill;
import com.yosh.coding.agent.skills.LogoGeneratorSkill;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ParallelResourceCollectionToolTest {

    @Test
    void keepsAvailableResourcesAndReportsOptionalSourceDegradation() {
        ImageSearchSkill imageSearchSkill = mock(ImageSearchSkill.class);
        LogoGeneratorSkill logoGeneratorSkill = mock(LogoGeneratorSkill.class);
        when(imageSearchSkill.searchImages("在线商城", 40))
                .thenReturn(resources("content", 40, ImageCategoryEnum.CONTENT));
        when(imageSearchSkill.searchIllustrations("在线商城", 9))
                .thenReturn(resources("illustration", 9, ImageCategoryEnum.ILLUSTRATION));
        when(logoGeneratorSkill.generateLogos("在线商城")).thenReturn(List.of());

        ParallelResourceCollectionTool tool = new ParallelResourceCollectionTool();
        ReflectionTestUtils.setField(tool, "imageSearchSkill", imageSearchSkill);
        ReflectionTestUtils.setField(tool, "logoGeneratorSkill", logoGeneratorSkill);
        ReflectionTestUtils.setField(tool, "resourceCollectionExecutor", (java.util.concurrent.Executor) Runnable::run);

        ResourceCollectionResult result = tool.collectVisualResources("在线商城");

        assertEquals(49, result.getResources().size());
        assertEquals(40, result.getResources().stream()
                .filter(resource -> resource.getImageCategory() == ImageCategoryEnum.CONTENT)
                .count());
        assertEquals(9, result.getResources().stream()
                .filter(resource -> resource.getImageCategory() == ImageCategoryEnum.ILLUSTRATION)
                .count());
        assertTrue(result.getWarnings().contains("logo collection returned no resources"));
        verify(imageSearchSkill).searchImages("在线商城", 40);
        verify(imageSearchSkill).searchIllustrations("在线商城", 9);
        verify(logoGeneratorSkill).generateLogos("在线商城");
    }

    private List<ImageResource> resources(String prefix, int count, ImageCategoryEnum category) {
        return IntStream.range(0, count)
                .mapToObj(index -> ImageResource.builder()
                        .imageCategory(category)
                        .description(prefix + index)
                        .imageUrl("https://example.com/" + prefix + index + ".jpg")
                        .build())
                .toList();
    }
}
