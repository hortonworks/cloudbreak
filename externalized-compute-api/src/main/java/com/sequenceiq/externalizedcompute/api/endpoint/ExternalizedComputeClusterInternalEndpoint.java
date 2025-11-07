package com.sequenceiq.externalizedcompute.api.endpoint;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.springframework.validation.annotation.Validated;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterCredentialValidationResponse;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterInternalRequest;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterRequest;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@Path("/internal/externalized_compute_cluster")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/internal/externalized_compute_cluster", description = "Externalized compute cluster internal operations")
public interface ExternalizedComputeClusterInternalEndpoint {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "create Externalized Compute cluster", description = "create Externalized Compute cluster",
            operationId = "createExternalizedComputeCluster",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier create(@Valid ExternalizedComputeClusterInternalRequest request,
            @InitiatorUserCrn @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @Path("reinitialize")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "reInitialize Externalized Compute cluster", description = "reInitialize Externalized Compute cluster",
            operationId = "reInitializeExternalizedComputeCluster",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier reInitialize(@Valid ExternalizedComputeClusterRequest request,
            @InitiatorUserCrn @QueryParam("initiatorUserCrn") String initiatorUserCrn, @QueryParam("force") boolean force);

    @DELETE
    @Path("{environmentCrn}/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "delete Externalized Compute cluster", description = "delete externalized cluster",
            operationId = "deleteExternalizedComputeCluster",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier delete(
            @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("environmentCrn") String environmentCrn,
            @InitiatorUserCrn @QueryParam("initiatorUserCrn") String initiatorUserCrn, @PathParam("name") @NotEmpty String name,
            @QueryParam("force") boolean force);

    @GET
    @Path("{environmentCrn}/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "get Externalized Compute cluster", description = "get externalized cluster",
            operationId = "getExternalizedComputeCluster",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ExternalizedComputeClusterResponse describe(
            @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("environmentCrn") String environmentCrn,
            @NotEmpty @PathParam("name") String name);

    @GET
    @Path("{environmentCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "list Externalized Compute cluster for env", description = "list externalized cluster for env",
            operationId = "listExternalizedComputeClusterForEnv",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<ExternalizedComputeClusterResponse> list(
            @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("environmentCrn") String environmentCrn);

    @Deprecated
    @GET
    @Path("validate_credential/{credentialName}")
    @Produces(MediaType.APPLICATION_JSON)
    ExternalizedComputeClusterCredentialValidationResponse validateCredential(@NotEmpty @PathParam("credentialName") String credentialName,
            @NotEmpty @QueryParam("region") String region, @InitiatorUserCrn @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @GET
    @Path("{environmentCrn}/validate_credential/{credentialName}")
    @Produces(MediaType.APPLICATION_JSON)
    ExternalizedComputeClusterCredentialValidationResponse validateCredential(
            @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("environmentCrn") String environmentCrn,
            @NotEmpty @PathParam("credentialName") String credentialName,
            @NotEmpty @QueryParam("region") String region,
            @InitiatorUserCrn @QueryParam("initiatorUserCrn") String initiatorUserCrn);
}
