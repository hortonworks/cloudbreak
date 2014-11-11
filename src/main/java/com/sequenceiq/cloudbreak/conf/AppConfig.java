package com.sequenceiq.cloudbreak.conf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;
import com.sequenceiq.cloudbreak.service.stack.connector.aws.TemplateReader;

@Configuration
public class AppConfig {

    @Value("${cb.threadpool.core.size:40}")
    private int corePoolSize;
    @Value("${cb.threadpool.capacity.size:4000}")
    private int queueCapacity;

    @Value("${cb.intermediate.threadpool.core.size:40}")
    private int intermediateCorePoolSize;
    @Value("${cb.intermediate.threadpool.capacity.size:4000}")
    private int intermediateQueueCapacity;

    @Autowired
    private TemplateReader templateReader;

    @Autowired
    private List<CloudPlatformConnector> cloudPlatformConnectorList;

    @Autowired
    private List<ProvisionSetup> provisionSetups;

    @Autowired
    private List<MetadataSetup> metadataSetups;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors() {
        Map<CloudPlatform, CloudPlatformConnector> map = new HashMap<>();
        for (CloudPlatformConnector provisionService : cloudPlatformConnectorList) {
            map.put(provisionService.getCloudPlatform(), provisionService);
        }
        return map;
    }

    @Bean
    public Map<CloudPlatform, ProvisionSetup> provisionSetups() {
        Map<CloudPlatform, ProvisionSetup> map = new HashMap<>();
        for (ProvisionSetup provisionSetup : provisionSetups) {
            map.put(provisionSetup.getCloudPlatform(), provisionSetup);
        }
        return map;
    }

    @Bean
    public Map<CloudPlatform, MetadataSetup> metadataSetups() {
        Map<CloudPlatform, MetadataSetup> map = new HashMap<>();
        for (MetadataSetup metadataSetup : metadataSetups) {
            map.put(metadataSetup.getCloudPlatform(), metadataSetup);
        }
        return map;
    }

    @Bean
    public AsyncTaskExecutor intermediateBuilderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(intermediateCorePoolSize);
        executor.setQueueCapacity(intermediateQueueCapacity);
        executor.setThreadNamePrefix("intermediateBuilderExecutor-");
        executor.initialize();
        return executor;
    }

    @Bean
    public AsyncTaskExecutor resourceBuilderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("resourceBuilderExecutor-");
        executor.initialize();
        return executor;
    }

}
