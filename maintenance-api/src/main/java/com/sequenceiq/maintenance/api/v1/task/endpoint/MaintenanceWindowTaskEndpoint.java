package com.sequenceiq.maintenance.api.v1.task.endpoint;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sequenceiq.maintenance.api.doc.MaintenanceWindowTaskOpDescription;
import com.sequenceiq.maintenance.api.v1.task.model.request.MaintenanceWindowTaskListParams;
import com.sequenceiq.maintenance.api.v1.task.model.request.MaintenanceWindowTaskRequest;
import com.sequenceiq.maintenance.api.v1.task.model.request.UpdateMaintenanceWindowTaskRequest;
import com.sequenceiq.maintenance.api.v1.task.model.response.MaintenanceWindowTaskListResponse;
import com.sequenceiq.maintenance.api.v1.task.model.response.MaintenanceWindowTaskResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/internal/maintenance-tasks")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = MaintenanceWindowTaskOpDescription.TAG, description = MaintenanceWindowTaskOpDescription.TAG_DESCRIPTION)
public interface MaintenanceWindowTaskEndpoint {

    @GET
    @Operation(summary = MaintenanceWindowTaskOpDescription.LIST, description = MaintenanceWindowTaskOpDescription.LIST_NOTES,
            operationId = "listMaintenanceWindowTasksInternal")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tasks returned", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Invalid list filter")
    })
    MaintenanceWindowTaskListResponse list(@QueryParam("accountId") @NotEmpty String accountId,
            @Valid @BeanParam MaintenanceWindowTaskListParams params);

    @GET
    @Path("{taskId}")
    @Operation(summary = MaintenanceWindowTaskOpDescription.GET, description = MaintenanceWindowTaskOpDescription.NOTES,
            operationId = "getMaintenanceWindowTaskInternal")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task returned", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    MaintenanceWindowTaskResponse get(
            @QueryParam("accountId") @NotEmpty String accountId,
            @Parameter(description = MaintenanceWindowTaskOpDescription.TASK_ID, required = true)
            @PathParam("taskId") Long taskId);

    @POST
    @Operation(summary = MaintenanceWindowTaskOpDescription.CREATE, description = MaintenanceWindowTaskOpDescription.NOTES,
            operationId = "registerMaintenanceWindowTaskInternal")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Task created",
                    headers = @Header(name = "Location", description = MaintenanceWindowTaskOpDescription.LOCATION,
                            schema = @Schema(type = "string")),
                    useReturnTypeSchema = true),
            @ApiResponse(responseCode = "200", description = "Existing ACTIVE task returned (idempotent registration)",
                    useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "ACTIVE task exists with a different payload")
    })
    Response register(@QueryParam("accountId") @NotEmpty String accountId, @Valid MaintenanceWindowTaskRequest request);

    @PATCH
    @Path("{taskId}")
    @Operation(summary = MaintenanceWindowTaskOpDescription.UPDATE, description = MaintenanceWindowTaskOpDescription.UPDATE_NOTES,
            operationId = "updateMaintenanceWindowTaskInternal")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task updated", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Task not found"),
            @ApiResponse(responseCode = "409", description = "Version mismatch or concurrent modification")
    })
    MaintenanceWindowTaskResponse update(
            @QueryParam("accountId") @NotEmpty String accountId,
            @Parameter(description = MaintenanceWindowTaskOpDescription.TASK_ID, required = true)
            @PathParam("taskId") Long taskId,
            @Valid UpdateMaintenanceWindowTaskRequest request);

    @DELETE
    @Path("{taskId}")
    @Operation(summary = MaintenanceWindowTaskOpDescription.DELETE, description = MaintenanceWindowTaskOpDescription.DELETE_NOTES,
            operationId = "deleteMaintenanceWindowTaskInternal")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Task deleted"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    void delete(
            @QueryParam("accountId") @NotEmpty String accountId,
            @Parameter(description = MaintenanceWindowTaskOpDescription.TASK_ID, required = true)
            @PathParam("taskId") Long taskId);
}
