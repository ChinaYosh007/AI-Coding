package com.yosh.coding.mapper;

import com.mybatisflex.core.BaseMapper;
import com.yosh.model.entity.AppCollaboration;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface AppCollaborationMapper extends BaseMapper<AppCollaboration> {

    @Delete("DELETE FROM app_collaboration WHERE appId = #{appId}")
    int physicalDeleteByAppId(@Param("appId") long appId);
}
