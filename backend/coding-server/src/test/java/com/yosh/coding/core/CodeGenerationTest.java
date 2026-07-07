package com.yosh.coding.core;

import com.yosh.coding.artificalIntelligence.AiCodeGeneratorService;
import com.yosh.coding.artificalIntelligence.model.MultiFileCodeResult;
import com.yosh.model.enums.CodeGenTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

@SpringBootTest
public class CodeGenerationTest {

    @Autowired
    private AiCodeGeneratorService aiCodeGeneratorService;
    @Autowired
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Test
    public void testGenerateAndSaveCode() {
        System.out.println("开始测试代码生成...");

        // 生成代码
        MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(0L,"创建一个简单的计数器网页");

        System.out.println("HTML 代码长度: " + (result.getHtmlCode() != null ? result.getHtmlCode().length() : 0));
        System.out.println("CSS 代码长度: " + (result.getCssCode() != null ? result.getCssCode().length() : 0));
        System.out.println("JS 代码长度: " + (result.getJsCode() != null ? result.getJsCode().length() : 0));

        // 保存代码
            File savedFile = aiCodeGeneratorFacade.generateAndSaveCode("测试", CodeGenTypeEnum.MULTI_FILE, 1L, 1L);

        System.out.println("保存的文件: " + savedFile.getAbsolutePath());
        System.out.println("文件是否存在: " + savedFile.exists());

        // 检查生成的文件
        if (savedFile.exists()) {
            File[] files = savedFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    System.out.println("生成的文件: " + file.getName() + " (大小: " + file.length() + " bytes)");
                }
            }
        }
    }
}
