package com.sequenceiq.periscope.api.endpoint.v2;

import static com.sequenceiq.periscope.doc.ApiDescription.CLUSTERS_DESCRIPTION;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.periscope.api.model.AutoscaleClusterResponse;
import com.sequenceiq.periscope.doc.ApiDescription.ClusterNotes;
import com.sequenceiq.periscope.doc.ApiDescription.ClusterOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v2/clusters")
@Consumes(MediaType.APPLICATION_JSON)
@Api(hidden = true, value = "/v2/clusters", description = CLUSTERS_DESCRIPTION, protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface AutoScaleClusterV2Endpoint {

    @GET
    @Path("{cbClusterId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.CLUSTER_GET, produces = MediaType.APPLICATION_JSON, notes = ClusterNotes.NOTES)
    AutoscaleClusterResponse getByCloudbreakCluster(@PathParam("cbClusterId") Long stackId);

    @DELETE
    @Path("{cbClusterId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.CLUSTER_DELETE, produces = MediaType.APPLICATION_JSON, notes = ClusterNotes.NOTES)
    void deleteByCloudbreakCluster(@PathParam("cbClusterId") Long stackId);

    @POST
    @Path("{cbClusterId}/running")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.CLUSTER_SET_STATE, produces = MediaType.APPLICATION_JSON, notes = ClusterNotes.NOTES)
    AutoscaleClusterResponse runByCloudbreakCluster(@PathParam("cbClusterId") Long clusterId);

    @POST
    @Path("{cbClusterId}/suspended")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.CLUSTER_SET_STATE, produces = MediaType.APPLICATION_JSON, notes = ClusterNotes.NOTES)
    AutoscaleClusterResponse suspendByCloudbreakCluster(@PathParam("cbClusterId") Long clusterId);

    @POST
    @Path("{cbClusterId}/enable")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.CLUSTER_SET_AUTOSCALE_STATE, produces = MediaType.APPLICATION_JSON, notes = ClusterNotes.NOTES)
    AutoscaleClusterResponse enableAutoscaleStateByCloudbreakCluster(@PathParam("cbClusterId") Long clusterId);

    @POST
    @Path("{cbClusterId}/disable")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.CLUSTER_SET_AUTOSCALE_STATE, produces = MediaType.APPLICATION_JSON, notes = ClusterNotes.NOTES)
    AutoscaleClusterResponse disableAutoscaleStateByCloudbreakCluster(@PathParam("cbClusterId") Long clusterId);
}
