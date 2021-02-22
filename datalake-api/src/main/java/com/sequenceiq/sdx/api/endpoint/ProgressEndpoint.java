package com.sequenceiq.sdx.api.endpoint;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.sdx.api.model.SdxProgressListResponse;
import com.sequenceiq.sdx.api.model.SdxProgressResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/progress")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/progress", description = "Get progression details of (flow) operations on SDX cluster",
        protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface ProgressEndpoint {

    @GET
    @Path("/resource/crn/{resourceCrn}/last")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get last flow operation progress details for resource by resource crn", produces = "application/json",
            notes = "Flow operations progress", nickname = "getSdxLastFlowLogProgressByResourceCrn")
    SdxProgressResponse getLastFlowLogProgressByResourceCrn(@PathParam("resourceCrn") String resourceCrn);

    @GET
    @Path("/resource/crn/{resourceCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List recent flow operations progress details for resource by resource crn", produces = "application/json",
            notes = "Flow operations progress", nickname = "getSdxFlowLogsProgressByResourceCrn")
    SdxProgressListResponse getFlowLogsProgressByResourceCrn(@PathParam("resourceCrn") String resourceCrn);
}
