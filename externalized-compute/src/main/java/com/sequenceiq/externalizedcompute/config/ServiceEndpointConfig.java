package com.sequenceiq.externalizedcompute.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.sequenceiq.externalizedcompute.service.registry.DNSServiceAddressResolver;
import com.sequenceiq.externalizedcompute.service.registry.RetryingServiceAddressResolver;
import com.sequenceiq.externalizedcompute.service.registry.ServiceAddressResolver;
import com.sequenceiq.externalizedcompute.service.registry.ServiceAddressResolvingException;

@Configuration
public class ServiceEndpointConfig {

    @Value("${externalizedcompute.address.resolving.timeout:60000}")
    private int resolvingTimeout;

    @Value("${externalizedcompute.db.port.5432.tcp.addr:}")
    private String dbHost;

    @Value("${externalizedcompute.db.port.5432.tcp.port:}")
    private String dbPort;

    @Value("${externalizedcompute.db.serviceid:}")
    private String databaseId;

    @Value("${externalizedcompute.environmentservice.serviceid:}")
    private String environmentServiceId;

    @Value("${externalizedcompute.environmentservice.url:}")
    private String environmentServerUrl;

    @Value("${externalizedcompute.environmentservice.server.contextPath:/environmentservice}")
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
    public String environmentServerUrl(ServiceAddressResolver serviceAddressResolver) throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveUrl(environmentServerUrl + environmentRootContextPath, "http", environmentServiceId);
    }
}
