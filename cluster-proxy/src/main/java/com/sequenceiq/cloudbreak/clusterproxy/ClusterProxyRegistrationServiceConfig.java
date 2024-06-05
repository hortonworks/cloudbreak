package com.sequenceiq.cloudbreak.clusterproxy;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ClusterProxyRegistrationServiceConfig {

    private static final int TIMEOUT = 30;

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(List.of(new RequestIdProviderRequestInterceptor()));
        return restTemplate;
    }

    @Bean
    public RestTemplate hybridRestTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(TIMEOUT))
                .setReadTimeout(Duration.ofSeconds(TIMEOUT))
                .interceptors(List.of(new RequestIdProviderRequestInterceptor()))
                .requestFactory(HttpComponentsClientHttpRequestFactory.class)
                .build();
    }

}
