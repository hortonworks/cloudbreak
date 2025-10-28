package com.sequenceiq.freeipa.api.v1.freeipa.stack;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions.ROTATE_SECRETS_BY_CRN;

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
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaNotes;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate.FreeIpaSecretRotationRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate.FreeipaSecretTypeResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v1/freeipa")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/freeipa")
public interface FreeIpaRotationV1Endpoint {

    @PUT
    @Path("rotate_secret")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ROTATE_SECRETS_BY_CRN, description = FreeIpaNotes.FREEIPA_NOTES, operationId = "rotateSecretsByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier rotateSecretsByCrn(
            @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @QueryParam("environment") @NotEmpty String environmentCrn,
            @Valid @NotNull FreeIpaSecretRotationRequest request);

    @GET
    @Path("list_secret_types")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List rotatable secret types for FreeIPA", operationId = "listRotatableFreeipaSecretType",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<FreeipaSecretTypeResponse> listRotatableFreeipaSecretType(
            @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @QueryParam("environmentCrn") @NotEmpty String environmentCrn);

    @PUT
    @Path("sync_outdated_secrets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Sync outdated vault secrets for FreeIPA", operationId = "syncOutdatedSecrets",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void syncOutdatedSecrets(
            @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @QueryParam("environmentCrn") @NotEmpty String environmentCrn);
}
