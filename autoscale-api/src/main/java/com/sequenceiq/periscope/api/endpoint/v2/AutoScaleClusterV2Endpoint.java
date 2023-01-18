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

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

/**
 * @deprecated Database ID based endpoints are deprecated for removal.
 */
@Path("/v2/clusters")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v2/clusters", description = CLUSTERS_DESCRIPTION)
@Deprecated(since = "CB 2.26.0", forRemoval = true)
public interface AutoScaleClusterV2Endpoint {

    @GET
    @Path("{cbClusterId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ClusterOpDescription.CLUSTER_GET, description =  ClusterNotes.NOTES)
    AutoscaleClusterResponse getByCloudbreakCluster(@PathParam("cbClusterId") Long stackId);

    @DELETE
    @Path("{cbClusterId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ClusterOpDescription.CLUSTER_DELETE, description =  ClusterNotes.NOTES)
    void deleteByCloudbreakCluster(@PathParam("cbClusterId") Long stackId);

    @POST
    @Path("{cbClusterId}/running")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ClusterOpDescription.CLUSTER_SET_STATE, description =  ClusterNotes.NOTES)
    AutoscaleClusterResponse runByCloudbreakCluster(@PathParam("cbClusterId") Long clusterId);

    @POST
    @Path("{cbClusterId}/suspended")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ClusterOpDescription.CLUSTER_SET_STATE, description =  ClusterNotes.NOTES)
    AutoscaleClusterResponse suspendByCloudbreakCluster(@PathParam("cbClusterId") Long clusterId);

    @POST
    @Path("{cbClusterId}/enable")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ClusterOpDescription.CLUSTER_SET_AUTOSCALE_STATE, description =  ClusterNotes.NOTES)
    AutoscaleClusterResponse enableAutoscaleStateByCloudbreakCluster(@PathParam("cbClusterId") Long clusterId);

    @POST
    @Path("{cbClusterId}/disable")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ClusterOpDescription.CLUSTER_SET_AUTOSCALE_STATE, description =  ClusterNotes.NOTES)
    AutoscaleClusterResponse disableAutoscaleStateByCloudbreakCluster(@PathParam("cbClusterId") Long clusterId);
}
