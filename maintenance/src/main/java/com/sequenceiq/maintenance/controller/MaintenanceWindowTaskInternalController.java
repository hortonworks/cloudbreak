package com.sequenceiq.maintenance.controller;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.maintenance.api.v1.task.endpoint.MaintenanceWindowTaskEndpoint;
import com.sequenceiq.maintenance.api.v1.task.model.request.MaintenanceWindowTaskListParams;
import com.sequenceiq.maintenance.api.v1.task.model.request.MaintenanceWindowTaskRequest;
import com.sequenceiq.maintenance.api.v1.task.model.request.UpdateMaintenanceWindowTaskRequest;
import com.sequenceiq.maintenance.api.v1.task.model.response.MaintenanceWindowTaskListResponse;
import com.sequenceiq.maintenance.api.v1.task.model.response.MaintenanceWindowTaskResponse;
import com.sequenceiq.maintenance.service.MaintenanceWindowTaskService;
import com.sequenceiq.maintenance.service.MaintenanceWindowTaskService.TaskRegistrationResult;

@Controller
public class MaintenanceWindowTaskInternalController implements MaintenanceWindowTaskEndpoint {

    private final MaintenanceWindowTaskService taskService;

    @Context
    private UriInfo uriInfo;

    public MaintenanceWindowTaskInternalController(MaintenanceWindowTaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    @InternalOnly
    public MaintenanceWindowTaskListResponse list(@AccountId String accountId, MaintenanceWindowTaskListParams params) {
        return taskService.list(accountId, params);
    }

    @Override
    @InternalOnly
    public MaintenanceWindowTaskResponse get(@AccountId String accountId, Long taskId) {
        return taskService.get(accountId, taskId);
    }

    @Override
    @InternalOnly
    public Response register(@AccountId String accountId, MaintenanceWindowTaskRequest request) {
        TaskRegistrationResult result = taskService.register(
                request,
                accountId,
                ThreadBasedUserCrnProvider.getUserCrn());
        Response.ResponseBuilder builder = Response.status(result.created() ? Response.Status.CREATED : Response.Status.OK)
                .entity(result.response());
        if (result.created()) {
            builder.location(uriInfo.getAbsolutePathBuilder()
                    .path(String.valueOf(result.response().getId()))
                    .build());
        }
        return builder.build();
    }

    @Override
    @InternalOnly
    public MaintenanceWindowTaskResponse update(@AccountId String accountId, Long taskId, UpdateMaintenanceWindowTaskRequest request) {
        return taskService.update(
                accountId,
                taskId,
                request,
                ThreadBasedUserCrnProvider.getUserCrn());
    }

    @Override
    @InternalOnly
    public void delete(@AccountId String accountId, Long taskId) {
        taskService.delete(
                accountId,
                taskId,
                ThreadBasedUserCrnProvider.getUserCrn());
    }
}
