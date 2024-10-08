package com.sequenceiq.redbeams.api.endpoint.v4.operation;

import static com.sequenceiq.redbeams.doc.OperationDescriptions.OperationOpDescriptions.GET_OPERATIONS;
import static com.sequenceiq.redbeams.doc.OperationDescriptions.OperationOpDescriptions.GET_OPERATION_STATUS;
import static com.sequenceiq.redbeams.doc.OperationDescriptions.OperationOpDescriptions.NOTES;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.flow.api.model.operation.OperationStatusResponse;
import com.sequenceiq.flow.api.model.operation.OperationView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/v4/operation")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v4/operation", description = "Get flow step progression")
public interface OperationV4Endpoint {

    @GET
    @Path("/resource/crn/{resourceCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = GET_OPERATIONS, description = NOTES,
            operationId = "getRedbeamsOperationProgressByResourceCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    OperationView getRedbeamsOperationProgressByResourceCrn(@PathParam("resourceCrn") String resourceCrn,
            @DefaultValue("false") @QueryParam("detailed") boolean detailed);

    @GET
    @Path("/resource/crn/{resourceCrn}/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = GET_OPERATION_STATUS, description = NOTES,
            operationId = "getOperationStatus",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    OperationStatusResponse getOperationStatus(@ValidCrn(resource = CrnResourceDescriptor.DATABASE_SERVER) @PathParam("resourceCrn") String resourceCrn,
            @QueryParam("operationId") String operationId);
}
