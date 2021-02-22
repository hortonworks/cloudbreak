package com.sequenceiq.freeipa.api.v1.progress;

import static com.sequenceiq.freeipa.api.v1.progress.docs.ProgressOperationDescriptions.GET_LAST_FLOW_PROGRESS;
import static com.sequenceiq.freeipa.api.v1.progress.docs.ProgressOperationDescriptions.LIST_FLOW_PROGRESS;
import static com.sequenceiq.freeipa.api.v1.progress.docs.ProgressOperationDescriptions.NOTES;

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

@Path("/v1/progress")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/progress", description = "Get flow step progression", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface ProgressV1Endpoint {

    @GET
    @Path("/resource/crn/{resourceCrn}/last")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = GET_LAST_FLOW_PROGRESS, produces = "application/json", notes = NOTES,
            nickname = "getFreeIpaLastFlowLogProgressByResourceCrn")
    FlowProgressResponse getLastFlowLogProgressByResourceCrn(@PathParam("resourceCrn") String resourceCrn);

    @GET
    @Path("/resource/crn/{resourceCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LIST_FLOW_PROGRESS, produces = "application/json", notes = NOTES,
            nickname = "getFreeIpaFlowLogsProgressByResourceCrn")
    List<FlowProgressResponse> getFlowLogsProgressByResourceCrn(@PathParam("resourceCrn") String resourceCrn);
}
