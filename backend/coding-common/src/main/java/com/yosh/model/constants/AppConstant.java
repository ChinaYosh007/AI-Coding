package com.yosh.model.constants;

import java.io.File;

public interface AppConstant {

    /**
     * 精选应用的优先级
     */
    Integer GOOD_APP_PRIORITY = 99;

    /**
     * 默认应用优先级
     */
    Integer DEFAULT_APP_PRIORITY = 0;
    /**
     * 应用生成目录
     */
    String CODE_OUTPUT_ROOT_DIR = getCodingServerDir() + File.separator + "src" + File.separator + "main"
            + File.separator + "resources" + File.separator + "static" + File.separator + "tmp"
            + File.separator + "output_file";

    /**
     * 应用部署目录
     */
    String CODE_DEPLOY_ROOT_DIR = getCodingServerDir() + File.separator + "src" + File.separator + "main"
            + File.separator + "resources" + File.separator + "static" + File.separator + "tmp"
            + File.separator + "develop_file";
    /**
     * 应用部署域名
     */
    String CODE_DEPLOY_HOST = "http://localhost:8123";

    Long DEFAULT_VERSION = 1L;

    String APP_VERSION_LOCK_KEY_PREFIX = "app:version:lock:";
    String VUE_PREFIX = "vue_project_";

    static String getCodingServerDir() {
        File userDir = new File(System.getProperty("user.dir"));
        if ("coding-server".equals(userDir.getName())) {
            return userDir.getAbsolutePath();
        }
        File codingServerDir = new File(userDir, "coding-server");
        if (codingServerDir.exists() && codingServerDir.isDirectory()) {
            return codingServerDir.getAbsolutePath();
        }
        return userDir.getAbsolutePath();
    }
}
