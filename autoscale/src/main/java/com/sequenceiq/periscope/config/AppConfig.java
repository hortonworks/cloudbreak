package com.sequenceiq.periscope.config;

import java.io.IOException;
import java.util.concurrent.Executor;

import javax.ws.rs.client.Client;

import org.quartz.simpl.SimpleJobFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.sequenceiq.cloudbreak.client.IdentityClient;
import com.sequenceiq.cloudbreak.client.RestClient;
import com.sequenceiq.cloudbreak.client.config.ConfigKey;

import freemarker.template.TemplateException;

@Configuration
@EnableAsync
@EnableScheduling
public class AppConfig implements AsyncConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppConfig.class);

    @Value("${periscope.threadpool.core.size:50}")
    private int corePoolSize;
    @Value("${periscope.threadpool.max.size:500}")
    private int maxPoolSize;
    @Value("${periscope.threadpool.queue.size:1000}")
    private int queueCapacity;
    @Value("${periscope.client.id}")
    private String clientId;
    @Value("${periscope.identity.server.url}")
    private String identityServerUrl;
    @Value("${rest.debug:false}")
    private boolean restDebug;
    @Value("${cert.validation:true}")
    private boolean certificateValidation;

    @Bean
    public ThreadPoolExecutorFactoryBean getThreadPoolExecutorFactoryBean() {
        ThreadPoolExecutorFactoryBean executorFactoryBean = new ThreadPoolExecutorFactoryBean();
        executorFactoryBean.setCorePoolSize(corePoolSize);
        executorFactoryBean.setMaxPoolSize(maxPoolSize);
        executorFactoryBean.setQueueCapacity(queueCapacity);
        return executorFactoryBean;
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
        SchedulerFactoryBean scheduler = new SchedulerFactoryBean();
        scheduler.setTaskExecutor(getAsyncExecutor());
        scheduler.setAutoStartup(true);
        scheduler.setJobFactory(new SimpleJobFactory());
        return scheduler;
    }

    @Bean
    public IdentityClient identityClient() {
        return new IdentityClient(identityServerUrl, clientId, new ConfigKey(certificateValidation, restDebug));
    }

    @Bean
    public Client restClient() {
        return RestClient.get(new ConfigKey(certificateValidation, restDebug));
    }

    @Bean
    public freemarker.template.Configuration freemarkerConfiguration() throws IOException, TemplateException {
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }

    @Override
    public Executor getAsyncExecutor() {
        try {
            return getThreadPoolExecutorFactoryBean().getObject();
        } catch (Exception e) {
            LOGGER.error("Error creating task executor.", e);
        }
        return null;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}