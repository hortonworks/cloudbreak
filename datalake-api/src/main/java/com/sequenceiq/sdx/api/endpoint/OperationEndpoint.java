package com.sequenceiq.sdx.api.endpoint;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.flow.api.model.operation.OperationView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/operation")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/operation", description = "Get progression details of (flow) operations on SDX cluster")
public interface OperationEndpoint {

    @GET
    @Path("/resource/crn/{resourceCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get provision flow operation progress details for resource by resource crn", description = "Flow operations progress",
            operationId = "getOperationProgressByResourceCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    OperationView getOperationProgressByResourceCrn(@PathParam("resourceCrn") String resourceCrn,
            @DefaultValue("false") @QueryParam("detailed") boolean detailed);
}
