package com.yosh.coding.mapper;

import com.mybatisflex.core.BaseMapper;
import com.yosh.model.entity.ChatHistory;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 对话历史 映射层。
 *
 * @author china_yosh
 * @since 2026-07-02
 */
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {

    @Delete("DELETE FROM chat_history WHERE appId = #{appId}")
    int physicalDeleteByAppId(@Param("appId") long appId);

}
