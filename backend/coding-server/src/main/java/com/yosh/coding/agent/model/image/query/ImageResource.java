package com.yosh.coding.agent.model.image.query;

import com.yosh.coding.agent.model.image.enums.ImageCategoryEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageResource {
    private ImageCategoryEnum imageCategory; // 图片类别
    private String description; // 描述
    private String imageUrl; // 图片地址
    private String source; // 来源
    private String photographer; // 摄影师
    private String photographerUrl; // 摄影师地址
    private String sourcePageUrl;
}
