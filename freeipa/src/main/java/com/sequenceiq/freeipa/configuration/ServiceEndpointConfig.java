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

    @Bean
    public ServiceAddressResolver serviceAddressResolver() {
        return new RetryingServiceAddressResolver(new DNSServiceAddressResolver(), resolvingTimeout);
    }

    @Bean
    @DependsOn("serviceAddressResolver")
    public String databaseAddress(ServiceAddressResolver serviceAddressResolver) throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveHostPort(dbHost, dbPort, databaseId);
    }
}
