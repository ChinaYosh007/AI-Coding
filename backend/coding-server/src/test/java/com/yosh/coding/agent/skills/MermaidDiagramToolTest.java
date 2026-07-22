package com.yosh.coding.agent.skills;

import com.yosh.coding.agent.model.image.enums.ImageCategoryEnum;
import com.yosh.coding.agent.model.image.query.ImageResource;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

@SpringBootTest
class MermaidDiagramToolTest {

    @Resource
    private MermaidDiagramSkill mermaidDiagramTool;

    @Test
    void testGenerateMermaidDiagram() {
        // 测试生成 Mermaid 架构图
        String mermaidCode = """
                flowchart LR
                    Start([开始]) --> Input[输入数据]
                    Input --> Process[处理数据]
                    Process --> Decision{是否有效?}
                    Decision -->|是| Output[输出结果]
                    Decision -->|否| Error[错误处理]
                    Output --> End([结束])
                    Error --> End
                """;
        String description = "简单系统架构图";
        List<ImageResource> diagrams = mermaidDiagramTool.generateMermaidDiagram(mermaidCode, description);
        assertNotNull(diagrams);
        // 如果有结果，验证图表资源
       for (ImageResource diagram : diagrams){
           System.out.println(diagram);
       }
    }
}
