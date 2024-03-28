package com.sequenceiq.freeipa.configuration;

import static com.sequenceiq.cloudbreak.service.executor.DelayedExecutorService.DELAYED_TASK_EXECUTOR;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.concurrent.CommonExecutorServiceFactory;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteria;
import com.sequenceiq.freeipa.service.filter.NetworkFilterProvider;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;

@Configuration
@EnableRetry
@EnableScheduling
public class AppConfig {

    @Value("${freeipa.intermediate.threadpool.core.size}")
    private int intermediateCorePoolSize;

    @Value("${freeipa.intermediate.threadpool.capacity.size}")
    private int intermediateQueueCapacity;

    @Value("${freeipa.intermediate.threadpool.termination.seconds:60}")
    private int intermediateAwaitTerminationSeconds;

    @Value("${freeipa.delayed.threadpool.core.size}")
    private int delayedCorePoolSize;

    @Value("${spring.threads.virtual.enabled:false}")
    private boolean virtualThreadsAvailable;

    @Inject
    private List<NetworkFilterProvider> networkFilterProviders;

    @Inject
    private MeterRegistry meterRegistry;

    @Inject
    private CommonExecutorServiceFactory commonExecutorServiceFactory;

    @Bean
    @Primary
    public AsyncTaskExecutor intermediateBuilderExecutor() {
        return commonExecutorServiceFactory.newAsyncTaskExecutor("intermediateBuilderExecutor", virtualThreadsAvailable, intermediateCorePoolSize,
                intermediateQueueCapacity, intermediateAwaitTerminationSeconds);
    }

    @Bean
    public Map<CloudPlatform, NetworkFilterProvider> networkFilterProviderMap() {
        Map<CloudPlatform, NetworkFilterProvider> result = new HashMap<>();
        for (NetworkFilterProvider networkFilterProvider : networkFilterProviders) {
            result.put(networkFilterProvider.cloudPlatform(), networkFilterProvider);
        }
        return result;
    }

    @Bean
    public ExitCriteria stackBasedExitCriteria() {
        return new StackBasedExitCriteria();
    }

    @Bean(name = DELAYED_TASK_EXECUTOR)
    public ScheduledExecutorService delayedTaskExecutor() {
        ThreadPoolTaskScheduler executor = new ThreadPoolTaskScheduler();
        executor.setPoolSize(delayedCorePoolSize);
        executor.setThreadNamePrefix("delayedExecutor-");
        executor.initialize();
        return ExecutorServiceMetrics.monitor(meterRegistry, executor.getScheduledExecutor(), DELAYED_TASK_EXECUTOR, "threadpool");
    }
}
