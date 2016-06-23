package com.sequenceiq.cloudbreak.api.endpoint


import javax.validation.Valid
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.DefaultValue
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

import com.sequenceiq.cloudbreak.api.model.AmbariAddressJson
import com.sequenceiq.cloudbreak.api.model.CertificateResponse
import com.sequenceiq.cloudbreak.api.model.IdJson
import com.sequenceiq.cloudbreak.api.model.PlatformVariantsJson
import com.sequenceiq.cloudbreak.api.model.StackRequest
import com.sequenceiq.cloudbreak.api.model.StackResponse
import com.sequenceiq.cloudbreak.api.model.StackValidationRequest
import com.sequenceiq.cloudbreak.api.model.UpdateStackJson
import com.sequenceiq.cloudbreak.doc.ContentType
import com.sequenceiq.cloudbreak.doc.ControllerDescription
import com.sequenceiq.cloudbreak.doc.Notes
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation

@Path("/stacks")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/stacks", description = ControllerDescription.STACK_DESCRIPTION, position = 3)
interface StackEndpoint {

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    fun postPrivate(@Valid stackRequest: StackRequest): IdJson

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    fun postPublic(@Valid stackRequest: StackRequest): IdJson

    @POST
    @Path("validate")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.VALIDATE, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    fun validate(@Valid stackValidationRequest: StackValidationRequest): Response

    @POST
    @Path(value = "ambari")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_BY_AMBARI_ADDRESS, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    fun getStackForAmbari(@Valid json: AmbariAddressJson): StackResponse

    val privates: Set<StackResponse>

    val publics: Set<StackResponse>

    @GET
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    fun getPrivate(@PathParam(value = "name") name: String): StackResponse

    @GET
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    fun getPublic(@PathParam(value = "name") name: String): StackResponse

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    operator fun get(@PathParam(value = "id") id: Long?): StackResponse

    @GET
    @Path(value = "{id}/status")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_STATUS_BY_ID, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    fun status(@PathParam(value = "id") id: Long?): Map<String, Any>

    @GET
    @Path(value = "platformVariants")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_PLATFORM_VARIANTS, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    fun variants(): PlatformVariantsJson

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    fun delete(@PathParam(value = "id") id: Long?, @QueryParam("forced") @DefaultValue("false") forced: Boolean?)

    @DELETE
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    fun deletePublic(@PathParam(value = "name") name: String, @QueryParam("forced") @DefaultValue("false") forced: Boolean?)

    @DELETE
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    fun deletePrivate(@PathParam(value = "name") name: String, @QueryParam("forced") @DefaultValue("false") forced: Boolean?)

    @DELETE
    @Path("{stackId}/{instanceId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.DELETE_INSTANCE_BY_ID, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    fun deleteInstance(@PathParam(value = "stackId") stackId: Long?, @PathParam(value = "instanceId") instanceId: String): Response

    @PUT
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.PUT_BY_ID, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    fun put(@PathParam(value = "id") id: Long?, @Valid updateRequest: UpdateStackJson): Response

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_STACK_CERT, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    @Path(value = "{id}/certificate")
    fun getCertificate(@PathParam(value = "id") stackId: Long?): CertificateResponse
}
