package com.sequenceiq.cloudbreak.cloud.handler.testcontext;

import static org.mockito.Mockito.when;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.annotation.PostConstruct;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.handler.ConsumerNotFoundHandler;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.service.Persister;

import reactor.Environment;
import reactor.bus.EventBus;
import reactor.bus.spec.EventBusSpec;

@Configuration
@ComponentScan("com.sequenceiq.cloudbreak.cloud")
@PropertySource("classpath:application.properties")
public class TestApplicationContext {

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CloudConnector cloudConnector;


    @Mock
    private ResourceConnector resourceConnector;

    @Mock
    private PersistenceNotifier persistenceNotifier;

    @Mock
    private Persister persister;

    @PostConstruct
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public ListeningScheduledExecutorService listeningScheduledExecutorService() {
        return MoreExecutors.listeningDecorator(new ScheduledThreadPoolExecutor(1));
    }


    @Bean
    public CloudPlatformConnectors cloudPlatformConnectors() {
        return cloudPlatformConnectors;
    }

    @Bean
    public CloudConnector cloudConnectors() {
        when(cloudConnector.platform()).thenReturn("TESTCONNECTOR");
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        return cloudConnector;
    }

    @Bean
    public PersistenceNotifier resourcePersistenceNotifier(){
        return persistenceNotifier;
    }

    @Bean
    public Environment env() {
        return Environment.initializeIfEmpty();
    }

    @Bean
    public Persister getPersister() {
        return persister;
    }

    @Bean
    public EventBus eventBus(Environment env) {
        EventBus bus = new EventBusSpec()
                .env(env)
                .defaultDispatcher()
                .traceEventPath()
                .consumerNotFoundHandler(new ConsumerNotFoundHandler())
                .get();
        return bus;
    }






}
