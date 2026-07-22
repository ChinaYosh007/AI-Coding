package com.yosh.coding.mapper;

import com.mybatisflex.core.BaseMapper;
import com.yosh.model.entity.AppVersion;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 应用代码版本 映射层。
 *
 * @author china_yosh
 * @since 2026-07-04
 */
public interface AppVersionMapper extends BaseMapper<AppVersion> {

    @Select("SELECT * FROM app_version WHERE appId = #{appId} ORDER BY id")
    List<AppVersion> selectAllByAppId(@Param("appId") long appId);

    @Delete("DELETE FROM app_version WHERE appId = #{appId}")
    int physicalDeleteByAppId(@Param("appId") long appId);

}
