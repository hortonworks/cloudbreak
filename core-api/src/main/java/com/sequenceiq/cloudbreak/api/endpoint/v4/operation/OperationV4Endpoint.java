package com.sequenceiq.cloudbreak.api.endpoint.v4.operation;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.operation.docs.OperationOpDescriptions.GET_OPERATIONS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.operation.docs.OperationOpDescriptions.NOTES;

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

@Path("/v4/operation")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v4/operation", description = "Get operation progression")
public interface OperationV4Endpoint {

    @GET
    @Path("/resource/crn/{resourceCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = GET_OPERATIONS, description = NOTES,
            operationId = "getStackOperationProgressByResourceCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    OperationView getOperationProgressByResourceCrn(@PathParam("resourceCrn") String resourceCrn,
            @DefaultValue("false") @QueryParam("detailed") boolean detailed);
}
