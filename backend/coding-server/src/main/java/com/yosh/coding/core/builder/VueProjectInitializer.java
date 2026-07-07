package com.yosh.coding.core.builder;

import cn.hutool.core.io.FileUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * Vue 项目初始化器
 * 在 AI 生成代码之前，预先创建项目骨架并安装依赖
 */
@Slf4j
@Component
public class VueProjectInitializer {

    private static final String PACKAGE_JSON = """
            {
              "scripts": {
                "dev": "vite",
                "build": "vite build"
              },
              "dependencies": {
                "vue": "^3.3.4",
                "vue-router": "^4.2.4"
              },
              "devDependencies": {
                "@vitejs/plugin-vue": "^4.2.3",
                "vite": "^4.4.5"
              }
            }
            """;

    private static final String VITE_CONFIG = """
            import { defineConfig } from 'vite'
            import vue from '@vitejs/plugin-vue'
            import { fileURLToPath, URL } from 'node:url'

            export default defineConfig({
              base: './',
              plugins: [vue()],
              resolve: {
                alias: {
                  '@': fileURLToPath(new URL('./src', import.meta.url))
                }
              }
            })
            """;

    private static final String INDEX_HTML = """
            <!DOCTYPE html>
            <html lang="zh-CN">
              <head>
                <meta charset="UTF-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <title>App</title>
              </head>
              <body>
                <div id="app"></div>
                <script type="module" src="/src/main.js"></script>
              </body>
            </html>
            """;

    @Resource
    private BuilderVueCommand builderVueCommand;

    /**
     * 初始化 Vue 项目骨架并预安装依赖
     *
     * @param projectPath 项目路径
     * @return 是否安装成功
     */
    public boolean initialize(String projectPath) {
        File projectDir = new File(projectPath);
        log.info("初始化 Vue 项目: {}", projectPath);

        // 如果目录已存在则删除重建
        if (projectDir.exists()) {
            log.info("目录已存在，删除重建: {}", projectPath);
            FileUtil.del(projectDir);
        }

        // 创建目录结构
        FileUtil.mkdir(projectDir);
        FileUtil.mkdir(new File(projectDir, "src"));

        // 写入预置文件
        FileUtil.writeString(PACKAGE_JSON, new File(projectDir, "package.json"), StandardCharsets.UTF_8);
        FileUtil.writeString(VITE_CONFIG, new File(projectDir, "vite.config.js"), StandardCharsets.UTF_8);
        FileUtil.writeString(INDEX_HTML, new File(projectDir, "index.html"), StandardCharsets.UTF_8);

        // 执行 npm install
        boolean result = builderVueCommand.executeNpmInstallOnly(projectDir);
        if (result) {
            log.info("Vue 项目初始化完成: {}", projectPath);
        } else {
            log.error("Vue 项目 npm install 失败: {}", projectPath);
        }
        return result;
    }
}
