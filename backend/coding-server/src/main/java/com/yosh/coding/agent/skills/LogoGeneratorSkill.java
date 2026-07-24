package com.yosh.coding.agent.skills;

import cn.hutool.core.util.StrUtil;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.yosh.coding.agent.model.image.enums.ImageCategoryEnum;
import com.yosh.coding.agent.model.image.query.ImageResource;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class LogoGeneratorSkill {

    @Value("${dashscope.api-key:}")
    private String dashScopeApiKey;

    @Value("${dashscope.image-model:wan2.2-t2i-flash}")
    private String imageModel;

    @Tool("根据描述生成 Logo 设计图片，用于网站品牌标识")
    public List<ImageResource> generateLogos(@P("Logo 设计描述，如名称、行业、风格等，尽量详细") String description) {
        List<ImageResource> logoList = new ArrayList<>();
        try {
            String logoPrompt = """
                    为网站导航栏生成一个可直接使用的品牌图形标志。
                    只生成一个简洁、清晰、居中的图形符号，不得包含任何文字、字母、数字、标语、水印或排版内容。
                    禁止生成白色方块底、卡片、边框、设备模型、名片、网页截图、按钮或 UI 组件；不要把标志放进方形容器。
                    使用扁平、矢量感、边缘清晰的图形，保留安全留白，确保缩小到导航栏尺寸时仍清楚可辨。图形本体保持干净，不加入模糊效果。
                    网站需求：%s
                    """.formatted(description);
            ImageSynthesisParam param = ImageSynthesisParam.builder()
                    .apiKey(dashScopeApiKey)
                    .model(imageModel)
                    .prompt(logoPrompt)
                    .size("512*512")
                    .n(1) // 生成 1 张足够，因为 AI 不知道哪张最好
                    .build();
            ImageSynthesis imageSynthesis = new ImageSynthesis();
            ImageSynthesisResult result = imageSynthesis.call(param);
            if (result != null && result.getOutput() != null && result.getOutput().getResults() != null) {
                List<Map<String, String>> results = result.getOutput().getResults();
                for (Map<String, String> imageResult : results) {
                    String imageUrl = imageResult.get("url");
                    if (StrUtil.isNotBlank(imageUrl)) {
                        logoList.add(ImageResource.builder()
                                .imageCategory(ImageCategoryEnum.LOGO)
                                .description(description)
                                .imageUrl(imageUrl)
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            // Logo 是可选资源，外部图像服务断流时由上层资源流程降级，避免中断代码生成。
            log.warn("Logo 生成服务暂不可用，已跳过: {}", conciseMessage(e));
        }
        return logoList;
    }

    private String conciseMessage(Exception exception) {
        String message = exception.getMessage();
        if (StrUtil.isBlank(message)) {
            return exception.getClass().getSimpleName();
        }
        return StrUtil.maxLength(message.replaceAll("\\s+", " "), 240);
    }
}
