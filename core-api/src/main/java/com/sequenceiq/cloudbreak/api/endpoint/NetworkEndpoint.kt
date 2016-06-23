package com.sequenceiq.cloudbreak.api.endpoint

import javax.validation.Valid
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

import com.sequenceiq.cloudbreak.doc.ContentType
import com.sequenceiq.cloudbreak.doc.ControllerDescription
import com.sequenceiq.cloudbreak.doc.Notes
import com.sequenceiq.cloudbreak.doc.OperationDescriptions
import com.sequenceiq.cloudbreak.api.model.IdJson
import com.sequenceiq.cloudbreak.api.model.NetworkJson

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation

@Path("/networks")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/networks", description = ControllerDescription.NETWORK_DESCRIPTION, position = 8)
interface NetworkEndpoint {

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.NetworkOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES)
    operator fun get(@PathParam("id") id: Long?): NetworkJson

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.NetworkOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES)
    fun delete(@PathParam(value = "id") id: Long?)

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.NetworkOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES)
    fun postPublic(@Valid networkJson: NetworkJson): IdJson

    val publics: Set<NetworkJson>

    @GET
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.NetworkOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES)
    fun getPublic(@PathParam(value = "name") name: String): NetworkJson

    @DELETE
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.NetworkOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES)
    fun deletePublic(@PathParam(value = "name") name: String)

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.NetworkOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES)
    fun postPrivate(@Valid networkJson: NetworkJson): IdJson

    val privates: Set<NetworkJson>

    @GET
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.NetworkOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES)
    fun getPrivate(@PathParam(value = "name") name: String): NetworkJson

    @DELETE
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.NetworkOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES)
    fun deletePrivate(@PathParam(value = "name") name: String)

}
