package com.sequenceiq.periscope.config;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.sequenceiq.cloudbreak.registry.ServiceAddressResolver;
import com.sequenceiq.cloudbreak.registry.ServiceAddressResolvingException;

@Configuration
public class ServiceEndpointConfig {

    @Value("${periscope.cloudbreak.url:}")
    private String cloudbreakUrl;

    @Value("${periscope.freeipa.url:}")
    private String freeIpaServerUrl;

    @Value("${periscope.freeipa.serviceid:}")
    private String freeIpaServiceId;

    @Value("${periscope.freeipa.contextPath:}")
    private String freeIpaContextPath;

    @Value("${periscope.cloudbreak.serviceid:}")
    private String cloudbreakServiceId;

    @Inject
    private ServiceAddressResolver serviceAddressResolver;

    @Bean
    @DependsOn("serviceAddressResolver")
    public String cloudbreakUrl() throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveUrl(cloudbreakUrl, "http", cloudbreakServiceId);
    }

    @Bean
    @DependsOn("serviceAddressResolver")
    public String freeIpaServerUrl() throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveUrl(freeIpaServerUrl + freeIpaContextPath, "http", freeIpaServiceId);
    }
}
