package com.sequenceiq.maintenance.controller;

import jakarta.ws.rs.core.Response;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.maintenance.api.v1.schedule.endpoint.MaintenanceWindowScheduleEndpoint;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleListParams;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowSkipRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.UpdateMaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowScheduleListResponse;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowScheduleResponse;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowSkipResponse;

@Controller
public class MaintenanceWindowScheduleController implements MaintenanceWindowScheduleEndpoint {

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public MaintenanceWindowScheduleListResponse list(MaintenanceWindowScheduleListParams params) {
        return new MaintenanceWindowScheduleListResponse();
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public MaintenanceWindowScheduleResponse get(String scopeType, String scopeId) {
        return new MaintenanceWindowScheduleResponse();
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public Response create(MaintenanceWindowScheduleRequest request) {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public MaintenanceWindowScheduleResponse update(
            String scopeType, String scopeId, UpdateMaintenanceWindowScheduleRequest request) {
        return new MaintenanceWindowScheduleResponse();
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public void delete(String scopeType, String scopeId) {
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public MaintenanceWindowSkipResponse skipNextWindow(
            String scopeType, String scopeId, MaintenanceWindowSkipRequest request) {
        return new MaintenanceWindowSkipResponse();
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public MaintenanceWindowSkipResponse cancelSkipNextWindow(String scopeType, String scopeId) {
        return new MaintenanceWindowSkipResponse();
    }
}
