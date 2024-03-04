package com.sequenceiq.remoteenvironment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.sequenceiq.remoteenvironment.service.registry.DNSServiceAddressResolver;
import com.sequenceiq.remoteenvironment.service.registry.RetryingServiceAddressResolver;
import com.sequenceiq.remoteenvironment.service.registry.ServiceAddressResolver;
import com.sequenceiq.remoteenvironment.service.registry.ServiceAddressResolvingException;

@Configuration
public class ServiceEndpointConfig {

    @Value("${remoteenvironment.address.resolving.timeout:60000}")
    private int resolvingTimeout;

    @Value("${remoteenvironment.db.port.5432.tcp.addr:}")
    private String dbHost;

    @Value("${remoteenvironment.db.port.5432.tcp.port:}")
    private String dbPort;

    @Value("${remoteenvironment.db.serviceid:}")
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
