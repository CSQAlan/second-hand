package com.test.secondhand.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync // 启用 Spring 异步任务支持
public class ThreadPoolConfig {

    @Value("${seckill.thread-pool.core-pool-size:10}")
    private int corePoolSize;

    @Value("${seckill.thread-pool.max-pool-size:50}")
    private int maxPoolSize;

    @Value("${seckill.thread-pool.queue-capacity:1000}")
    private int queueCapacity;

    @Value("${seckill.thread-pool.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    @Bean(name = "seckillExecutor")
    public Executor seckillExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("seckill-async-");
        
        // 拒绝策略：当队列满且最大线程数也达到时，由提交请求的线程直接同步执行（降级写入）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.initialize();
        return executor;
    }
}
