package com.sequenceiq.periscope.modul.rejected;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;
import org.springframework.test.context.TestPropertySource;

import com.sequenceiq.cloudbreak.auth.security.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.periscope.aspects.RequestLogging;
import com.sequenceiq.periscope.domain.ClusterManagerVariant;
import com.sequenceiq.periscope.monitor.ClusterManagerHostHealthMonitor;
import com.sequenceiq.periscope.monitor.evaluator.ClusterManagerHostHealthEvaluator;
import com.sequenceiq.periscope.monitor.evaluator.ClusterManagerSpecificHostHealthEvaluator;
import com.sequenceiq.periscope.monitor.evaluator.EventPublisher;
import com.sequenceiq.periscope.monitor.evaluator.ambari.AmbariAgentHealthEvaluator;
import com.sequenceiq.periscope.monitor.executor.EvaluatorExecutorRegistry;
import com.sequenceiq.periscope.monitor.executor.ExecutorServiceWithRegistry;
import com.sequenceiq.periscope.monitor.handler.PersistRejectedThreadExecutionHandler;
import com.sequenceiq.periscope.repository.FailedNodeRepository;
import com.sequenceiq.periscope.service.AmbariClientProvider;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.RejectedThreadService;
import com.sequenceiq.periscope.service.configuration.CloudbreakClientConfiguration;
import com.sequenceiq.periscope.service.ha.PeriscopeNodeConfig;
import com.sequenceiq.periscope.utils.MetricUtils;

@TestPropertySource(properties = "profile=dev")
public class RejectedThreadContext {

    @Configuration
    @ComponentScan(basePackages = {"com.sequenceiq.periscope", "com.sequenceiq.cloudbreak.service"},
            useDefaultFilters = false,
            includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE,
                    value = {
                            ClusterManagerHostHealthMonitor.class,
                            PersistRejectedThreadExecutionHandler.class,
                            RejectedThreadService.class,
                            RequestLogging.class,
                            PeriscopeNodeConfig.class,
                            ClusterManagerHostHealthEvaluator.class,
                            AmbariAgentHealthEvaluator.class,
                            EventPublisher.class,
                            ExecutorServiceWithRegistry.class,
                            EvaluatorExecutorRegistry.class,
                            Clock.class
                    })
    )
    @MockBean({Clock.class, ClusterService.class, AmbariClientProvider.class, CloudbreakClientConfiguration.class,
            MetricUtils.class, InternalCrnBuilder.class, FailedNodeRepository.class})
    @EnableAsync
    public static class SpringConfig implements AsyncConfigurer {

        @Inject
        private PersistRejectedThreadExecutionHandler persistRejectedThreadExecutionHandler;

        @Inject
        private List<ClusterManagerSpecificHostHealthEvaluator> hostHealthEvaluators;

        @Bean("periscopeListeningScheduledExecutorService")
        ExecutorService listeningScheduledExecutorService() {
            return getThreadPoolExecutorFactoryBean().getObject();
        }

        @Bean
        public ThreadPoolExecutorFactoryBean getThreadPoolExecutorFactoryBean() {
            ThreadPoolExecutorFactoryBean executorFactoryBean = new ThreadPoolExecutorFactoryBean();
            executorFactoryBean.setCorePoolSize(1);
            executorFactoryBean.setMaxPoolSize(2);
            executorFactoryBean.setQueueCapacity(2);
            executorFactoryBean.setRejectedExecutionHandler(persistRejectedThreadExecutionHandler);
            return executorFactoryBean;
        }

        @Bean
        public Map<ClusterManagerVariant, ClusterManagerSpecificHostHealthEvaluator> hostHealthEvaluatorMap() {
            return hostHealthEvaluators.stream()
                    .collect(Collectors.toMap(
                            ClusterManagerSpecificHostHealthEvaluator::getSupportedClusterManagerVariant,
                            hostHealthEvaluator -> hostHealthEvaluator
                    ));
        }

        @Override
        public Executor getAsyncExecutor() {
            return getThreadPoolExecutorFactoryBean().getObject();
        }

        @Override
        public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
            return new SimpleAsyncUncaughtExceptionHandler();
        }
    }
}
