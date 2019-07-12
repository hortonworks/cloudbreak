package com.sequenceiq.datalake.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.sequenceiq.datalake.service.registry.DNSServiceAddressResolver;
import com.sequenceiq.datalake.service.registry.RetryingServiceAddressResolver;
import com.sequenceiq.datalake.service.registry.ServiceAddressResolver;
import com.sequenceiq.datalake.service.registry.ServiceAddressResolvingException;

@Configuration
public class ServiceEndpointConfig {
    @Value("${datalake.address.resolving.timeout:60000}")
    private int resolvingTimeout;

    @Value("${datalake.db.port.5432.tcp.addr:}")
    private String dbHost;

    @Value("${datalake.db.port.5432.tcp.port:}")
    private String dbPort;

    @Value("${datalake.db.serviceid:}")
    private String databaseId;

    @Value("${datalake.redbeams.url:}")
    private String redbeamsServerUrl;

    @Value("${datalake.redbeams.contextPath:/redbeams}")
    private String redbeamsRootContextPath;

    @Value("${datalake.redbeams.serviceid:}")
    private String redbeamsServiceId;

    @Value("${datalake.cloudbreak.url:}")
    private String cloudbreakUrl;

    @Value("${datalake.cloudbreak.serviceid:}")
    private String cloudbreakServiceId;

    @Value("${datalake.cloudbreak.server.contextPath:/cb}")
    private String cbRootContextPath;

    @Value("${datalake.environmentservice.serviceid:}")
    private String environmentServiceId;

    @Value("${datalake.environmentservice.url:}")
    private String environmentServerUrl;

    @Value("${datalake.environmentservice.server.contextPath:/environmentservice}")
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
    public String cloudbreakUrl(ServiceAddressResolver serviceAddressResolver) throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveUrl(cloudbreakUrl + cbRootContextPath, "http", cloudbreakServiceId);
    }

    @Bean
    @DependsOn("serviceAddressResolver")
    public String environmentServerUrl(ServiceAddressResolver serviceAddressResolver) throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveUrl(environmentServerUrl + environmentRootContextPath, "http", environmentServiceId);
    }

    @Bean
    @DependsOn("serviceAddressResolver")
    public String redbeamsServerUrl(ServiceAddressResolver serviceAddressResolver) throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveUrl(redbeamsServerUrl + redbeamsRootContextPath, "http", redbeamsServiceId);
    }
}
