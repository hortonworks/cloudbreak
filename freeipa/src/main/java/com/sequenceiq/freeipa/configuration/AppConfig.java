package com.sequenceiq.freeipa.configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.sequenceiq.cloudbreak.concurrent.MDCCleanerTaskDecorator;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteria;

@Configuration
@EnableRetry
@EnableScheduling
public class AppConfig {

    @Value("${freeipa.intermediate.threadpool.core.size}")
    private int intermediateCorePoolSize;

    @Value("${freeipa.intermediate.threadpool.capacity.size}")
    private int intermediateQueueCapacity;

    @Inject
    private List<HostOrchestrator> hostOrchestrators;

    @Value("${freeipa.autosync.threadpool.core.size:50}")
    private int corePoolSize;

    @Value("${freeipa.autosync.threadpool.max.size:500}")
    private int maxPoolSize;

    @Value("${freeipa.autosync.threadpool.queue.size:1000}")
    private int queueCapacity;

    @Bean
    @Primary
    public AsyncTaskExecutor intermediateBuilderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(intermediateCorePoolSize);
        executor.setQueueCapacity(intermediateQueueCapacity);
        executor.setThreadNamePrefix("intermediateBuilderExecutor-");
        executor.setTaskDecorator(new MDCCleanerTaskDecorator());
        executor.initialize();
        return executor;
    }

    @Bean
    public Map<String, HostOrchestrator> hostOrchestrators() {
        Map<String, HostOrchestrator> map = new HashMap<>();
        for (HostOrchestrator hostOrchestrator : hostOrchestrators) {
            hostOrchestrator.init(new StackBasedExitCriteria());
            map.put(hostOrchestrator.name(), hostOrchestrator);
        }
        return map;
    }

    @Bean
    public ThreadPoolExecutorFactoryBean getThreadPoolExecutorFactoryBean() {
        ThreadPoolExecutorFactoryBean executorFactoryBean = new ThreadPoolExecutorFactoryBean();
        executorFactoryBean.setCorePoolSize(corePoolSize);
        executorFactoryBean.setMaxPoolSize(maxPoolSize);
        executorFactoryBean.setQueueCapacity(queueCapacity);
        return executorFactoryBean;
    }
}
