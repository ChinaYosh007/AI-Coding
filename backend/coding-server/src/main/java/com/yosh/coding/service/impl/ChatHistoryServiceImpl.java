package com.yosh.coding.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yosh.coding.mapper.ChatHistoryMapper;
import com.yosh.coding.service.AppService;
import com.yosh.coding.service.ChatHistoryService;
import com.yosh.exception.BusinessException;
import com.yosh.exception.ErrorCode;
import com.yosh.exception.ThrowUtils;
import com.yosh.model.dto.chathistory.ChatHistoryQueryRequest;
import com.yosh.model.entity.App;
import com.yosh.model.entity.ChatHistory;
import com.yosh.model.enums.MessageTypeEnum;
import com.yosh.model.vo.LoginUserVO;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话历史 服务层实现。
 *
 * @author china_yosh
 * @since 2026-07-02
 */
@Slf4j
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>
        implements ChatHistoryService {

    @Autowired
    @Lazy
    private AppService appService;

    @Override
    public Boolean addChatHistory(Long appId, Long userId, String message, String messageType) {
    ThrowUtils.throwIf(appId == null || userId == null,ErrorCode.OPERATION_ERROR,"appId or user Id is null");
    ThrowUtils.throwIf(StrUtil.isBlank(message),ErrorCode.OPERATION_ERROR,"msg isn't null");
    ThrowUtils.throwIf(StrUtil.isBlank(messageType),ErrorCode.OPERATION_ERROR,"msgType isn't null");
    MessageTypeEnum msgType = MessageTypeEnum.getEnumByValue(messageType);
    if(msgType == null) throw   new BusinessException(ErrorCode.OPERATION_ERROR,"msgType isn't at this point");
    ChatHistory chatHistory = ChatHistory.builder()
            .appId(appId)
            .userId(userId)
            .message(message)
            .messageType(messageType)
            .createTime(LocalDateTime.now())
            .updateTime(LocalDateTime.now())
            .build();

    return this.save(chatHistory);
    }


    @Override
    public void deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR);
        QueryWrapper qw = QueryWrapper.create().eq(ChatHistory::getAppId, appId);
        this.remove(qw);
    }
    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (chatHistoryQueryRequest == null) {
            return queryWrapper;
        }

        // 拼接查询条件
        queryWrapper.eq(ChatHistory::getId, chatHistoryQueryRequest.getId())
                .like(ChatHistory::getMessage, chatHistoryQueryRequest.getMessage())
                .eq(ChatHistory::getMessageType, chatHistoryQueryRequest.getMessageType())
                .eq(ChatHistory::getAppId, chatHistoryQueryRequest.getAppId())
                .eq(ChatHistory::getUserId, chatHistoryQueryRequest.getUserId())
                .orderBy(ChatHistory::getCreateTime, false);
        // 游标查询逻辑 - 只使用 createTime 作为游标
        if (chatHistoryQueryRequest.getLastCreateTime() != null) {
            queryWrapper.lt(ChatHistory::getCreateTime, chatHistoryQueryRequest.getLastCreateTime());
        }
        // 排序
        if (StrUtil.isNotBlank(chatHistoryQueryRequest.getSortField())) {
            queryWrapper.orderBy(chatHistoryQueryRequest.getSortField(), "ascend".equals(chatHistoryQueryRequest.getSortOrder()));
        } else {
            // 默认按创建时间降序排列
            queryWrapper.orderBy(ChatHistory::getCreateTime, false);
        }
        return queryWrapper;
    }


    @Override
    public Page<ChatHistory> listChatHistory(Long appId, int pageSize,
                                             LocalDateTime lastCreateTime,
                                             @MonotonicNonNull LoginUserVO userVO) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");
        ThrowUtils.throwIf(userVO == null, ErrorCode.NOT_LOGIN_ERROR);
        // 验证权限：只有应用创建者和管理员可以查看
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        boolean isAdmin = "admin".equals(userVO.getUserRole());
        boolean isOwner = app.getUserId().equals(userVO.getId());
        ThrowUtils.throwIf(!isAdmin && !isOwner, ErrorCode.NO_AUTH_ERROR, "无权限访问");
        // 构建查询条件
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);
        // 查询数据
        return this.page(Page.of(1, pageSize), queryWrapper);
    }

    @Override
    public void loadHistoryMessage(MessageWindowChatMemory messageWindowChatMemory, Long appId, Long pageSize) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");
        ThrowUtils.throwIf(messageWindowChatMemory == null, ErrorCode.PARAMS_ERROR, "消息窗口内存不能为空");

        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setPageSize(pageSize);
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);
        try{
            this.list(queryWrapper).reversed().stream().forEach(chatHistory -> {
                if (chatHistory.getMessageType().equals(MessageTypeEnum.USER.getValue())) {
                    messageWindowChatMemory.add( UserMessage.from(chatHistory.getMessage()));
                } else{
                    messageWindowChatMemory.add( AiMessage.from(chatHistory.getMessage()));
                }
            });
        }catch (Exception e){
            log.error("Failed to load history messages: {}", e.getMessage(), e);
        }

    }


}
