package com.lexiang.server.service.runner;

import com.lexiang.server.service.redisService.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动后自动预热库存到 Redis
 * 实现 ApplicationRunner，Spring Boot 启动完成后自动执行
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockWarmupRunner implements ApplicationRunner {
    private final StockService stockService;
    @Override
    public void run(ApplicationArguments args) {
        log.info("===== 开始预热库存到 Redis =====");
        stockService.loadStockToRedis();
        log.info("===== 库存预热完成 =====");
    }
}
