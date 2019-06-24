package com.sequenceiq.redbeams.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.sequenceiq.redbeams.service.registry.DNSServiceAddressResolver;
import com.sequenceiq.redbeams.service.registry.RetryingServiceAddressResolver;
import com.sequenceiq.redbeams.service.registry.ServiceAddressResolver;
import com.sequenceiq.redbeams.service.registry.ServiceAddressResolvingException;

@Configuration
public class ServiceEndpointConfig {
    @Value("${redbeams.address.resolving.timeout:60000}")
    private int resolvingTimeout;

    @Value("${redbeams.db.port.5432.tcp.addr:}")
    private String dbHost;

    @Value("${redbeams.db.port.5432.tcp.port:}")
    private String dbPort;

    @Value("${redbeams.db.serviceid:}")
    private String databaseId;

    @Value("${redbeams.identity.server.url:}")
    private String identityServiceUrl;

    @Value("${redbeams.identity.serviceid:}")
    private String identityServiceId;

    @Value("${redbeams.cloudbreak.url:}")
    private String cloudbreakUrl;

    @Value("${redbeams.cloudbreak.serviceid:}")
    private String cloudbreakServiceId;

    @Value("${redbeams.environment.url}")
    private String environmentServiceUrl;

    @Value("${redbeams.environment.contextPath}")
    private String environmentRootContextPath;

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
    public String cloudbreakUrl(ServiceAddressResolver serviceAddressResolver) throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveUrl(cloudbreakUrl, "http", cloudbreakServiceId);
    }

    @Bean
    @DependsOn("serviceAddressResolver")
    public String environmentServiceUrl(ServiceAddressResolver serviceAddressResolver) throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveUrl(environmentServiceUrl + environmentRootContextPath, "", null);
    }
}
