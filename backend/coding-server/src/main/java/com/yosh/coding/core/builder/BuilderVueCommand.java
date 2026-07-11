package com.yosh.coding.core.builder;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.RuntimeUtil;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Component
public class BuilderVueCommand {

    private static final Pattern V_IF_PATTERN = Pattern.compile("\\bv-if\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern V_ELSE_IF_PATTERN = Pattern.compile("\\bv-else-if\\s*=\\s*\"([^\"]+)\"");

    /** 开发服务器进程（appId → Process），应用关闭时自动清理 */
    private final ConcurrentHashMap<Long, Process> devServers = new ConcurrentHashMap<>();

    @PreDestroy
    public void shutdownDevServers() {
        devServers.forEach((appId, process) -> {
            log.info("关闭开发服务器 appId={}", appId);
            process.destroyForcibly();
        });
        devServers.clear();
    }

    /**
     * 执行 npm install 命令（可被外部调用，用于预安装依赖）
     */
    public boolean executeNpmInstallOnly(File projectDir) {
        log.info("执行 npm install...");
        String command = String.format("%s install", buildCommand("npm"));
        return executeCommand(projectDir, command, 180);
    }

    /**
     * 执行 npm run build 命令（可被外部调用）
     */
    public boolean executeNpmBuildOnly(File projectDir) {
        log.info("执行 npm run build...");
        String command = String.format("%s run build", buildCommand("npm"));
        return executeCommand(projectDir, command, 120);
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }
    private String buildCommand(String baseCommand) {
        if (isWindows()) {
            return baseCommand + ".cmd";
        }
        return baseCommand;
    }

    /**
     * 执行命令
     *
     * @param workingDir     工作目录
     * @param command        命令字符串
     * @param timeoutSeconds 超时时间（秒）
     * @return 是否执行成功
     */
    private boolean executeCommand(File workingDir, String command, int timeoutSeconds) {
        Process process = null;
        try {
            log.info("在目录 {} 中执行命令: {}", workingDir.getAbsolutePath(), command);
            process = RuntimeUtil.exec(
                    null,
                    workingDir,
                    command.split("\\s+") // 命令分割为数组
            );
            
            // 异步读取输出流和错误流，避免阻塞
            StringBuilder outputBuilder = new StringBuilder();
            StringBuilder errorBuilder = new StringBuilder();

            Process finalProcess1 = process;
            Thread outputThread = new Thread(() -> {
                try (InputStream inputStream = finalProcess1.getInputStream()) {
                    String output = IoUtil.read(inputStream, StandardCharsets.UTF_8);
                    outputBuilder.append(output);
                } catch (Exception e) {
                    log.debug("读取输出流异常: {}", e.getMessage());
                }
            });

            Process finalProcess = process;
            Thread errorThread = new Thread(() -> {
                try (InputStream errorStream =  finalProcess.getErrorStream()) {
                    String error = IoUtil.read(errorStream, StandardCharsets.UTF_8);
                    errorBuilder.append(error);
                } catch (Exception e) {
                    log.debug("读取错误流异常: {}", e.getMessage());
                }
            });
            
            outputThread.start();
            errorThread.start();
            
            // 等待进程完成，设置超时
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                log.error("命令执行超时（{}秒），强制终止进程", timeoutSeconds);
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                return false;
            }
            
            // 等待输出读取完成
            outputThread.join(5000);
            errorThread.join(5000);
            
            int exitCode = process.exitValue();
            String output = outputBuilder.toString();
            String error = errorBuilder.toString();
            
            if (exitCode == 0) {
                log.info("命令执行成功: {}", command);
                if (!output.isEmpty()) {
                    log.debug("命令输出:\n{}", output);
                }
                return true;
            } else {
                log.error("命令执行失败，退出码: {}", exitCode);
                if (!error.isEmpty()) {
                    log.error("错误输出:\n{}", error);
                }
                if (!output.isEmpty()) {
                    log.error("标准输出:\n{}", output);
                }
                return false;
            }
        } catch (Exception e) {
            log.error("执行命令失败: {}, 错误信息: {}", command, e.getMessage(), e);
            return false;
        } finally {
            if (process != null) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroy();
            }
        }
    }

    /**
     * 构建 Vue 项目
     *
     * @param projectPath 项目根目录路径
     * @return 是否构建成功
     */
    public boolean buildProject(String projectPath) {
        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            log.error("项目目录不存在: {}", projectPath);
            return false;
        }
        // 检查 package.json 是否存在
        File packageJson = new File(projectDir, "package.json");
        if (!packageJson.exists()) {
            log.error("package.json 文件不存在: {}", packageJson.getAbsolutePath());
            return false;
        }
        log.info("开始构建 Vue 项目: {}", projectPath);
        sanitizeVueTemplates(projectDir);
        // 执行 npm install
        if (!executeNpmInstallOnly(projectDir)) {
            log.error("npm install 执行失败");
            cleanNpmInstallArtifacts(projectDir);
            return false;
        }
        // 执行 npm run build
        if (!executeNpmBuildOnly(projectDir)) {
            log.error("npm run build 执行失败");
            cleanNpmInstallArtifacts(projectDir);
            return false;
        }
        // 验证 dist 目录是否生成
        File distDir = new File(projectDir, "dist");
        if (!distDir.exists()) {
            log.error("构建完成但 dist 目录未生成: {}", distDir.getAbsolutePath());
            cleanNpmInstallArtifacts(projectDir);
            return false;
        }
        cleanNpmInstallArtifacts(projectDir);
        log.info("Vue 项目构建成功，dist 目录: {}", distDir.getAbsolutePath());
        return true;
    }

    /**
     * 仅执行构建（假设依赖可能已预安装），兜底检查 node_modules
     *
     * @param projectPath 项目根目录路径
     * @return 是否构建成功
     */
    public boolean buildOnly(String projectPath) {
        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            log.error("项目目录不存在: {}", projectPath);
            return false;
        }
        log.info("开始构建 Vue 项目（buildOnly）: {}", projectPath);
        sanitizeVueTemplates(projectDir);
        // 兜底检查：如果 node_modules 不存在则补安装
        File nodeModules = new File(projectDir, "node_modules");
        if (!nodeModules.exists()) {
            log.info("node_modules 不存在，补执行 npm install");
            if (!executeNpmInstallOnly(projectDir)) {
                log.error("npm install fallback failed");
                cleanNpmInstallArtifacts(projectDir);
                return false;
            }
        }
        // 执行 npm run build
        if (!executeNpmBuildOnly(projectDir)) {
            log.error("npm run build 执行失败");
            cleanNpmInstallArtifacts(projectDir);
            return false;
        }
        // 验证 dist 目录是否生成
        File distDir = new File(projectDir, "dist");
        if (!distDir.exists()) {
            log.error("构建完成但 dist 目录未生成: {}", distDir.getAbsolutePath());
            cleanNpmInstallArtifacts(projectDir);
            return false;
        }
        cleanNpmInstallArtifacts(projectDir);
        log.info("Vue 项目构建成功（buildOnly），dist 目录: {}", distDir.getAbsolutePath());
        return true;
    }

    /**
     * 停止指定应用的开发服务器
     */
    public void stopDevServer(long appId) {
        Process old = devServers.remove(appId);
        if (old != null && old.isAlive()) {
            log.info("关闭旧的开发服务器 appId={}", appId);
            old.descendants().forEach(ProcessHandle::destroyForcibly);
            old.destroyForcibly();
        }
    }

    /**
     * 启动 npm run dev 开发服务器（支持 HMR 热加载），返回预览 URL。
     * 同一 appId 的旧服务器会被自动关闭。
     *
     * @param projectDir 项目目录
     * @param appId      应用 ID（用于进程追踪）
     * @return 开发服务器 URL，失败返回 null
     */
    public String startDevServer(File projectDir, long appId) {
        stopDevServer(appId);
        log.info("启动开发服务器 appId={} path={}", appId, projectDir.getAbsolutePath());
        String command = String.format("%s run dev", buildCommand("npm"));
        try {
            Process process = RuntimeUtil.exec(
                    null,
                    projectDir,
                    command.split("\\s+")
            );
            // 读取 stdout 找 Vite 输出的 Local URL
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String url = null;
            long deadline = System.currentTimeMillis() + 20_000; // 20s 超时
            String line;
            while ((line = reader.readLine()) != null && System.currentTimeMillis() < deadline) {
                log.debug("dev server: {}", line);
                // 去掉 ANSI 颜色码，避免正则捕获到 http://localhost:[1m5174 这种非法 URL
                String clean = line.replaceAll("\u001B\\[[;\\d]*m", "");
                if (clean.contains("Local:")) {
                    // Vite 输出格式: "  ➜  Local:   http://localhost:5173/"
                    url = clean.replaceAll(".*(https?://[\\w.\\-:]+\\S*).*", "$1");
                    // 去掉末尾可能的斜杠后的不可见字符
                    url = url.replaceAll("/+$", "");
                    break;
                }
            }
            if (url != null) {
                devServers.put(appId, process);
                log.info("开发服务器已启动 appId={} url={}", appId, url);
                return url;
            }
            // 超时或未找到 URL，杀进程
            log.error("开发服务器启动超时或未获取到 URL appId={}", appId);
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            return null;
        } catch (Exception e) {
            log.error("启动开发服务器失败 appId={}: {}", appId, e.getMessage());
            return null;
        }
    }

    private void sanitizeVueTemplates(File projectDir) {
        File srcDir = new File(projectDir, "src");
        if (!srcDir.exists() || !srcDir.isDirectory()) {
            return;
        }
        try (Stream<Path> paths = Files.walk(srcDir.toPath())) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".vue"))
                    .forEach(path -> {
                        try {
                            if (fixInvalidTransitionElse(path)) {
                                log.warn("Fixed invalid transition v-else usage in {}", path);
                            }
                        } catch (IOException e) {
                            log.warn("Failed to sanitize Vue file {}: {}", path, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to scan Vue files in {}: {}", srcDir.getAbsolutePath(), e.getMessage());
        }
    }

    private boolean fixInvalidTransitionElse(Path vueFile) throws IOException {
        List<String> lines = Files.readAllLines(vueFile, StandardCharsets.UTF_8);
        boolean changed = false;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!isElseAfterTransition(lines, i)) {
                continue;
            }

            Matcher elseIfMatcher = V_ELSE_IF_PATTERN.matcher(line);
            if (elseIfMatcher.find()) {
                lines.set(i, replaceRange(line, elseIfMatcher.start(), elseIfMatcher.end(),
                        "v-if=\"" + elseIfMatcher.group(1).trim() + "\""));
                changed = true;
                continue;
            }

            if (line.contains("v-else")) {
                String previousCondition = findVIfConditionInTransition(lines, previousNonBlankLineIndex(lines, i));
                if (previousCondition != null) {
                    lines.set(i, replaceFirst(line, "v-else", "v-if=\"" + invertCondition(previousCondition) + "\""));
                    changed = true;
                }
            }
        }

        if (changed) {
            Files.write(vueFile, lines, StandardCharsets.UTF_8);
        }
        return changed;
    }

    private boolean isElseAfterTransition(List<String> lines, int lineIndex) {
        String line = lines.get(lineIndex);
        if (!line.contains("v-else")) {
            return false;
        }
        int previousIndex = previousNonBlankLineIndex(lines, lineIndex);
        return previousIndex >= 0 && lines.get(previousIndex).contains("</transition>");
    }

    private int previousNonBlankLineIndex(List<String> lines, int lineIndex) {
        for (int i = lineIndex - 1; i >= 0; i--) {
            if (!lines.get(i).trim().isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private String findVIfConditionInTransition(List<String> lines, int transitionEndIndex) {
        if (transitionEndIndex < 0) {
            return null;
        }
        String condition = null;
        for (int i = transitionEndIndex - 1; i >= 0 && transitionEndIndex - i <= 80; i--) {
            Matcher matcher = V_IF_PATTERN.matcher(lines.get(i));
            if (matcher.find()) {
                condition = matcher.group(1).trim();
            }
            if (lines.get(i).contains("<transition")) {
                return condition;
            }
        }
        return null;
    }

    private String invertCondition(String condition) {
        String trimmed = condition.trim();
        if (trimmed.startsWith("!") && !trimmed.startsWith("!=")) {
            return trimmed.substring(1).trim();
        }
        return "!(" + trimmed + ")";
    }

    private String replaceFirst(String line, String target, String replacement) {
        int index = line.indexOf(target);
        if (index < 0) {
            return line;
        }
        return replaceRange(line, index, index + target.length(), replacement);
    }

    private String replaceRange(String line, int start, int end, String replacement) {
        return line.substring(0, start) + replacement + line.substring(end);
    }

    private void releaseNodeModuleLocks(File nodeModules) {
        if (!isWindows() || !nodeModules.exists()) {
            return;
        }
        String nodeModulesPath = nodeModules.getAbsolutePath().toLowerCase();
        ProcessHandle.allProcesses().forEach(processHandle -> {
            processHandle.info().command().ifPresent(command -> {
                if (command.toLowerCase().startsWith(nodeModulesPath)) {
                    log.warn("Destroying locked node_modules process: {}", command);
                    processHandle.destroyForcibly();
                }
            });
        });
    }

    private void cleanNpmInstallArtifacts(File projectDir) {
        File nodeModules = new File(projectDir, "node_modules");
        File packageLock = new File(projectDir, "package-lock.json");

        // Windows 上 esbuild.exe 等原生二进制文件可能被进程短暂锁定，需要重试
        // 使用递增等待时间（2s/4s/6s/8s/10s），并在重试间主动触发 GC 释放文件句柄
        if (nodeModules.exists()) {
            boolean deleted = false;
            for (int i = 0; i < 5; i++) {
                try {
                    long waitMs = 2000L * (i + 1);
                    log.info("等待 {}ms 后清理 node_modules（第 {} 次）...", waitMs, i + 1);
                    Thread.sleep(waitMs);
                    releaseNodeModuleLocks(nodeModules);
                    // 主动请求 GC，帮助释放已被进程释放但尚未回收的文件句柄
                    System.gc();
                    FileUtil.del(nodeModules);
                    deleted = true;
                    log.info("node_modules 清理成功（第 {} 次）", i + 1);
                    break;
                } catch (Exception e) {
                    log.warn("第 {} 次清理 node_modules 失败: {}", i + 1, e.getMessage());
                }
            }
            if (!deleted) {
                log.error("node_modules 清理失败，可能需要手动删除: {}", nodeModules.getAbsolutePath());
            }
        }

        if (packageLock.exists()) {
            try {
                FileUtil.del(packageLock);
            } catch (Exception e) {
                log.warn("package-lock.json 清理失败: {}", e.getMessage());
            }
        }
    }

}
