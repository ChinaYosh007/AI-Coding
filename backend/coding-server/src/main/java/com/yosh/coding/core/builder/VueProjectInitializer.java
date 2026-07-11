package com.yosh.coding.core.builder;

import cn.hutool.core.io.FileUtil;
import com.yosh.model.constants.AppConstant;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * Vue 项目初始化器
 * 从预构建的模板目录复制项目骨架（含完整 Vue 文件），然后异步安装依赖。
 * 模板位于 static/template/vue/，AI 只需在此基础上修改内容即可。
 *
 * 设计要点：
 * - copyTemplate() 同步执行（<1 秒），确保模板文件在 AI 生成代码前就位
 * - installDependencies() 异步执行（~30 秒），在后台跑 npm install，不阻塞用户
 * - buildOnly() 已有兜底：如果 node_modules 不存在会补执行 npm install
 */
@Slf4j
@Component
public class VueProjectInitializer {

    /**
     * 模板目录：static/template/vue/
     */
    private static final String VUE_TEMPLATE_DIR = AppConstant.getCodingServerDir()
            + File.separator + "src" + File.separator + "main" + File.separator + "resources"
            + File.separator + "static" + File.separator + "template" + File.separator + "vue";

    @Resource
    private BuilderVueCommand builderVueCommand;

    /**
     * 阶段一：从模板复制项目骨架（同步，<1 秒）
     * 只复制模板文件，不执行 npm install。
     * 调用后项目目录即包含完整的 Vue 文件，AI 可以立即开始写入。
     *
     * @param projectPath 项目输出路径
     * @return 是否复制成功
     */
    public boolean copyTemplate(String projectPath) {
        File projectDir = new File(projectPath);
        File templateDir = new File(VUE_TEMPLATE_DIR);
        log.info("复制 Vue 模板文件: {} -> {}", VUE_TEMPLATE_DIR, projectPath);

        // 如果目录已存在则删除重建
        if (projectDir.exists()) {
            log.info("目录已存在，清理重建: {}", projectPath);
            FileUtil.del(projectDir);
        }

        // 检查模板目录是否存在
        if (!templateDir.exists() || !templateDir.isDirectory()) {
            log.error("模板目录不存在: {}，请确认 static/template/vue/ 已创建", VUE_TEMPLATE_DIR);
            return false;
        }

        // 从模板复制所有文件（排除 node_modules，通过 npm install 安装）
        File[] templateFiles = templateDir.listFiles();
        if (templateFiles != null) {
            for (File file : templateFiles) {
                if ("node_modules".equals(file.getName())) {
                    log.info("跳过模板中的 node_modules，将通过 npm install 安装");
                    continue;
                }
                File target = new File(projectDir, file.getName());
                if (file.isDirectory()) {
                    FileUtil.copyContent(file, target, true);
                } else {
                    FileUtil.copy(file, target, true);
                }
            }
        }

        log.info("模板文件复制完成: {}", projectPath);
        return true;
    }

    /**
     * 阶段二：安装 npm 依赖（异步，~30 秒）
     * 在后台线程中执行 npm install，不阻塞 AI 代码生成流程。
     * buildOnly() 已内置兜底：如果构建时发现 node_modules 不存在，会补执行 npm install。
     *
     * @param projectPath 项目输出路径
     */
    public void installDependencies(String projectPath) {
        File projectDir = new File(projectPath);
        log.info("开始异步安装 npm 依赖: {}", projectPath);
        boolean result = builderVueCommand.executeNpmInstallOnly(projectDir);
        if (result) {
            log.info("npm 依赖安装完成: {}", projectPath);
        } else {
            log.error("npm 依赖安装失败: {}", projectPath);
        }
    }
}
