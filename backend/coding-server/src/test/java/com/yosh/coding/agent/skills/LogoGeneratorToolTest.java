package com.yosh.coding.agent.skills;

import com.yosh.coding.agent.model.image.enums.ImageCategoryEnum;
import com.yosh.coding.agent.model.image.query.ImageResource;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertEquals;

@SpringBootTest
class LogoGeneratorToolTest {

    @Resource
    private LogoGeneratorSkill logoGeneratorSkill;

    @Test
    void testGenerateLogos() {
        // 测试生成Logo
        List<ImageResource> logos = logoGeneratorSkill.generateLogos("技术公司现代简约风格Logo");
        assertNotNull(logos);
        ImageResource firstLogo = logos.getFirst();
        assertEquals(ImageCategoryEnum.LOGO, firstLogo.getImageCategory());
        assertNotNull(firstLogo.getDescription());
        assertNotNull(firstLogo.getImageUrl());
        logos.forEach(logo ->
                System.out.println("Logo: " + logo.getDescription() + " - " + logo.getImageUrl())
        );
    }

    private void assertEquals(ImageCategoryEnum imageCategoryEnum, ImageCategoryEnum imageCategory) {
        if (imageCategoryEnum != imageCategory) {
            throw new AssertionError("Expected " + imageCategoryEnum + " but was " + imageCategory);
        }
    }
}
