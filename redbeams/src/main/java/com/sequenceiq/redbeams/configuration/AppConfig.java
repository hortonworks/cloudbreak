package com.sequenceiq.redbeams.configuration;

import static com.sequenceiq.cloudbreak.service.executor.DelayedExecutorService.DELAYED_TASK_EXECUTOR;

import java.util.concurrent.ScheduledExecutorService;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.sequenceiq.cloudbreak.concurrent.CommonExecutorServiceFactory;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;

@Configuration
@EnableRetry
public class AppConfig implements ResourceLoaderAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppConfig.class);

    // from com.sequenceiq.cloudbreak.auth.filter.AuthFilterConfiguration
    // redbeams should probably control this itself
    // private static final int BEAN_ORDER_CRN_FILTER = 0;

    private static final int BEAN_ORDER_REQUEST_ID_GENERATING_FILTER = 100;

    private static final int BEAN_ORDER_REQUEST_ID_FILTER = 110;

    @Value("${redbeams.etc.config.dir}")
    private String etcConfigDir;

    @Value("${redbeams.intermediate.threadpool.core.size:}")
    private int intermediateCorePoolSize;

    @Value("${redbeams.intermediate.threadpool.capacity.size:}")
    private int intermediateQueueCapacity;

    @Value("${redbeams.intermediate.threadpool.termination.seconds:60}")
    private int intermediateAwaitTerminationSeconds;

    @Value("${redbeams.delayed.threadpool.core.size:10}")
    private int delayedCorePoolSize;

    @Value("${redbeams.client.id}")
    private String clientId;

    @Value("${rest.debug}")
    private boolean restDebug;

    @Value("${cert.validation}")
    private boolean certificateValidation;

    @Value("${cert.ignorePreValidation}")
    private boolean ignorePreValidation;

    @Value("${spring.threads.virtual.enabled:false}")
    private boolean virtualThreadsAvailable;

    @Inject
    private MeterRegistry meterRegistry;

    @Inject
    private CommonExecutorServiceFactory commonExecutorServiceFactory;

    private ResourceLoader resourceLoader;

    @Bean
    public AsyncTaskExecutor intermediateBuilderExecutor() {
        return commonExecutorServiceFactory.newAsyncTaskExecutor("intermediateBuilderExecutor", virtualThreadsAvailable, intermediateCorePoolSize,
                intermediateQueueCapacity, intermediateAwaitTerminationSeconds);
    }

    @Bean(name = DELAYED_TASK_EXECUTOR)
    public ScheduledExecutorService delayedTaskExecutor() {
        ThreadPoolTaskScheduler executor = new ThreadPoolTaskScheduler();
        executor.setPoolSize(delayedCorePoolSize);
        executor.setThreadNamePrefix("delayedExecutor-");
        executor.initialize();
        return ExecutorServiceMetrics.monitor(meterRegistry, executor.getScheduledExecutor(), DELAYED_TASK_EXECUTOR, "threadpool");
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
