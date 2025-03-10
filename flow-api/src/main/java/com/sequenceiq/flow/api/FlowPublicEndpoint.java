package com.sequenceiq.flow.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.flow.api.model.FlowCheckResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/flow-public")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/flow-public", description = "Operations on flow logs")
public interface FlowPublicEndpoint {

    @GET
    @Path("/check/chainId/{chainId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Check if there is a running flow for chain id and resourceCrn", description = "Flow log operations",
            operationId = "hasFlowRunningByChainIdAndResourceCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowCheckResponse hasFlowRunningByChainId(@PathParam("chainId") String chainId, @QueryParam("resourceCrn") String resourceCrn);

    @GET
    @Path("/check/flowId/{flowId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Check if there is a running flow for flow id and resourceId", description = "Flow log operations",
            operationId = "hasFlowRunningByFlowIdAndResourceCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowCheckResponse hasFlowRunningByFlowId(@PathParam("flowId") String flowId, @QueryParam("resourceCrn") String resourceCrn);
}
