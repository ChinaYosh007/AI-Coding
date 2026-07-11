package com.yosh.coding.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "spring.datasource")
public class MysqlConfig {

    private String url;
    private String username;
    private String password;
    private String driverClassName;

    @PostConstruct
    public void init() {
        log.info("MySQL 配置加载完成: url={}, username={}", url, username);
    }
}
