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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/v4/progress")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v4/progress", description = "Get progression of flow executions")
public interface ProgressV4Endpoint {

    @GET
    @Path("/resource/crn/{resourceCrn}/last")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = GET_LAST_FLOW_PROGRESS, description = NOTES,
            operationId = "getLastFlowLogProgressByResourceCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowProgressResponse getLastFlowLogProgressByResourceCrn(@PathParam("resourceCrn") String resourceCrn);

    @GET
    @Path("/resource/crn/{resourceCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = LIST_FLOW_PROGRESS, description = NOTES,
            operationId = "getFlowLogsProgressByResourceCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<FlowProgressResponse> getFlowLogsProgressByResourceCrn(@PathParam("resourceCrn") String resourceCrn);
}
