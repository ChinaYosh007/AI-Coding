package com.yosh.coding.agent.skills;

import com.yosh.coding.agent.WorkFlowAgent;
import com.yosh.coding.agent.model.image.query.ImageResource;
import com.yosh.coding.agent.state.WorkflowContext;
import jakarta.annotation.Resource;
import org.bsc.langgraph4j.GraphStateException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ImageSearchSkillTest {
    @Resource
    private ImageSearchSkill imageSearchSkill;
    @Test
    void testSearchImages() {
        // 测试正常搜索
        List<ImageResource> images = imageSearchSkill.searchImages("technology", 10);
        assertNotNull(images);
        assertFalse(images.isEmpty());
        // 验证返回的图片资源
        images.forEach(image -> {
            assertNotNull(image.getDescription());
            assertNotNull(image.getImageUrl());
            assertNotNull(image.getSource());
            assertNotNull(image.getPhotographer());
            assertNotNull(image.getPhotographerUrl());
            assertNotNull(image.getSourcePageUrl());
        });
    }
    @Test
    void executeWorkflow() throws GraphStateException {
        WorkflowContext result =
                WorkFlowAgent.executeWorkflow(
                        "创建一个咖啡品牌网站"
                );

        assertNotNull(result);
        assertEquals(
                "创建一个咖啡品牌网站",
                result.getOriginalPrompt()
        );
        assertEquals(
                "图片收集",
                result.getCurrentStep()
        );
        assertNotNull(result.getImageListStr());
        assertFalse(result.getImageListStr().isBlank());

        System.out.println(result.getImageListStr());
    }

}