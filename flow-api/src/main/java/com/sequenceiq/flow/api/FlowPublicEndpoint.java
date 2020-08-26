package com.sequenceiq.flow.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.flow.api.model.FlowCheckResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/flow-public")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/flow-public", description = "Operations on flow logs", protocols = "http,https",
        consumes = MediaType.APPLICATION_JSON)
public interface FlowPublicEndpoint {

    @GET
    @Path("/check/chainId/{chainId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Check if there is a running flow for chain id and resourceCrn", produces = "application/json", notes = "Flow log operations",
            nickname = "hasFlowRunningByChainIdAndResourceCrn")
    FlowCheckResponse hasFlowRunningByChainId(@PathParam("chainId") String chainId, @QueryParam("resourceCrn") String resourceCrn);

    @GET
    @Path("/check/flowId/{flowId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Check if there is a running flow for flow id and resourceId", produces = "application/json", notes = "Flow log operations",
            nickname = "hasFlowRunningByFlowIdAndResourceCrn")
    FlowCheckResponse hasFlowRunningByFlowId(@PathParam("flowId") String flowId, @QueryParam("resourceCrn") String resourceCrn);

}
