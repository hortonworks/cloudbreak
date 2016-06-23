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
import com.sequenceiq.cloudbreak.api.model.CredentialRequest
import com.sequenceiq.cloudbreak.api.model.CredentialResponse
import com.sequenceiq.cloudbreak.api.model.IdJson

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation

@Path("/credentials")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/credentials", description = ControllerDescription.CREDENTIAL_DESCRIPTION, position = 1)
interface CredentialEndpoint {

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.CredentialOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES)
    fun postPrivate(@Valid credentialRequest: CredentialRequest): IdJson

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.CredentialOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES)
    fun postPublic(@Valid credentialRequest: CredentialRequest): IdJson

    val privates: Set<CredentialResponse>

    val publics: Set<CredentialResponse>

    @GET
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.CredentialOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES)
    fun getPrivate(@PathParam(value = "name") name: String): CredentialResponse

    @GET
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.CredentialOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES)
    fun getPublic(@PathParam(value = "name") name: String): CredentialResponse

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.CredentialOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES)
    operator fun get(@PathParam(value = "id") credentialId: Long?): CredentialResponse

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.CredentialOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES)
    fun delete(@PathParam(value = "id") credentialId: Long?)

    @DELETE
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.CredentialOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES)
    fun deletePublic(@PathParam(value = "name") name: String)

    @DELETE
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.CredentialOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES)
    fun deletePrivate(@PathParam(value = "name") name: String)
}
