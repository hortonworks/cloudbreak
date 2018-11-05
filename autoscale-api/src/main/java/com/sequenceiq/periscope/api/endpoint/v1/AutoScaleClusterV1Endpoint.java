package com.sequenceiq.periscope.api.endpoint.v1;

import static com.sequenceiq.periscope.doc.ApiDescription.CLUSTERS_DESCRIPTION;
import static com.sequenceiq.periscope.doc.ApiDescription.JSON;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.periscope.api.model.AutoscaleClusterResponse;
import com.sequenceiq.periscope.api.model.AutoscaleClusterState;
import com.sequenceiq.periscope.api.model.StateJson;
import com.sequenceiq.periscope.doc.ApiDescription.ClusterNotes;
import com.sequenceiq.periscope.doc.ApiDescription.ClusterOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/clusters")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/clusters", description = CLUSTERS_DESCRIPTION, protocols = "http,https")
public interface AutoScaleClusterV1Endpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.CLUSTER_GET_ALL, produces = JSON, notes = ClusterNotes.NOTES)
    List<AutoscaleClusterResponse> getClusters();

    @GET
    @Path("{clusterId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.CLUSTER_GET, produces = JSON, notes = ClusterNotes.NOTES)
    AutoscaleClusterResponse getCluster(@PathParam("clusterId") Long clusterId);

    @DELETE
    @Path("{clusterId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.CLUSTER_DELETE, produces = JSON, notes = ClusterNotes.NOTES)
    void deleteCluster(@PathParam("clusterId") Long clusterId);

    @POST
    @Path("{clusterId}/state")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.CLUSTER_SET_STATE, produces = JSON, notes = ClusterNotes.NOTES)
    AutoscaleClusterResponse setState(@PathParam("clusterId") Long clusterId, StateJson stateJson);

    @POST
    @Path("{clusterId}/autoscale")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.CLUSTER_SET_AUTOSCALE_STATE, produces = JSON, notes = ClusterNotes.NOTES)
    AutoscaleClusterResponse setAutoscaleState(@PathParam("clusterId") Long clusterId, AutoscaleClusterState autoscaleState);

}
