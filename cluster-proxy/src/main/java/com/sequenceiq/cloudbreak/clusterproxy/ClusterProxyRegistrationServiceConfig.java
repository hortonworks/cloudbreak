package com.sequenceiq.cloudbreak.clusterproxy;

import java.util.List;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.web.client.TracingRestTemplateInterceptor;

@Configuration
public class ClusterProxyRegistrationServiceConfig {

    @Inject
    private Tracer tracer;

    @Bean
    public RestTemplate restTemplate() {
        TracingRestTemplateInterceptor tracingInterceptor = new TracingRestTemplateInterceptor(tracer);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(List.of(new RequestIdProviderRequestInterceptor(), tracingInterceptor));
        return restTemplate;
    }

}
