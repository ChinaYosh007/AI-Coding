package com.yosh.coding.artificalIntelligence.skill;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.yosh.model.constants.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class WriteToFile {
    private static final int MAX_FILE_WRITES = 18;

    private final Long appId;
    private final Long version;
    private int invocationCount = 0;

    public WriteToFile(Long appId, Long version) {
        this.appId = appId;
        this.version = version;
    }

    @Tool("Write content to a file at the specified path. The content should be complete and ready to use.")
    public String writeToFile(@P("relativePath - The relative file path where the content should be written, e.g., 'src/App.vue'") String relativePath, 
                              @P("content - The complete file content to write. Must be properly formatted and escaped for JSON.") String content) {
        invocationCount++;
        log.info("writeToFile 第 {} 次调用, appId={}, version={}, path={}", invocationCount, appId, version, relativePath);
        // 跳过预置文件
        if ("package.json".equals(relativePath) || "vite.config.js".equals(relativePath) || "index.html".equals(relativePath)) {
            log.info("跳过预置文件: {}", relativePath);
            return "Skipped: " + relativePath + " is pre-configured.";
        }
        if (invocationCount > MAX_FILE_WRITES) {
            String errorMessage = "writeToFile called too many times, possible loop. appId=" + appId + ", version=" + version;
            log.warn(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
        try{
            if (StrUtil.isBlank(relativePath)) {
                return "Error writing to file: relativePath is blank";
            }
            if (appId == null || version == null) {
                return "Error writing to file: appId or version is blank";
            }
            Path path = Paths.get(relativePath);
            if(!path.isAbsolute()){
                String DirName = AppConstant.VUE_PREFIX + appId + "_" + version;
                Path root = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, DirName);
                path = root.resolve(path);
            }
            Path parent = path.getParent();
            if (parent != null) {
                FileUtil.mkdir(parent.toFile());
            }
            FileUtil.writeUtf8String(content, path.toFile());
            log.info("Wrote to file: " + path.toAbsolutePath());
            return "Wrote to file: " + relativePath;
        } catch (Exception e) {
            log.error("Error writing to file: " + e.getMessage());
            return "Error writing to file: " + e.getMessage();
        }

    }
}
