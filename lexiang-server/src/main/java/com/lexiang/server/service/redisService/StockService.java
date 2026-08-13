package com.lexiang.server.service.redisService;

import com.lexiang.server.entity.Dish;
import com.lexiang.server.mapper.DishMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 库存服务 — Redis Lua 原子扣减
 * 1. Lua 脚本在 Redis 中单线程原子执行，查询+扣减，杜绝并发超卖
 * 2. 启动时从 MySQL 加载库存到 Redis，后续读写都走 Redis
 * 3. Redis 和 MySQL 最终一致性：支付成功后双写，定时回刷兜底
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {
    private final StringRedisTemplate redisTemplate;
    private final DishMapper dishMapper;

    /** 库存 Key 前缀：stock:{dishId} → 库存数量 */
    private static final String STOCK_KEY = "stock:";

    /**
     * Lua 原子扣减库存脚本
     * KEYS[1] = stock:{dishId}
     * ARGV[1] = 扣减数量
     * 返回值：
     *   >= 0 → 扣减后剩余库存（成功）
     *   -1   → 库存不足（失败）
     * 【原理】Redis 执行 Lua 脚本时是原子的，不会被打断
     * 两个请求同时读到 stock=5，传统 GET+DECRBY 会超卖
     * Lua 脚本保证了 GET 判断和 DECRBY 是一步完成
     */
    private static final String DEDUCT_LUA =
            "local stock = tonumber(redis.call('GET', KEYS[1])) " +
                    "if stock == nil then return -2 end " +      // key不存在
                    "if stock >= tonumber(ARGV[1]) then " +
                    "  redis.call('DECRBY', KEYS[1], ARGV[1]) " +
                    "  return stock - tonumber(ARGV[1]) " +      // 返回剩余库存（数值，避免 StringRedisTemplate GET 返回字符串导致 ClassCastException）
                    "else return -1 end";                   // 库存不足

    /**
     * Lua 原子恢复库存脚本（取消/超时回滚）
     */
    private static final String RESTORE_LUA =
            "if redis.call('EXISTS', KEYS[1]) == 1 then " +
                    "  redis.call('INCRBY', KEYS[1], ARGV[1]) " +
                    "end " +
                    "return 1";
/**
 * 启动时加载全部菜品库存到 Redis
 * 在应用启动后自动执行一次
 */
public void loadStockToRedis() {
    List<Dish> dishes = dishMapper.selectList(null);
    int count = 0;
    for (Dish dish : dishes) {
        if (dish.getStock() != null) {
            redisTemplate.opsForValue().set(STOCK_KEY + dish.getId(), String.valueOf(dish.getStock()));
            count++;
        }
    }
    log.info("库存预热完成，加载 {} 个菜品库存到 Redis", count);
}
    /**
     * 原子扣减库存（Lua 脚本执行）
     * @param dishId   菜品ID
     * @param quantity 扣减数量
     * @return true=扣减成功，false=库存不足
     */
    public boolean deduct(Long dishId, Integer quantity){
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(DEDUCT_LUA, Long.class);
        Long result = redisTemplate.execute(
                script, Collections.singletonList(STOCK_KEY +dishId),
                String.valueOf(quantity)
        );
        if (result == null || result == -2) {
            // Redis 中没有库存缓存，从 DB 加载后重试一次
            log.warn("库存缓存未命中 dishId={}，从 DB 加载", dishId);
            Dish dish = dishMapper.selectById(dishId);
            if (dish == null || dish.getStock() == null) return false;
            redisTemplate.opsForValue().set(STOCK_KEY + dishId, String.valueOf(dish.getStock()));
            // 重试扣减
            result = redisTemplate.execute(script,
                    Collections.singletonList(STOCK_KEY + dishId),
                    String.valueOf(quantity));
        }
        if (result != null && result >= 0) {
            log.debug("Redis 扣库存成功 dishId={} qty={} 剩余={}", dishId, quantity, result);
            return true;
        }
        log.debug("Redis 扣库存失败 dishId={} qty={} result={}", dishId, quantity, result);
        return false;
    }
    /**
     * 原子恢复库存（取消/超时回滚）
     */
    public void restore(Long dishId, Integer quantity) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(RESTORE_LUA, Long.class);
        redisTemplate.execute(script,
                Collections.singletonList(STOCK_KEY + dishId),
                String.valueOf(quantity));
        log.debug("Redis 恢复库存 dishId={} qty={}", dishId, quantity);
    }
    /**
     * 同步 Redis 库存到 MySQL（双写保证最终一致性）
     * 在支付成功、定时回刷时调用
     */
    public void syncToMySQL(Long dishId) {
        String stockStr = redisTemplate.opsForValue().get(STOCK_KEY + dishId);
        if (stockStr != null) {
            Dish dish = dishMapper.selectById(dishId);
            if (dish != null) {
                dish.setStock(Integer.parseInt(stockStr));
                dishMapper.updateById(dish);
            }
        }
    }
}

