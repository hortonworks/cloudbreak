package com.sequenceiq.environment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.sequenceiq.environment.config.registry.DNSServiceAddressResolver;
import com.sequenceiq.environment.config.registry.RetryingServiceAddressResolver;
import com.sequenceiq.environment.config.registry.ServiceAddressResolver;
import com.sequenceiq.environment.config.registry.ServiceAddressResolvingException;

@Configuration
public class ServiceEndpointConfig {

    @Value("${environment.address.resolving.timeout:60000}")
    private int resolvingTimeout;

    @Value("${environment.db.port.5432.tcp.addr:}")
    private String dbHost;

    @Value("${environment.db.port.5432.tcp.port:}")
    private String dbPort;

    @Value("${environment.db.serviceid:}")
    private String databaseId;

    @Value("${environment.cloudbreak.url:}")
    private String cloudbreakUrl;

    @Value("${environment.cloudbreak.serviceid:}")
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
    public String cloudbreakUrl(ServiceAddressResolver serviceAddressResolver) throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveUrl(cloudbreakUrl, "http", cloudbreakServiceId);
    }

}
