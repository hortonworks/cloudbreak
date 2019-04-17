package com.sequenceiq.rdb.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.sequenceiq.rdb.service.registry.DNSServiceAddressResolver;
import com.sequenceiq.rdb.service.registry.RetryingServiceAddressResolver;
import com.sequenceiq.rdb.service.registry.ServiceAddressResolver;
import com.sequenceiq.rdb.service.registry.ServiceAddressResolvingException;

@Configuration
public class ServiceEndpointConfig {
    @Value("${rdb.address.resolving.timeout:60000}")
    private int resolvingTimeout;

    @Value("${rdb.db.port.5432.tcp.addr:}")
    private String dbHost;

    @Value("${rdb.db.port.5432.tcp.port:}")
    private String dbPort;

    @Value("${rdb.db.serviceid:}")
    private String databaseId;

    @Value("${rdb.cloudbreak.url:}")
    private String cloudbreakUrl;

    @Value("${rdb.cloudbreak.serviceid:}")
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
