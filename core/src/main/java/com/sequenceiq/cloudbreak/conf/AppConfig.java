package com.sequenceiq.cloudbreak.conf;

import static com.sequenceiq.cloudbreak.service.executor.DelayedExecutorService.DELAYED_TASK_EXECUTOR;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.ws.rs.client.Client;

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
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.filter.GenericFilterBean;

import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.concurrent.CommonExecutorServiceFactory;
import com.sequenceiq.cloudbreak.converter.v4.environment.network.EnvironmentNetworkConverter;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.service.StackUnderOperationService;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigurator;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.environment.client.internal.EnvironmentApiClientParams;
import com.sequenceiq.freeipa.api.client.internal.FreeIpaApiClientParams;
import com.sequenceiq.periscope.client.internal.AutoscaleApiClientParams;
import com.sequenceiq.redbeams.client.internal.RedbeamsApiClientParams;
import com.sequenceiq.sdx.client.internal.SdxApiClientParams;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;

@Configuration
@EnableRetry
public class AppConfig implements ResourceLoaderAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppConfig.class);

    @Value("${cb.etc.config.dir:}")
    private String etcConfigDir;

    @Value("#{'${cb.supported.container.orchestrators:}'.split(',')}")
    private List<String> orchestrators;

    @Value("${cb.intermediate.threadpool.core.size:}")
    private int intermediateCorePoolSize;

    @Value("${cb.intermediate.threadpool.capacity.size:}")
    private int intermediateQueueCapacity;

    @Value("${cb.intermediate.threadpool.termination.seconds:60}")
    private int intermediateAwaitTerminationSeconds;

    @Value("${cb.delayed.threadpool.core.size:10}")
    private int delayedCorePoolSize;

    @Value("${cb.periscope.connection.timeout:2000}")
    private Integer periscopeConnectionTimeout;

    @Value("${cb.periscope.read.timeout:10000}")
    private Integer periscopeReadTimeout;

    @Value("${rest.debug}")
    private boolean restDebug;

    @Value("${cert.validation}")
    private boolean certificateValidation;

    @Value("${cert.ignorePreValidation}")
    private boolean ignorePreValidation;

    @Value("${spring.threads.virtual.enabled:false}")
    private boolean virtualThreadsAvailable;

    @Inject
    private StackUnderOperationService stackUnderOperationService;

    @Inject
    private List<FileSystemConfigurator<?>> fileSystemConfigurators;

    @Inject
    private ConfigurableEnvironment environment;

    @Inject
    @Named("environmentServerUrl")
    private String environmentServerUrl;

    @Inject
    @Named("freeIpaServerUrl")
    private String freeIpaServerUrl;

    @Inject
    @Named("redbeamsServerUrl")
    private String redbeamsServerUrl;

    @Inject
    @Named("sdxServerUrl")
    private String sdxServerUrl;

    @Inject
    @Named("autoscaleServerUrl")
    private String autoscaleServerUrl;

    @Inject
    private List<EnvironmentNetworkConverter> environmentNetworkConverters;

    @Inject
    private MeterRegistry meterRegistry;

    @Inject
    private CommonExecutorServiceFactory commonExecutorServiceFactory;

    private ResourceLoader resourceLoader;

    @PostConstruct
    public void init() throws IOException {
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
    public FilterRegistrationBean<GenericFilterBean> turnOnStackUnderOperationService() {
        FilterRegistrationBean<GenericFilterBean> registration = new FilterRegistrationBean();
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
    public Map<FileSystemType, FileSystemConfigurator<?>> fileSystemConfigurators() {
        Map<FileSystemType, FileSystemConfigurator<?>> map = new EnumMap<>(FileSystemType.class);
        for (FileSystemConfigurator<?> fileSystemConfigurator : fileSystemConfigurators) {
            map.put(fileSystemConfigurator.getFileSystemType(), fileSystemConfigurator);
        }
        return map;
    }

    @Bean
    public AsyncTaskExecutor intermediateBuilderExecutor() {
        return commonExecutorServiceFactory.newAsyncTaskExecutor("intermediateBuilderExecutor", virtualThreadsAvailable, intermediateCorePoolSize,
                intermediateQueueCapacity, intermediateAwaitTerminationSeconds);
    }

    @Bean
    public FreeIpaApiClientParams freeIpaApiClientParams() {
        return new FreeIpaApiClientParams(restDebug, certificateValidation, ignorePreValidation, freeIpaServerUrl);
    }

    @Bean
    public EnvironmentApiClientParams environmentApiClientParams() {
        return new EnvironmentApiClientParams(restDebug, certificateValidation, ignorePreValidation, environmentServerUrl);
    }

    @Bean
    public RedbeamsApiClientParams redbeamsApiClientParams() {
        return new RedbeamsApiClientParams(restDebug, certificateValidation, ignorePreValidation, redbeamsServerUrl);
    }

    @Bean
    public SdxApiClientParams sdxApiClientParams() {
        return new SdxApiClientParams(restDebug, certificateValidation, ignorePreValidation, sdxServerUrl);
    }

    @Bean
    public AutoscaleApiClientParams autoscaleApiClientParams() {
        return new AutoscaleApiClientParams(restDebug, certificateValidation, ignorePreValidation, autoscaleServerUrl,
                periscopeConnectionTimeout, periscopeReadTimeout);
    }

    @Bean
    public Client restClient() {
        return RestClientUtil.get(new ConfigKey(certificateValidation, restDebug, ignorePreValidation));
    }

    @Bean
    public Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConvertersByCloudPlatform() {
        return environmentNetworkConverters
                .stream()
                .collect(Collectors.toMap(EnvironmentNetworkConverter::getCloudPlatform, x -> x));
    }

    @Bean(name = DELAYED_TASK_EXECUTOR)
    public ScheduledExecutorService delayedTaskExecutor() {
        ThreadPoolTaskScheduler executor = new ThreadPoolTaskScheduler();
        executor.setPoolSize(delayedCorePoolSize);
        executor.setThreadNamePrefix("delayedExecutor-");
        executor.initialize();
        return ExecutorServiceMetrics.monitor(meterRegistry, executor.getScheduledExecutor(), DELAYED_TASK_EXECUTOR, "threadpool");
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
