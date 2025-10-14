package com.sequenceiq.freeipa.configuration;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.registry.ServiceAddressResolver;
import com.sequenceiq.cloudbreak.registry.ServiceAddressResolvingException;

@Configuration
public class ServiceEndpointConfig {

    @Value("${freeipa.environment.url}")
    private String environmentServiceUrl;

    @Value("${freeipa.environment.contextPath}")
    private String environmentRootContextPath;

    @Value("${freeipa.sdx.url:}")
    private String sdxServiceUrl;

    @Value("${freeipa.sdx.serviceId:}")
    private String sdxServiceId;

    @Value("${freeipa.sdx.server.contextPath:/dl}")
    private String sdxRootContextPath;

    @Value("${freeipa.cloudbreak.url}")
    private String cloudbreakUrl;

    @Value("${freeipa.cloudbreak.contextPath}")
    private String cbRootContextPath;

    @Value("${freeipa.cloudbreak.serviceId:}")
    private String cloudbreakServiceId;

    @Value("${freeipa.remoteEnvironment.url}")
    private String remoteEnvironmentServiceUrl;

    @Value("${freeipa.remoteEnvironment.contextPath}")
    private String remoteEnvironmentRootContextPath;

    @Inject
    private ServiceAddressResolver serviceAddressResolver;

    @Bean
    public String environmentServiceUrl() throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveUrl(environmentServiceUrl + environmentRootContextPath, "", null);
    }

    @Bean
    public String sdxServerUrl() throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveUrl(sdxServiceUrl + sdxRootContextPath, "http", sdxServiceId);
    }

    @Bean
    public String cloudbreakServerUrl(ServiceAddressResolver serviceAddressResolver) throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveUrl(cloudbreakUrl + cbRootContextPath, "http", cloudbreakServiceId);
    }

    @Bean
    public String remoteEnvironmentServiceUrl() throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveUrl(remoteEnvironmentServiceUrl + remoteEnvironmentRootContextPath, "", null);
    }
}
