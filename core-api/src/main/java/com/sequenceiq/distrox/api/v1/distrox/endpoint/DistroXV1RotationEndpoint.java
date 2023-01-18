package com.sequenceiq.distrox.api.v1.distrox.endpoint;

import static com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor.DATALAKE;
import static com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor.ENVIRONMENT;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.rotation.annotation.ValidMultiSecretType;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXChildResourceMarkingRequest;
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
    @Path("multi_secret/check_children")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Check ongoing child DistroX multi secret rotations by parent", operationId = "checkDistroXMultiSecretsByParent",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    boolean checkOngoingChildrenMultiSecretRotationsByParent(@ValidCrn(resource = { ENVIRONMENT, DATALAKE }) @QueryParam("parentCrn") String parentCrn,
            @ValidMultiSecretType @QueryParam("secret") String multiSecret,
            @InitiatorUserCrn @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @PUT
    @Path("multi_secret/mark_children")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Mark child resources for DistroX multi secret rotations by parent", operationId = "markResourcesDistroXMultiSecretsByParent",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void markMultiClusterChildrenResourcesByParent(@Valid DistroXChildResourceMarkingRequest request,
            @InitiatorUserCrn @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @GET
    @Path("list_secret_types")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List rotatable secret types for distroX", operationId = "listRotatableDistroXSecretType",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<DistroXSecretTypeResponse> listRotatableDistroXSecretType(
            @ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @QueryParam("datahubCrn") @NotEmpty String datahubCrn);
}
