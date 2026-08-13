package com.lexiang.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // 开启 Spring Task 定时任务
public class LexiangFoodApplication {
    public static void main(String[] args) {
        SpringApplication.run(LexiangFoodApplication.class, args);

    }

}
