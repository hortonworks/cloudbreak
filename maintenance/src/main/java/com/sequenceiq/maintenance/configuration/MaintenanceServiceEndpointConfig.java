package com.sequenceiq.maintenance.configuration;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.sequenceiq.cloudbreak.registry.ServiceAddressResolvingException;
import com.sequenceiq.freeipa.api.client.internal.FreeIpaApiClientConfig;
import com.sequenceiq.freeipa.api.client.internal.FreeIpaApiClientParams;
import com.sequenceiq.maintenance.api.model.MaintenanceSubmitterService;

/**
 * Resolves peer service URLs for maintenance outbound calls: submitter dispatch HTTP and typed API clients
 * (FreeIPA internal API for schedule authorization). Bean methods throw {@link ServiceAddressResolvingException}
 * at startup when a configured endpoint cannot be resolved, matching {@code ServiceEndpointConfig} in other
 * Cloudbreak services.
 */
@Configuration
@Import(FreeIpaApiClientConfig.class)
public class MaintenanceServiceEndpointConfig {

    public static final String SUBMITTER_BASE_URLS = "maintenanceSubmitterBaseUrls";

    private final MaintenancePeerServiceUrlResolver peerServiceUrlResolver;

    @Value("${rest.debug:false}")
    private boolean restDebug;

    @Value("${cert.validation:true}")
    private boolean certificateValidation;

    @Value("${cert.ignorePreValidation:true}")
    private boolean ignorePreValidation;

    @Inject
    public MaintenanceServiceEndpointConfig(MaintenancePeerServiceUrlResolver peerServiceUrlResolver) {
        this.peerServiceUrlResolver = peerServiceUrlResolver;
    }

    @Bean(name = SUBMITTER_BASE_URLS)
    public Map<String, String> submitterBaseUrls() throws ServiceAddressResolvingException {
        Map<String, String> urls = new HashMap<>();
        for (MaintenanceSubmitterService submitterService : MaintenanceSubmitterService.values()) {
            registerSubmitter(urls, submitterService, peerServiceUrlResolver.resolveBaseUrl(submitterService));
        }
        return Map.copyOf(urls);
    }

    @Bean
    public FreeIpaApiClientParams freeIpaApiClientParams() throws ServiceAddressResolvingException {
        return new FreeIpaApiClientParams(
                restDebug, certificateValidation, ignorePreValidation,
                peerServiceUrlResolver.resolveBaseUrl(MaintenanceSubmitterService.FREEIPA));
    }

    private static void registerSubmitter(
            Map<String, String> urls, MaintenanceSubmitterService submitterService, String baseUrl) {
        if (StringUtils.isNotBlank(baseUrl)) {
            urls.put(submitterService.serviceName(), baseUrl);
        }
    }
}
