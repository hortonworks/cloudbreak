package com.sequenceiq.sdx.api.endpoint;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.springframework.validation.annotation.Validated;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.model.SdxSecretRotationRequest;
import com.sequenceiq.sdx.api.model.SdxSecretTypeResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@Path("/sdx")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/sdx")
public interface SdxRotationEndpoint {

    @PUT
    @Path("rotate_secret")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Rotate SDX secrets", operationId = "rotateSDXSecrets",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier rotateSecrets(@Valid @NotNull SdxSecretRotationRequest request);

    @GET
    @Path("list_secret_types")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List rotatable secret types for SDX", operationId = "listRotatableSdxSecretType",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<SdxSecretTypeResponse> listRotatableSdxSecretType(
            @ValidCrn(resource = CrnResourceDescriptor.VM_DATALAKE) @QueryParam("datalakeCrn") @NotEmpty String datalakeCrn);
}
