package com.sequenceiq.redbeams.configuration;

import java.io.IOException;
import java.security.Security;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.concurrent.MDCCleanerTaskDecorator;
import com.sequenceiq.redbeams.filter.RequestIdFilter;
import com.sequenceiq.redbeams.filter.RequestIdGeneratingFilter;
import com.sequenceiq.redbeams.service.ThreadBasedRequestIdProvider;

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

    @Value("${redbeams.threadpool.core.size:}")
    private int corePoolSize;

    @Value("${redbeams.threadpool.capacity.size:}")
    private int queueCapacity;

    @Value("${redbeams.intermediate.threadpool.core.size:}")
    private int intermediateCorePoolSize;

    @Value("${redbeams.intermediate.threadpool.capacity.size:}")
    private int intermediateQueueCapacity;

    // @Value("${cb.container.threadpool.core.size:}")
    // private int containerCorePoolSize;

    // @Value("${cb.container.threadpool.capacity.size:}")
    // private int containerteQueueCapacity;

    @Value("${redbeams.client.id}")
    private String clientId;

    @Value("${rest.debug}")
    private boolean restDebug;

    @Value("${cert.validation}")
    private boolean certificateValidation;

    @Value("${cert.ignorePreValidation}")
    private boolean ignorePreValidation;

    @Value("${caas.protocol:http}")
    private String caasProtocol;

    @Value("${caas.url:}")
    private String caasUrl;

    @Value("${caas.cert.validation:false}")
    private boolean caasCertificateValidation;

    @Value("${caas.cert.ignorePreValidation:false}")
    private boolean caasIgnorePreValidation;

    @Inject
    private ThreadBasedRequestIdProvider threadBasedRequestIdProvider;

    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    private ResourceLoader resourceLoader;

    @PostConstruct
    public void init() throws IOException {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    @Bean
    public FilterRegistrationBean<RequestIdGeneratingFilter> requestIdGeneratingFilterRegistrationBean() {
        FilterRegistrationBean<RequestIdGeneratingFilter> registrationBean = new FilterRegistrationBean<>();
        RequestIdGeneratingFilter filter = new RequestIdGeneratingFilter();
        registrationBean.setFilter(filter);
        registrationBean.setOrder(BEAN_ORDER_REQUEST_ID_GENERATING_FILTER);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<RequestIdFilter> requestIdFilterRegistrationBean() {
        FilterRegistrationBean<RequestIdFilter> registrationBean = new FilterRegistrationBean<>();
        RequestIdFilter filter = new RequestIdFilter(threadBasedRequestIdProvider);
        registrationBean.setFilter(filter);
        registrationBean.setOrder(BEAN_ORDER_REQUEST_ID_FILTER);
        return registrationBean;
    }

    @Bean
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
    public AsyncTaskExecutor resourceBuilderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("resourceBuilderExecutor-");
        executor.setTaskDecorator(new MDCCleanerTaskDecorator());
        executor.initialize();
        return executor;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
