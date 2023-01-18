package com.sequenceiq.sdx.api.endpoint;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.validation.annotation.Validated;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.sdx.api.model.SdxRecoverableResponse;
import com.sequenceiq.sdx.api.model.SdxRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRecoveryResponse;

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
}
