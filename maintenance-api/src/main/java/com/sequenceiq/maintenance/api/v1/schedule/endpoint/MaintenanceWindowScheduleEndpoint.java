package com.sequenceiq.maintenance.api.v1.schedule.endpoint;

import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.util.OneOfEnum;
import com.sequenceiq.maintenance.api.doc.MaintenanceWindowScheduleOpDescription;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleListParams;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowSkipRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.UpdateMaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowScheduleListResponse;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowScheduleResponse;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowSkipResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/v1/maintenance/schedules")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = MaintenanceWindowScheduleOpDescription.TAG, description = MaintenanceWindowScheduleOpDescription.TAG_DESCRIPTION)
public interface MaintenanceWindowScheduleEndpoint {

    @GET
    @Operation(summary = MaintenanceWindowScheduleOpDescription.LIST, description = MaintenanceWindowScheduleOpDescription.NOTES,
            operationId = "listMaintenanceWindowSchedulesV1")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Schedules returned", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Invalid scope filter (scopeType and scopeId must both be set or both omitted)")
    })
    MaintenanceWindowScheduleListResponse list(@Valid @BeanParam MaintenanceWindowScheduleListParams params);

    @GET
    @Path("scope/{scopeType}/scopeId/{scopeId}")
    @Operation(summary = MaintenanceWindowScheduleOpDescription.GET, description = MaintenanceWindowScheduleOpDescription.NOTES,
            operationId = "getMaintenanceWindowScheduleV1")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Schedule returned", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "Schedule not found")
    })
    MaintenanceWindowScheduleResponse get(
            @OneOfEnum(enumClass = MaintenanceScopeType.class, message = "Value must be one of the followings %s", fieldName = "scopeType")
            @Parameter(description = MaintenanceWindowScheduleOpDescription.SCOPE_TYPE, required = true)
            @PathParam("scopeType") String scopeType,
            @Parameter(description = MaintenanceWindowScheduleOpDescription.SCOPE_ID, required = true)
            @PathParam("scopeId") String scopeId);

    @POST
    @Operation(summary = MaintenanceWindowScheduleOpDescription.CREATE, description = MaintenanceWindowScheduleOpDescription.NOTES,
            operationId = "createMaintenanceWindowScheduleV1")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Schedule created",
                    headers = @Header(name = "Location", description = MaintenanceWindowScheduleOpDescription.LOCATION,
                            schema = @Schema(type = "string")),
                    useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "Schedule already exists for the scope")
    })
    Response create(@Valid MaintenanceWindowScheduleRequest request);

    @PATCH
    @Path("scope/{scopeType}/scopeId/{scopeId}")
    @Operation(summary = MaintenanceWindowScheduleOpDescription.UPDATE, description = MaintenanceWindowScheduleOpDescription.UPDATE_NOTES,
            operationId = "updateMaintenanceWindowScheduleV1")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Schedule updated", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Schedule not found")
    })
    MaintenanceWindowScheduleResponse update(
            @OneOfEnum(enumClass = MaintenanceScopeType.class, message = "Value must be one of the followings %s", fieldName = "scopeType")
            @Parameter(description = MaintenanceWindowScheduleOpDescription.SCOPE_TYPE, required = true)
            @PathParam("scopeType") String scopeType,
            @Parameter(description = MaintenanceWindowScheduleOpDescription.SCOPE_ID, required = true)
            @PathParam("scopeId") String scopeId,
            @Valid UpdateMaintenanceWindowScheduleRequest request);

    @DELETE
    @Path("scope/{scopeType}/scopeId/{scopeId}")
    @Operation(summary = MaintenanceWindowScheduleOpDescription.DELETE, description = MaintenanceWindowScheduleOpDescription.NOTES,
            operationId = "deleteMaintenanceWindowScheduleV1")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Schedule deleted"),
            @ApiResponse(responseCode = "404", description = "Schedule not found")
    })
    void delete(
            @OneOfEnum(enumClass = MaintenanceScopeType.class, message = "Value must be one of the followings %s", fieldName = "scopeType")
            @Parameter(description = MaintenanceWindowScheduleOpDescription.SCOPE_TYPE, required = true)
            @PathParam("scopeType") String scopeType,
            @Parameter(description = MaintenanceWindowScheduleOpDescription.SCOPE_ID, required = true)
            @PathParam("scopeId") String scopeId);

    @POST
    @Path("scope/{scopeType}/scopeId/{scopeId}/skip-next")
    @Operation(summary = MaintenanceWindowScheduleOpDescription.SKIP, description = MaintenanceWindowScheduleOpDescription.SKIP_NOTES,
            operationId = "skipNextMaintenanceWindowV1")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Skip recorded", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "No upcoming occurrence or invalid request"),
            @ApiResponse(responseCode = "404", description = "Schedule not found"),
            @ApiResponse(responseCode = "409", description = "Occurrence already skipped or maintenance window already in progress")
    })
    MaintenanceWindowSkipResponse skipNextWindow(
            @OneOfEnum(enumClass = MaintenanceScopeType.class, message = "Value must be one of the followings %s", fieldName = "scopeType")
            @Parameter(description = MaintenanceWindowScheduleOpDescription.SCOPE_TYPE, required = true)
            @PathParam("scopeType") String scopeType,
            @Parameter(description = MaintenanceWindowScheduleOpDescription.SCOPE_ID, required = true)
            @PathParam("scopeId") String scopeId,
            @Valid MaintenanceWindowSkipRequest request);

    @DELETE
    @Path("scope/{scopeType}/scopeId/{scopeId}/skip-next")
    @Operation(summary = MaintenanceWindowScheduleOpDescription.CANCEL_SKIP, description = MaintenanceWindowScheduleOpDescription.CANCEL_SKIP_NOTES,
            operationId = "cancelSkipNextMaintenanceWindowV1")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Skip cancelled", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "Schedule or skip not found")
    })
    MaintenanceWindowSkipResponse cancelSkipNextWindow(
            @OneOfEnum(enumClass = MaintenanceScopeType.class, message = "Value must be one of the followings %s", fieldName = "scopeType")
            @Parameter(description = MaintenanceWindowScheduleOpDescription.SCOPE_TYPE, required = true)
            @PathParam("scopeType") String scopeType,
            @Parameter(description = MaintenanceWindowScheduleOpDescription.SCOPE_ID, required = true)
            @PathParam("scopeId") String scopeId);
}
