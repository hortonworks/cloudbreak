package com.sequenceiq.maintenance.configuration;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.registry.ServiceAddressResolver;
import com.sequenceiq.cloudbreak.registry.ServiceAddressResolvingException;
import com.sequenceiq.maintenance.api.model.MaintenanceSubmitterService;

/**
 * Resolves peer service base URLs (Cloudbreak / Datalake / FreeIPA) from maintenance's {@code maintenance.*} properties.
 */
@Component
public class MaintenancePeerServiceUrlResolver {

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
    public MaintenancePeerServiceUrlResolver(ServiceAddressResolver serviceAddressResolver) {
        this.serviceAddressResolver = serviceAddressResolver;
    }

    public String resolveBaseUrl(MaintenanceSubmitterService peerService) throws ServiceAddressResolvingException {
        return switch (peerService) {
            case CLOUDBREAK -> serviceAddressResolver.resolveUrl(
                    cloudbreakUrl + cloudbreakContextPath, "http", cloudbreakServiceId);
            case DATALAKE -> serviceAddressResolver.resolveUrl(
                    datalakeUrl + datalakeContextPath, "http", datalakeServiceId);
            case FREEIPA -> serviceAddressResolver.resolveUrl(
                    freeipaUrl + freeipaContextPath, "http", freeipaServiceId);
        };
    }
}
