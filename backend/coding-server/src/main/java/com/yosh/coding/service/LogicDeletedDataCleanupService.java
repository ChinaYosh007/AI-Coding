package com.yosh.coding.service;

import com.yosh.coding.mapper.AppCollaborationMapper;
import com.yosh.coding.mapper.AppMapper;
import com.yosh.coding.mapper.AppVersionMapper;
import com.yosh.coding.mapper.ChatHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LogicDeletedDataCleanupService {

    private final AppMapper appMapper;
    private final ChatHistoryMapper chatHistoryMapper;
    private final AppVersionMapper appVersionMapper;
    private final AppCollaborationMapper appCollaborationMapper;

    /**
     * 按依赖顺序物理删除一个已逻辑删除应用的全部数据库记录。
     */
    @Transactional
    public void physicalDeleteAppData(long appId) {
        chatHistoryMapper.physicalDeleteByAppId(appId);
        appCollaborationMapper.physicalDeleteByAppId(appId);
        appVersionMapper.physicalDeleteByAppId(appId);
        int deletedApps = appMapper.physicalDeleteLogicDeletedById(appId);
        if (deletedApps != 1) {
            throw new IllegalStateException("应用未处于逻辑删除状态，拒绝物理删除，appId=" + appId);
        }
    }
}
