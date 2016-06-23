package com.sequenceiq.periscope.api.endpoint

import com.sequenceiq.periscope.doc.ApiDescription.CLUSTERS_DESCRIPTION
import com.sequenceiq.periscope.doc.ApiDescription.JSON

import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

import com.sequenceiq.periscope.doc.ApiDescription
import com.sequenceiq.periscope.api.model.AmbariJson
import com.sequenceiq.periscope.api.model.ClusterJson
import com.sequenceiq.periscope.api.model.StateJson

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation

@Path("/clusters")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/clusters", description = CLUSTERS_DESCRIPTION, position = 5)
interface ClusterEndpoint {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.ClusterOpDescription.CLUSTER_POST, produces = JSON, notes = ApiDescription.ClusterNotes.NOTES)
    fun addCluster(ambariServer: AmbariJson): ClusterJson

    @PUT
    @Path(value = "{clusterId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.ClusterOpDescription.CLUSTER_PUT, produces = JSON, notes = ApiDescription.ClusterNotes.NOTES)
    fun modifyCluster(ambariServer: AmbariJson, @PathParam(value = "clusterId") clusterId: Long?): ClusterJson

    val clusters: List<ClusterJson>

    @GET
    @Path("{clusterId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.ClusterOpDescription.CLUSTER_GET, produces = JSON, notes = ApiDescription.ClusterNotes.NOTES)
    fun getCluster(@PathParam(value = "clusterId") clusterId: Long?): ClusterJson

    @DELETE
    @Path("{clusterId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.ClusterOpDescription.CLUSTER_DELETE, produces = JSON, notes = ApiDescription.ClusterNotes.NOTES)
    fun deleteCluster(@PathParam(value = "clusterId") clusterId: Long?)

    @POST
    @Path("{clusterId}/state")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.ClusterOpDescription.CLUSTER_SET_STATE, produces = JSON, notes = ApiDescription.ClusterNotes.NOTES)
    fun setState(@PathParam(value = "clusterId") clusterId: Long?, stateJson: StateJson): ClusterJson

}
