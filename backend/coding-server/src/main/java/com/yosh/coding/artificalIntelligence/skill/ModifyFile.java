package com.yosh.coding.artificalIntelligence.skill;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.yosh.model.constants.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class ModifyFile extends BaseTool {
    private final Long appId;
    private final Long version;

    public ModifyFile(Long appId, Long version) {
        this.appId = appId;
        this.version = version;
    }

    @Tool("Modify an existing file by precisely replacing old content with new content. oldContent must exactly match the text in the file.")
    public String modifyFile(@P("relativePath - The relative file path to modify, e.g., 'src/App.vue'") String relativePath,
                             @P("oldContent - The exact text content to be replaced.") String oldContent,
                             @P("newContent - The new text content that will replace the old content.") String newContent) {
        log.info("modifyFile 调用, appId={}, version={}, path={}", appId, version, relativePath);
        try {
            if (StrUtil.isBlank(relativePath)) {
                return "Error modifying file: relativePath is blank";
            }
            if (StrUtil.isBlank(oldContent)) {
                return "Error modifying file: oldContent is blank";
            }
            if (appId == null || version == null) {
                return "Error modifying file: appId or version is blank";
            }
            Path path = Paths.get(relativePath);
            if (!path.isAbsolute()) {
                String DirName = AppConstant.VUE_PREFIX + appId + "_" + version;
                Path root = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, DirName);
                path = root.resolve(path);
            }
            if (!FileUtil.exist(path.toFile())) {
                return "File does not exist: " + relativePath;
            }
            String content = FileUtil.readUtf8String(path.toFile());
            if (!content.contains(oldContent)) {
                return "Error modifying file: oldContent not found in the file. Please use readFile to check the exact content.";
            }
            
            String updatedContent = content.replace(oldContent, newContent);
            FileUtil.writeUtf8String(updatedContent, path.toFile());
            log.info("Modified file: " + path.toAbsolutePath());
            return "Modified file successfully: " + relativePath;
        } catch (Exception e) {
            log.error("Error modifying file: " + e.getMessage());
            return "Error modifying file: " + e.getMessage();
        }
    }

    @Override
    public String getToolName() {
        return "modifyFile";
    }

    @Override
    public String getDisplayName() {
        return "Modify File";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeFilePath = getRelativeFilePath(arguments);
        return String.format("""
                    [工具调用] %s %s
                    """, getDisplayName(), relativeFilePath);
    }
}
