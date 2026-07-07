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
    private static final int MAX_FILE_WRITES = 25;

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
            // 清理 content 中的 UTF-8 BOM 字符（大模型接口有时会在返回内容的开头带上 ﻿）
            // BOM 会导致 Vite/PostCSS 的 JSON 解析器报 "Unexpected token '﻿'" 错误
            String cleanContent = removeBOM(content);
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
            FileUtil.writeUtf8String(cleanContent, path.toFile());
            log.info("Wrote to file: " + path.toAbsolutePath());
            return "Wrote to file: " + relativePath;
        } catch (Exception e) {
            log.error("Error writing to file: " + e.getMessage());
            return "Error writing to file: " + e.getMessage();
        }

    }

    /**
     * 移除 UTF-8 BOM 字符（﻿ / U+FEFF）
     * 大模型接口有时会在返回内容的开头附带 BOM，导致 Vite 等构建工具解析 JSON/JS 时失败
     */
    private String removeBOM(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        // 移除开头的 BOM 字符
        if (content.startsWith("﻿")) {
            return content.substring(1);
        }
        // 防御：有些响应可能带有多个 BOM 或被 encode 成其他形式
        return content.replace("﻿", "");
    }
}
