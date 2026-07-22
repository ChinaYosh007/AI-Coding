package com.yosh.coding.agent.skills;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yosh.coding.agent.model.image.enums.ImageCategoryEnum;
import com.yosh.coding.agent.model.image.query.ImageResource;
import com.yosh.exception.BusinessException;
import com.yosh.exception.ErrorCode;
import com.yosh.exception.ThrowUtils;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ImageSearchSkill {
    private static final String SEARCH_URL =  "https://api.pexels.com/v1/search";
    private static final int MAX_SEARCH_COUNT = 80;
    @Value("${pexels.api-key}")
    private String apiKey;

    /**
     * @param query
     * @param count
     * @return
     */
    @Tool("Search Pexels images for website content")
    public List<ImageResource> searchImages(@P("search query")String query,@P("number of images:number of [1,80]")int count) {
        log.info("Searching images for query: {}", query);
        ThrowUtils.throwIf(count < 1 || count > MAX_SEARCH_COUNT, ErrorCode.OPERATION_ERROR, "Invalid count value");
        ThrowUtils.throwIf(
                StrUtil.isBlank(query),
                ErrorCode.PARAMS_ERROR,
                "Search query cannot be blank"
        );
        List<ImageResource> imageResources = new ArrayList<>();
        try(HttpResponse response = HttpRequest.get(SEARCH_URL)
                                                .timeout(10000)
                                                .header("Authorization", apiKey)
                                                .form("query", query)
                                                .form("per_page", String.valueOf(count))
                                                .form("page", 1)
                                                .execute()){
            if(response.isOk()){
                JSONObject jsonObject = JSONUtil.parseObj(response.body());
                JSONArray jsonArray = jsonObject.getJSONArray("photos");
                for(int i = 0 ; i < jsonArray.size(); i++){
                    JSONObject photo = jsonArray.getJSONObject(i);
                    JSONObject src = photo.getJSONObject("src");
                    imageResources.add(ImageResource.builder()
                            .description(photo.getStr("alt"))
                            .imageUrl(src.getStr("medium"))
                            .source("Pexels")
                            .imageCategory(ImageCategoryEnum.CONTENT)
                            .photographer(photo.getStr("photographer"))
                            .photographerUrl(photo.getStr("photographer_url"))
                            .sourcePageUrl(photo.getStr("url"))
                            .build()
                    );
                }

            }else {

                    log.error(
                            "Pexels API failed, status={}, body={}",
                            response.getStatus(),
                            response.body()
                    );
                    throw new BusinessException(
                            ErrorCode.OPERATION_ERROR,
                            "Image search service unavailable"
                    );
            }

        }catch (BusinessException e){
            log.error("BusinessException searching images for query: {}", query, e);
            throw e;
        }
        catch (Exception e){
            log.error("Error searching images for query: {}", query, e);
        }

        return imageResources;
    }
}
