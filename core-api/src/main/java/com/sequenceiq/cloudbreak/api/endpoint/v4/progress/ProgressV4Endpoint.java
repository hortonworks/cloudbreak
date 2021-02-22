package com.sequenceiq.cloudbreak.api.endpoint.v4.progress;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.progress.docs.ProgressOperationDescriptions.GET_LAST_FLOW_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.progress.docs.ProgressOperationDescriptions.LIST_FLOW_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.progress.docs.ProgressOperationDescriptions.NOTES;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.flow.api.model.FlowProgressResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v4/progress")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/progress", description = "Get progression of flow executions", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface ProgressV4Endpoint {

    @GET
    @Path("/resource/crn/{resourceCrn}/last")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = GET_LAST_FLOW_PROGRESS, produces = "application/json", notes = NOTES,
            nickname = "getLastFlowLogProgressByResourceName")
    FlowProgressResponse getLastFlowLogProgressByResourceCrn(@PathParam("resourceCrn") String resourceCrn);

    @GET
    @Path("/resource/crn/{resourceCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LIST_FLOW_PROGRESS, produces = "application/json", notes = NOTES,
            nickname = "getFlowLogsProgressByResourceName")
    List<FlowProgressResponse> getFlowLogsProgressByResourceCrn(@PathParam("resourceCrn") String resourceCrn);
}
