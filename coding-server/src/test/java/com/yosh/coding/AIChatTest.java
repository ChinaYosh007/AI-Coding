package com.yosh.coding;

import com.yosh.coding.artificalIntelligence.AiCodeGeneratorService;
import com.yosh.coding.artificalIntelligence.model.HtmlCodeResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class AIChatTest {
    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Test
    public void chat()
    {
        HtmlCodeResult s = aiCodeGeneratorService.generateHtmlCode("我是苹果");
        System.out.println(s);
    }
}
