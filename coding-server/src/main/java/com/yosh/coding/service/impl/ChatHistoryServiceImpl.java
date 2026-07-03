package com.yosh.coding.service.impl;

import cn.hutool.core.bean.BeanUtil;
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
import com.yosh.model.costants.UserContants;
import com.yosh.model.dto.chathistory.ChatHistoryAdminQueryRequest;
import com.yosh.model.dto.chathistory.ChatHistoryQueryRequest;
import com.yosh.model.entity.App;
import com.yosh.model.entity.ChatHistory;
import com.yosh.model.enums.MessageTypeEnum;
import com.yosh.model.vo.ChatHistoryVO;
import com.yosh.model.vo.LoginUserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * 对话历史 服务层实现。
 *
 * @author china_yosh
 * @since 2026-07-02
 */
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
        QueryWrapper qw = QueryWrapper.create().eq("appId", appId);
        this.remove(qw);
    }
    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (chatHistoryQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chatHistoryQueryRequest.getId();
        String message = chatHistoryQueryRequest.getMessage();
        String messageType = chatHistoryQueryRequest.getMessageType();
        Long appId = chatHistoryQueryRequest.getAppId();
        Long userId = chatHistoryQueryRequest.getUserId();
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();
        // 拼接查询条件
        queryWrapper.eq("id", id)
                .like("message", message)
                .eq("messageType", messageType)
                .eq("appId", appId)
                .eq("userId", userId);
        // 游标查询逻辑 - 只使用 createTime 作为游标
        if (lastCreateTime != null) {
            queryWrapper.lt("createTime", lastCreateTime);
        }
        // 排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            // 默认按创建时间降序排列
            queryWrapper.orderBy("createTime", false);
        }
        return queryWrapper;
    }


    @Override
    public Page<ChatHistory> listChatHistory(Long appId, int pageSize,
                                             LocalDateTime lastCreateTime,
                                              LoginUserVO loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 验证权限：只有应用创建者和管理员可以查看
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean isAdmin = UserContants.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isCreator = app.getUserId().equals(loginUser.getId());
        ThrowUtils.throwIf(!isAdmin && !isCreator, ErrorCode.NO_AUTH_ERROR, "无权查看该应用的对话历史");
        // 构建查询条件
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);
        // 查询数据
        return this.page(Page.of(1, pageSize), queryWrapper);
    }


    @Override
    public Page<ChatHistoryVO> adminListChatHistoryByPage(ChatHistoryAdminQueryRequest request) {
        long pageNum = request.getPageNum();
        long pageSize = request.getPageSize();
        QueryWrapper qw = QueryWrapper.create()
                .eq("appId", request.getAppId(), request.getAppId() != null)
                .eq("userId", request.getUserId(), request.getUserId() != null)
                .eq("messageType", request.getMessageType())
                .orderBy("createTime", false);
        Page<ChatHistory> page = this.page(Page.of(pageNum, pageSize), qw);
        Page<ChatHistoryVO> voPage = new Page<>(pageNum, pageSize, page.getTotalRow());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return voPage;
    }

    private ChatHistoryVO toVO(ChatHistory chatHistory) {
        ChatHistoryVO vo = new ChatHistoryVO();
        BeanUtil.copyProperties(chatHistory, vo);
        return vo;
    }
}
