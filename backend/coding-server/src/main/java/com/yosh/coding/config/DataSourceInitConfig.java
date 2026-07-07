package com.yosh.coding.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Configuration
@Slf4j
public class DataSourceInitConfig {
    @Resource
    private DataSource dataSource;
    
    @PostConstruct
    public void init() throws SQLException {
        // 启动时主动获取连接，触发连接池初始化
        try (Connection conn = dataSource.getConnection()) {
            log.info("数据库连接池预初始化完成");
        }
    }
}
