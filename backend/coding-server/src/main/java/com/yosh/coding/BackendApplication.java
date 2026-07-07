package com.yosh.coding;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@MapperScan("com.yosh.coding.mapper")
public class
BackendApplication {

    public static void main(String[] args) {

        SpringApplication.run(BackendApplication.class, args);
        //启动mysql
    }

}
