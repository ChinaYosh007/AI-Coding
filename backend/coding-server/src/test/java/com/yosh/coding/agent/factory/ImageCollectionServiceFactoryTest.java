package com.yosh.coding.agent.factory;

import com.yosh.coding.agent.ai.ImageCollectionService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class ImageCollectionServiceTest {

    @Resource
    private ImageCollectionService imageCollectionService;

    @Test
    void collectImagesForTechWebsite() {
        String result = imageCollectionService.searchImages(
                "创建一个人工智能技术博客，需要首页横幅和文章配图"
        );

        assertNotNull(result);
        assertFalse(result.isBlank());
        System.out.println(result);
    }

    @Test
    void collectImagesForCoffeeWebsite() {
        String result = imageCollectionService.searchImages(
                "创建一个精品咖啡品牌官网，需要咖啡豆、咖啡杯和咖啡店环境图片"
        );

        assertNotNull(result);
        assertFalse(result.isBlank());
        System.out.println(result);
    }
}