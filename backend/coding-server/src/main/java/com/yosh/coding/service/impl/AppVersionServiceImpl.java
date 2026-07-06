package com.yosh.coding.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yosh.model.entity.AppVersion;
import com.yosh.coding.mapper.AppVersionMapper;
import com.yosh.coding.service.AppVersionService;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * 应用代码版本 服务层实现。
 *
 * @author china_yosh
 * @since 2026-07-04
 */
@Service
public class AppVersionServiceImpl extends ServiceImpl<AppVersionMapper, AppVersion>  implements AppVersionService{

    @Override
    public AppVersion getByAppId(Long appId) {
        //mybatis-flex
        QueryWrapper limit = this.query().eq(AppVersion::getAppId, appId).orderBy(AppVersion::getVersion, false).limit(0, 1);

        return this.getOne(limit);
    }

    @Override
    public AppVersion getByAppIdAndVersion(Long appId, Long version) {
            QueryWrapper queryWrapper = this.query()
                    .eq(AppVersion::getAppId, appId)
                    .eq(AppVersion::getVersion, version)
                    .limit(1);

            return this.getOne(queryWrapper);

    }

    @Override
    public File getResource(Long appId, Long version) {
        AppVersion appVersion = this.getByAppIdAndVersion(appId, version);
        return appVersion == null ? null : new File(appVersion.getSourcePath());
    }
}
