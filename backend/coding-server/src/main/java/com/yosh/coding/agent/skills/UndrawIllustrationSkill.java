package com.yosh.coding.agent.skills;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yosh.coding.agent.model.image.enums.ImageCategoryEnum;
import com.yosh.coding.agent.model.image.query.ImageResource;
import com.yosh.exception.ErrorCode;
import com.yosh.exception.ThrowUtils;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class UndrawIllustrationSkill {

    private static final String UNDRAW_API_URL = "https://undraw.co/_next/data/mMWmJSt23qpgo8cLTD_pB/search/%s.json?term=%s";

    @Tool("搜索插画图片，用于网站美化和装饰")
    public List<ImageResource> searchIllustrations(@P("搜索关键词") String query, @P("搜索数量") int searchCount) {
        List<ImageResource> imageList = new ArrayList<>();
        int actualSearchCount = Math.min(searchCount, 20);
        String apiUrl = String.format(UNDRAW_API_URL, query, query);
        ThrowUtils.throwIf(searchCount < 1 || searchCount > 20, ErrorCode.OPERATION_ERROR,"搜索数量必须在 1 到 20 之间");
        ThrowUtils.throwIf(query == null || query.length() < 1, ErrorCode.OPERATION_ERROR, "搜索关键词不能为空");
        // 使用 try-with-resources 自动释放 HTTP 资源
        try (HttpResponse response = HttpRequest.get(apiUrl).timeout(10000).execute()) {
            if (!response.isOk()) {
                log.error("Failed to fetch illustrations from Undraw API: {}", response.body());
                return imageList;
            }
            JSONObject result = JSONUtil.parseObj(response.body());
            JSONObject pageProps = result.getJSONObject("pageProps");
            if (pageProps == null) {
                return imageList;
            }
            JSONArray initialResults = pageProps.getJSONArray("initialResults");
            if (initialResults == null || initialResults.isEmpty()) {
                return imageList;
            }
            int actualCount = Math.min(searchCount, initialResults.size());
            for (int i = 0; i < actualCount; i++) {
                JSONObject illustration = initialResults.getJSONObject(i);
                String title = illustration.getStr("title", "插画");
                String media = illustration.getStr("media", "");
                if (StrUtil.isNotBlank(media)) {
                    imageList.add(ImageResource.builder()
                            .imageCategory(ImageCategoryEnum.ILLUSTRATION)
                            .description(title)
                            .imageUrl(media)
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("搜索插画失败：{}", e.getMessage(), e);
        }
        return imageList;
    }
}
