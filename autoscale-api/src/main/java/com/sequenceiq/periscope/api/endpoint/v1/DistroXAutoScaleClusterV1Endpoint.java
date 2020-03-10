package com.sequenceiq.periscope.api.endpoint.v1;

import static com.sequenceiq.periscope.doc.ApiDescription.CLUSTERS_DESCRIPTION;

import java.util.List;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.periscope.api.endpoint.validator.ValidDistroXAutoscaleRequest;
import com.sequenceiq.periscope.api.model.AutoscaleClusterState;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterRequest;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterResponse;
import com.sequenceiq.periscope.doc.ApiDescription;
import com.sequenceiq.periscope.doc.ApiDescription.ClusterOpDescription;
import com.sequenceiq.periscope.doc.ApiDescription.DistroXClusterNotes;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/distrox")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/distrox", description = CLUSTERS_DESCRIPTION, protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface DistroXAutoScaleClusterV1Endpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.CLUSTER_GET_ALL, produces = MediaType.APPLICATION_JSON, notes = DistroXClusterNotes.NOTES)
    List<DistroXAutoscaleClusterResponse> getClusters();

    @GET
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.CLUSTER_GET, produces = MediaType.APPLICATION_JSON, notes = DistroXClusterNotes.NOTES)
    DistroXAutoscaleClusterResponse getClusterByCrn(@PathParam("crn") String clusterCrn);

    @GET
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.CLUSTER_GET, produces = MediaType.APPLICATION_JSON, notes = DistroXClusterNotes.NOTES)
    DistroXAutoscaleClusterResponse getClusterByName(@PathParam("name") String clusterName);

    @POST
    @Path("crn/{crn}/autoscaleconfig")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.CLUSTER_UPDATE_AUTOSCALE_CONFIG, produces = MediaType.APPLICATION_JSON, notes = DistroXClusterNotes.NOTES)
    DistroXAutoscaleClusterResponse updateAutoscaleConfigByClusterCrn(@PathParam("crn") String clusterCrn,
            @ValidDistroXAutoscaleRequest @Valid DistroXAutoscaleClusterRequest autoscaleClusterRequest);

    @POST
    @Path("name/{name}/autoscaleconfig")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.CLUSTER_UPDATE_AUTOSCALE_CONFIG, produces = MediaType.APPLICATION_JSON, notes = DistroXClusterNotes.NOTES)
    DistroXAutoscaleClusterResponse updateAutoscaleConfigByClusterName(@PathParam("name") String clusterName,
            @ValidDistroXAutoscaleRequest @Valid DistroXAutoscaleClusterRequest autoscaleClusterRequest);

    @DELETE
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.CLUSTER_DELETE_ALERTS, produces = MediaType.APPLICATION_JSON, notes = DistroXClusterNotes.NOTES)
    void deleteAlertsForClusterName(@PathParam("name") String clusterName);

    @DELETE
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.CLUSTER_DELETE_ALERTS, produces = MediaType.APPLICATION_JSON, notes = DistroXClusterNotes.NOTES)
    void deleteAlertsForClusterCrn(@PathParam("crn") String clusterCrn);

    @POST
    @Path("name/{name}/autoscale")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.CLUSTER_SET_AUTOSCALE_STATE, produces = MediaType.APPLICATION_JSON, notes = ApiDescription.ClusterNotes.NOTES)
    DistroXAutoscaleClusterResponse enableAutoscaleForClusterName(@PathParam("name") String clusterName, AutoscaleClusterState autoscaleState);

    @POST
    @Path("crn/{crn}/autoscale")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.CLUSTER_SET_AUTOSCALE_STATE, produces = MediaType.APPLICATION_JSON, notes = ApiDescription.ClusterNotes.NOTES)
    DistroXAutoscaleClusterResponse enableAutoscaleForClusterCrn(@PathParam("crn") String clusterCrn, AutoscaleClusterState autoscaleState);
}
