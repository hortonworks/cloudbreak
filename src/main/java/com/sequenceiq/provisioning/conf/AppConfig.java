package com.sequenceiq.provisioning.conf;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.sequenceiq.provisioning.domain.CloudFormationTemplate;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.service.ProvisionService;
import com.sequenceiq.provisioning.service.aws.TemplateReader;

@Configuration
@EnableAsync
public class AppConfig implements AsyncConfigurer {

    private static final String DEFAULT_TEMPLATE_NAME = "ambari-cluster.template";
    private static final int CORE_POOL_SIZE = 7;
    private static final int MAX_POOL_SIZE = 42;
    private static final int QUEUE_CAPACITY = 11;

    @Autowired
    private TemplateReader templateReader;

    @Autowired
    private List<ProvisionService> provisionServices;

    @Bean
    public CloudFormationTemplate defaultTemplate() throws IOException {
        return templateReader.readTemplateFromFile(DEFAULT_TEMPLATE_NAME);
    }

    @Bean
    public Map<CloudPlatform, ProvisionService> provisionServices() {
        Map<CloudPlatform, ProvisionService> map = new HashMap<>();
        for (ProvisionService provisionService : provisionServices) {
            map.put(provisionService.getCloudPlatform(), provisionService);
        }
        return map;
    }

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("MyExecutor-");
        executor.initialize();
        return executor;
    }
}
