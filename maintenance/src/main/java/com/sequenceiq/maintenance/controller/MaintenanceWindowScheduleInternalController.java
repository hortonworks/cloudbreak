package com.sequenceiq.maintenance.controller;

import static com.sequenceiq.maintenance.domain.MaintenanceEnumValues.toScopeType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.maintenance.api.internal.schedule.endpoint.MaintenanceWindowScheduleInternalEndpoint;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleRestoreRequest;
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

    @Override
    @InternalOnly
    public MaintenanceWindowScheduleResponse restore(
            @AccountId String accountId,
            @InitiatorUserCrn String initiatorUserCrn,
            String scopeType,
            String scopeId,
            MaintenanceWindowScheduleRestoreRequest request) {
        return scheduleService.restore(
            accountId,
            toScopeType(scopeType),
            scopeId,
            request,
            initiatorUserCrn);
    }

    @Override
    @InternalOnly
    public MaintenanceWindowScheduleResponse create(
            @AccountId String accountId,
            @InitiatorUserCrn String initiatorUserCrn,
            MaintenanceWindowScheduleRequest request) {
        return scheduleService.create(request, accountId, initiatorUserCrn);
    }

    @Override
    @InternalOnly
    public void delete(
            @AccountId String accountId,
            @InitiatorUserCrn String initiatorUserCrn,
            String scopeType,
            String scopeId) {
        scheduleService.delete(accountId, toScopeType(scopeType), scopeId, initiatorUserCrn);
    }
}
