package com.sequenceiq.sdx.api.endpoint;

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

    @GET
    @Path("/resource/crn/{resourceCrn}/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get flow operation status for resource by resource crn and operation id", description = "Flow operations status",
            operationId = "getOperationStatus",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    OperationStatusResponse getOperationStatus(@ValidCrn(resource = CrnResourceDescriptor.VM_DATALAKE) @PathParam("resourceCrn") String resourceCrn,
            @QueryParam("operationId") String operationId);
}
