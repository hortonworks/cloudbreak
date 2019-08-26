package com.sequenceiq.environment.configuration.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.environment.configuration.registry.DNSServiceAddressResolver;
import com.sequenceiq.environment.configuration.registry.RetryingServiceAddressResolver;
import com.sequenceiq.environment.configuration.registry.ServiceAddressResolver;
import com.sequenceiq.environment.configuration.registry.ServiceAddressResolvingException;

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

    @Value("${environment.cloudbreak.url}")
    private String cloudbreakUrl;

    @Value("${environment.cloudbreak.contextPath}")
    private String cbRootContextPath;

    @Value("${environment.cloudbreak.serviceId:}")
    private String cloudbreakServiceId;

    @Value("${environment.redbeams.url}")
    private String redbeamsServiceUrl;

    @Value("${environment.redbeams.contextPath}")
    private String redbeamsRootContextPath;

    @Value("${environment.redbeams.serviceId:}")
    private String redbeamsServiceId;

    @Value("${environment.freeipa.url}")
    private String freeipaServiceUrl;

    @Value("${environment.freeipa.contextPath}")
    private String freeipaRootContextPath;

    @Value("${environment.freeipa.serviceId:}")
    private String freeipaServiceId;

    @Value("${environment.sdx.url:}")
    private String sdxServiceUrl;

    @Value("${environment.sdx.serviceId:}")
    private String sdxServiceId;

    @Value("${environment.sdx.server.contextPath:/dl}")
    private String sdxRootContextPath;

    @Bean
    public ServiceAddressResolver serviceAddressResolver() {
        return new RetryingServiceAddressResolver(new DNSServiceAddressResolver(), resolvingTimeout);
    }

    @Bean
    public String databaseAddress(ServiceAddressResolver serviceAddressResolver) {
        return serviceAddressResolver.resolveHostPort(dbHost, dbPort, databaseId);
    }

    @Bean
    public String cloudbreakServerUrl(ServiceAddressResolver serviceAddressResolver) {
        return serviceAddressResolver.resolveUrl(cloudbreakUrl + cbRootContextPath, "http", cloudbreakServiceId);
    }

    @Bean
    public String redbeamsServerUrl() throws ServiceAddressResolvingException {
        return serviceAddressResolver().resolveUrl(redbeamsServiceUrl + redbeamsRootContextPath, "http", redbeamsServiceId);
    }

    @Bean
    public String freeIpaServerUrl() throws ServiceAddressResolvingException {
        return serviceAddressResolver().resolveUrl(freeipaServiceUrl + freeipaRootContextPath, "http", freeipaServiceId);
    }

    @Bean
    public String sdxServerUrl() throws ServiceAddressResolvingException {
        return serviceAddressResolver().resolveUrl(sdxServiceUrl + sdxRootContextPath, "http", sdxServiceId);
    }
}
