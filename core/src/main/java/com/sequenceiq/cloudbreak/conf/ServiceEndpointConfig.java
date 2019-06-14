package com.sequenceiq.cloudbreak.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.service.registry.DNSServiceAddressResolver;
import com.sequenceiq.cloudbreak.service.registry.RetryingServiceAddressResolver;
import com.sequenceiq.cloudbreak.service.registry.ServiceAddressResolver;
import com.sequenceiq.cloudbreak.service.registry.ServiceAddressResolvingException;

@Configuration
public class ServiceEndpointConfig {
    @Value("${cb.address.resolving.timeout:}")
    private int resolvingTimeout;

    @Value("${cb.db.port.5432.tcp.addr:}")
    private String dbHost;

    @Value("${cb.db.port.5432.tcp.port:}")
    private String dbPort;

    @Value("${cb.db.serviceid:}")
    private String databaseId;

    @Value("${cb.identity.server.url:}")
    private String identityServiceUrl;

    @Value("${cb.identity.serviceid:}")
    private String identityServiceId;

    @Value("${cb.environment.url}")
    private String environmentServiceUrl;

    @Value("${cb.environment.contextPath}")
    private String environmentContextPath;

    @Value("${cb.environment.serviceid:}")
    private String environmentServiceId;

    @Value("${cb.freeipa.url}")
    private String freeipaServiceUrl;

    @Value("${cb.freeipa.contextPath}")
    private String freeipaContextPath;

    @Value("${cb.freeipa.serviceid:}")
    private String freeipaServiceId;

    @Bean
    public ServiceAddressResolver serviceAddressResolver() {
        return new RetryingServiceAddressResolver(new DNSServiceAddressResolver(), resolvingTimeout);
    }

    @Bean
    public String databaseAddress() throws ServiceAddressResolvingException {
        return serviceAddressResolver().resolveHostPort(dbHost, dbPort, databaseId);
    }

    @Bean
    public String identityServerUrl()  throws ServiceAddressResolvingException {
        return serviceAddressResolver().resolveUrl(identityServiceUrl, "http", identityServiceId);
    }

    @Bean
    public String environmentServerUrl()  throws ServiceAddressResolvingException {
        return serviceAddressResolver().resolveUrl(environmentServiceUrl + environmentContextPath, "http", environmentServiceId);
    }

    @Bean
    public String freeIpaServerUrl()  throws ServiceAddressResolvingException {
        return serviceAddressResolver().resolveUrl(freeipaServiceUrl + freeipaContextPath, "http", freeipaServiceId);
    }
}
