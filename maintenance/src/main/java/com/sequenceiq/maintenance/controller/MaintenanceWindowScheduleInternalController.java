package com.sequenceiq.maintenance.controller;

import static com.sequenceiq.maintenance.domain.MaintenanceEnumValues.toScopeType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.maintenance.api.internal.schedule.endpoint.MaintenanceWindowScheduleInternalEndpoint;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowScheduleResponse;
import com.sequenceiq.maintenance.service.MaintenanceWindowScheduleService;

@Controller
public class MaintenanceWindowScheduleInternalController implements MaintenanceWindowScheduleInternalEndpoint {

    private final MaintenanceWindowScheduleService scheduleService;

    public MaintenanceWindowScheduleInternalController(MaintenanceWindowScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @Override
    @InternalOnly
    public MaintenanceWindowScheduleResponse get(
            @AccountId String accountId,
            String scopeType,
            String scopeId) {
        return scheduleService.get(accountId, toScopeType(scopeType), scopeId);
    }
}
