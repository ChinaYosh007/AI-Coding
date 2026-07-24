package com.yosh.coding.artificalIntelligence.skill;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.yosh.model.enums.CodeGenTypeEnum;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.nio.file.Files;
import java.nio.file.Path;

public class DeleteFile  extends BaseTool{
    private final Long appId;
    private final Long version;
    private final CodeGenTypeEnum codeGenType;

    public DeleteFile(Long appId, Long version) {
        this(appId, version, CodeGenTypeEnum.VUE_PROJECT);
    }

    public DeleteFile(Long appId, Long version, CodeGenTypeEnum codeGenType) {
        this.appId = appId;
        this.version = version;
        this.codeGenType = codeGenType;
    }

    @Tool("删除对应路径下对应的文件")
    public String deleteFile(@P("相对路径")String relativePath,
                             @P("文件名")String fileName) {
        try{
            if (StrUtil.isBlank(fileName)) {
                return "wrong: 文件名不能为空";
            }
            Path fileNamePath = Path.of(fileName);
            if (fileNamePath.isAbsolute() || fileNamePath.getNameCount() != 1) {
                return "wrong: 文件名不能包含路径";
            }
            if(isImportantFile(fileName)){
                return "wrong: 不能删除重要文件";
            }
            String targetPath = StrUtil.isBlank(relativePath)
                    ? fileName
                    : Path.of(relativePath, fileName).toString();
            Path path = resolveProjectPath(appId, version, codeGenType, targetPath);
            if(!Files.exists(path)){
                return "wrong: 文件不存在";
            }
            if(!Files.isRegularFile(path)){
                return "wrong: 不是普通文件";
            }
            Files.delete(path);
            return "success: 文件删除成功";
        } catch (Exception e) {
           return "wrong: 删除文件时出错: " + e.getMessage();
        }
    }

    private boolean isImportantFile(String fileName) {
        String[] importantFiles = {
                "package.json", "package-lock.json", "yarn.lock", "pnpm-lock.yaml",
                "vite.config.js", "vite.config.ts", "vue.config.js",
                "tsconfig.json", "tsconfig.app.json", "tsconfig.node.json",
                "index.html", "main.js", "main.ts", "App.vue", ".gitignore", "README.md"
        };
        for (String importantFile : importantFiles) {
            if (fileName.equals(importantFile)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getToolName() {
        return "deleteFile";
    }

    @Override
    public String getDisplayName() {
        return "delete-file";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        return super.generateToolExecutedResult(arguments);
    }
}
