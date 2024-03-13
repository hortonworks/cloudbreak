package com.sequenceiq.redbeams.configuration;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.sequenceiq.cloudbreak.registry.ServiceAddressResolver;
import com.sequenceiq.cloudbreak.registry.ServiceAddressResolvingException;

@Configuration
public class ServiceEndpointConfig {

    @Value("${redbeams.cloudbreak.url:}")
    private String cloudbreakUrl;

    @Value("${redbeams.cloudbreak.serviceid:}")
    private String cloudbreakServiceId;

    @Value("${redbeams.cloudbreak.server.contextPath:/cb}")
    private String cbRootContextPath;

    @Value("${redbeams.environment.url}")
    private String environmentServiceUrl;

    @Value("${redbeams.environment.contextPath}")
    private String environmentRootContextPath;

    @Value("${redbeams.sdx.url:}")
    private String sdxServiceUrl;

    @Value("${redbeams.sdx.serviceId:}")
    private String sdxServiceId;

    @Value("${redbeams.sdx.server.contextPath:/dl}")
    private String sdxRootContextPath;

    @Inject
    private ServiceAddressResolver serviceAddressResolver;

    @Bean
    @DependsOn("serviceAddressResolver")
    public String cloudbreakUrl() throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveUrl(cloudbreakUrl + cbRootContextPath, "http", cloudbreakServiceId);
    }

    @Bean
    @DependsOn("serviceAddressResolver")
    public String environmentServiceUrl() throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveUrl(environmentServiceUrl + environmentRootContextPath, "", null);
    }

    @Bean
    @DependsOn("serviceAddressResolver")
    public String sdxServerUrl() throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveUrl(sdxServiceUrl + sdxRootContextPath, "http", sdxServiceId);
    }
}
