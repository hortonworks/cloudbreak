package com.sequenceiq.externalizedcompute.config;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.sequenceiq.cloudbreak.registry.ServiceAddressResolver;
import com.sequenceiq.cloudbreak.registry.ServiceAddressResolvingException;

@Configuration
public class ServiceEndpointConfig {

    @Value("${externalizedcompute.environmentservice.serviceid:}")
    private String environmentServiceId;

    @Value("${externalizedcompute.environmentservice.url:}")
    private String environmentServerUrl;

    @Value("${externalizedcompute.environmentservice.server.contextPath:/environmentservice}")
    private String environmentRootContextPath;

    @Inject
    private ServiceAddressResolver serviceAddressResolver;

    @Bean
    @DependsOn("serviceAddressResolver")
    public String environmentServerUrl() throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveUrl(environmentServerUrl + environmentRootContextPath, "http", environmentServiceId);
    }
}
