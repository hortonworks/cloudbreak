package com.sequenceiq.consumption.configuration.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.consumption.configuration.registry.DNSServiceAddressResolver;
import com.sequenceiq.consumption.configuration.registry.RetryingServiceAddressResolver;
import com.sequenceiq.consumption.configuration.registry.ServiceAddressResolver;
import com.sequenceiq.consumption.configuration.registry.ServiceAddressResolvingException;

@Configuration
public class ServiceEndpointConfig {

    @Value("${consumption.address.resolving.timeout:60000}")
    private int resolvingTimeout;

    @Value("${consumption.db.host:}")
    private String dbHost;

    @Value("${consumption.db.port:}")
    private String dbPort;

    @Value("${consumption.db.serviceId:}")
    private String databaseId;

    @Value("${consumption.environment.url}")
    private String environmentServiceUrl;

    @Value("${consumption.environment.contextPath}")
    private String environmentRootContextPath;

    @Value("${consumption.cloudbreak.url}")
    private String cloudbreakUrl;

    @Value("${consumption.cloudbreak.contextPath}")
    private String cbRootContextPath;

    @Value("${consumption.cloudbreak.serviceId:}")
    private String cloudbreakServiceId;

    @Bean
    public ServiceAddressResolver serviceAddressResolver() {
        return new RetryingServiceAddressResolver(new DNSServiceAddressResolver(), resolvingTimeout);
    }

    @Bean
    public String databaseAddress() throws ServiceAddressResolvingException {
        return serviceAddressResolver().resolveHostPort(dbHost, dbPort, databaseId);
    }

    @Bean
    public String environmentServiceUrl() throws ServiceAddressResolvingException {
        return serviceAddressResolver().resolveUrl(environmentServiceUrl + environmentRootContextPath, "", null);
    }

    @Bean
    public String cloudbreakServerUrl(ServiceAddressResolver serviceAddressResolver) throws ServiceAddressResolvingException {
        return serviceAddressResolver().resolveUrl(cloudbreakUrl + cbRootContextPath, "http", cloudbreakServiceId);
    }
}
