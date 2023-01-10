package com.sequenceiq.cloudbreak.clusterproxy;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ClusterProxyRegistrationServiceConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(List.of(new RequestIdProviderRequestInterceptor()));
        return restTemplate;
    }

}
