package com.yosh.model.costants;

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
    String CODE_OUTPUT_ROOT_DIR = System.getProperty("user.dir") + "/src/main/resources/static/tmp/output_file";

    /**
     * 应用部署目录
     */
    String CODE_DEPLOY_ROOT_DIR = System.getProperty("user.dir") + "/src/main/resources/static/tmp/develop_file";
    /**
     * 应用部署域名
     */
    String CODE_DEPLOY_HOST = "http://localhost";

    Long DEFAULT_VERSION = 1L;

    String APP_VERSION_LOCK_KEY_PREFIX = "app:version:lock:";
}
