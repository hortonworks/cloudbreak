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
import com.sequenceiq.cloudbreak.api.model.BlueprintRequest
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse
import com.sequenceiq.cloudbreak.api.model.IdJson

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation

@Path("/blueprints")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/blueprints", description = ControllerDescription.BLUEPRINT_DESCRIPTION, position = 0)
interface BlueprintEndpoint {

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.BlueprintOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.BLUEPRINT_NOTES)
    operator fun get(@PathParam(value = "id") id: Long?): BlueprintResponse

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.BlueprintOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.BLUEPRINT_NOTES)
    fun delete(@PathParam(value = "id") id: Long?)

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.BlueprintOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.BLUEPRINT_NOTES)
    fun postPrivate(@Valid blueprintRequest: BlueprintRequest): IdJson

    val privates: Set<BlueprintResponse>

    @GET
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.BlueprintOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.BLUEPRINT_NOTES)
    fun getPrivate(@PathParam(value = "name") name: String): BlueprintResponse

    @DELETE
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.BlueprintOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.BLUEPRINT_NOTES)
    fun deletePrivate(@PathParam(value = "name") name: String)

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.BlueprintOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.BLUEPRINT_NOTES)
    fun postPublic(@Valid blueprintRequest: BlueprintRequest): IdJson

    val publics: Set<BlueprintResponse>

    @GET
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.BlueprintOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.BLUEPRINT_NOTES)
    fun getPublic(@PathParam(value = "name") name: String): BlueprintResponse

    @DELETE
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.BlueprintOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.BLUEPRINT_NOTES)
    fun deletePublic(@PathParam(value = "name") name: String)

}
