package com.sequenceiq.cloudbreak.conf;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_CONTAINER_THREADPOOL_CAPACITY_SIZE;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_CONTAINER_THREADPOOL_CORE_SIZE;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_INTERMEDIATE_THREADPOOL_CAPACITY_SIZE;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_INTERMEDIATE_THREADPOOL_CORE_SIZE;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_SUPPORTED_CONTAINER_ORCHESTRATORS;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_THREADPOOL_CAPACITY_SIZE;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_THREADPOOL_CORE_SIZE;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.StackServiceComponentDescriptorMapFactory;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ExecutorBasedParallelContainerRunner;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.ParallelContainerRunner;
import com.sequenceiq.cloudbreak.service.credential.CredentialHandler;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Configuration
public class AppConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppConfig.class);

    @Value("#{'${cb.supported.container.orchestrators:" + CB_SUPPORTED_CONTAINER_ORCHESTRATORS + "}'.split(',')}")
    private List<String> orchestrators;

    @Value("${cb.threadpool.core.size:" + CB_THREADPOOL_CORE_SIZE + "}")
    private int corePoolSize;

    @Value("${cb.threadpool.capacity.size:" + CB_THREADPOOL_CAPACITY_SIZE + "}")
    private int queueCapacity;

    @Value("${cb.intermediate.threadpool.core.size:" + CB_INTERMEDIATE_THREADPOOL_CORE_SIZE + "}")
    private int intermediateCorePoolSize;

    @Value("${cb.intermediate.threadpool.capacity.size:" + CB_INTERMEDIATE_THREADPOOL_CAPACITY_SIZE + "}")
    private int intermediateQueueCapacity;

    @Value("${cb.container.threadpool.core.size:" + CB_CONTAINER_THREADPOOL_CORE_SIZE + "}")
    private int containerCorePoolSize;

    @Value("${cb.container.threadpool.capacity.size:" + CB_CONTAINER_THREADPOOL_CAPACITY_SIZE + "}")
    private int containerteQueueCapacity;

    @Inject
    private List<CloudPlatformConnector> cloudPlatformConnectorList;

    @Inject
    private List<ProvisionSetup> provisionSetups;

    @Inject
    private List<MetadataSetup> metadataSetups;

    @Inject
    private List<CredentialHandler<? extends Credential>> credentialHandlers;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public Map<String, ContainerOrchestrator> containerOrchestrators() throws CloudbreakException {
        Map<String, ContainerOrchestrator> map = new HashMap<>();
        for (String className : orchestrators) {
            try {
                Class<?> coClass = AppConfig.class.getClassLoader().loadClass(className);
                if (ContainerOrchestrator.class.isAssignableFrom(coClass)) {
                    ContainerOrchestrator co = (ContainerOrchestrator) coClass.newInstance();
                    co.init(simpleParalellContainerRunnerExecutor());
                    map.put(co.name(), co);
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                throw new CloudbreakException("Invalid ContainerOrchestrator definition: " + className, e);
            }
        }

        if (map.isEmpty()) {
            LOGGER.error("No any ContainerOrchestrator has been loaded. Following ContainerOrchestrators were tried {}", orchestrators);
            throw new CloudbreakException("No any ContainerOrchestrator has been loaded");
        }

        return map;
    }

    @Bean
    public ParallelContainerRunner simpleParalellContainerRunnerExecutor() {
        return new ExecutorBasedParallelContainerRunner(containerBootstrapBuilderExecutor());
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
    public Map<CloudPlatform, CredentialHandler> credentialHandlers() {
        Map<CloudPlatform, CredentialHandler> map = new HashMap<>();
        for (CredentialHandler credentialHandler : credentialHandlers) {
            map.put(credentialHandler.getCloudPlatform(), credentialHandler);
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

    @Bean
    public AsyncTaskExecutor containerBootstrapBuilderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(containerCorePoolSize);
        executor.setQueueCapacity(containerteQueueCapacity);
        executor.setThreadNamePrefix("containerBootstrapBuilderExecutor-");
        executor.initialize();
        return executor;
    }

    @Bean
    public RestOperations restTemplate() {
        return new RestTemplate();
    }

    @Bean(name = "autoSSLAcceptorRestTemplate")
    public RestOperations autoSSLAcceptorRestTemplate() throws Exception {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
        sslContextBuilder.loadTrustMaterial(null, new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                return true;
            }
        });
        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContextBuilder.build());
        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).build();
        requestFactory.setHttpClient(httpClient);
        return new RestTemplate(requestFactory);
    }

    @Bean
    public StackServiceComponentDescriptorMapFactory stackServiceComponentDescriptorMapFactory() throws IOException {
        Map<String, Integer> maxCardinalityReps = Maps.newHashMap();
        maxCardinalityReps.put("1", 1);
        maxCardinalityReps.put("0-1", 1);
        maxCardinalityReps.put("1-2", 1);
        maxCardinalityReps.put("0+", Integer.MAX_VALUE);
        maxCardinalityReps.put("1+", Integer.MAX_VALUE);
        maxCardinalityReps.put("ALL", Integer.MAX_VALUE);
        String stackServiceComponentsJson = FileReaderUtils.readFileFromClasspath("hdp/hdp-services.json");
        return new StackServiceComponentDescriptorMapFactory(stackServiceComponentsJson, maxCardinalityReps, new ObjectMapper());
    }

}
