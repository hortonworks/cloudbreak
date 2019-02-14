package com.sequenceiq.cloudbreak.conf;

import java.io.File;
import java.io.IOException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.ws.rs.client.Client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.filter.GenericFilterBean;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.services.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.clusterdefinition.validation.StackServiceComponentDescriptor;
import com.sequenceiq.cloudbreak.clusterdefinition.validation.StackServiceComponentDescriptors;
import com.sequenceiq.cloudbreak.client.CaasClient;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.cloudbreak.client.IdentityClient;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.concurrent.MDCCleanerTaskDecorator;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteria;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ExecutorBasedParallelOrchestratorComponentRunner;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelOrchestratorComponentRunner;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.service.StackUnderOperationService;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigurator;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Configuration
@EnableRetry
public class AppConfig implements ResourceLoaderAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppConfig.class);

    @Value("${cb.etc.config.dir}")
    private String etcConfigDir;

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
    private StackUnderOperationService stackUnderOperationService;

    @Inject
    private List<ContainerOrchestrator> containerOrchestrators;

    @Inject
    private List<HostOrchestrator> hostOrchestrators;

    @Inject
    private List<FileSystemConfigurator> fileSystemConfigurators;

    @Inject
    private ConfigurableEnvironment environment;

    @Inject
    @Named("identityServerUrl")
    private String identityServerUrl;

    private ResourceLoader resourceLoader;

    @PostConstruct
    public void init() throws IOException {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        ResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();
        PropertySourceLoader load = new YamlPropertySourceLoader();
        for (Resource resource : patternResolver.getResources("classpath*:*-images.yml")) {
            for (PropertySource<?> propertySource : load.load(resource.getFilename(), resource)) {
                environment.getPropertySources().addLast(propertySource);
            }
        }
        for (Resource resource : loadEtcResources()) {
            for (PropertySource<?> propertySource : load.load(resource.getFilename(), resource)) {
                environment.getPropertySources().addFirst(propertySource);
            }
        }
    }

    @Bean
    public ExitCriteria clusterDeletionBasedExitCriteria() {
        return new ClusterDeletionBasedExitCriteria();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public FilterRegistrationBean turnOnStackUnderOperationService() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new GenericFilterBean() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
                stackUnderOperationService.on();
                chain.doFilter(request, response);
                stackUnderOperationService.off();
            }
        });
        registration.addUrlPatterns("/*");
        registration.setName("turnOnStackUnderOperationService");
        return registration;
    }

    @Bean
    public Map<String, ContainerOrchestrator> containerOrchestrators() {
        Map<String, ContainerOrchestrator> map = new HashMap<>();
        for (ContainerOrchestrator containerOrchestrator : containerOrchestrators) {
            containerOrchestrator.init(simpleParallelContainerRunnerExecutor(), clusterDeletionBasedExitCriteria());
            map.put(containerOrchestrator.name(), containerOrchestrator);
        }
        return map;
    }

    @Bean
    public Map<String, HostOrchestrator> hostOrchestrators() {
        Map<String, HostOrchestrator> map = new HashMap<>();
        for (HostOrchestrator hostOrchestrator : hostOrchestrators) {
            hostOrchestrator.init(simpleParallelContainerRunnerExecutor(), clusterDeletionBasedExitCriteria());
            map.put(hostOrchestrator.name(), hostOrchestrator);
        }
        return map;
    }

    @Bean
    public ParallelOrchestratorComponentRunner simpleParallelContainerRunnerExecutor() {
        return new ExecutorBasedParallelOrchestratorComponentRunner(containerBootstrapBuilderExecutor());
    }

    @Bean
    public Map<FileSystemType, FileSystemConfigurator> fileSystemConfigurators() {
        Map<FileSystemType, FileSystemConfigurator> map = new EnumMap<>(FileSystemType.class);
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

    @Bean
    public AsyncTaskExecutor containerBootstrapBuilderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(containerCorePoolSize);
        executor.setQueueCapacity(containerteQueueCapacity);
        executor.setThreadNamePrefix("containerBootstrapBuilderExecutor-");
        executor.setTaskDecorator(new MDCCleanerTaskDecorator());
        executor.initialize();
        return executor;
    }

    @Bean
    public CaasClient caasClient() {
        return new CaasClient(caasProtocol, caasUrl, new ConfigKey(caasCertificateValidation, restDebug, caasIgnorePreValidation));
    }

    @Bean
    public IdentityClient identityClient() {
        return new IdentityClient(identityServerUrl, clientId, new ConfigKey(certificateValidation, restDebug, ignorePreValidation));
    }

    @Bean
    public Client restClient() {
        return RestClientUtil.get(new ConfigKey(certificateValidation, restDebug, ignorePreValidation));
    }

    @Bean
    public StackServiceComponentDescriptors stackServiceComponentDescriptors() throws Exception {
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
        maxCardinalityReps.put("1-2", 2);
        maxCardinalityReps.put("0+", Integer.MAX_VALUE);
        maxCardinalityReps.put("1+", Integer.MAX_VALUE);
        maxCardinalityReps.put("ALL", Integer.MAX_VALUE);
        String stackServiceComponentsJson = FileReaderUtils.readFileFromClasspath("hdp/hdp-services.json");
        return createServiceComponentDescriptors(stackServiceComponentsJson, minCardinalityReps, maxCardinalityReps);
    }

    private StackServiceComponentDescriptors createServiceComponentDescriptors(String stackServiceComponentsJson, Map<String, Integer> minCardinalityReps,
            Map<String, Integer> maxCardinalityReps) throws Exception {
        Map<String, StackServiceComponentDescriptor> stackServiceComponentDescriptorMap = Maps.newHashMap();
        JsonNode rootNode = JsonUtil.readTree(stackServiceComponentsJson);
        JsonNode itemsNode = rootNode.get("items");
        for (JsonNode itemNode : itemsNode) {
            JsonNode componentsNode = itemNode.get("components");
            for (JsonNode componentNode : componentsNode) {
                JsonNode stackServiceComponentsNode = componentNode.get("StackServiceComponents");
                String componentName = stackServiceComponentsNode.get("component_name").asText();
                String componentCategory = stackServiceComponentsNode.get("component_category").asText();
                int minCardinality = parseCardinality(minCardinalityReps, stackServiceComponentsNode.get("cardinality").asText(), 0);
                int maxCardinality = parseCardinality(maxCardinalityReps, stackServiceComponentsNode.get("cardinality").asText(), Integer.MAX_VALUE);
                stackServiceComponentDescriptorMap.put(componentName, new StackServiceComponentDescriptor(componentName, componentCategory, minCardinality,
                        maxCardinality));
            }
        }
        return new StackServiceComponentDescriptors(stackServiceComponentDescriptorMap);
    }

    private int parseCardinality(Map<String, Integer> cardinalityReps, String cardinalityString, int defaultValue) {
        Integer cardinality = cardinalityReps.get(cardinalityString);
        return cardinality == null ? defaultValue : cardinality;
    }

    private Iterable<Resource> loadEtcResources() {
        File folder = new File(etcConfigDir);
        File[] listOfFiles = folder.listFiles();
        Collection<Resource> resources = new ArrayList<>();
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                try {
                    if (file.isFile() && file.getName().endsWith("yml") || file.getName().endsWith("yaml")) {
                        resources.add(resourceLoader.getResource("file:" + file.getAbsolutePath()));
                    }
                } catch (RuntimeException ignored) {
                    LOGGER.error("Cannot load file into property source: {}", file.getAbsolutePath());
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
