package com.yosh.coding.artificalIntelligence.skill;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

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
                             @P("oldContent - The exact raw text content to be replaced. Do not add a backslash before apostrophes.") String oldContent,
                             @P("newContent - The complete raw replacement text. Do not add a backslash before apostrophes.") String newContent) {
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
            Path path = resolveProjectPath(appId, version, relativePath);
            if (!FileUtil.exist(path.toFile())) {
                return "File does not exist: " + relativePath;
            }
            String content = FileUtil.readUtf8String(path.toFile());
            int matchIndex = content.indexOf(oldContent);
            if (matchIndex < 0) {
                return "Error modifying file: oldContent not found in the file. Please use readFile to check the exact content.";
            }
            if (content.indexOf(oldContent, matchIndex + oldContent.length()) >= 0) {
                return "Error modifying file: oldContent matches multiple locations. Include more surrounding content so the match is unique.";
            }

            String updatedContent = content.substring(0, matchIndex)
                    + newContent
                    + content.substring(matchIndex + oldContent.length());
            FileUtil.writeUtf8String(updatedContent, path.toFile());
            log.info("Modified file: " + path.toAbsolutePath());
            return "Modified file successfully: " + relativePath
                    + ". The requested change is complete; call exit now and do not invoke more tools.";
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
