package com.sequenceiq.cloudbreak.clusterproxy;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ClusterProxyRegistrationServiceConfig {

    private static final int TIMEOUT = 5000;

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(List.of(new RequestIdProviderRequestInterceptor()));
        return restTemplate;
    }

    @Bean
    public RestTemplate hybridRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(List.of(new RequestIdProviderRequestInterceptor()));
        restTemplate.setRequestFactory(httpComponentsClientHttpRequestFactory());
        return restTemplate;
    }

    private HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        httpRequestFactory.setConnectionRequestTimeout(TIMEOUT);
        httpRequestFactory.setConnectTimeout(TIMEOUT);
        return httpRequestFactory;
    }

}
