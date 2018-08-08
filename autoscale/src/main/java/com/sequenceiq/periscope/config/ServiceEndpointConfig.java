package com.sequenceiq.periscope.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.sequenceiq.periscope.service.registry.DNSServiceAddressResolver;
import com.sequenceiq.periscope.service.registry.RetryingServiceAddressResolver;
import com.sequenceiq.periscope.service.registry.ServiceAddressResolver;
import com.sequenceiq.periscope.service.registry.ServiceAddressResolvingException;

@Configuration
public class ServiceEndpointConfig {
    @Value("${periscope.address.resolving.timeout:60000}")
    private int resolvingTimeout;

    @Value("${periscope.db.port.5432.tcp.addr:}")
    private String dbHost;

    @Value("${periscope.db.port.5432.tcp.port:}")
    private String dbPort;

    @Value("${periscope.db.serviceid:}")
    private String databaseId;

    @Value("${periscope.identity.server.url:}")
    private String identityServiceUrl;

    @Value("${periscope.identity.serviceid:}")
    private String identityServiceId;

    @Value("${periscope.cloudbreak.url:}")
    private String cloudbreakUrl;

    @Value("${periscope.cloudbreak.serviceid:}")
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
    @DependsOn("serviceAddressResolver")
    public String identityServerUrl(ServiceAddressResolver serviceAddressResolver) throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveUrl(identityServiceUrl, "http", identityServiceId);
    }

    @Bean
    @DependsOn("serviceAddressResolver")
    public String cloudbreakUrl(ServiceAddressResolver serviceAddressResolver) throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveUrl(cloudbreakUrl, "http", cloudbreakServiceId);
    }
}
