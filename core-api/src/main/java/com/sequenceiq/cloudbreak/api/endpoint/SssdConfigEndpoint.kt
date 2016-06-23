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

import com.sequenceiq.cloudbreak.api.model.IdJson
import com.sequenceiq.cloudbreak.api.model.SssdConfigRequest
import com.sequenceiq.cloudbreak.api.model.SssdConfigResponse
import com.sequenceiq.cloudbreak.doc.ContentType
import com.sequenceiq.cloudbreak.doc.ControllerDescription
import com.sequenceiq.cloudbreak.doc.Notes
import com.sequenceiq.cloudbreak.doc.OperationDescriptions

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation

@Path("/sssd")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/sssd", description = ControllerDescription.SSSDCONFIG_DESCRIPTION)
interface SssdConfigEndpoint {

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SssdConfigOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.SSSDCONFIG_NOTES)
    fun postPrivate(@Valid sssdConfigRequest: SssdConfigRequest): IdJson

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SssdConfigOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.SSSDCONFIG_NOTES)
    fun postPublic(@Valid sssdConfigRequest: SssdConfigRequest): IdJson

    val privates: Set<SssdConfigResponse>

    val publics: Set<SssdConfigResponse>

    @GET
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SssdConfigOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.SSSDCONFIG_NOTES)
    fun getPrivate(@PathParam(value = "name") name: String): SssdConfigResponse

    @GET
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SssdConfigOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.SSSDCONFIG_NOTES)
    fun getPublic(@PathParam(value = "name") name: String): SssdConfigResponse

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SssdConfigOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.SSSDCONFIG_NOTES)
    operator fun get(@PathParam(value = "id") id: Long?): SssdConfigResponse

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SssdConfigOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.SSSDCONFIG_NOTES)
    fun delete(@PathParam(value = "id") id: Long?)

    @DELETE
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SssdConfigOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.SSSDCONFIG_NOTES)
    fun deletePublic(@PathParam(value = "name") name: String)

    @DELETE
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SssdConfigOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.SSSDCONFIG_NOTES)
    fun deletePrivate(@PathParam(value = "name") name: String)
}
