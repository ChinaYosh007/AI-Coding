package com.yosh.coding;

import com.mybatisflex.codegen.Generator;
import com.mybatisflex.codegen.config.ColumnConfig;
import com.mybatisflex.codegen.config.GlobalConfig;
import com.yosh.common.PageResquest;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class Codegen {
    /***
     * #mysql config
     * MYSQL_URL=47.99.125.14
     * MYSQL_PORT=33099
     * MYSQL_DATABASE=yu_ai_code_mother
     * MYSQL_ACCOUNT=root
     * MYSQL_PASSWORD=147258369
     */

    @Value("${MYSQL_URL}")
    private String URL;
    @Value("${MYSQL_PORT}")
    private String PORT;
    @Value("${MYSQL_ACCOUNT}")
    private String account;
    @Value("${MYSQL_PASSWORD}")
    private String  password;
    @Value("${MYSQL_DATABASE}")
    private String database;
    private static final String[] TABLE_NAMES = {"app"};
    @Test
    public  void test() {
        //配置数据源
        HikariDataSource dataSource = new HikariDataSource();
        String LINK_URL = "jdbc:mysql://" + URL + ":" + PORT + "/" + database + "?characterEncoding=utf-8";
        dataSource.setJdbcUrl(LINK_URL);
        dataSource.setUsername(account);
        dataSource.setPassword(password);

        //创建配置内容，两种风格都可以。
        GlobalConfig globalConfig = createGlobalConfigUseStyle1();
        //GlobalConfig globalConfig = createGlobalConfigUseStyle2();

        //通过 datasource 和 globalConfig 创建代码生成器
        Generator generator = new Generator(dataSource, globalConfig);

        //生成代码
        generator.generate();
    }

    public static GlobalConfig createGlobalConfigUseStyle1() {
        // 创建配置内容
        GlobalConfig globalConfig = new GlobalConfig();

        // 设置根包，建议先生成到一个临时目录下，生成代码后，再移动到项目目录下
        globalConfig.getPackageConfig()
                .setBasePackage("com.yosh.coding.genresult");

        // 设置表前缀和只生成哪些表，setGenerateTable 未配置时，生成所有表
        globalConfig.getStrategyConfig()
                .setGenerateTable(TABLE_NAMES)
                // 设置逻辑删除的默认字段名称
                .setLogicDeleteColumn("isDelete");

        // 设置生成 entity 并启用 Lombok
        globalConfig.enableEntity()
                .setWithLombok(true)
                .setJdkVersion(21);

        // 设置生成 mapper
        globalConfig.enableMapper();
        globalConfig.enableMapperXml();

        // 设置生成 service
        globalConfig.enableService();
        globalConfig.enableServiceImpl();

        // 设置生成 controller
        globalConfig.enableController();

        return globalConfig;
    }

}