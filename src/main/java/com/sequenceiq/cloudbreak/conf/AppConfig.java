package com.sequenceiq.cloudbreak.conf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformRollbackHandler;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;
import com.sequenceiq.cloudbreak.service.stack.connector.Provisioner;
import com.sequenceiq.cloudbreak.service.stack.connector.aws.TemplateReader;

@Configuration
public class AppConfig {

    @Value("${cb.threadpool.core.size:10}")
    private int corePoolSize;
    @Value("${cb.threadpool.max.size:100}")
    private int maxPoolSize;
    @Value("${cb.threadpool.capacity.size:11}")
    private int queueCapacity;

    @Autowired
    private TemplateReader templateReader;

    @Autowired
    private List<CloudPlatformConnector> cloudPlatformConnectorList;

    @Autowired
    private List<ProvisionSetup> provisionSetups;

    @Autowired
    private List<CloudPlatformRollbackHandler> rollbackHandlers;

    @Autowired
    private List<Provisioner> provisioners;

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
    public Map<CloudPlatform, CloudPlatformRollbackHandler> cloudPlatformRollbackHandlers() {
        Map<CloudPlatform, CloudPlatformRollbackHandler> map = new HashMap<>();
        for (CloudPlatformRollbackHandler cloudPlatformRollbackHandler : rollbackHandlers) {
            map.put(cloudPlatformRollbackHandler.getCloudPlatform(), cloudPlatformRollbackHandler);
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
    public Map<CloudPlatform, Provisioner> provisioners() {
        Map<CloudPlatform, Provisioner> map = new HashMap<>();
        for (Provisioner provisioner : provisioners) {
            map.put(provisioner.getCloudPlatform(), provisioner);
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

    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("MyExecutor-");
        executor.initialize();
        return executor;
    }

    @Bean
    public Executor resourceBuilderExecutor() {
        ConcurrentTaskExecutor concurrentTaskExecutor = new ConcurrentTaskExecutor();
        concurrentTaskExecutor.setConcurrentExecutor(getAsyncExecutor());
        return concurrentTaskExecutor;
    }

}
