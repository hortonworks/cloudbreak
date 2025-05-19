package com.sequenceiq.cloudbreak.cloud.azure.conf;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.cloud.azure.AzureDatabaseTemplateModelBuilder;
import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;
import com.sequenceiq.cloudbreak.concurrent.CommonExecutorServiceFactory;
import com.sequenceiq.cloudbreak.concurrent.MDCCopyDecorator;
import com.sequenceiq.common.model.AzureDatabaseType;

@Configuration
public class AzureConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureConfig.class);

    private static final String AZURE_CLIENT_THREAD_POOL = "azureClientThreadPool";

    @Value("${cb.azure.tag.amount:50}")
    private Integer maxAmount;

    @Value("${cb.azure.tag.key.min.length:1}")
    private Integer minKeyLength;

    @Value("${cb.azure.tag.key.max.length:512}")
    private Integer maxKeyLength;

    @Value("${cb.azure.tag.key.validator:^(?!microsoft|azure|windows|\\s)[^,<>%&\\\\/\\?]*(?<!\\s)$}")
    private String keyValidator;

    @Value("${cb.azure.tag.value.min.length:1}")
    private Integer minValueLength;

    @Value("${cb.azure.tag.value.max.length:256}")
    private Integer maxValueLength;

    @Value("${cb.azure.tag.value.validator:^(?!\\s).*(?<!\\s)$}")
    private String valueValidator;

    @Value("${cb.azure.client.thread.keepAliveTimeInSeconds:20}")
    private Integer azureClientThreadKeepAliveTimeInSeconds;

    @Inject
    private CommonExecutorServiceFactory commonExecutorServiceFactory;

    @Value("${spring.threads.virtual.enabled:false}")
    private boolean virtualThreadsAvailable;

    @Bean(name = "AzureTagSpecification")
    public TagSpecification getTagSpecification() {
        return new TagSpecification(maxAmount, minKeyLength, maxKeyLength, keyValidator, minValueLength, maxValueLength, valueValidator);
    }

    @Bean(name = "azureClientThreadPool")
    public ExecutorService azureClientThreadPool() {
        if (virtualThreadsAvailable) {
            return commonExecutorServiceFactory.newVirtualThreadExecutorService(AZURE_CLIENT_THREAD_POOL, AZURE_CLIENT_THREAD_POOL,
                    List.of(new MDCCopyDecorator()));
        } else {
            return commonExecutorServiceFactory.newThreadPoolExecutorService(AZURE_CLIENT_THREAD_POOL, AZURE_CLIENT_THREAD_POOL, 0, Integer.MAX_VALUE,
                    azureClientThreadKeepAliveTimeInSeconds, TimeUnit.SECONDS, new SynchronousQueue<>(),
                    (r, executor) -> LOGGER.error("Task has been rejected from 'azure-worker' threadpool. Executor state: {}", executor),
                    List.of(new MDCCopyDecorator()));
        }
    }

    @Bean(name = "azureDatabaseTemplateModelBuilderMap")
    public Map<AzureDatabaseType, AzureDatabaseTemplateModelBuilder> azureDatabaseTemplateModelBuilderMap(
            List<AzureDatabaseTemplateModelBuilder> templateModelBuilders) {
        return templateModelBuilders.stream().collect(
                Collectors.toMap(AzureDatabaseTemplateModelBuilder::azureDatabaseType, modelBuilder -> modelBuilder));
    }
}
