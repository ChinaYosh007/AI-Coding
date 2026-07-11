package com.yosh.coding.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.yosh.model.dto.app.AppCollaborationInviteRequest;
import com.yosh.model.dto.app.AppQueryRequest;
import com.yosh.model.entity.App;
import com.yosh.model.enums.CodeGenTypeEnum;
import com.yosh.model.vo.AppCollaborationMemberVO;
import com.yosh.model.vo.AppVO;
import com.yosh.model.vo.LoginUserVO;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;

/**
 * 应用 服务层。
 *
 * @author yaoxi_rf7anxm
 * @since 2026-06-29
 */
public interface AppService extends IService<App> {

    AppVO getAppVO(App app);

    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    List<AppVO> getAppVOList(List<App> appList);


    CodeGenTypeEnum generateRoute(String prompt);

    Flux<String> chatToGenCode(Long appId, String message, LoginUserVO loginUser);

    String developApp(Long appId, LoginUserVO user,Long version);

    String generateAppName(String initPrompt);
    Long getAppChatHistoryStats(Long appId);

    String exportAppChatHistoryAsMarkdown(Long appId);

    void summarizeAppChatHistoryMemory(Long appId, Long version);

    String getAppChatHistoryMemory(Long appId);

    void inviteAppCollaborator(Long appId, AppCollaborationInviteRequest request, LoginUserVO loginUser);

    List<AppCollaborationMemberVO> getAppCollaborationMembers(Long appId, LoginUserVO loginUser);


    File getAppCodeZip(Long appId, Long version, LoginUserVO loginUser);


}
