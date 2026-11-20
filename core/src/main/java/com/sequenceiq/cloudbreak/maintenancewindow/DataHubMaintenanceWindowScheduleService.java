package com.sequenceiq.cloudbreak.maintenancewindow;

import jakarta.inject.Inject;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import com.sequenceiq.maintenance.api.internal.schedule.endpoint.MaintenanceWindowScheduleInternalEndpoint;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.client.schedule.AbstractMaintenanceWindowScheduleService;

@Service
@ConditionalOnExpression(MaintenanceServiceConditions.URL_CONFIGURED)
public class DataHubMaintenanceWindowScheduleService extends AbstractMaintenanceWindowScheduleService {

    @Inject
    private MaintenanceWindowScheduleInternalEndpoint maintenanceWindowScheduleInternalEndpoint;

    @Override
    protected MaintenanceWindowScheduleInternalEndpoint scheduleInternalEndpoint() {
        return maintenanceWindowScheduleInternalEndpoint;
    }

    @Override
    protected MaintenanceScopeType scopeType() {
        return MaintenanceScopeType.DATAHUB;
    }
}
