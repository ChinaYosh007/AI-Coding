package com.yosh.coding.service;

import com.mybatisflex.core.service.IService;
import com.yosh.model.entity.AppVersion;

import java.io.File;

/**
 * 应用代码版本 服务层。
 *
 * @author china_yosh
 * @since 2026-07-04
 */
public interface AppVersionService extends IService<AppVersion> {

   public AppVersion getByAppId(Long appId);

    AppVersion getByAppIdAndVersion(Long appId, Long version);

    File getResource(Long appId, Long version);
}
