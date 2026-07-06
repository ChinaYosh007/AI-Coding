package com.yosh.coding.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.yosh.model.dto.chathistory.ChatHistoryQueryRequest;
import com.yosh.model.entity.ChatHistory;
import com.yosh.model.vo.LoginUserVO;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author china_yosh
 * @since 2026-07-02
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 保存一条对话记录（用户消息 / AI 回复 / AI 错误信息均走此方法）
     */

    Boolean addChatHistory(Long appId, Long userId, String message, String messageType);

    /**
     * 按应用 id 关联删除所有对话历史（删除应用时级联调用）
     */

    void deleteByAppId(Long appId);

    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);

    /**
     * 游标分页查询某应用的对话历史（仅创建者 / 管理员可见）
     * 第一次不传 lastId，加载最新 N 条；向前翻页传上次最早那条的 id
     */

    Page<ChatHistory> listChatHistory(Long appId, int pageSize,
                                      LocalDateTime lastCreateTime,
                                      LoginUserVO loginUserVO);
    void loadHistoryMessage(MessageWindowChatMemory messageWindowChatMemory, Long appId, Long pageSize);


}