package com.yosh.coding.artificalIntelligence.skill;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.util.Comparator;
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
    @Tool("List project files and return paths relative to the project root. Call this at most once before reading a target file.")
    public String readProjectDir(@P("path - Relative directory path, use '.' for the project root") String relativePath) {
        try {
            Path projectRoot = getProjectRoot(appId, version);
            String requestedPath = StrUtil.blankToDefault(relativePath, ".");
            Path path = resolveProjectPath(appId, version, requestedPath);
            if (!path.toFile().exists()) {
                return "File does not exist: " + relativePath;
            }
            if (!path.toFile().isDirectory()) {
                return "Path is not a directory: " + relativePath;
            }

            List<File> files = FileUtil.loopFiles(path.toFile(),
                    file -> !shouldIgnore(projectRoot, file.toPath()));
            StringBuilder content = new StringBuilder("Project files:\n");
            files.stream()
                    .map(file -> projectRoot.relativize(file.toPath().toAbsolutePath().normalize()))
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(file -> content.append("- ")
                            .append(file.toString().replace('\\', '/'))
                            .append('\n'));
            return content.toString();
        } catch (Exception e) {
            log.error("Error reading project directory: {}", e.getMessage());
            return "Error reading project directory: " + e.getMessage();
        }

    }

    private boolean shouldIgnore(Path projectRoot, Path file) {
        Path relativePath = projectRoot.relativize(file.toAbsolutePath().normalize());
        for (Path part : relativePath) {
            if (IGNORED_NAMES.contains(part.toString())) {
                return true;
            }
        }
        String fileName = file.getFileName().toString();
        return IGNORED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
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
