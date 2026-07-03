package com.yosh.coding.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yosh.coding.core.AiCodeGeneratorFacade;
import com.yosh.coding.service.AppVersionService;
import com.yosh.coding.service.ChatHistoryService;
import com.yosh.coding.service.UserService;
import com.yosh.exception.BusinessException;
import com.yosh.exception.ErrorCode;
import com.yosh.exception.ThrowUtils;
import com.yosh.model.costants.AppConstant;
import com.yosh.model.dto.app.AppQueryRequest;
import com.yosh.model.entity.App;
import com.yosh.coding.mapper.AppMapper;
import com.yosh.coding.service.AppService;
import com.yosh.model.entity.AppVersion;
import com.yosh.model.entity.User;
import com.yosh.model.enums.CodeGenTypeEnum;
import com.yosh.model.enums.MessageTypeEnum;
import com.yosh.model.vo.AppVO;
import com.yosh.model.vo.LoginUserVO;
import com.yosh.model.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private UserService userService;
    @Autowired
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;
    @Autowired
    private ChatHistoryService chatHistoryService;
    @Autowired
    private AppVersionService appVersionService;
    @Autowired
    private RedissonClient redissonClient;

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
        AppVersion reservedVersion = reserveAppVersion(appId, msg, app.getCodeGenType());
        Long version = reservedVersion.getVersion();
        // generate and save code
        Flux<String> stringFlux = aiCodeGeneratorFacade.generateAndSaveCodeStream(msg, codeGenTypeEnum, appId, version);
        StringBuilder respone = new StringBuilder();
        return stringFlux.map(dunk -> {
            respone.append(dunk);
            return dunk;
        }).doOnComplete(() -> {
            String res = respone.toString();
            if (StrUtil.isNotBlank(res)) {
                chatHistoryService.addChatHistory(appId, loginUser.getId(), res, MessageTypeEnum.AI.getValue());
            }
            reservedVersion.setAiResponse(res);
            appVersionService.updateById(reservedVersion);
        }).doOnError(e -> {
            String err = "Ai Respone is error" + e;
            chatHistoryService.addChatHistory(appId, loginUser.getId(), err, MessageTypeEnum.AI.getValue());
            if (reservedVersion.getId() != null) {
                appVersionService.removeById(reservedVersion.getId());
            }
        });
    }

    private AppVersion reserveAppVersion(Long appId, String msg, String codeGenType) {
        String lockKey = AppConstant.APP_VERSION_LOCK_KEY_PREFIX + appId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(5, 30, TimeUnit.SECONDS);
            ThrowUtils.throwIf(!locked, ErrorCode.OPERATION_ERROR, "app is generating, please try later");
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
            boolean saveResult = appVersionService.save(newVersion);
            ThrowUtils.throwIf(!saveResult, ErrorCode.OPERATION_ERROR, "save app version failed");
            return newVersion;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "get app version lock failed");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
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


}
