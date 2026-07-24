package com.yosh.coding.artificalIntelligence.skill;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.yosh.model.constants.AppConstant;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 工具基类
 * 定义所有工具的通用接口
 */
public abstract class BaseTool {

    protected Path getProjectRoot(Long appId, Long version) {
        if (appId == null || version == null) {
            throw new IllegalArgumentException("appId or version is blank");
        }
        String projectName = AppConstant.VUE_PREFIX + appId + "_" + version;
        return Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectName)
                .toAbsolutePath()
                .normalize();
    }

    protected Path resolveProjectPath(Long appId, Long version, String relativePath) {
        if (StrUtil.isBlank(relativePath)) {
            throw new IllegalArgumentException("relativePath is blank");
        }

        Path requestedPath = Paths.get(relativePath);
        if (requestedPath.isAbsolute()) {
            throw new IllegalArgumentException("absolute path is not allowed");
        }

        Path projectRoot = getProjectRoot(appId, version);
        Path resolvedPath = projectRoot.resolve(requestedPath).normalize();
        if (!resolvedPath.startsWith(projectRoot)) {
            throw new IllegalArgumentException("path escapes project root");
        }
        return resolvedPath;
    }

    /**
     * 获取工具的英文名称（对应方法名）
     *
     * @return 工具英文名称
     */
    public abstract String getToolName();

    /**
     * 获取工具的中文显示名称
     *
     * @return 工具中文名称
     */
    public abstract String getDisplayName();

    /**
     * 生成工具请求时的返回值（显示给用户）
     *
     * @return 工具请求显示内容
     */
    public String generateToolRequestResponse() {
        return String.format("\n\n[选择工具] %s\n\n", getDisplayName());
    }

    /**
     * 生成工具执行结果格式（保存到数据库）
     *
     * @param arguments 工具执行参数
     * @return 格式化的工具执行结果
     */
    public String generateToolExecutedResult(JSONObject arguments){
        return String.format("[工具调用] %s %s", getDisplayName(), getRelativeFilePath(arguments));
    }

    protected String getRelativeFilePath(JSONObject arguments) {
        String relativePath = arguments.getStr("relativePath");
        if (StrUtil.isBlank(relativePath)) {
            relativePath = arguments.getStr("relativeFilePath");
        }
        if (StrUtil.isBlank(relativePath)) {
            relativePath = arguments.getStr("path", "");
        }

        String fileName = arguments.getStr("fileName");
        if (StrUtil.isBlank(fileName)) {
            return relativePath;
        }
        if (StrUtil.isBlank(relativePath)) {
            return fileName;
        }
        return relativePath.endsWith("/") || relativePath.endsWith("\\")
                ? relativePath + fileName
                : relativePath + "/" + fileName;
    }
}
