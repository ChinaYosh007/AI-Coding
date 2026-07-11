package com.yosh.coding.artificalIntelligence.skill;

import cn.hutool.json.JSONObject;
import com.yosh.model.constants.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DeleteFile  extends BaseTool{
    private final Long appId;
    private final Long version;

    public DeleteFile(Long appId, Long version) {
        this.appId = appId;
        this.version = version;
    }

    @Tool("删除对应路径下对应的文件")
    public String deleteFile(@P("相对路径")String relativePath,
                             @P("文件名")String fileName) {
        try{
            if(isImportantFile(fileName)){
                return "wrong: 不能删除重要文件";
            }
            String projectName = AppConstant.VUE_PREFIX + "_" + appId + "_" + version;
            Path path = Paths.get(AppConstant.getCodingServerDir(), projectName, relativePath, fileName);
            if(!Files.exists(path)){
                return "wrong: 文件不存在";
            }
            if(!Files.isRegularFile(path)){
                return "wrong: 不是普通文件";
            }
            Files.delete(path);
            return "success: 文件删除成功";
        } catch (IOException e) {
           return "wrong: 删除文件时出错";
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
