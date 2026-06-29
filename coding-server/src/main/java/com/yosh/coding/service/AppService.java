package com.yosh.coding.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.yosh.model.dto.app.AppQueryRequest;
import com.yosh.model.entity.App;
import com.yosh.model.vo.AppVO;

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
}
