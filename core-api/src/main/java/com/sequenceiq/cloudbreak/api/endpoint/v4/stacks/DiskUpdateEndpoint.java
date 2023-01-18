package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskModificationRequest;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/v4/disks")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "v4/disks")
public interface DiskUpdateEndpoint {

    @GET
    @Path("/{platform}/diskTypeChangeSupported")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Checks if Disk Type Change is supported", operationId = "diskTypeChangeSupported",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Boolean isDiskTypeChangeSupported(@NotEmpty @PathParam("platform") String platform);

    @PUT
    @Path("/updateDisks")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Updates Disk Type and / or size", operationId = "updateDiskTypeAndSize",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier updateDiskTypeAndSize(@Valid DiskModificationRequest diskModificationRequest) throws Exception;

    @PUT
    @Path("/{stackId}/stopCluster")
    @Operation(summary = "Stops CM services running on Datalake", operationId = "stopCMServices",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void stopCMServices(@PathParam("stackId") long stackId) throws Exception;

    @PUT
    @Path("/{stackId}/startCluster")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Start CM services running on Datalake", operationId = "startCMServices",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void startCMServices(@PathParam("stackId") long stackId) throws Exception;
}
