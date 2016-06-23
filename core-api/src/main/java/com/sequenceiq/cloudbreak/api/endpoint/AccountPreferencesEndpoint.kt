package com.sequenceiq.cloudbreak.api.endpoint

import javax.validation.Valid
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

import com.sequenceiq.cloudbreak.doc.ContentType
import com.sequenceiq.cloudbreak.doc.ControllerDescription
import com.sequenceiq.cloudbreak.doc.Notes
import com.sequenceiq.cloudbreak.doc.OperationDescriptions
import com.sequenceiq.cloudbreak.api.model.AccountPreferencesJson

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation

@Path("/accountpreferences")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/accountpreferences", description = ControllerDescription.ACCOUNT_PREFERENCES_DESCRIPTION)
interface AccountPreferencesEndpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.AccountPreferencesDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.ACCOUNT_PREFERENCES_NOTES)
    fun get(): AccountPreferencesJson

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.AccountPreferencesDescription.PUT_PRIVATE, produces = ContentType.JSON, notes = Notes.ACCOUNT_PREFERENCES_NOTES)
    fun put(@Valid updateRequest: AccountPreferencesJson)

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.AccountPreferencesDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.ACCOUNT_PREFERENCES_NOTES)
    fun post(@Valid updateRequest: AccountPreferencesJson)

    @GET
    @Path("validate")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.AccountPreferencesDescription.VALIDATE, produces = ContentType.JSON, notes = Notes.ACCOUNT_PREFERENCES_NOTES)
    fun validate(): Response
}
