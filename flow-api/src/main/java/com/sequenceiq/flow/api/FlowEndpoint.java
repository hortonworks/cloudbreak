package com.sequenceiq.flow.api;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.springframework.validation.annotation.Validated;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowLogResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/flow")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/flow", description = "Operations on flow logs")
@Validated
public interface FlowEndpoint {

    @GET
    @Path("/logs/{flowId}/last")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get last flow log by flow id", description = "Flow log operations",
            operationId = "getLastFlowById",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowLogResponse getLastFlowById(@PathParam("flowId") String flowId);

    @GET
    @Path("/logs/{flowId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get flow logs by flow id", description = "Flow log operations",
            operationId = "getFlowLogsByFlowId",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<FlowLogResponse> getFlowLogsByFlowId(@PathParam("flowId") String flowId);

    @GET
    @Path("/logs/resource/name/{resourceName}/last")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get last flow log for resource by resource name", description = "Flow log operations",
            operationId = "getLastFlowByResourceName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowLogResponse getLastFlowByResourceName(@QueryParam ("accountId") String accountId, @PathParam("resourceName") String resourceName);

    @GET
    @Path("/logs/resource/crn/{resourceCrn}/last")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get last flow log for resource by resource CRN", description = "Flow log operations",
            operationId = "getLastFlowByResourceCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowLogResponse getLastFlowByResourceCrn(@PathParam("resourceCrn") String resourceCrn);

    @GET
    @Path("/logs/resource/name/{resourceName}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get flow logs for resource by resource name", description = "Flow log operations",
            operationId = "getFlowLogsByResourceName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<FlowLogResponse> getFlowLogsByResourceName(@QueryParam ("accountId") String accountId, @PathParam("resourceName") String resourceName);

    @GET
    @Path("/logs/resource/crn/{resourceCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get flow logs for resource by resource CRN", description = "Flow log operations",
            operationId = "getFlowLogsByResourceCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<FlowLogResponse> getFlowLogsByResourceCrn(@PathParam("resourceCrn") String resourceCrn);

    @GET
    @Path("/check/chainId/{chainId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Check if there is a running flow for chain id", description = "Flow log operations",
            operationId = "hasFlowRunningByChainId",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowCheckResponse hasFlowRunningByChainId(@PathParam("chainId") String chainId);

    @GET
    @Path("/check/flowId/{flowId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Check if there is a running flow for flow id", description = "Flow log operations",
            operationId = "hasFlowRunningByFlowId",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowCheckResponse hasFlowRunningByFlowId(@PathParam("flowId") String flowId);

    @GET
    @Path("/logs/flowIds")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get flow logs by a list of flow ids - Input size max 50", description = "Flow log operations",
            operationId = "getFlowLogsByFlowIds",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<FlowLogResponse> getFlowLogsByFlowIds(@NotNull @Size(max = 50, message = "Input size should not be over 50")
        @QueryParam("flowIds") List<String> flowIds,
        @Min(value = 1, message = "Page size should be greater than 0") @QueryParam("size") int size, @QueryParam("page") int page);

    @GET
    @Path("/check/chainIds")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets flow check responses for parent chains - Input size max 50",
            description = "Flow check log operations",
            operationId = "getFlowChainsStatusesByChainIds",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<FlowCheckResponse> getFlowChainsStatusesByChainIds(@NotNull @Size(max = 50, message = "Input size should not be over 50")
        @QueryParam("chainIds") List<String> chainIds,
        @Min(value = 1, message = "Page size should be greater than 0") @QueryParam("size") int size, @QueryParam("page") int page);

    @POST
    @Path("/resource/crn/{resourceCrn}/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Cancel execution of the given flow by flow id", description = "Flow log operations",
            operationId = "cancelFlowsByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<FlowLogResponse> cancelFlowsByCrn(@PathParam("resourceCrn") String resourceCrn);
}
