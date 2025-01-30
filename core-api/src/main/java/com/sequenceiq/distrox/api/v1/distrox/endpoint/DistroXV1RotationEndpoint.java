package com.sequenceiq.distrox.api.v1.distrox.endpoint;

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

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXSecretRotationRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXSecretTypeResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v1/distrox")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/distrox")
public interface DistroXV1RotationEndpoint {

    @PUT
    @Path("rotate_secret")
    @Operation(summary = "Rotate DistroX secrets", operationId = "rotateDistroXSecrets",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier rotateSecrets(@Valid @NotNull DistroXSecretRotationRequest request);

    @GET
    @Path("list_secret_types")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List rotatable secret types for distroX", operationId = "listRotatableDistroXSecretType",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<DistroXSecretTypeResponse> listRotatableDistroXSecretType(
            @ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @QueryParam("datahubCrn") @NotEmpty String datahubCrn);
}
