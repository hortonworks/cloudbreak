package com.sequenceiq.cloudbreak.cloud.aws;

import static org.mockito.Mockito.mock;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

import com.amazonaws.auth.BasicSessionCredentials;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.service.Persister;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.NetworkResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.ResourceContextBuilder;

@Configuration
@ComponentScan("com.sequenceiq.cloudbreak.cloud")
@PropertySource("classpath:application.properties")
public class TestContext {

    private BasicSessionCredentials credentials = new BasicSessionCredentials("awsAccessKey", "awsSecretKey", "sessionToken");

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
    public AsyncTaskExecutor asyncTaskExecutor() {
        return new ConcurrentTaskExecutor(Executors.newSingleThreadExecutor());
    }

    @Bean
    public NetworkResourceBuilder networkResourceBuilder() {
        return mock(NetworkResourceBuilder.class);
    }

    @Bean
    public ComputeResourceBuilder computeResourceBuilder() {
        return mock(ComputeResourceBuilder.class);
    }

    @Bean
    public ResourceContextBuilder resourceContextBuilder() {
        return mock(ResourceContextBuilder.class);
    }

    @Bean
    public AwsClientWrapper awsClientWrapper() {
        return new AwsClientWrapper(credentials);
    }

    @Bean
    public Persister getPersister() {
        return persister;
    }

    public static class AwsClientWrapper extends AwsClient {
        private static AtomicInteger retrieveCalled = new AtomicInteger(0);

        private BasicSessionCredentials c;

        public AwsClientWrapper(BasicSessionCredentials c) {
            this.c = c;
        }

        @Override
        public BasicSessionCredentials retrieveSessionCredentials(AwsCredentialView awsCredential) {
            retrieveCalled.incrementAndGet();
            return c;
        }

        public static int getCounter() {
            return retrieveCalled.get();
        }

    }
}
