package com.sequenceiq.maintenance.client.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.maintenance.api.internal.schedule.endpoint.MaintenanceWindowScheduleInternalEndpoint;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeTypes;
import com.sequenceiq.maintenance.api.model.MaintenanceSubmitterService;
import com.sequenceiq.maintenance.api.util.MaintenanceNotFoundExceptions;

/**
 * Resolves whether a maintenance window schedule exists for a resource (resource → environment → tenant).
 * Shared by submitter services; {@link MaintenanceSubmitterService} defines which resource scopes each service owns.
 */
public class MaintenanceWindowScheduleLookup {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceWindowScheduleLookup.class);

    private final MaintenanceWindowScheduleInternalEndpoint scheduleInternalEndpoint;

    private final MaintenanceSubmitterService submitter;

    public MaintenanceWindowScheduleLookup(
            MaintenanceWindowScheduleInternalEndpoint scheduleInternalEndpoint,
            MaintenanceSubmitterService submitter) {
        this.scheduleInternalEndpoint = scheduleInternalEndpoint;
        this.submitter = submitter;
    }

    /**
     * Returns true when a schedule exists at resource, environment, or tenant scope (most specific wins at dispatch time).
     *
     * @param tenantAccountId account id used for TENANT scope lookup and internal actor context (not inferred from the caller CRN)
     * @param resourceCrn CRN of the resource registering maintenance tasks; scope is derived from the CRN
     * @throws IllegalArgumentException if {@code resourceCrn} resolves to a scope this submitter does not own
     */
    public boolean hasConfiguredSchedule(String tenantAccountId, String resourceCrn, String environmentCrn) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> hasConfiguredScheduleInternal(tenantAccountId, resourceCrn, environmentCrn),
                tenantAccountId);
    }

    private boolean hasConfiguredScheduleInternal(String tenantAccountId, String resourceCrn, String environmentCrn) {
        MaintenanceScopeType resourceScope = submitter.validateResourceScope(
                resourceCrn, MaintenanceScopeTypes.fromResourceCrn(resourceCrn));
        if (scheduleExists(tenantAccountId, resourceScope.name(), resourceCrn)) {
            return true;
        }
        if (scheduleExists(tenantAccountId, MaintenanceScopeType.ENVIRONMENT.name(), environmentCrn)) {
            return true;
        }
        return scheduleExists(tenantAccountId, MaintenanceScopeType.TENANT.name(), tenantAccountId);
    }

    private boolean scheduleExists(String accountId, String scopeType, String scopeId) {
        try {
            scheduleInternalEndpoint.get(accountId, scopeType, scopeId);
            return true;
        } catch (RuntimeException e) {
            if (MaintenanceNotFoundExceptions.isNotFound(e)) {
                return false;
            }
            LOGGER.warn("Failed to resolve maintenance schedule for accountId={} scopeType={} scopeId={}",
                    accountId, scopeType, scopeId, e);
            throw e;
        }
    }
}
