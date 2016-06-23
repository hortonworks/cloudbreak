package com.sequenceiq.cloudbreak.api.endpoint

import javax.validation.Valid
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

import com.sequenceiq.cloudbreak.doc.ContentType
import com.sequenceiq.cloudbreak.doc.ControllerDescription
import com.sequenceiq.cloudbreak.doc.Notes
import com.sequenceiq.cloudbreak.doc.OperationDescriptions
import com.sequenceiq.cloudbreak.api.model.ClusterRequest
import com.sequenceiq.cloudbreak.api.model.ClusterResponse
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation

@Path("/stacks")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/cluster", description = ControllerDescription.CLUSTER_DESCRIPTION, position = 4)
interface ClusterEndpoint {

    @POST
    @Path("{id}/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterOpDescription.POST_FOR_STACK, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES)
    fun post(@PathParam(value = "id") id: Long?, @Valid request: ClusterRequest): Response

    @GET
    @Path("{id}/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterOpDescription.GET_BY_STACK_ID, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES)
    operator fun get(@PathParam(value = "id") id: Long?): ClusterResponse

    @GET
    @Path("account/{name}/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES)
    fun getPublic(@PathParam(value = "name") name: String): ClusterResponse

    @GET
    @Path("user/{name}/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES)
    fun getPrivate(@PathParam(value = "name") name: String): ClusterResponse

    @DELETE
    @Path("{id}/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterOpDescription.DELETE_BY_STACK_ID, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES)
    @Throws(Exception::class)
    fun delete(@PathParam(value = "id") stackId: Long?)

    @PUT
    @Path("{id}/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterOpDescription.PUT_BY_STACK_ID, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES)
    @Throws(Exception::class)
    fun put(@PathParam(value = "id") stackId: Long?, @Valid updateJson: UpdateClusterJson): Response

}
