package com.yosh.coding.artificalIntelligence.skill;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import com.yosh.model.constants.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
@Slf4j
public class ReadProjectDir  extends BaseTool{
    private final Long appId;
    private final Long version;
    public ReadProjectDir(Long appId, Long version) {
        this.appId = appId;
        this.version = version;
    }
    /**

     * 需要忽略的文件和目录
     */
    private static final Set<String> IGNORED_NAMES = Set.of(
            "node_modules", ".git", "dist", "build", ".DS_Store",
            ".env", "target", ".mvn", ".idea", ".vscode", "coverage"
    );

    /**

     * 需要忽略的文件扩展名
     */
    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            ".log", ".tmp", ".cache", ".lock"
    );
    @Tool("Read project dir")
    public String readProjectDir(@P("path") String relativePath) {
        try {

            //逆向文件
            String projectName = AppConstant.VUE_PREFIX + "_" + appId + "_" + version;
            Path path = Path.of(projectName, relativePath);
            if(!path.toFile().exists()){
                return "File does not exist: " + relativePath;
            }
            StringBuilder content = new StringBuilder();
            content.append("File content[项目文件结构]: ");
            List<File> files = FileUtil.loopFiles(path.toFile(), file -> !shouldIgnore(file.getName()));

            File root = Path.of(projectName).toFile();
            files.stream()
                    .sorted((f1,f2) ->{

                        int dep1 = getRelativeDepth(root, f1);
                        int dep2 = getRelativeDepth(root, f2);
                        if(dep1 == dep2) {
                            return f1.getName().compareTo(f2.getName());
                        }
                        return dep1 - dep2;
                    })
                    .forEach(file->{
                        int dep = getRelativeDepth(root, file);
                        content.append(" ").append(" ".repeat(dep * 2)).append(file.getName()).append("\n");
                    });
            return content.toString();
        } catch (Exception e) {
            log.error("Error reading file: {}", e.getMessage());
            return "Error reading file: " + e.getMessage();
        }

    }
    /**

     * 计算文件相对于根目录的深度
     */
    private int getRelativeDepth(File root, File file) {
        Path rootPath = root.toPath();
        Path filePath = file.toPath();
        return rootPath.relativize(filePath).getNameCount() - 1;
    }


 private boolean shouldIgnore(String fileName) {
        return IGNORED_NAMES.contains(fileName) || IGNORED_EXTENSIONS.contains(fileName);
    }

    @Override
    public String getToolName() {
        return "readProjectDir";
    }

    @Override
    public String getDisplayName() {
        return "Read Project Dir";
    }

}
