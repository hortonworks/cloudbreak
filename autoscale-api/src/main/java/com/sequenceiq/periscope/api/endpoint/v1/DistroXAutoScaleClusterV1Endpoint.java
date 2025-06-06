package com.sequenceiq.periscope.api.endpoint.v1;

import static com.sequenceiq.periscope.doc.ApiDescription.CLUSTERS_DESCRIPTION;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.periscope.api.model.AutoscaleClusterState;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterRequest;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterResponse;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterServerCertUpdateRequest;
import com.sequenceiq.periscope.doc.ApiDescription;
import com.sequenceiq.periscope.doc.ApiDescription.ClusterOpDescription;
import com.sequenceiq.periscope.doc.ApiDescription.DistroXClusterNotes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/v1/distrox")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/distrox", description = CLUSTERS_DESCRIPTION)
public interface DistroXAutoScaleClusterV1Endpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ClusterOpDescription.CLUSTER_GET_ALL, description = DistroXClusterNotes.NOTES, operationId = "getClusters",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<DistroXAutoscaleClusterResponse> getClusters();

    @GET
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ClusterOpDescription.CLUSTER_GET, description = DistroXClusterNotes.NOTES, operationId = "getClusterByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DistroXAutoscaleClusterResponse getClusterByCrn(@PathParam("crn") String clusterCrn);

    @GET
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ClusterOpDescription.CLUSTER_GET, description = DistroXClusterNotes.NOTES, operationId = "getClusterByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DistroXAutoscaleClusterResponse getClusterByName(@PathParam("name") String clusterName);

    @POST
    @Path("crn/{crn}/autoscale_config")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ClusterOpDescription.CLUSTER_UPDATE_AUTOSCALE_CONFIG, description = DistroXClusterNotes.NOTES,
            operationId = "updateAutoscaleConfigByClusterCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DistroXAutoscaleClusterResponse updateAutoscaleConfigByClusterCrn(@PathParam("crn") String clusterCrn,
            @Valid DistroXAutoscaleClusterRequest autoscaleClusterRequest);

    @POST
    @Path("name/{name}/autoscale_config")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ClusterOpDescription.CLUSTER_UPDATE_AUTOSCALE_CONFIG, description = DistroXClusterNotes.NOTES,
            operationId = "updateAutoscaleConfigByClusterName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DistroXAutoscaleClusterResponse updateAutoscaleConfigByClusterName(@PathParam("name") String clusterName,
            @Valid DistroXAutoscaleClusterRequest autoscaleClusterRequest);

    @DELETE
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ClusterOpDescription.CLUSTER_DELETE_ALERTS, description = DistroXClusterNotes.NOTES, operationId = "deleteAlertsForClusterName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void deleteAlertsForClusterName(@PathParam("name") String clusterName);

    @DELETE
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ClusterOpDescription.CLUSTER_DELETE_ALERTS, description = DistroXClusterNotes.NOTES, operationId = "deleteAlertsForClusterCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void deleteAlertsForClusterCrn(@PathParam("crn") String clusterCrn);

    @POST
    @Path("name/{name}/autoscale")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ClusterOpDescription.CLUSTER_SET_AUTOSCALE_STATE, description = ApiDescription.ClusterNotes.NOTES,
            operationId = "enableAutoscaleForClusterName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DistroXAutoscaleClusterResponse enableAutoscaleForClusterName(@PathParam("name") String clusterName, AutoscaleClusterState autoscaleState);

    @POST
    @Path("crn/{crn}/autoscale")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ClusterOpDescription.CLUSTER_SET_AUTOSCALE_STATE, description = ApiDescription.ClusterNotes.NOTES,
            operationId = "enableAutoscaleForClusterCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DistroXAutoscaleClusterResponse enableAutoscaleForClusterCrn(@PathParam("crn") String clusterCrn, AutoscaleClusterState autoscaleState);

    @PUT
    @Path("updateServerCertificate")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ClusterOpDescription.CLUSTER_SERVER_CERT_UPDATE, description = DistroXClusterNotes.NOTES, operationId = "updateServerCertificate",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void updateServerCertificate(@Valid DistroXAutoscaleClusterServerCertUpdateRequest request);
}
