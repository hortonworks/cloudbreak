package com.sequenceiq.cloudbreak.api.v1.maintenance.endpoint;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.v1.maintenance.model.MaintenanceTaskDispatchRequest;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/v1/internal/maintenance-tasks")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "internal-maintenance-tasks", description = "Internal maintenance window task execution for Cloudbreak")
public interface MaintenanceWindowTaskExecuteEndpoint {

    @POST
    @Path("execute")
    @Operation(summary = "Execute a maintenance window task dispatched to Cloudbreak",
            description = "Internal submitter callback. Routes by task_type to the registered handler (e.g. SECRET_ROTATION). "
                    + "Returns 202 with a FlowIdentifier when async work is accepted.",
            operationId = "executeMaintenanceWindowTaskInternal")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Async work accepted",
                    content = @Content(schema = @Schema(implementation = FlowIdentifier.class))),
            @ApiResponse(responseCode = "400", description = "Invalid dispatch payload"),
            @ApiResponse(responseCode = "409", description = "Conflicting work already running")
    })
    Response execute(@Valid @NotNull MaintenanceTaskDispatchRequest request);
}
