package com.yosh.coding.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@Slf4j
public class RedisCacheMangerConfig {
    /**
     * 查找相关的redis connect factory
     */
    @Resource
    private RedisConnectionFactory redisConnectionFactory;
    public static final int DEFAULT_CACHE_EXPIRE = 30;

    @Bean
    public CacheManager cacheManager() {
        /**
         *ObjectMapper 是 Jackson 提供的“Java 对象和 JSON 之间的转换器”。
         */
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        RedisCacheConfiguration redisCacheMangerConfig = RedisCacheConfiguration.defaultCacheConfig()
                            .entryTtl(Duration.ofMinutes(DEFAULT_CACHE_EXPIRE))
                // 禁用缓存 null 值
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        // 使用 StringRedisSerializer 序列化 key
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        // 使用 GenericJackson2JsonRedisSerializer 序列化 value
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)));
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(redisCacheMangerConfig)
                .build();

    }
}
