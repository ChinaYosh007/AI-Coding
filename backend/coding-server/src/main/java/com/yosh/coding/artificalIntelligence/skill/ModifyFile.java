package com.yosh.coding.artificalIntelligence.skill;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import com.yosh.model.constants.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
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
    @Tool("修改对应路径下对应的文件")
    public String modifyFile(@P("相对路径")String relativePath,
                             @P("文件名")String fileName,
                             @P("内容") String oldContent,
                             @P("新内容") String newContent) {
        String projectName = AppConstant.VUE_PREFIX + "_" + appId + "_" + version;
        File file = new File(AppConstant.getCodingServerDir(), projectName + File.separator + relativePath + File.separator + fileName);
        log.info("path: {}", file);
        if(!file.exists()){
            return "wrong: 文件不存在";
        }
        String originalContent = FileUtil.readUtf8String(file);
        if(!originalContent.contains(oldContent)){
            return "wrong: 文件内容不存在";
        }
        FileUtil.writeUtf8String(originalContent.replace(oldContent, newContent), file);
        return "success: 文件修改成功";

    }

    @Override
    public String getToolName() {
        return "modifyFile";
    }

    @Override
    public String getDisplayName() {
        return "modify-file";
    }
    @Override
    public String generateToolExecutedResult(JSONObject toolInput) {
        String relativeFilePath = getRelativeFilePath(toolInput);
        String oldContent = toolInput.getStr("oldContent", "");
        String newContent = toolInput.getStr("newContent", "");
        // 显示对比内容
        return String.format("""
            [工具调用] %s %s
            

            替换前：
            ```
            %s
            ```
            
            替换后：
            ```
            %s
            ```
            """, getDisplayName(), relativeFilePath, oldContent, newContent);
    }
}
