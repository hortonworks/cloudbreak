package com.sequenceiq.environment.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.sequenceiq.environment.configuration.registry.DNSServiceAddressResolver;
import com.sequenceiq.environment.configuration.registry.RetryingServiceAddressResolver;
import com.sequenceiq.environment.configuration.registry.ServiceAddressResolver;

@Configuration
public class ServiceEndpointConfig {

    @Value("${environment.address.resolving.timeout:60000}")
    private int resolvingTimeout;

    @Value("${environment.db.host:}")
    private String dbHost;

    @Value("${environment.db.port:}")
    private String dbPort;

    @Value("${environment.db.serviceId:}")
    private String databaseId;

    @Value("${environment.cloudbreak.url:}")
    private String cloudbreakUrl;

    @Value("${environment.cloudbreak.serviceId:}")
    private String cloudbreakServiceId;

    @Bean
    public ServiceAddressResolver serviceAddressResolver() {
        return new RetryingServiceAddressResolver(new DNSServiceAddressResolver(), resolvingTimeout);
    }

    @Bean
    @DependsOn("serviceAddressResolver")
    public String databaseAddress(ServiceAddressResolver serviceAddressResolver) {
        return serviceAddressResolver.resolveHostPort(dbHost, dbPort, databaseId);
    }

    @Bean
    @DependsOn("serviceAddressResolver")
    public String cloudbreakUrl(ServiceAddressResolver serviceAddressResolver) {
        return serviceAddressResolver.resolveUrl(cloudbreakUrl, "http", cloudbreakServiceId);
    }

}
