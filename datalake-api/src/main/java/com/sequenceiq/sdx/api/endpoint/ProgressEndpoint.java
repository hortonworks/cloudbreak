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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/progress")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/progress", description = "Get progression details of (flow) operations on SDX cluster")
public interface ProgressEndpoint {

    @GET
    @Path("/resource/crn/{resourceCrn}/last")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get last flow operation progress details for resource by resource crn", description = "Flow operations progress",
            operationId = "getSdxLastFlowLogProgressByResourceCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SdxProgressResponse getLastFlowLogProgressByResourceCrn(@PathParam("resourceCrn") String resourceCrn);

    @GET
    @Path("/resource/crn/{resourceCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List recent flow operations progress details for resource by resource crn", description = "Flow operations progress",
            operationId = "getSdxFlowLogsProgressByResourceCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SdxProgressListResponse getFlowLogsProgressByResourceCrn(@PathParam("resourceCrn") String resourceCrn);
}
