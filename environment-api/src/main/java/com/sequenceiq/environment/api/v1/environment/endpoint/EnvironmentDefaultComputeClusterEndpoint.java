package com.sequenceiq.environment.api.v1.environment.endpoint;

import static com.sequenceiq.environment.api.doc.environment.EnvironmentDescription.ENVIRONMENT_NOTES;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.environment.api.doc.environment.EnvironmentOpDescription;
import com.sequenceiq.environment.api.v1.environment.model.request.ExternalizedComputeCreateRequest;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/v1/env/crn/{crn}/default_compute_cluster")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/env/crn/{crn}/default_compute_cluster")
public interface EnvironmentDefaultComputeClusterEndpoint {

    @PUT
    @Path("/create")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.CREATE_DEFAULT_EXTERNALIZED_COMPUTE_CLUSTER, description = ENVIRONMENT_NOTES,
            operationId = "createDefaultExternalizedComputeCluster",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier createDefaultExternalizedComputeCluster(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("crn") String crn,
            @NotNull ExternalizedComputeCreateRequest request, @QueryParam("force") boolean force);

    /**
     * It will be removed because I realized it is unnecessary and only complicates things. The create endpoint can be used instead.
     *
     * @deprecated use {@link #createDefaultExternalizedComputeCluster(String, ExternalizedComputeCreateRequest, boolean)} instead.
     */
    @Deprecated
    @PUT
    @Path("/reinitialize")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.REINITIALIZE_DEFAULT_EXTERNALIZED_COMPUTE_CLUSTER, description = ENVIRONMENT_NOTES,
            operationId = "reinitializeDefaultExternalizedComputeCluster",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier reinitializeDefaultExternalizedComputeCluster(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("crn") String crn,
            @NotNull ExternalizedComputeCreateRequest request, @QueryParam("force") boolean force);

}
