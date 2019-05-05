package com.sequenceiq.periscope.modul.rejected;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

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

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.periscope.aspects.AmbariRequestLogging;
import com.sequenceiq.periscope.monitor.evaluator.ClusterCreationEvaluator;
import com.sequenceiq.periscope.monitor.executor.EvaluatorExecutorRegistry;
import com.sequenceiq.periscope.monitor.executor.ExecutorServiceWithRegistry;
import com.sequenceiq.periscope.monitor.executor.LoggedExecutorService;
import com.sequenceiq.periscope.monitor.handler.PersistRejectedThreadExecutionHandler;
import com.sequenceiq.periscope.notification.HttpNotificationSender;
import com.sequenceiq.periscope.service.AmbariClientProvider;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.HistoryService;
import com.sequenceiq.periscope.service.RejectedThreadService;
import com.sequenceiq.periscope.service.StackCollectorService;
import com.sequenceiq.periscope.service.configuration.CloudbreakClientConfiguration;
import com.sequenceiq.periscope.service.ha.PeriscopeNodeConfig;
import com.sequenceiq.periscope.service.security.TlsSecurityService;
import com.sequenceiq.periscope.utils.LoggerUtils;
import com.sequenceiq.periscope.utils.MetricUtils;

@TestPropertySource(properties = "profile=dev")
public class StackCollectorContext {

    @Configuration
    @ComponentScan(basePackages = {"com.sequenceiq.periscope", "com.sequenceiq.cloudbreak.service"},
            useDefaultFilters = false,
            includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE,
                    value = {
                            PersistRejectedThreadExecutionHandler.class,
                            RejectedThreadService.class,
                            AmbariRequestLogging.class,
                            PeriscopeNodeConfig.class,
                            ClusterCreationEvaluator.class,
                            StackCollectorService.class,
                            ExecutorServiceWithRegistry.class,
                            EvaluatorExecutorRegistry.class,
                            LoggedExecutorService.class,
                            Clock.class
                    })
    )
    @MockBean({ClusterService.class, AmbariClientProvider.class, CloudbreakClientConfiguration.class, TlsSecurityService.class, HistoryService.class,
            HttpNotificationSender.class, MetricUtils.class, LoggerUtils.class, Clock.class})
    @EnableAsync
    public static class StackCollectorSpringConfig implements AsyncConfigurer {

        @Inject
        private PersistRejectedThreadExecutionHandler persistRejectedThreadExecutionHandler;

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
