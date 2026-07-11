package com.yosh.coding.config;

import com.yosh.common.OssEntry;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
//从yml拿
@Data
@ConfigurationProperties(prefix = "oss")
public class AliOssConfig {
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    @Bean
    public OssEntry AliOssConfig() {
        return OssEntry.builder()
                        .endpoint(endpoint)
                        .accessKeyId(accessKeyId)
                        .accessKeySecret(accessKeySecret)
                        .bucketName(bucketName)
                        .build();
    }
}
