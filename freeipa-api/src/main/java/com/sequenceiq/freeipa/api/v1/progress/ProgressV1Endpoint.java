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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/v1/progress")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/progress", description = "Get flow step progression")
public interface ProgressV1Endpoint {

    @GET
    @Path("/resource/crn/{environmentCrn}/last")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = GET_LAST_FLOW_PROGRESS, description = NOTES,
            operationId = "getFreeIpaLastFlowLogProgressByEnvironmentCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowProgressResponse getLastFlowLogProgressByResourceCrn(@PathParam("environmentCrn") String environmentCrn);

    @GET
    @Path("/resource/crn/{environmentCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = LIST_FLOW_PROGRESS, description = NOTES,
            operationId = "getFreeIpaFlowLogsProgressByEnvironmentCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<FlowProgressResponse> getFlowLogsProgressByResourceCrn(@PathParam("environmentCrn") String environmentCrn);
}
