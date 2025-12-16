package com.sequenceiq.sdx.api.endpoint;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.springframework.validation.annotation.Validated;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.sdx.api.model.SdxRecoverableResponse;
import com.sequenceiq.sdx.api.model.SdxRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRecoveryResponse;
import com.sequenceiq.sdx.api.model.SdxResizeOperationResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@Path("/sdx")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/sdx")
public interface SdxRecoveryEndpoint {

    @POST
    @Path("{name}/recover")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "recovers the datalake upgrade", operationId = "recoverDatalakeCluster",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SdxRecoveryResponse recoverClusterByName(@PathParam("name") String name, @Valid SdxRecoveryRequest recoverSdxClusterRequest);

    @POST
    @Path("/crn/{crn}/recover")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "recovers the datalake upgrade", operationId = "recoverDatalakeClusterByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SdxRecoveryResponse recoverClusterByCrn(@PathParam("crn") String crn, @Valid SdxRecoveryRequest recoverSdxClusterRequest);

    @GET
    @Path("{name}/recoverable")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "validates if the data lake is recoverable or not", operationId = "getClusterRecoverableByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SdxRecoverableResponse getClusterRecoverableByName(@PathParam("name") String name);

    @GET
    @Path("/crn/{crn}/recoverable")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "validates if the data lake is recoverable or not", operationId = "getClusterRecoverableByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SdxRecoverableResponse getClusterRecoverableByCrn(@PathParam("crn") String crn);

    @GET
    @Path("{environmentCrn}/resize/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Re-size SDX cluster", operationId = "resizeSdx",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SdxResizeOperationResponse getResizeStatus(@PathParam("environmentCrn") @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) String environmentCrn);
}
