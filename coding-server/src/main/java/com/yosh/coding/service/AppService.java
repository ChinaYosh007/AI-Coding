package com.yosh.coding.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.yosh.model.dto.app.AppQueryRequest;
import com.yosh.model.entity.App;
import com.yosh.model.entity.User;
import com.yosh.model.vo.AppVO;
import com.yosh.model.vo.LoginUserVO;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author yaoxi_rf7anxm
 * @since 2026-06-29
 */
public interface AppService extends IService<App> {

    AppVO getAppVO(App app);

    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    List<AppVO> getAppVOList(List<App> appList);


    Flux<String> chatToGenCode(Long appId, String message, LoginUserVO loginUser);

    String developApp(Long appId, LoginUserVO user,Long version);

    String generateAppName(String initPrompt);
}
