package com.sequenceiq.cloudbreak.conf;

import java.io.File;
import java.io.IOException;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.client.Client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.client.IdentityClient;
import com.sequenceiq.cloudbreak.client.RestClient;
import com.sequenceiq.cloudbreak.client.config.ConfigKey;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.StackServiceComponentDescriptorMapFactory;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ExecutorBasedParallelContainerRunner;
import com.sequenceiq.cloudbreak.core.bootstrap.service.StackDeletionBasedExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelContainerRunner;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemConfigurator;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Configuration
public class AppConfig implements ResourceLoaderAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppConfig.class);
    private static final int TIMEOUT = 5_000;
    private static final String ETC_DIR = "/etc/cloudbreak";

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

    @Value("${cb.client.id}")
    private String clientId;

    @Value("${cb.identity.server.url}")
    private String identityServerUrl;

    @Value("${rest.debug:false}")
    private boolean restDebug;

    @Value("${cert.validation:true}")
    private boolean certificateValidation;

    @Inject
    private List<FileSystemConfigurator> fileSystemConfigurators;

    @Inject
    private ConfigurableEnvironment environment;

    private ResourceLoader resourceLoader;

    @PostConstruct
    public void init() throws IOException {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        ResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();
        YamlPropertySourceLoader load = new YamlPropertySourceLoader();
        for (Resource resource : patternResolver.getResources("classpath*:*-images.yml")) {
            environment.getPropertySources().addLast(load.load(resource.getFilename(), resource, null));
        }
        for (Resource resource : loadEtcResources()) {
            environment.getPropertySources().addFirst(load.load(resource.getFilename(), resource, null));
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
    public IdentityClient identityClient() {
        return new IdentityClient(identityServerUrl, clientId, new ConfigKey(certificateValidation, restDebug));
    }

    @Bean
    public Client restClient() {
        return RestClient.get(new ConfigKey(certificateValidation, restDebug));
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

    private List<Resource> loadEtcResources() {
        File folder = new File(ETC_DIR);
        File[] listOfFiles = folder.listFiles();
        List<Resource> resources = new ArrayList<>();
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                try {
                    if (file.isFile() && file.getName().endsWith("yml") || file.getName().endsWith("yaml")) {
                        resources.add(resourceLoader.getResource("file:" + file.getAbsolutePath()));
                    }
                } catch (Exception e) {
                    LOGGER.warn("Cannot load file into property source: {}", file.getAbsolutePath());
                }
            }
        }
        return resources;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
