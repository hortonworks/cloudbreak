package com.sequenceiq.flow.api;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.flow.api.model.FlowLogResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/flow")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/flow", description = "Operations on flow logs", protocols = "http,https",
        consumes = MediaType.APPLICATION_JSON)
public interface FlowEndpoint {

    @GET
    @Path("/logs/{flowId}/last")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get last flow log by flow id", produces = "application/json", notes = "Flow log operations",
            nickname = "getLastFlowById")
    FlowLogResponse getLastFlowById(@PathParam("flowId") String flowId);

    @GET
    @Path("/logs/{flowId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get flow logs by flow id", produces = "application/json", notes = "Flow log operations",
            nickname = "getFlowLogsByFlowId")
    List<FlowLogResponse> getFlowLogsByFlowId(@PathParam("flowId") String flowId);

    @GET
    @Path("/logs/resource/name/{resourceName}/last")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get last flow log for resource by resource name", produces = "application/json", notes = "Flow log operations",
            nickname = "getLastFlowByResourceName")
    FlowLogResponse getLastFlowByResourceName(@QueryParam ("accountId") @AccountId String accountId, @PathParam("resourceName") String resourceName);

    @GET
    @Path("/logs/resource/crn/{resourceCrn}/last")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get last flow log for resource by resource CRN", produces = "application/json", notes = "Flow log operations",
            nickname = "getLastFlowByResourceCrn")
    FlowLogResponse getLastFlowByResourceCrn(@PathParam("resourceCrn") String resourceCrn);

    @GET
    @Path("/logs/resource/name/{resourceName}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get flow logs for resource by resource name", produces = "application/json", notes = "Flow log operations",
            nickname = "getFlowLogsByResourceName")
    List<FlowLogResponse> getFlowLogsByResourceName(@QueryParam ("accountId") @AccountId String accountId, @PathParam("resourceName") String resourceName);

    @GET
    @Path("/logs/resource/crn/{resourceCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get flow logs for resource by resource CRN", produces = "application/json", notes = "Flow log operations",
            nickname = "getFlowLogsByResourceCrn")
    List<FlowLogResponse> getFlowLogsByResourceCrn(@PathParam("resourceCrn") String resourceCrn);

    @GET
    @Path("/check/chainId/{chainId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Check if there is a running flow for chain id", produces = "application/json", notes = "Flow log operations",
            nickname = "hasFlowRunningByChainId")
    FlowCheckResponse hasFlowRunningByChainId(@PathParam("chainId") String chainId);

    @GET
    @Path("/check/flowId/{flowId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Check if there is a running flow for flow id", produces = "application/json", notes = "Flow log operations",
            nickname = "hasFlowRunningByFlowId")
    FlowCheckResponse hasFlowRunningByFlowId(@PathParam("flowId") String flowId);
}
