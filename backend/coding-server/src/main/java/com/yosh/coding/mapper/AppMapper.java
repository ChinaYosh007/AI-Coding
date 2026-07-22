package com.yosh.coding.mapper;

import com.mybatisflex.core.BaseMapper;
import com.yosh.model.entity.App;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 应用 映射层。
 *
 * @author yaoxi_rf7anxm
 * @since 2026-06-29
 */
public interface AppMapper extends BaseMapper<App> {

    @Select("""
            SELECT *
            FROM app
            WHERE isDelete = 1
              AND updateTime < #{expireTime}
              AND id > #{lastId}
            ORDER BY id
            LIMIT #{batchSize}
            """)
    List<App> selectExpiredLogicDeletedApps(@Param("expireTime") LocalDateTime expireTime,
                                            @Param("lastId") long lastId,
                                            @Param("batchSize") int batchSize);

    @Select("""
            SELECT COUNT(*)
            FROM app
            WHERE cover = #{cover}
              AND id != #{appId}
            """)
    long countOtherCoverReferences(@Param("cover") String cover, @Param("appId") long appId);

    @Delete("DELETE FROM app WHERE id = #{appId} AND isDelete = 1")
    int physicalDeleteLogicDeletedById(@Param("appId") long appId);

}
