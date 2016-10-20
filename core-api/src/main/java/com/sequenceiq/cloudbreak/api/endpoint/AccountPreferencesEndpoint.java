package com.sequenceiq.cloudbreak.api.endpoint;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.model.AccountPreferencesJson;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/accountpreferences")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/accountpreferences", description = ControllerDescription.ACCOUNT_PREFERENCES_DESCRIPTION, protocols = "http,https")
public interface AccountPreferencesEndpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.AccountPreferencesDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.ACCOUNT_PREFERENCES_NOTES)
    AccountPreferencesJson get();

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.AccountPreferencesDescription.PUT_PRIVATE, produces = ContentType.JSON, notes = Notes.ACCOUNT_PREFERENCES_NOTES)
    AccountPreferencesJson put(@Valid AccountPreferencesJson updateRequest);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.AccountPreferencesDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.ACCOUNT_PREFERENCES_NOTES)
    AccountPreferencesJson post(@Valid AccountPreferencesJson updateRequest);

    @GET
    @Path("validate")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.AccountPreferencesDescription.VALIDATE, produces = ContentType.JSON, notes = Notes.ACCOUNT_PREFERENCES_NOTES)
    Response validate();
}
