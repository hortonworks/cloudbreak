package com.sequenceiq.externalizedcompute.api.endpoint;

import java.util.List;

import jakarta.validation.Valid;
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

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterRequest;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@Path("/externalized_compute_cluster")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/externalized_compute_cluster", description = "Externalized cluster operations")
public interface ExternalizedComputeClusterEndpoint {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "create Externalized Compute cluster", description = "create Externalized Compute cluster",
            operationId = "createExternalizedComputeCluster",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier create(@Valid ExternalizedComputeClusterRequest externalizedComputeClusterRequest);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "delete Externalized Compute cluster", description = "delete externalized cluster",
            operationId = "deleteExternalizedComputeCluster",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier delete(@PathParam("name") String name);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "get Externalized Compute cluster", description = "get externalized cluster",
            operationId = "getExternalizedComputeCluster",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ExternalizedComputeClusterResponse describe(@PathParam("name") String name);

    @GET
    @Path("list")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "list Externalized Compute cluster for env", description = "list externalized cluster for env",
            operationId = "listExternalizedComputeClusterForEnv",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<ExternalizedComputeClusterResponse> list(@QueryParam("envCrn") String envCrn);

}