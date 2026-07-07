package com.etimesheet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池配置
 * 支持多用户并发访问，隔离不同用户的异步任务执行
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${async.executor.core-pool-size:10}")
    private int corePoolSize;

    @Value("${async.executor.max-pool-size:50}")
    private int maxPoolSize;

    @Value("${async.executor.queue-capacity:200}")
    private int queueCapacity;

    @Value("${async.executor.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    @Value("${async.executor.await-termination-seconds:30}")
    private int awaitTerminationSeconds;

    @Bean(name = "asyncExecutor")
    public ThreadPoolTaskExecutor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("async-");
        executor.setTaskDecorator(new ContextPropagatingDecorator());
        // 拒绝策略：由调用者线程执行（避免任务丢失）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        executor.initialize();
        return executor;
    }

    /**
     * 任务装饰器 - 将父线程的请求上下文（userId等）传播到子线程
     */
    static class ContextPropagatingDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable task) {
            // 捕获父线程的上下文
            Map<String, String> contextMap = org.slf4j.MDC.getCopyOfContextMap();
            return () -> {
                try {
                    if (contextMap != null) {
                        org.slf4j.MDC.setContextMap(contextMap);
                    }
                    task.run();
                } finally {
                    org.slf4j.MDC.clear();
                }
            };
        }
    }
}
