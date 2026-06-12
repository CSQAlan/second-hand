package com.test.secondhand.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
public class RedisCacheHelper {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheHelper.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private ObjectMapper objectMapper;

    // 线程池用于执行延迟双删中的第二次删除
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    private static final String NULL_PLACEHOLDER = "{}";

    /**
     * 双写一致性：先改数据库，后删缓存 + 延迟双删
     */
    public void updateDbAndEvictCache(String cacheKey, Runnable dbUpdateTask) {
        log.info("[缓存一致性] 开始执行写操作，键: {}", cacheKey);
        
        // 1. 更新数据库
        dbUpdateTask.run();

        // 2. 第一次删除缓存
        redisTemplate.delete(cacheKey);
        log.info("[缓存一致性] 数据库更新完成，执行第一次缓存删除，键: {}", cacheKey);

        // 3. 异步延迟双删
        executorService.submit(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(500); // 延迟 500ms
                redisTemplate.delete(cacheKey);
                log.info("[缓存一致性] 延迟 500ms 结束，执行第二次缓存删除（双删），键: {}", cacheKey);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("[缓存一致性] 延迟双删线程被中断", e);
            }
        });
    }

    /**
     * 高防型获取缓存（防击穿、防穿透、防雪崩）
     */
    public <T> T getOrLoad(String cacheKey, String lockKey, Class<T> clazz, Supplier<T> dbLoader, long ttlSeconds) {
        // 1. 查询缓存
        String json = redisTemplate.opsForValue().get(cacheKey);
        
        if (json != null) {
            // 防穿透：如果是空值占位符，直接返回 null
            if (NULL_PLACEHOLDER.equals(json)) {
                log.debug("[缓存查询] 命中空占位符，直接返回空。键: {}", cacheKey);
                return null;
            }
            try {
                return objectMapper.readValue(json, clazz);
            } catch (Exception e) {
                log.error("[缓存解析] 反序列化失败，键: {}", cacheKey, e);
            }
        }

        // 2. 缓存未命中，加分布式锁（防击穿）
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 等待锁最多 3 秒，锁定后持锁 5 秒
            if (lock.tryLock(3, 5, TimeUnit.SECONDS)) {
                try {
                    // 3. 双重检测：拿锁后再次检查缓存，可能其他线程在此期间已经加载了缓存
                    json = redisTemplate.opsForValue().get(cacheKey);
                    if (json != null) {
                        if (NULL_PLACEHOLDER.equals(json)) {
                            return null;
                        }
                        return objectMapper.readValue(json, clazz);
                    }

                    // 4. 加载数据库
                    log.info("[缓存未命中] 双重检测未通过，执行数据库查询并加载缓存，键: {}", cacheKey);
                    T dbValue = dbLoader.get();

                    if (dbValue == null) {
                        // 防穿透：数据库无此数据，缓存空值占位符，设置较短过期时间（如 2 分钟）
                        redisTemplate.opsForValue().set(cacheKey, NULL_PLACEHOLDER, 2, TimeUnit.MINUTES);
                        log.info("[缓存加载] 数据库无数据，写入空值占位符防穿透，键: {}", cacheKey);
                        return null;
                    } else {
                        // 防雪崩：为基础 TTL 加上随机偏置（0 - 300 秒），防止大批缓存同时过期
                        long randomOffset = (long) (Math.random() * 300);
                        long finalTtl = ttlSeconds + randomOffset;
                        
                        String serialized = objectMapper.writeValueAsString(dbValue);
                        redisTemplate.opsForValue().set(cacheKey, serialized, finalTtl, TimeUnit.SECONDS);
                        log.info("[缓存加载] 数据加载成功，写入缓存，过期时间: {} 秒，键: {}", finalTtl, cacheKey);
                        return dbValue;
                    }
                } finally {
                    lock.unlock();
                }
            } else {
                // 没拿到锁的线程，退避等待并重试
                log.warn("[并发控制] 抢锁失败，退避重试。键: {}", cacheKey);
                TimeUnit.MILLISECONDS.sleep(100);
                return getOrLoad(cacheKey, lockKey, clazz, dbLoader, ttlSeconds);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[分布式锁] 获取锁被中断", e);
            return dbLoader.get(); // 中断后兜底，直接查库
        } catch (Exception e) {
            log.error("[缓存加载失败] 直接查库兜底", e);
            return dbLoader.get();
        }
    }
}
