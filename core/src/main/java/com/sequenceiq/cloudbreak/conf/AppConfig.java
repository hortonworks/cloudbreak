package com.sequenceiq.cloudbreak.conf;

import java.io.IOException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.StackServiceComponentDescriptorMapFactory;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ExecutorBasedParallelContainerRunner;
import com.sequenceiq.cloudbreak.core.bootstrap.service.StackDeletionBasedExitCriteria;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelContainerRunner;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemConfigurator;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Configuration
public class AppConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppConfig.class);
    private static final int TIMEOUT = 5_000;

    @Value("#{'${cb.supported.container.orchestrators:}'.split(',')}")
    private List<String> orchestrators;

    @Value("${cb.threadpool.core.size:}")
    private int corePoolSize;

    @Value("${cb.threadpool.capacity.size:}")
    private int queueCapacity;

    @Value("${cb.intermediate.threadpool.core.size:}")
    private int intermediateCorePoolSize;

    @Value("${cb.intermediate.threadpool.capacity.size:}")
    private int intermediateQueueCapacity;

    @Value("${cb.container.threadpool.core.size:}")
    private int containerCorePoolSize;

    @Value("${cb.container.threadpool.capacity.size:}")
    private int containerteQueueCapacity;

    @Inject
    private List<FileSystemConfigurator> fileSystemConfigurators;

    @Inject
    private ConfigurableEnvironment environment;

    @PostConstruct
    public void init() throws IOException {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        ResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();
        Resource [] mappingLocations = patternResolver.getResources("classpath*:*-images.yml");
        YamlPropertySourceLoader load = new YamlPropertySourceLoader();
        for (Resource resource : mappingLocations) {
            String filename = resource.getFilename();
            environment.getPropertySources().addLast(load.load(filename, new ClassPathResource(filename), null));
        }
    }

    @Bean
    public ExitCriteria stackDeletionBasedExitCriteria() {
        return new StackDeletionBasedExitCriteria();
    }

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
                    co.init(simpleParalellContainerRunnerExecutor(), stackDeletionBasedExitCriteria());
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
    public Map<FileSystemType, FileSystemConfigurator> fileSystemConfigurators() {
        Map<FileSystemType, FileSystemConfigurator> map = new HashMap<>();
        for (FileSystemConfigurator fileSystemConfigurator : fileSystemConfigurators) {
            map.put(fileSystemConfigurator.getFileSystemType(), fileSystemConfigurator);
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
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(factory);
        return restTemplate;
    }

    @Bean(name = "autoSSLAcceptorRestTemplate")
    public RestOperations autoSSLAcceptorRestTemplate() throws Exception {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        org.apache.http.ssl.SSLContextBuilder sslContextBuilder = new org.apache.http.ssl.SSLContextBuilder();
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
        Map<String, Integer> minCardinalityReps = Maps.newHashMap();
        minCardinalityReps.put("1", 1);
        minCardinalityReps.put("0-1", 0);
        minCardinalityReps.put("1-2", 1);
        minCardinalityReps.put("0+", 0);
        minCardinalityReps.put("1+", 1);
        minCardinalityReps.put("ALL", 0);
        Map<String, Integer> maxCardinalityReps = Maps.newHashMap();
        maxCardinalityReps.put("1", 1);
        maxCardinalityReps.put("0-1", 1);
        maxCardinalityReps.put("1-2", 1);
        maxCardinalityReps.put("0+", Integer.MAX_VALUE);
        maxCardinalityReps.put("1+", Integer.MAX_VALUE);
        maxCardinalityReps.put("ALL", Integer.MAX_VALUE);
        String stackServiceComponentsJson = FileReaderUtils.readFileFromClasspath("hdp/hdp-services.json");
        return new StackServiceComponentDescriptorMapFactory(stackServiceComponentsJson, minCardinalityReps, maxCardinalityReps);
    }
}
