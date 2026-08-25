package com.sequenceiq.maintenance.controller;

import jakarta.ws.rs.core.Response;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.maintenance.api.v1.task.endpoint.MaintenanceWindowTaskEndpoint;
import com.sequenceiq.maintenance.api.v1.task.model.request.MaintenanceWindowTaskListParams;
import com.sequenceiq.maintenance.api.v1.task.model.request.MaintenanceWindowTaskRequest;
import com.sequenceiq.maintenance.api.v1.task.model.request.UpdateMaintenanceWindowTaskRequest;
import com.sequenceiq.maintenance.api.v1.task.model.response.MaintenanceWindowTaskListResponse;
import com.sequenceiq.maintenance.api.v1.task.model.response.MaintenanceWindowTaskResponse;

@Controller
public class MaintenanceWindowTaskInternalController implements MaintenanceWindowTaskEndpoint {

    private static final String NOT_IMPLEMENTED = "Maintenance window task registration is not implemented yet.";

    @Override
    @InternalOnly
    public MaintenanceWindowTaskListResponse list(@AccountId String accountId, MaintenanceWindowTaskListParams params) {
        throw notImplemented();
    }

    @Override
    @InternalOnly
    public MaintenanceWindowTaskResponse get(@AccountId String accountId, Long taskId) {
        throw notImplemented();
    }

    @Override
    @InternalOnly
    public Response register(@AccountId String accountId, MaintenanceWindowTaskRequest request) {
        throw notImplemented();
    }

    @Override
    @InternalOnly
    public MaintenanceWindowTaskResponse update(
            @AccountId String accountId, Long taskId, UpdateMaintenanceWindowTaskRequest request) {
        throw notImplemented();
    }

    @Override
    @InternalOnly
    public void delete(@AccountId String accountId, Long taskId) {
        throw notImplemented();
    }

    private static CloudbreakServiceException notImplemented() {
        return new CloudbreakServiceException(NOT_IMPLEMENTED);
    }
}
