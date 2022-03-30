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

    @Bean
    public ServiceAddressResolver serviceAddressResolver() {
        return new RetryingServiceAddressResolver(new DNSServiceAddressResolver(), resolvingTimeout);
    }

    @Bean
    public String databaseAddress() throws ServiceAddressResolvingException {
        return serviceAddressResolver().resolveHostPort(dbHost, dbPort, databaseId);
    }
}
