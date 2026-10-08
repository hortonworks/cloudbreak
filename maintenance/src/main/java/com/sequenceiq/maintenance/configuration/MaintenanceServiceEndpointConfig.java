package com.sequenceiq.maintenance.configuration;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.registry.ServiceAddressResolver;
import com.sequenceiq.cloudbreak.registry.ServiceAddressResolvingException;
import com.sequenceiq.maintenance.api.model.MaintenanceSubmitterService;

/**
 * Resolves submitter service base URLs for outbound dispatch HTTP. Bean methods throw
 * {@link ServiceAddressResolvingException} at startup when a configured endpoint cannot be resolved,
 * matching {@code ServiceEndpointConfig} in other Cloudbreak services.
 */
@Configuration
public class MaintenanceServiceEndpointConfig {

    public static final String SUBMITTER_BASE_URLS = "maintenanceSubmitterBaseUrls";

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

    @Bean(name = SUBMITTER_BASE_URLS)
    public Map<String, String> submitterBaseUrls() throws ServiceAddressResolvingException {
        Map<String, String> urls = new HashMap<>();
        for (MaintenanceSubmitterService submitterService : MaintenanceSubmitterService.values()) {
            registerSubmitter(urls, submitterService, resolveSubmitterBaseUrl(submitterService));
        }
        return Map.copyOf(urls);
    }

    private String resolveSubmitterBaseUrl(MaintenanceSubmitterService submitterService) throws ServiceAddressResolvingException {
        return switch (submitterService) {
            case CLOUDBREAK -> serviceAddressResolver.resolveUrl(
                    cloudbreakUrl + cloudbreakContextPath, "http", cloudbreakServiceId);
            case DATALAKE -> serviceAddressResolver.resolveUrl(
                    datalakeUrl + datalakeContextPath, "http", datalakeServiceId);
            case FREEIPA -> serviceAddressResolver.resolveUrl(
                    freeipaUrl + freeipaContextPath, "http", freeipaServiceId);
        };
    }

    private static void registerSubmitter(
            Map<String, String> urls, MaintenanceSubmitterService submitterService, String baseUrl) {
        if (StringUtils.isNotBlank(baseUrl)) {
            urls.put(submitterService.serviceName(), baseUrl);
        }
    }
}
