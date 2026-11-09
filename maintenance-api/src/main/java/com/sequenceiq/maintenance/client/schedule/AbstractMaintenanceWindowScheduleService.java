package com.sequenceiq.maintenance.client.schedule;

import jakarta.inject.Inject;

import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.maintenance.api.internal.schedule.endpoint.MaintenanceWindowScheduleInternalEndpoint;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleFieldsRequest;

/**
 * Shared base for the per-service maintenance window schedule wrappers. Exposes the common create/clear delegation to a
 * scope-typed {@link MaintenanceWindowScheduleProvisioner} so each service only adds its own resource-specific
 * behaviour (e.g. the Data Lake resize guard). The services field-inject their beans (constructor injection is not used
 * in these modules), so subclasses expose the internal endpoint and scope type through accessors instead of passing
 * them to a constructor.
 */
public abstract class AbstractMaintenanceWindowScheduleService {

    @Inject
    private WebApplicationExceptionMessageExtractor exceptionMessageExtractor;

    protected abstract MaintenanceWindowScheduleInternalEndpoint scheduleInternalEndpoint();

    protected abstract MaintenanceScopeType scopeType();

    public void createScheduleIfPresent(String scopeCrn, MaintenanceWindowScheduleFieldsRequest scheduleRequest) {
        provisioner().createIfPresent(scopeCrn, scheduleRequest);
    }

    public void clearScheduleQuietly(String scopeCrn) {
        provisioner().clearQuietly(scopeCrn);
    }

    private MaintenanceWindowScheduleProvisioner provisioner() {
        return new MaintenanceWindowScheduleProvisioner(scheduleInternalEndpoint(), scopeType(), exceptionMessageExtractor);
    }
}
