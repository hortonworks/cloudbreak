package com.sequenceiq.maintenance.configuration;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.registry.ServiceAddressResolver;
import com.sequenceiq.cloudbreak.registry.ServiceAddressResolvingException;

/**
 * Resolves submitter service base URLs for outbound dispatch HTTP. Bean methods throw
 * {@link ServiceAddressResolvingException} at startup when a configured endpoint cannot be resolved,
 * matching {@code ServiceEndpointConfig} in other Cloudbreak services.
 */
@Configuration
public class MaintenanceServiceEndpointConfig {

    public static final String CLOUDBREAK_SUBMITTER_BASE_URL = "maintenanceCloudbreakSubmitterBaseUrl";

    public static final String DATALAKE_SUBMITTER_BASE_URL = "maintenanceDatalakeSubmitterBaseUrl";

    public static final String FREEIPA_SUBMITTER_BASE_URL = "maintenanceFreeipaSubmitterBaseUrl";

    private final ServiceAddressResolver serviceAddressResolver;

    @Value("${maintenance.cloudbreak.url:}")
    private String cloudbreakUrl;

    @Value("${maintenance.cloudbreak.serviceid:}")
    private String cloudbreakServiceId;

    @Value("${maintenance.cloudbreak.server.contextPath:/cb}")
    private String cloudbreakContextPath;

    @Value("${maintenance.datalake.url:}")
    private String datalakeUrl;

    @Value("${maintenance.datalake.serviceid:}")
    private String datalakeServiceId;

    @Value("${maintenance.datalake.server.contextPath:/dl}")
    private String datalakeContextPath;

    @Value("${maintenance.freeipa.url:}")
    private String freeipaUrl;

    @Value("${maintenance.freeipa.serviceid:}")
    private String freeipaServiceId;

    @Value("${maintenance.freeipa.server.contextPath:/freeipa}")
    private String freeipaContextPath;

    @Inject
    public MaintenanceServiceEndpointConfig(ServiceAddressResolver serviceAddressResolver) {
        this.serviceAddressResolver = serviceAddressResolver;
    }

    @Bean(name = CLOUDBREAK_SUBMITTER_BASE_URL)
    public String cloudbreakSubmitterBaseUrl() throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveUrl(cloudbreakUrl + cloudbreakContextPath, "http", cloudbreakServiceId);
    }

    @Bean(name = DATALAKE_SUBMITTER_BASE_URL)
    public String datalakeSubmitterBaseUrl() throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveUrl(datalakeUrl + datalakeContextPath, "http", datalakeServiceId);
    }

    @Bean(name = FREEIPA_SUBMITTER_BASE_URL)
    public String freeipaSubmitterBaseUrl() throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveUrl(freeipaUrl + freeipaContextPath, "http", freeipaServiceId);
    }
}
