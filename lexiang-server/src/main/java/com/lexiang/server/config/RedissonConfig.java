package com.lexiang.server.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 分布式锁配置
 *
 * 替代硬编码的 redisson.yaml，动态读取 application.yaml 中的 spring.data.redis 配置
 * 本地环境：localhost:6379  →  Docker 环境：redis:6379
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + host + ":" + port;

        if (password != null && !password.isEmpty()) {
            config.useSingleServer()
                    .setAddress(address)
                    .setPassword(password)
                    .setDatabase(0)
                    .setConnectionPoolSize(8)
                    .setConnectionMinimumIdleSize(2)
                    .setConnectTimeout(10000)
                    .setTimeout(3000)
                    .setRetryAttempts(3)
                    .setRetryInterval(1500);
        } else {
            config.useSingleServer()
                    .setAddress(address)
                    .setDatabase(0)
                    .setConnectionPoolSize(8)
                    .setConnectionMinimumIdleSize(2);
        }

        return Redisson.create(config);
    }
}
