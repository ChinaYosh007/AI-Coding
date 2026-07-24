package com.yosh.coding.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.yosh.coding.annotation.AuthCheck;
import com.yosh.coding.annotation.RateLimit;
import com.yosh.coding.core.handle.AiGenerationErrorMessageResolver;
import com.yosh.coding.service.AppService;
import com.yosh.coding.service.UserService;
import com.yosh.common.BaseResponse;
import com.yosh.common.DeleteRequest;
import com.yosh.common.ResultUtils;
import com.yosh.exception.BusinessException;
import com.yosh.exception.ErrorCode;
import com.yosh.exception.ThrowUtils;
import com.yosh.model.constants.AppConstant;
import com.yosh.model.constants.UserContants;
import com.yosh.model.dto.app.*;
import com.yosh.model.entity.App;
import com.yosh.model.enums.CodeGenTypeEnum;
import com.yosh.model.enums.RateLimitType;
import com.yosh.model.vo.AppCollaborationMemberVO;
import com.yosh.model.vo.AppVO;
import com.yosh.model.vo.LoginUserVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 应用 控制层。
 *
 * @author yaoxi_rf7anxm
 * @since 2026-06-29
 */
@RestController
@RequestMapping("/app")
public class AppController {
    public static final int MAX_APP_SAVE_2_REDIS = 3;
    @Autowired
    private AppService appService;
    @Autowired
    private UserService userService;
    @Autowired
    private com.yosh.coding.service.AppVersionService appVersionService;

    /**
     * 创建应用
     *
     * @param appAddRequest 创建应用请求
     * @param request       请求
     * @return 应用 id
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        // 参数校验
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");
        // 获取当前登录用户
        LoginUserVO loginUser = userService.getLoginUser(request);
        // 构造入库对象
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setUserId(loginUser.getId());
        // 创建应用不依赖模型调用，避免标题生成超时阻塞进入聊天页。
        boolean shouldGenerateAppName = StrUtil.isBlank(app.getAppName());
        if (shouldGenerateAppName) {
            app.setAppName(buildDefaultAppName(initPrompt));
        }

        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(appAddRequest.getCodeGenType());

        if(codeGenTypeEnum == null || codeGenTypeEnum.getValue() == CodeGenTypeEnum.AUTO.getValue()){
            //调用生成路由
            codeGenTypeEnum = appService.generateRoute(appAddRequest.getInitPrompt());
        }
        app.setCodeGenType(codeGenTypeEnum.getValue());
        // 插入数据库
        boolean result = appService.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        if (shouldGenerateAppName) {
            appService.generateAppNameAsync(app.getId(), initPrompt, app.getAppName());
        }
        return ResultUtils.success(app.getId());
    }

    private String buildDefaultAppName(String initPrompt) {
        String normalizedPrompt = initPrompt.replaceAll("\\s+", " ").trim();
        String candidate = normalizedPrompt
                .replaceFirst("^(请帮我|帮我|请|设计|开发|创建|制作|生成|做一个|做个)(一个|一套|一个)?", "")
                .split("[，,。；;：:]", 2)[0]
                .trim();
        if (candidate.isBlank() || candidate.matches("(?is).*?(<!doctype|<html|<script|\\b(function|const|import)\\b).*")) {
            return "新建应用";
        }
        return candidate.length() > 16 ? candidate.substring(0, 16) : candidate;
    }
    /**
     * 更新应用（用户只能更新自己的应用名称）
     *
     * @param appUpdateRequest 更新请求
     * @param request          请求
     * @return 更新结果
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        if (appUpdateRequest == null || appUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LoginUserVO loginUser = userService.getLoginUser(request);
        long id = appUpdateRequest.getId();
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人可更新
        if (!oldApp.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        App app = new App();
        app.setId(id);
        app.setAppName(appUpdateRequest.getAppName());
        // 设置编辑时间
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }
    /**
     * 删除应用（用户只能删除自己的应用）
     *
     * @param deleteRequest 删除请求
     * @param request       请求
     * @return 删除结果
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LoginUserVO loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldApp.getUserId().equals(loginUser.getId()) && !UserContants.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = appService.removeById(id);
        return ResultUtils.success(result);
    }
    /**
     * 根据 id 获取应用详情
     *
     * @param id      应用 id
     * @return 应用详情
     */
    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类（包含用户信息）
        return ResultUtils.success(appService.getAppVO(app));
    }
    /**
     * 分页获取当前用户创建的应用列表
     *
     * @param appQueryRequest 查询请求
     * @param request         请求
     * @return 应用列表
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@RequestBody AppQueryRequest appQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUser = userService.getLoginUser(request);
        // 限制每页最多 20 个
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        long pageNum = appQueryRequest.getPageNum();
        // 只查询当前用户的应用
        appQueryRequest.setUserId(loginUser.getId());
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }
    /**
     * 分页获取精选应用列表
     * 缓存说明: condition 为缓存成立的前置条件
     * @param appQueryRequest 查询请求
     * @return 精选应用列表
     */
    @PostMapping("/good/list/page/vo")
    @Cacheable(
                value = "good_app_page",
                key = "T(com.yosh.utils.GeneraterCacheKey).generateCacheKey(#appQueryRequest)",
                condition = "#appQueryRequest.pageSize < T(com.yosh.coding.controller.AppController).MAX_APP_SAVE_2_REDIS")
    public BaseResponse<Page<AppVO>> listGoodAppVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 限制每页最多 20 个
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        long pageNum = appQueryRequest.getPageNum();
        // 只查询精选的应用
        appQueryRequest.setPriority(AppConstant.GOOD_APP_PRIORITY);
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        // 分页查询
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }
    /**
     * 管理员删除应用
     *
     * @param deleteRequest 删除请求
     * @return 删除结果
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserContants.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteAppByAdmin(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = appService.removeById(id);
        return ResultUtils.success(result);
    }
    /**
     * 管理员更新应用
     *
     * @param appAdminUpdateRequest 更新请求
     * @return 更新结果
     */
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserContants.ADMIN_ROLE)
    public BaseResponse<Boolean> updateAppByAdmin(@RequestBody AppAdminUpdateRequest appAdminUpdateRequest) {
        if (appAdminUpdateRequest == null || appAdminUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = appAdminUpdateRequest.getId();
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        App app = new App();
        BeanUtil.copyProperties(appAdminUpdateRequest, app);
        // 设置编辑时间
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }/**
     * 管理员分页获取应用列表
     *
     * @param appQueryRequest 查询请求
     * @return 应用列表
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserContants.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> listAppVOByPageByAdmin(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }
    /**
     * 管理员根据 id 获取应用详情
     *
     * @param id 应用 id
     * @return 应用详情
     */
    @GetMapping("/admin/get/vo")
    @AuthCheck(mustRole = UserContants.ADMIN_ROLE)
    public BaseResponse<AppVO> getAppVOByIdByAdmin(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(appService.getAppVO(app));
    }
    /**
     * 应用聊天生成代码（流式 SSE）
     *
     * @param appId   应用 ID
     * @param message 用户消息
     * @param request 请求对象
     * @return 生成结果流
     */
    @GetMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimit( rate = 5, rateInterval = 60, limitType = RateLimitType.USER,message = "请求过于频繁，请稍后再试")
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestParam Long appId,
                                                       @RequestParam String message,
                                                       @RequestParam(required = false) Long sourceVersion,
                                                       HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        ThrowUtils.throwIf(sourceVersion != null && sourceVersion <= 0,
                ErrorCode.PARAMS_ERROR, "源版本号无效");
        // 获取当前登录用户
        LoginUserVO loginUser = userService.getLoginUser(request);
        // 调用服务生成代码（流式）
        try {
            Flux<String> flux = appService.chatToGenCode(appId, message, sourceVersion, loginUser);
            return flux.map(chunk ->{
                Map<String,String> map = Map.of("d",chunk);
                String json = JSONUtil.toJsonStr(map);
                return ServerSentEvent.<String>builder()
                        .data(json)
                        .build();
            }).concatWith(Mono.just(
                    ServerSentEvent.<String>builder()
                            .event("done")
                            .data("")
                            .build()
            )).onErrorResume(e -> Flux.just(
                    ServerSentEvent.<String>builder()
                            .event("business-error")
                            .data(JSONUtil.toJsonStr(Map.of(
                                    "message", AiGenerationErrorMessageResolver.resolve(e))))
                            .build()
            ));
        } catch (Exception e) {
            return Flux.just(
                    ServerSentEvent.<String>builder()
                            .event("business-error")
                            .data(JSONUtil.toJsonStr(Map.of(
                                    "message", AiGenerationErrorMessageResolver.resolve(e))))
                            .build());
        }

    }
    /**
     * 应用部署
     *
     * @param appDeployRequest 部署请求
     * @param request          请求
     * @return 部署 URL
     */
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = appDeployRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        Long version = appDeployRequest.getVersion();
        ThrowUtils.throwIf(version == null, ErrorCode.PARAMS_ERROR, "版本号不能为空");
        // 获取当前登录用户
        LoginUserVO loginUser = userService.getLoginUser(request);
        // 调用服务部署应用
        String deployUrl = appService.developApp(appId, loginUser, version);
        return ResultUtils.success(deployUrl);
    }
    /**
     * 获取某个应用的对话历史统计信息
     * @param appId
     * @return
     */
    @GetMapping("/{appId}/stats")
    public BaseResponse<Long> getAppChatHistoryStats(@PathVariable Long appId) {
        Long result = appService.getAppChatHistoryStats(appId);
        return ResultUtils.success(result);
    }
    /**
     * 导出某个应用的对话历史为 Markdown 文件
     * @param appId
     * @return
     */
    @GetMapping("/{appId}/export/markdown")
    public BaseResponse<String> exportAppChatHistoryAsMarkdown(@PathVariable Long appId) {
        String result = appService.exportAppChatHistoryAsMarkdown(appId);
        return ResultUtils.success(result);
    }

    /**
     *
     * @param appId
     * @return
     */
    @PostMapping("/{appId}/memory/summarize")
    public BaseResponse summarizeAppChatHistoryMemory(@PathVariable Long appId, @RequestParam Long version) {
        appService.summarizeAppChatHistoryMemory(appId, version);
        return ResultUtils.success();
    }
    @GetMapping("/{appId}/memory")
    public BaseResponse getAppChatHistoryMemory(@PathVariable Long appId) {
        return ResultUtils.success(appService.getAppChatHistoryMemory(appId));
    }
    @GetMapping("/{appId}/collaboration/members")
    public BaseResponse<List<AppCollaborationMemberVO>> getAppCollaborationMembers(@PathVariable Long appId,
                                                                                   HttpServletRequest request) {
        LoginUserVO loginUser = userService.getLoginUser(request);
        return ResultUtils.success(appService.getAppCollaborationMembers(appId, loginUser));
    }
    @PostMapping("/{appId}/collaboration/invite")
    public BaseResponse<Void> inviteAppCollaborator(@PathVariable Long appId,
                                                    @RequestBody AppCollaborationInviteRequest inviteRequest,
                                                    HttpServletRequest request) {
        LoginUserVO loginUser = userService.getLoginUser(request);
        appService.inviteAppCollaborator(appId, inviteRequest, loginUser);
        return ResultUtils.success();
    }
    @GetMapping("/download/{appId}")
    public ResponseEntity<Resource> downloadAppCode(@PathVariable Long appId, @RequestParam Long version, HttpServletRequest request) {
        LoginUserVO loginUser = userService.getLoginUser(request);
        File zipFile = appService.getAppCodeZip(appId, version, loginUser);

        Resource resource = new FileSystemResource(zipFile);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"app-" + appId + ".zip\"")
                .body(resource);
    }
    @PostMapping("/{appId}/version/{version}/save-file")
    public BaseResponse<Boolean> saveAppFile(@PathVariable Long appId,
                                             @PathVariable Long version,
                                             @RequestBody AppSaveFileRequest requestBody,
                                             HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(version == null || version <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(requestBody == null || StrUtil.isBlank(requestBody.getFilePath()), ErrorCode.PARAMS_ERROR);

        LoginUserVO loginUser = userService.getLoginUser(request);
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        if (!app.getUserId().equals(loginUser.getId()) && !UserContants.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        com.yosh.model.entity.AppVersion appVersion = appVersionService.getByAppIdAndVersion(appId, version);
        ThrowUtils.throwIf(appVersion == null, ErrorCode.NOT_FOUND_ERROR);

        String sourcePath = appVersion.getSourcePath();
        ThrowUtils.throwIf(StrUtil.isBlank(sourcePath), ErrorCode.NOT_FOUND_ERROR);

        Path requestedPath = Paths.get(requestBody.getFilePath());
        ThrowUtils.throwIf(requestedPath.isAbsolute(), ErrorCode.PARAMS_ERROR, "文件路径无效");
        Path projectRoot = Paths.get(sourcePath).toAbsolutePath().normalize();
        Path targetPath = projectRoot.resolve(requestedPath).normalize();
        ThrowUtils.throwIf(!targetPath.startsWith(projectRoot), ErrorCode.PARAMS_ERROR, "文件路径无效");
        cn.hutool.core.io.FileUtil.writeUtf8String(
                requestBody.getContent() == null ? "" : requestBody.getContent(),
                targetPath.toFile());

        return ResultUtils.success(true);
    }
}
