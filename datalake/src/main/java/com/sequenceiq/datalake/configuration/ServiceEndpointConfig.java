package com.sequenceiq.datalake.configuration;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.sequenceiq.cloudbreak.registry.ServiceAddressResolver;
import com.sequenceiq.cloudbreak.registry.ServiceAddressResolvingException;

@Configuration
public class ServiceEndpointConfig {

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

    @Value("${datalake.freeipa.url}")
    private String freeipaServiceUrl;

    @Value("${datalake.freeipa.contextPath}")
    private String freeipaRootContextPath;

    @Value("${datalake.freeipa.serviceId:}")
    private String freeipaServiceId;

    @Inject
    private ServiceAddressResolver serviceAddressResolver;

    @Bean
    @DependsOn("serviceAddressResolver")
    public String cloudbreakUrl() throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveUrl(cloudbreakUrl + cbRootContextPath, "http", cloudbreakServiceId);
    }

    @Bean
    @DependsOn("serviceAddressResolver")
    public String environmentServerUrl() throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveUrl(environmentServerUrl + environmentRootContextPath, "http", environmentServiceId);
    }

    @Bean
    @DependsOn("serviceAddressResolver")
    public String redbeamsServerUrl() throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveUrl(redbeamsServerUrl + redbeamsRootContextPath, "http", redbeamsServiceId);
    }

    @Bean
    @DependsOn("serviceAddressResolver")
    public String freeIpaServerUrl() throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveUrl(freeipaServiceUrl + freeipaRootContextPath, "http", freeipaServiceId);
    }
}
