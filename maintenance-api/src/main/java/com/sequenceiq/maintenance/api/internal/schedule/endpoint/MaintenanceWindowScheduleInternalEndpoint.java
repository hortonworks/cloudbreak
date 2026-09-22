package com.sequenceiq.maintenance.api.internal.schedule.endpoint;

import jakarta.validation.constraints.NotEmpty;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.util.OneOfEnum;
import com.sequenceiq.maintenance.api.doc.MaintenanceWindowScheduleInternalOpDescription;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowScheduleResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/internal/maintenance-schedules")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = MaintenanceWindowScheduleInternalOpDescription.TAG, description = MaintenanceWindowScheduleInternalOpDescription.TAG_DESCRIPTION)
public interface MaintenanceWindowScheduleInternalEndpoint {

    @GET
    @Path("scope/{scopeType}/scopeId/{scopeId}")
    @Operation(summary = MaintenanceWindowScheduleInternalOpDescription.GET,
            description = MaintenanceWindowScheduleInternalOpDescription.GET_NOTES,
            operationId = "getMaintenanceWindowScheduleInternal")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Schedule returned", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "Schedule not found")
    })
    MaintenanceWindowScheduleResponse get(
            @QueryParam("accountId") @NotEmpty String accountId,
            @OneOfEnum(enumClass = MaintenanceScopeType.class, message = "Value must be one of the followings %s", fieldName = "scopeType")
            @Parameter(description = MaintenanceWindowScheduleInternalOpDescription.SCOPE_TYPE, required = true)
            @PathParam("scopeType") String scopeType,
            @Parameter(description = MaintenanceWindowScheduleInternalOpDescription.SCOPE_ID, required = true)
            @PathParam("scopeId") String scopeId);
}
