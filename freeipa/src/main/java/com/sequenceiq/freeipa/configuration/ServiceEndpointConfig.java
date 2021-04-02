package com.sequenceiq.freeipa.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.sequenceiq.freeipa.service.registry.DNSServiceAddressResolver;
import com.sequenceiq.freeipa.service.registry.RetryingServiceAddressResolver;
import com.sequenceiq.freeipa.service.registry.ServiceAddressResolver;
import com.sequenceiq.freeipa.service.registry.ServiceAddressResolvingException;

@Configuration
public class ServiceEndpointConfig {
    @Value("${freeipa.address.resolving.timeout:60000}")
    private int resolvingTimeout;

    @Value("${freeipa.db.addr:}")
    private String dbHost;

    @Value("${freeipa.db.port:}")
    private String dbPort;

    @Value("${freeipa.db.serviceid:}")
    private String databaseId;

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

    @Bean
    public ServiceAddressResolver serviceAddressResolver() {
        return new RetryingServiceAddressResolver(new DNSServiceAddressResolver(), resolvingTimeout);
    }

    @Bean
    @DependsOn("serviceAddressResolver")
    public String databaseAddress(ServiceAddressResolver serviceAddressResolver) throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveHostPort(dbHost, dbPort, databaseId);
    }

    @Bean
    public String environmentServiceUrl() throws ServiceAddressResolvingException {
        return serviceAddressResolver().resolveUrl(environmentServiceUrl + environmentRootContextPath, "", null);
    }

    @Bean
    public String sdxServerUrl() throws ServiceAddressResolvingException {
        return serviceAddressResolver().resolveUrl(sdxServiceUrl + sdxRootContextPath, "http", sdxServiceId);
    }

    @Bean
    public String cloudbreakServerUrl(ServiceAddressResolver serviceAddressResolver) throws ServiceAddressResolvingException {
        return serviceAddressResolver().resolveUrl(cloudbreakUrl + cbRootContextPath, "http", cloudbreakServiceId);
    }
}
