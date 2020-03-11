package com.sequenceiq.freeipa.configuration;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.sequenceiq.cloudbreak.concurrent.CompositeTaskDecorator;
import com.sequenceiq.cloudbreak.concurrent.MDCCleanerTaskDecorator;
import com.sequenceiq.cloudbreak.concurrent.ActorCrnTaskDecorator;

@Configuration
public class UsersyncConfig {

    public static final String USERSYNC_TASK_EXECUTOR = "USERSYNC_TASK_EXECUTOR";

    @Value("${freeipa.usersync.threadpool.core.size}")
    private int usersyncCorePoolSize;

    @Value("${freeipa.usersync.threadpool.capacity.size}")
    private int usersyncQueueCapacity;

    @Bean(name = USERSYNC_TASK_EXECUTOR)
    public AsyncTaskExecutor usersyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(usersyncCorePoolSize);
        executor.setQueueCapacity(usersyncQueueCapacity);
        executor.setThreadNamePrefix("usersyncExecutor-");
        executor.setTaskDecorator(
                new CompositeTaskDecorator(
                        List.of(new MDCCleanerTaskDecorator(), new ActorCrnTaskDecorator())));
        executor.initialize();
        return executor;
    }
}
