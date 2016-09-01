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

import com.sequenceiq.periscope.api.model.AmbariJson;
import com.sequenceiq.periscope.api.model.ClusterJson;
import com.sequenceiq.periscope.api.model.StateJson;
import com.sequenceiq.periscope.doc.ApiDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/clusters")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/clusters", description = CLUSTERS_DESCRIPTION)
public interface ClusterEndpoint {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.ClusterOpDescription.CLUSTER_POST, produces = JSON, notes = ApiDescription.ClusterNotes.NOTES)
    ClusterJson addCluster(AmbariJson ambariServer);

    @PUT
    @Path(value = "{clusterId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.ClusterOpDescription.CLUSTER_PUT, produces = JSON, notes = ApiDescription.ClusterNotes.NOTES)
    ClusterJson modifyCluster(AmbariJson ambariServer, @PathParam(value = "clusterId") Long clusterId);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.ClusterOpDescription.CLUSTER_GET_ALL, produces = JSON, notes = ApiDescription.ClusterNotes.NOTES)
    List<ClusterJson> getClusters();

    @GET
    @Path("{clusterId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.ClusterOpDescription.CLUSTER_GET, produces = JSON, notes = ApiDescription.ClusterNotes.NOTES)
    ClusterJson getCluster(@PathParam(value = "clusterId") Long clusterId);

    @DELETE
    @Path("{clusterId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.ClusterOpDescription.CLUSTER_DELETE, produces = JSON, notes = ApiDescription.ClusterNotes.NOTES)
    void deleteCluster(@PathParam(value = "clusterId") Long clusterId);

    @POST
    @Path("{clusterId}/state")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.ClusterOpDescription.CLUSTER_SET_STATE, produces = JSON, notes = ApiDescription.ClusterNotes.NOTES)
    ClusterJson setState(@PathParam(value = "clusterId") Long clusterId, StateJson stateJson);

}
