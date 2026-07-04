package com.yosh.coding.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yosh.coding.core.AiCodeGeneratorFacade;
import com.yosh.coding.service.AppCollaborationService;
import com.yosh.coding.service.AppVersionService;
import com.yosh.coding.service.ChatHistoryService;
import com.yosh.coding.service.UserService;
import com.yosh.exception.BusinessException;
import com.yosh.exception.ErrorCode;
import com.yosh.exception.ThrowUtils;
import com.yosh.model.costants.AppConstant;
import com.yosh.model.dto.app.AppCollaborationInviteRequest;
import com.yosh.model.dto.app.AppQueryRequest;
import com.yosh.model.entity.App;
import com.yosh.coding.mapper.AppMapper;
import com.yosh.coding.service.AppService;
import com.yosh.model.entity.AppCollaboration;
import com.yosh.model.entity.AppVersion;
import com.yosh.model.entity.ChatHistory;
import com.yosh.model.entity.User;
import com.yosh.model.costants.UserContants;
import com.yosh.model.enums.CodeGenTypeEnum;
import com.yosh.model.enums.MessageTypeEnum;
import com.yosh.model.enums.UserRoleEnum;
import com.yosh.model.vo.AppCollaborationMemberVO;
import com.yosh.model.vo.AppVO;
import com.yosh.model.vo.LoginUserVO;
import com.yosh.model.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author yaoxi_rf7anxm
 * @since 2026-06-29
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{
    @Autowired
    @Lazy
    private UserService userService;
    @Autowired
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;
    @Autowired
    private ChatHistoryService chatHistoryService;
    @Autowired
    private AppVersionService appVersionService;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private AppCollaborationService appCollaborationService;

    @Override
    public Long getAppChatHistoryStats(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");

        return this.count(QueryWrapper.create().eq(ChatHistory::getAppId, appId));
    }

    @Override
    public String exportAppChatHistoryAsMarkdown(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create().eq(ChatHistory::getAppId, appId);
        List<ChatHistory> chatHistoryList = chatHistoryService.list(queryWrapper);
        return chatHistoryList.stream().map(chatHistory -> {
            String message = chatHistory.getMessage();
            String messageType = chatHistory.getMessageType();
            return messageType.equals(MessageTypeEnum.USER.getValue()) ? "user：" + message : "agent：" + message;
        }).collect(Collectors.joining("\n\n"));
    }

    @Override
    public void summarizeAppChatHistoryMemory(Long appId) {
        String markdown = exportAppChatHistoryAsMarkdown(appId);
        String str = aiCodeGeneratorFacade.summarizeAppChatHistoryMemory(markdown, appId);
        //清楚旧的数据
        this.remove(QueryWrapper.create().eq(ChatHistory::getAppId, appId));
        chatHistoryService.save(BeanUtil.toBean(ChatHistory.builder().appId(appId).message(str).messageType(MessageTypeEnum.AI.getValue()).build(), ChatHistory.class));
    }

    @Override
    public String getAppChatHistoryMemory(Long appId) {
        return aiCodeGeneratorFacade.getAppChatHistoryMemory(appId);
    }

    @Override
    public void inviteAppCollaborator(Long appId, AppCollaborationInviteRequest request, LoginUserVO loginUser) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getAppId() != null && !request.getAppId().equals(appId),
                ErrorCode.PARAMS_ERROR, "path appId and request appId are inconsistent");
        ThrowUtils.throwIf(StrUtil.isBlank(request.getUserAccount()), ErrorCode.PARAMS_ERROR, "userAccount is blank");

        App app = getValidApp(appId);
        checkAppOwnerOrAdmin(app, loginUser);

        List<User> users = userService.list(QueryWrapper.create().eq(User::getUserAccount, request.getUserAccount()));
        User invitedUser = CollUtil.isEmpty(users) ? null : users.get(0);
        ThrowUtils.throwIf(invitedUser == null, ErrorCode.NOT_FOUND_ERROR, "invited user not found");
        ThrowUtils.throwIf(app.getUserId().equals(invitedUser.getId()), ErrorCode.OPERATION_ERROR,
                "owner already has app access");

        QueryWrapper existsWrapper = QueryWrapper.create()
                .eq(AppCollaboration::getAppId, appId)
                .eq(AppCollaboration::getUserId, invitedUser.getId());
        ThrowUtils.throwIf(appCollaborationService.count(existsWrapper) > 0, ErrorCode.OPERATION_ERROR,
                "user already collaborator");

        AppCollaboration collaboration = new AppCollaboration();
        collaboration.setAppId(appId);
        collaboration.setUserId(invitedUser.getId());
        collaboration.setRole(UserRoleEnum.APP_COLLABORATOR.getValue());
        boolean result = appCollaborationService.save(collaboration);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "invite collaborator failed");
    }

    @Override
    public List<AppCollaborationMemberVO> getAppCollaborationMembers(Long appId, LoginUserVO loginUser) {
        App app = getValidApp(appId);
        checkAppViewAuth(app, loginUser);

        Map<Long, AppCollaborationMemberVO> memberMap = new LinkedHashMap<>();
        User owner = userService.getById(app.getUserId());
        if (owner != null) {
            memberMap.put(owner.getId(), buildCollaborationMemberVO(owner, UserRoleEnum.APP_OWNER.getValue()));
        }
        // 获取应用 collaborators
        List<AppCollaboration> collaborations = appCollaborationService.list(
                QueryWrapper.create().eq(AppCollaboration::getAppId, appId)
        );
        if (CollUtil.isEmpty(collaborations)) {
            return new ArrayList<>(memberMap.values());
        }
        // 获取 collaborators 的 userId
        Set<Long> collaboratorUserIds = collaborations.stream()
                .map(AppCollaboration::getUserId)
                .filter(userId -> userId != null && !memberMap.containsKey(userId))
                .collect(Collectors.toSet());
        // 如果没有 collaborators，则返回应用的 owner
        if (CollUtil.isEmpty(collaboratorUserIds)) {
            return new ArrayList<>(memberMap.values());
        }
        // 获取 collaborators 的用户信息
        Map<Long, User> userMap = userService.listByIds(collaboratorUserIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        for (AppCollaboration collaboration : collaborations) {
            User user = userMap.get(collaboration.getUserId());
            if (user == null || memberMap.containsKey(user.getId())) {
                continue;
            }
            String role = StrUtil.blankToDefault(collaboration.getRole(), UserRoleEnum.APP_COLLABORATOR.getValue());
            memberMap.put(user.getId(), buildCollaborationMemberVO(user, role));
        }
        return new ArrayList<>(memberMap.values());
    }

    @Override
    public File getAppCodeZip(Long appId, Long version, LoginUserVO loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "appId is invalid");
        App app = getValidApp(appId);
        checkAppViewAuth(app, loginUser);

        File file = appVersionService.getResource(appId, version);
        ThrowUtils.throwIf(file == null, ErrorCode.NOT_FOUND_ERROR, "app version source not found");
        ThrowUtils.throwIf(!file.exists() || !file.isDirectory(), ErrorCode.NOT_FOUND_ERROR,
                "app source directory not found");
        //对保存地址数据进行压缩
        File zipFile = ZipUtil.zip(file);
        ThrowUtils.throwIf(zipFile == null || !zipFile.exists(), ErrorCode.OPERATION_ERROR, "zip app source failed");
        return zipFile;
    }

    private App getValidApp(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "appId is invalid");
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "app not found");
        return app;
    }

    private void checkAppOwnerOrAdmin(App app, LoginUserVO loginUser) {
        ThrowUtils.throwIf(loginUser == null || loginUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR);
        boolean isOwner = app.getUserId().equals(loginUser.getId());
        boolean isAdmin = UserContants.ADMIN_ROLE.equals(loginUser.getUserRole());
        ThrowUtils.throwIf(!isOwner && !isAdmin, ErrorCode.NO_AUTH_ERROR);
    }

    private void checkAppViewAuth(App app, LoginUserVO loginUser) {
        ThrowUtils.throwIf(loginUser == null || loginUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR);
        if (app.getUserId().equals(loginUser.getId()) || UserContants.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            return;
        }
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(AppCollaboration::getAppId, app.getId())
                .eq(AppCollaboration::getUserId, loginUser.getId());
        ThrowUtils.throwIf(appCollaborationService.count(queryWrapper) <= 0, ErrorCode.NO_AUTH_ERROR);
    }

    private AppCollaborationMemberVO buildCollaborationMemberVO(User user, String role) {
        AppCollaborationMemberVO memberVO = new AppCollaborationMemberVO();
        memberVO.setUserId(user.getId());
        memberVO.setUserAccount(user.getUserAccount());
        memberVO.setUserName(user.getUserName());
        memberVO.setUserAvatar(user.getUserAvatar());
        memberVO.setRole(role);
        return memberVO;
    }

    @Override
    public Flux<String>  chatToGenCode(Long appId, String msg, LoginUserVO loginUser){
        //权限校验
        ThrowUtils.throwIf(appId == null,ErrorCode.ERROR_QUERY,"select id is bad!!!");
        ThrowUtils.throwIf(StrUtil.isBlank(msg),ErrorCode.ERROR_QUERY,"init message isn't null!");
        ThrowUtils.throwIf(loginUser == null,ErrorCode.ERROR_QUERY,"user isn't login!");
        //查询id
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null,ErrorCode.ERROR_QUERY,"select is bad!!!");
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()),ErrorCode.ERROR_QUERY,"you haven't this power!!!");
        //获取类型
        String GenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(GenType);
        ThrowUtils.throwIf(codeGenTypeEnum == null,ErrorCode.ERROR_QUERY,"this file type current isn't brace!!!");
        chatHistoryService.addChatHistory(appId,loginUser.getId(),msg, MessageTypeEnum.USER.getValue());

        // 预占版本号：先生成一条 version 记录并拿到版本号，保证后续文件保存路径与 DB 一致
        AppVersion newVersion = reserveAppVersion(appId, msg, app.getCodeGenType());
        Long version = newVersion.getVersion();

        // generate and save code（使用预占的版本号，文件将存到与 DB sourcePath 一致的目录）
        Flux<String> stringFlux = aiCodeGeneratorFacade.generateAndSaveCodeStream(msg, codeGenTypeEnum, appId, version);
        StringBuilder respone = new StringBuilder();
        return stringFlux.map(dunk -> {
            respone.append(dunk);
            return dunk;
        }).doOnComplete(() -> {
            String res = respone.toString();
            saveGenerateResultAsync(appId, loginUser.getId(), res, newVersion);
        }).doOnError(e -> {
            String err = "Ai Respone is error" + e;
            rollbackGenerateVersionAsync(appId, loginUser.getId(), err, newVersion);
        });
    }

    private void saveGenerateResultAsync(Long appId, Long userId, String aiResponse, AppVersion appVersion) {
        CompletableFuture.runAsync(() -> {
            try {
                if (StrUtil.isNotBlank(aiResponse)) {
                    chatHistoryService.addChatHistory(appId, userId, aiResponse, MessageTypeEnum.AI.getValue());
                }
                appVersion.setAiResponse(aiResponse);
                appVersionService.updateById(appVersion);
            } catch (Exception e) {
                log.error("save generate result failed. appId={}, version={}", appId, appVersion.getVersion(), e);
            }
        });
    }

    private void rollbackGenerateVersionAsync(Long appId, Long userId, String errorMessage, AppVersion appVersion) {
        CompletableFuture.runAsync(() -> {
            try {
                chatHistoryService.addChatHistory(appId, userId, errorMessage, MessageTypeEnum.AI.getValue());
                if (appVersion.getId() != null) {
                    appVersionService.removeById(appVersion.getId());
                }
            } catch (Exception e) {
                log.error("rollback generate version failed. appId={}, version={}", appId, appVersion.getVersion(), e);
            }
        });
    }

    /**
     * 普通对话不应该保存
     * @param appId
     * @param msg
     * @param codeGenType
     * @return
     */
    private AppVersion reserveAppVersion(Long appId, String msg, String codeGenType) {
        try {
            return reserveAppVersionWithRedisLock(appId, msg, codeGenType);
        } catch (RedisException e) {
            log.warn("Redis lock unavailable, fallback to database version retry. appId={}", appId, e);
            return reserveAppVersionWithDatabaseRetry(appId, msg, codeGenType);
        }
    }

    private AppVersion reserveAppVersionWithRedisLock(Long appId, String msg, String codeGenType) {
        String lockKey = AppConstant.APP_VERSION_LOCK_KEY_PREFIX + appId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(5, 30, TimeUnit.SECONDS);
            ThrowUtils.throwIf(!locked, ErrorCode.OPERATION_ERROR, "app is generating, please try later");
            return createAppVersionRecord(appId, msg, codeGenType);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "get app version lock failed");
        } finally {
            try {
                if (locked && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            } catch (RedisException e) {
                log.warn("release Redis lock failed. appId={}", appId, e);
            }
        }
    }

    private AppVersion reserveAppVersionWithDatabaseRetry(Long appId, String msg, String codeGenType) {
        RuntimeException lastException = null;
        for (int i = 0; i < 3; i++) {
            try {
                return createAppVersionRecord(appId, msg, codeGenType);
            } catch (RuntimeException e) {
                lastException = e;
                log.warn("save app version failed, retrying. appId={}, attempt={}", appId, i + 1, e);
            }
        }
        log.error("save app version failed after retry. appId={}", appId, lastException);
        throw new BusinessException(ErrorCode.OPERATION_ERROR, "save app version failed, please retry later");
    }

    private AppVersion createAppVersionRecord(Long appId, String msg, String codeGenType) {
        AppVersion currentVersion = appVersionService.getByAppId(appId);
        Long version = currentVersion != null ? currentVersion.getVersion() + 1 : AppConstant.DEFAULT_VERSION;
        AppVersion newVersion = new AppVersion();
        newVersion.setAppId(appId);
        newVersion.setVersion(version);
        newVersion.setCodeGenType(codeGenType);
        newVersion.setUserMessage(msg);
        String sourceDir = codeGenType + "_" + appId + "_" + version;
        String sourcePath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDir;
        newVersion.setSourcePath(sourcePath);
        FileUtil.mkdir(sourcePath);
        boolean saveResult = appVersionService.save(newVersion);
        ThrowUtils.throwIf(!saveResult, ErrorCode.OPERATION_ERROR, "save app version failed");
        return newVersion;
    }
    @Override
    public boolean removeById(Serializable id){
        if(id == null) return false;
        long appId = Long.parseLong(id.toString());
        if(appId <= 0 ) return  false;
        try{
            QueryWrapper chatHistoryQw = QueryWrapper.create().eq("appId", appId);
            chatHistoryService.remove(chatHistoryQw);

            QueryWrapper appVersionQw = QueryWrapper.create().eq(AppVersion::getAppId, appId);
            appVersionService.remove(appVersionQw);

        }catch (Exception e){
           log.error("error message {}",e);
        }


        return  super.removeById(id);
    }

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }
    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }
    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }
    @Override
    public String developApp(Long appId, LoginUserVO loginUser,Long version){
        //权限校验
        ThrowUtils.throwIf(version == null,ErrorCode.ERROR_QUERY,"version isn't null!!!");
        ThrowUtils.throwIf(appId == null,ErrorCode.ERROR_QUERY,"select id is bad!!!");
        ThrowUtils.throwIf(loginUser == null,ErrorCode.ERROR_QUERY,"user isn't login!");
        //查询id
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null,ErrorCode.ERROR_QUERY,"select is bad!!!");
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()),ErrorCode.ERROR_QUERY,"you haven't this power!!!");

        String devKey = app.getDeployKey();
        if(StrUtil.isBlank(devKey)){
            devKey = RandomUtil.randomString(6);
        }
        // 逆向出存储路径
        AppVersion appVersion = appVersionService.getByAppIdAndVersion(appId, version);
        ThrowUtils.throwIf(appVersion == null, ErrorCode.NOT_FOUND_ERROR, "version not found");
        String sourcePath = appVersion.getSourcePath();
        ThrowUtils.throwIf(StrUtil.isBlank(sourcePath), ErrorCode.NOT_FOUND_ERROR, "version source path is empty");
        File sourceDirFile = new File(sourcePath);

        ThrowUtils.throwIf(!sourceDirFile.exists() || !sourceDirFile.isDirectory(),ErrorCode.NOT_FOUND_ERROR,"please create app of your");
        String devPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + devKey;
        File devDir = new File(devPath);

        try {
            // 清理旧部署（如果存在）并重新创建
            FileUtil.del(devDir);
            FileUtil.mkdir(devPath);
            // 复制文件
            FileUtil.copyContent(sourceDirFile, devDir, true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to deploy application: " + e.getMessage());
        }

        App upApp = BeanUtil.copyProperties(app,App.class);
        upApp.setDeployedTime(LocalDateTime.now());
        upApp.setDeployKey(devKey);
        Boolean res = this.updateById(upApp);
        ThrowUtils.throwIf(!res,ErrorCode.OPERATION_ERROR,"error,sorry...");
        return String.format("%s/%s", AppConstant.CODE_DEPLOY_HOST, devKey);


    }

    @Override
    public String generateAppName(String initPrompt) {
        return aiCodeGeneratorFacade.generateAppName(initPrompt);
    }


}
