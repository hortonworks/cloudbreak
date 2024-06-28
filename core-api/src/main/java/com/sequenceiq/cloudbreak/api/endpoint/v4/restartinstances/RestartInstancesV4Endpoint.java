package com.sequenceiq.cloudbreak.api.endpoint.v4.restartinstances;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/v4/restart_instances/stack")
@Consumes(APPLICATION_JSON)
@Tag(name = "/v4/restart_instances/stack", description = ControllerDescription.RESTART_INSTANCES_DESCRIPTION)
public interface RestartInstancesV4Endpoint {

    @POST
    @Path("/crn/{crn}/instances")
    @Produces(APPLICATION_JSON)
    @Operation(summary = OperationDescriptions.StackOpDescription.RESTART_MULTIPLE_INSTANCES,
            description = Notes.STACK_NOTES, operationId = "restartInstancesForClusterCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void restartInstancesForClusterCrn(@PathParam("crn") String clusterCrn,
            @QueryParam("instanceId") @NotEmpty List<String> instanceIds);

    @POST
    @Path("/name/{name}/instances")
    @Produces(APPLICATION_JSON)
    @Operation(summary = OperationDescriptions.StackOpDescription.RESTART_MULTIPLE_INSTANCES,
            description = Notes.STACK_NOTES, operationId = "restartInstancesForClusterName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void restartInstancesForClusterName(@PathParam("name") String clusterName,
            @QueryParam("instanceId") @NotEmpty List<String> instanceIds);
}