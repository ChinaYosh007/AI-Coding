package com.yosh.coding.core;

import com.yosh.coding.agent.model.image.enums.ImageCategoryEnum;
import com.yosh.coding.agent.model.image.query.ImageResource;
import com.yosh.coding.agent.model.image.query.ResourceCollectionResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourcePromptAssemblerTest {

    @Test
    void includesAvailableUrlsAndDegradedSourceWarnings() {
        ImageResource image = ImageResource.builder()
                .imageCategory(ImageCategoryEnum.CONTENT)
                .description("商城商品图")
                .imageUrl("https://example.com/product.jpg")
                .build();
        ResourceCollectionResult result = new ResourceCollectionResult(
                List.of(image),
                List.of("Use product image in the product grid"),
                List.of("logo collection returned no resources"));

        String prompt = new ResourcePromptAssembler().assemble("生成商城", result);

        assertTrue(prompt.contains("https://example.com/product.jpg"));
        assertTrue(prompt.contains("logo collection returned no resources"));
        assertTrue(prompt.contains("use collected URLs or CSS fallback instead"));
    }
}
