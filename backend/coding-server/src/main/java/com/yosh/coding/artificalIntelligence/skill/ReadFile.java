package com.yosh.coding.artificalIntelligence.skill;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class ReadFile extends BaseTool {
    private final Long appId;
    private final Long version;

    public ReadFile(Long appId, Long version) {
        this.appId = appId;
        this.version = version;
    }

    @Tool("Read the content of a file at the specified relative path.")
    public String readFile(@P("relativePath - The relative file path to read, e.g., 'src/App.vue'") String relativePath) {
        log.info("readFile 调用, appId={}, version={}, path={}", appId, version, relativePath);
        try {
            if (StrUtil.isBlank(relativePath)) {
                return "Error reading file: relativePath is blank";
            }
            if (appId == null || version == null) {
                return "Error reading file: appId or version is blank";
            }
            Path path = resolveProjectPath(appId, version, relativePath);
            if (!FileUtil.exist(path.toFile())) {
                return "File does not exist: " + relativePath;
            }
            if (!Files.isRegularFile(path)) {
                return "Path is not a regular file: " + relativePath;
            }
            return FileUtil.readUtf8String(path.toFile());
        } catch (Exception e) {
            log.error("Error reading file: " + e.getMessage());
            return "Error reading file: " + e.getMessage();
        }
    }

    @Override
    public String getToolName() {
        return "readFile";
    }

    @Override
    public String getDisplayName() {
        return "Read File";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeFilePath = getRelativeFilePath(arguments);
        return String.format("""
                    [工具调用] %s %s
                    """, getDisplayName(), relativeFilePath);
    }
}
