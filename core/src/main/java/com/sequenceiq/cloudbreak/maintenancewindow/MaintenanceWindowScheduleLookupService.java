package com.sequenceiq.cloudbreak.maintenancewindow;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.maintenance.api.internal.schedule.endpoint.MaintenanceWindowScheduleInternalEndpoint;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeTypes;

@Service
@ConditionalOnExpression(MaintenanceServiceConditions.URL_CONFIGURED)
public class MaintenanceWindowScheduleLookupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceWindowScheduleLookupService.class);

    private final MaintenanceWindowScheduleInternalEndpoint scheduleInternalEndpoint;

    @Inject
    public MaintenanceWindowScheduleLookupService(MaintenanceWindowScheduleInternalEndpoint scheduleInternalEndpoint) {
        this.scheduleInternalEndpoint = scheduleInternalEndpoint;
    }

    /**
     * Returns true when a schedule exists at resource, environment, or tenant scope (most specific wins at dispatch time).
     *
     * @param tenantAccountId account id used for TENANT scope lookup and internal actor context (not inferred from the caller CRN)
     * @param resourceCrn CRN of the stack registering maintenance tasks. Core owns both Data Hub
     *         ({@code crn:cdp:datahub:…:cluster:…}) and Data Lake ({@code crn:cdp:datalake:…:datalake:…}) stacks, and the
     *         resource scope is derived from the CRN so a schedule is looked up under the scope it was registered against.
     * @throws IllegalArgumentException if {@code resourceCrn} resolves to a scope core does not own
     */
    public boolean hasConfiguredSchedule(String tenantAccountId, String resourceCrn, String environmentCrn) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> hasConfiguredScheduleInternal(tenantAccountId, resourceCrn, environmentCrn),
                tenantAccountId);
    }

    private boolean hasConfiguredScheduleInternal(String tenantAccountId, String resourceCrn, String environmentCrn) {
        if (scheduleExists(tenantAccountId, resourceScopeType(resourceCrn).name(), resourceCrn)) {
            return true;
        }
        if (scheduleExists(tenantAccountId, MaintenanceScopeType.ENVIRONMENT.name(), environmentCrn)) {
            return true;
        }
        return scheduleExists(tenantAccountId, MaintenanceScopeType.TENANT.name(), tenantAccountId);
    }

    /**
     * Core registers tasks for the stacks it owns: Data Hub (StackType.WORKLOAD) and Data Lake (StackType.DATALAKE).
     * Anything else means the caller passed a resource core does not manage, so fail loudly rather than probe the
     * wrong scope and silently report "no schedule".
     */
    private MaintenanceScopeType resourceScopeType(String resourceCrn) {
        MaintenanceScopeType scopeType = MaintenanceScopeTypes.fromResourceCrn(resourceCrn);
        if (scopeType != MaintenanceScopeType.DATAHUB && scopeType != MaintenanceScopeType.DATALAKE) {
            throw new IllegalArgumentException(String.format(
                    "Core registers maintenance tasks for Data Hub and Data Lake resources only, but resourceCrn %s resolves to scope %s",
                    resourceCrn, scopeType));
        }
        return scopeType;
    }

    private boolean scheduleExists(String accountId, String scopeType, String scopeId) {
        try {
            scheduleInternalEndpoint.get(accountId, scopeType, scopeId);
            return true;
        } catch (RuntimeException e) {
            if (isNotFound(e)) {
                return false;
            }
            LOGGER.warn("Failed to resolve maintenance schedule for accountId={} scopeType={} scopeId={}",
                    accountId, scopeType, scopeId, e);
            throw e;
        }
    }

    private static boolean isNotFound(Throwable throwable) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (current instanceof WebApplicationException webApplicationException
                    && webApplicationException.getResponse().getStatus() == HttpStatus.NOT_FOUND.value()) {
                return true;
            }
        }
        return false;
    }
}
