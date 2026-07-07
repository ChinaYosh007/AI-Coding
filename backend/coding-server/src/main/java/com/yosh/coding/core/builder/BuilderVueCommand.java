package com.yosh.coding.core.builder;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class BuilderVueCommand {
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
        // 兜底检查：如果 node_modules 不存在则补安装
        File nodeModules = new File(projectDir, "node_modules");
        if (!nodeModules.exists()) {
            log.info("node_modules 不存在，补执行 npm install");
            if (!executeNpmInstallOnly(projectDir)) {
                log.error("npm install 补执行失败");
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

    private void cleanNpmInstallArtifacts(File projectDir) {
        FileUtil.del(new File(projectDir, "node_modules"));
        FileUtil.del(new File(projectDir, "package-lock.json"));
    }

}
