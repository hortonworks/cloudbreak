package com.sequenceiq.maintenance.service;

import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXInternalV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaInternalV1Endpoint;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.util.MaintenanceNotFoundExceptions;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;

@Service
public class MaintenanceResourceEnvironmentCrnResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceResourceEnvironmentCrnResolver.class);

    private final DistroXInternalV1Endpoint distroXInternalV1Endpoint;

    private final SdxEndpoint sdxEndpoint;

    private final FreeIpaInternalV1Endpoint freeIpaInternalV1Endpoint;

    @Inject
    public MaintenanceResourceEnvironmentCrnResolver(
            DistroXInternalV1Endpoint distroXInternalV1Endpoint,
            SdxEndpoint sdxEndpoint,
            FreeIpaInternalV1Endpoint freeIpaInternalV1Endpoint) {
        this.distroXInternalV1Endpoint = distroXInternalV1Endpoint;
        this.sdxEndpoint = sdxEndpoint;
        this.freeIpaInternalV1Endpoint = freeIpaInternalV1Endpoint;
    }

    /**
     * Returns empty when the resource is not found in the owning service or has no parent environment, so inheritance
     * can still fall back to resource and tenant scopes.
     */
    public Optional<String> resolveOptional(String accountId, String resourceCrn, MaintenanceScopeType resourceScopeType) {
        return switch (resourceScopeType) {
            case DATAHUB -> tryEnvironmentCrn(resourceCrn, () -> ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> distroXInternalV1Endpoint.getByCrn(resourceCrn).getEnvironmentCrn(), accountId));
            case DATALAKE -> tryEnvironmentCrn(resourceCrn, () -> ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> datalakeEnvironmentCrn(resourceCrn), accountId));
            case FREEIPA -> tryEnvironmentCrn(resourceCrn, () -> ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> freeIpaInternalV1Endpoint.getEnvironmentCrnByResourceCrn(resourceCrn).getEnvironmentCrn(), accountId));
            case ENVIRONMENT, TENANT -> throw new IllegalArgumentException(
                    "Cannot resolve environment CRN for scope type " + resourceScopeType);
        };
    }

    /**
     * {@link com.sequenceiq.sdx.api.endpoint.SdxEndpoint#getByCrn(String)} only accepts {@code datalake}-typed CRNs on the client;
     * legacy {@code sdxcluster} CRNs still map to {@link MaintenanceScopeType#DATALAKE} via {@link
     * com.sequenceiq.maintenance.api.model.MaintenanceScopeTypes}.
     */
    private String datalakeEnvironmentCrn(String resourceCrn) {
        if (Crn.fromString(resourceCrn).getResourceType() == Crn.ResourceType.SDX_CLUSTER) {
            return sdxEndpoint.getDetailByCrn(resourceCrn, Set.of()).getEnvironmentCrn();
        }
        return sdxEndpoint.getByCrn(resourceCrn).getEnvironmentCrn();
    }

    private Optional<String> tryEnvironmentCrn(String resourceCrn, EnvironmentCrnSupplier supplier) {
        try {
            String environmentCrn = supplier.get();
            if (StringUtils.isBlank(environmentCrn)) {
                LOGGER.debug("Owning service returned no parent environment for resourceCrn={}", resourceCrn);
                return Optional.empty();
            }
            return Optional.of(environmentCrn);
        } catch (RuntimeException e) {
            if (MaintenanceNotFoundExceptions.isNotFound(e)) {
                LOGGER.debug("No owning-service resource for resourceCrn={}; skipping environment inheritance", resourceCrn, e);
                return Optional.empty();
            }
            throw e;
        }
    }

    @FunctionalInterface
    private interface EnvironmentCrnSupplier {
        String get();
    }
}
