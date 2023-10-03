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

    @Value("${cb.redbeams.url}")
    private String redbeamsServiceUrl;

    @Value("${cb.redbeams.contextPath}")
    private String redbeamsContextPath;

    @Value("${cb.redbeams.serviceid:}")
    private String redbeamsServiceId;

    @Value("${cb.sdx.url}")
    private String sdxServiceUrl;

    @Value("${cb.sdx.contextPath}")
    private String sdxContextPath;

    @Value("${cb.sdx.serviceid:}")
    private String sdxServiceId;

    @Value("${cb.periscope.url}")
    private String autoscaleServiceUrl;

    @Value("${cb.periscope.contextPath}")
    private String autoscaleContextPath;

    @Value("${cb.periscope.serviceid:}")
    private String autoscaleServiceId;

    @Value("${cb.consumption.url:}")
    private String consumptionServiceUrl;

    @Value("${cb.consumption.serviceId:}")
    private String consumptionServiceId;

    @Value("${cb.consumption.contextPath}")
    private String consumptionRootContextPath;

    @Bean
    public ServiceAddressResolver serviceAddressResolver() {
        return new RetryingServiceAddressResolver(new DNSServiceAddressResolver(), resolvingTimeout);
    }

    @Bean
    public String databaseAddress() throws ServiceAddressResolvingException {
        return serviceAddressResolver().resolveHostPort(dbHost, dbPort, databaseId);
    }

    @Bean
    public String environmentServerUrl() throws ServiceAddressResolvingException {
        return serviceAddressResolver().resolveUrl(environmentServiceUrl + environmentContextPath, "http", environmentServiceId);
    }

    @Bean
    public String freeIpaServerUrl() throws ServiceAddressResolvingException {
        return serviceAddressResolver().resolveUrl(freeipaServiceUrl + freeipaContextPath, "http", freeipaServiceId);
    }

    @Bean
    public String redbeamsServerUrl() throws ServiceAddressResolvingException {
        return serviceAddressResolver().resolveUrl(redbeamsServiceUrl + redbeamsContextPath, "http", redbeamsServiceId);
    }

    @Bean
    public String sdxServerUrl() throws ServiceAddressResolvingException {
        return serviceAddressResolver().resolveUrl(sdxServiceUrl + sdxContextPath, "http", sdxServiceId);
    }

    @Bean
    public String autoscaleServerUrl() throws ServiceAddressResolvingException {
        return serviceAddressResolver().resolveUrl(autoscaleServiceUrl + autoscaleContextPath, "http", autoscaleServiceId);
    }

    @Bean
    public String consumptionServerUrl() throws ServiceAddressResolvingException {
        return serviceAddressResolver().resolveUrl(consumptionServiceUrl + consumptionRootContextPath, "http", consumptionServiceId);
    }
}
