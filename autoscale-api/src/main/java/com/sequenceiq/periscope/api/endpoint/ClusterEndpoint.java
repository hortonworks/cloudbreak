package com.sequenceiq.periscope.api.endpoint;

import static com.sequenceiq.periscope.doc.ApiDescription.CLUSTERS_DESCRIPTION;
import static com.sequenceiq.periscope.doc.ApiDescription.JSON;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.periscope.api.model.ClusterAutoscaleState;
import com.sequenceiq.periscope.api.model.ClusterJson;
import com.sequenceiq.periscope.api.model.ClusterRequestJson;
import com.sequenceiq.periscope.api.model.StateJson;
import com.sequenceiq.periscope.doc.ApiDescription.ClusterNotes;
import com.sequenceiq.periscope.doc.ApiDescription.ClusterOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/clusters")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/clusters", description = CLUSTERS_DESCRIPTION, protocols = "http,https")
public interface ClusterEndpoint {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.CLUSTER_POST, produces = JSON, notes = ClusterNotes.NOTES)
    ClusterJson addCluster(ClusterRequestJson ambariServer);

    @PUT
    @Path("{clusterId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.CLUSTER_PUT, produces = JSON, notes = ClusterNotes.NOTES)
    ClusterJson modifyCluster(ClusterRequestJson ambariServer, @PathParam("clusterId") Long clusterId);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.CLUSTER_GET_ALL, produces = JSON, notes = ClusterNotes.NOTES)
    List<ClusterJson> getClusters();

    @GET
    @Path("{clusterId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.CLUSTER_GET, produces = JSON, notes = ClusterNotes.NOTES)
    ClusterJson getCluster(@PathParam("clusterId") Long clusterId);

    @DELETE
    @Path("{clusterId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.CLUSTER_DELETE, produces = JSON, notes = ClusterNotes.NOTES)
    void deleteCluster(@PathParam("clusterId") Long clusterId);

    @POST
    @Path("{clusterId}/state")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.CLUSTER_SET_STATE, produces = JSON, notes = ClusterNotes.NOTES)
    ClusterJson setState(@PathParam("clusterId") Long clusterId, StateJson stateJson);

    @POST
    @Path("{clusterId}/autoscale")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.CLUSTER_SET_AUTOSCALE_STATE, produces = JSON, notes = ClusterNotes.NOTES)
    ClusterJson setAutoscaleState(@PathParam("clusterId") Long clusterId, ClusterAutoscaleState autoscaleState);
}
