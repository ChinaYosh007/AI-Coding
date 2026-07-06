package com.yosh.coding.artificalIntelligence.skill;

import cn.hutool.core.io.FileUtil;
import com.yosh.model.constants.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class WriteToFile {
    @Tool("writeToFile")
    public String writeToFile(@P("path") String relativePath, @P("content") String content,
                              @ToolMemoryId Long appId,
                              @ToolMemoryId Long version) {
        try{
            Path path = Paths.get(relativePath);
            if(!path.isAbsolute()){
                String DirName = AppConstant.VUE_PREFIX + appId + "_" + version;
                Path root = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, DirName);
                path = root.resolve(path);
            }
            Path parent = path.getParent();
            if (parent == null) {
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
