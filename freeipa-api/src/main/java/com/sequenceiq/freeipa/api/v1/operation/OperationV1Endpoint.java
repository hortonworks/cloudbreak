package com.sequenceiq.freeipa.api.v1.operation;

import jakarta.validation.constraints.NotNull;
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
import com.sequenceiq.freeipa.api.v1.operation.doc.OperationDescriptions;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v1/operation")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/operation", description = "Manage operations in FreeIPA")
public interface OperationV1Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = OperationDescriptions.OPERATION_STATUS, description = OperationDescriptions.NOTES,
            operationId = "getOperationStatusV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    OperationStatus getOperationStatus(@NotNull @QueryParam("operationId") String operationId, @QueryParam("accountId") String accountId);

    @GET
    @Path("/resource/crn/{environmentCrn}/latest")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = OperationDescriptions.LATEST_OPERATION_STATUS_BY_ENV_AND_TYPE, description = OperationDescriptions.NOTES,
            operationId = "getLatestOperationStatusByEnvironmentAndTypeV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    OperationStatus getLatestOperationStatusByEnvironmentAndType(@PathParam("environmentCrn") String environmentCrn,
            @QueryParam("operationType") String operationType);

    @GET
    @Path("/resource/crn/{environmentCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = OperationDescriptions.GET_OPERATION, description = OperationDescriptions.NOTES,
            operationId = "getOperationProgressByEnvironmentCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    OperationView getOperationProgressByEnvironmentCrn(@PathParam("environmentCrn") String environmentCrn,
            @DefaultValue("false") @QueryParam("detailed") boolean detailed);

    @GET
    @Path("/resource/crn/{environmentCrn}/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = OperationDescriptions.GET_FLOW_OPERATION_STATUS, description = OperationDescriptions.NOTES,
            operationId = "getFlowOperationStatus",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    OperationStatusResponse getFlowOperationStatus(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("environmentCrn") String environmentCrn,
            @QueryParam("flowOperationId") String flowOperationId);
}
