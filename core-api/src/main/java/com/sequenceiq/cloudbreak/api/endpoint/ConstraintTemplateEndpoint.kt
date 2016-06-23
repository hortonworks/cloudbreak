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
import com.sequenceiq.cloudbreak.doc.ContentType
import com.sequenceiq.cloudbreak.doc.ControllerDescription
import com.sequenceiq.cloudbreak.doc.Notes
import com.sequenceiq.cloudbreak.doc.OperationDescriptions
import com.sequenceiq.cloudbreak.api.model.ConstraintTemplateRequest
import com.sequenceiq.cloudbreak.api.model.ConstraintTemplateResponse

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation

@Path("/constraints")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/constraints", description = ControllerDescription.CONSTRAINT_TEMPLATE_DESCRIPTION, position = 2)
interface ConstraintTemplateEndpoint {

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConstraintOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.CONSTRAINT_NOTES)
    fun postPrivate(@Valid constraintTemplateRequest: ConstraintTemplateRequest): IdJson

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConstraintOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.CONSTRAINT_NOTES)
    fun postPublic(constraintTemplateRequest: ConstraintTemplateRequest): IdJson

    val privates: Set<ConstraintTemplateResponse>

    val publics: Set<ConstraintTemplateResponse>

    @GET
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConstraintOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.CONSTRAINT_NOTES)
    fun getPrivate(@PathParam(value = "name") name: String): ConstraintTemplateResponse

    @GET
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConstraintOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.CONSTRAINT_NOTES)
    fun getPublic(@PathParam(value = "name") name: String): ConstraintTemplateResponse

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConstraintOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.CONSTRAINT_NOTES)
    operator fun get(@PathParam(value = "id") id: Long?): ConstraintTemplateResponse

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConstraintOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.CONSTRAINT_NOTES)
    fun delete(@PathParam(value = "id") id: Long?)

    @DELETE
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConstraintOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.CONSTRAINT_NOTES)
    fun deletePublic(@PathParam(value = "name") name: String)

    @DELETE
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConstraintOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.CONSTRAINT_NOTES)
    fun deletePrivate(@PathParam(value = "name") name: String)
}
