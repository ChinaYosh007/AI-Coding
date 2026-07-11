package com.yosh.coding.artificalIntelligence.skill;

import cn.hutool.core.io.FileUtil;
import com.yosh.model.constants.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;

@Slf4j
public class ReadFile extends BaseTool{
    private final Long appId;
    private final Long version;

    public ReadFile(Long appId, Long version) {
        this.appId = appId;
        this.version = version;
    }
    @Tool("阅读对应的文件")
    public String readFIle(@P("相对路径")String relativePath,
                           @P("文件名")String fileName) {

        String projectName = AppConstant.VUE_PREFIX + "_" + appId + "_" + version;
        File file = Path.of(projectName, relativePath, fileName).toFile();
        if (!file.exists()) {
            return "File does not exist: " +   file.getAbsolutePath();
        }
        return "File content: " + FileUtil.readUtf8String(file);
    }

    @Override
    public String getToolName() {
        return "readFIle";
    }

    @Override
    public String getDisplayName() {
        return "read-file";
    }
}
