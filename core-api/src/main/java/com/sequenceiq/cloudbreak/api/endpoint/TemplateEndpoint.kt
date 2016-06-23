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
import com.sequenceiq.cloudbreak.api.model.TemplateRequest
import com.sequenceiq.cloudbreak.api.model.TemplateResponse

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation

@Path("/templates")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/templates", description = ControllerDescription.TEMPLATE_DESCRIPTION, position = 2)
interface TemplateEndpoint {

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.TemplateOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.TEMPLATE_NOTES)
    fun postPrivate(@Valid templateRequest: TemplateRequest): IdJson

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.TemplateOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.TEMPLATE_NOTES)
    fun postPublic(@Valid templateRequest: TemplateRequest): IdJson

    val privates: Set<TemplateResponse>

    val publics: Set<TemplateResponse>

    @GET
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.TemplateOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.TEMPLATE_NOTES)
    fun getPrivate(@PathParam(value = "name") name: String): TemplateResponse

    @GET
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.TemplateOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.TEMPLATE_NOTES)
    fun getPublic(@PathParam(value = "name") name: String): TemplateResponse

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.TemplateOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.TEMPLATE_NOTES)
    operator fun get(@PathParam(value = "id") id: Long?): TemplateResponse

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.TemplateOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.TEMPLATE_NOTES)
    fun delete(@PathParam(value = "id") id: Long?)

    @DELETE
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.TemplateOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.TEMPLATE_NOTES)
    fun deletePublic(@PathParam(value = "name") name: String)

    @DELETE
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.TemplateOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.TEMPLATE_NOTES)
    fun deletePrivate(@PathParam(value = "name") name: String)
}
