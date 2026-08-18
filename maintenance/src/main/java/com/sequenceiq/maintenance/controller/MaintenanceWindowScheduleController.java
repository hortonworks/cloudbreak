package com.sequenceiq.maintenance.controller;

import static com.sequenceiq.maintenance.domain.MaintenanceEnumValues.toScopeType;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.maintenance.api.v1.schedule.endpoint.MaintenanceWindowScheduleEndpoint;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleListParams;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowSkipRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.UpdateMaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowScheduleListResponse;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowScheduleResponse;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowSkipResponse;
import com.sequenceiq.maintenance.service.MaintenanceWindowScheduleService;
import com.sequenceiq.maintenance.service.MaintenanceWindowSkipService;

@Controller
public class MaintenanceWindowScheduleController implements MaintenanceWindowScheduleEndpoint {

    private final MaintenanceWindowScheduleService scheduleService;

    private final MaintenanceWindowSkipService skipService;

    public MaintenanceWindowScheduleController(MaintenanceWindowScheduleService scheduleService, MaintenanceWindowSkipService skipService) {
        this.scheduleService = scheduleService;
        this.skipService = skipService;
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public MaintenanceWindowScheduleListResponse list(MaintenanceWindowScheduleListParams params) {
        return scheduleService.list(
                ThreadBasedUserCrnProvider.getAccountId(),
                toScopeType(params.getScopeType()),
                params.getScopeId());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public MaintenanceWindowScheduleResponse get(String scopeType, String scopeId) {
        return scheduleService.get(ThreadBasedUserCrnProvider.getAccountId(), toScopeType(scopeType), scopeId);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public Response create(MaintenanceWindowScheduleRequest request) {
        MaintenanceWindowScheduleResponse response = scheduleService.create(
                request,
                ThreadBasedUserCrnProvider.getAccountId(),
                ThreadBasedUserCrnProvider.getUserCrn());
        return Response.status(Response.Status.CREATED)
                .location(UriBuilder.fromPath("scope/{scopeType}/scopeId/{scopeId}")
                        .build(response.getScopeType(), response.getScopeId()))
                .entity(response)
                .build();
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public MaintenanceWindowScheduleResponse update(
            String scopeType, String scopeId, UpdateMaintenanceWindowScheduleRequest request) {
        return scheduleService.update(
                ThreadBasedUserCrnProvider.getAccountId(),
                toScopeType(scopeType),
                scopeId,
                request,
                ThreadBasedUserCrnProvider.getUserCrn());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public void delete(String scopeType, String scopeId) {
        scheduleService.delete(
                ThreadBasedUserCrnProvider.getAccountId(),
                toScopeType(scopeType),
                scopeId,
                ThreadBasedUserCrnProvider.getUserCrn());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public MaintenanceWindowSkipResponse skipNextWindow(
            String scopeType, String scopeId, MaintenanceWindowSkipRequest request) {
        return skipService.skipNextWindow(
                ThreadBasedUserCrnProvider.getAccountId(),
                toScopeType(scopeType),
                scopeId,
                request,
                ThreadBasedUserCrnProvider.getUserCrn());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public MaintenanceWindowSkipResponse cancelSkipNextWindow(String scopeType, String scopeId) {
        return skipService.cancelSkipNextWindow(
                ThreadBasedUserCrnProvider.getAccountId(),
                toScopeType(scopeType),
                scopeId);
    }
}
