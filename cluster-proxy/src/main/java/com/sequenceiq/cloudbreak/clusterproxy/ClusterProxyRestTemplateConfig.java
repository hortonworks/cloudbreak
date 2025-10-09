package com.sequenceiq.cloudbreak.clusterproxy;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ClusterProxyRestTemplateConfig {

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    @Bean
    public RestTemplate registrationRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(List.of(new RequestIdProviderRequestInterceptor()));
        return restTemplate;
    }

}
