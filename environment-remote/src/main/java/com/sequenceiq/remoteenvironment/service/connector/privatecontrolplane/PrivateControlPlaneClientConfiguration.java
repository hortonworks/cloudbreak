package com.sequenceiq.remoteenvironment.service.connector.privatecontrolplane;

import java.time.Duration;

import jakarta.inject.Inject;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyProxyClientFactory;

@Configuration
public class PrivateControlPlaneClientConfiguration {

    private static final long FAST_TIMEOUT = 5;

    private static final long SLOW_TIMEOUT = 30;

    @Inject
    private RestTemplateBuilder restTemplateBuilder;

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    @Bean
    public ClusterProxyProxyClientFactory fastClusterProxyProxyClientFactory() {
        return new ClusterProxyProxyClientFactory(clusterProxyConfiguration, restTemplateBuilderWithTimeout(FAST_TIMEOUT));
    }

    @Bean
    public ClusterProxyProxyClientFactory slowClusterProxyProxyClientFactory() {
        return new ClusterProxyProxyClientFactory(clusterProxyConfiguration, restTemplateBuilderWithTimeout(SLOW_TIMEOUT));
    }

    private RestTemplateBuilder restTemplateBuilderWithTimeout(long seconds) {
        return restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(seconds))
                .setReadTimeout(Duration.ofSeconds(seconds));
    }
}
