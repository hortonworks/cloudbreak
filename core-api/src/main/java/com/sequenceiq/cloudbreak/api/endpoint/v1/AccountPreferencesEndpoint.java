package com.sequenceiq.cloudbreak.api.endpoint.v1;

import java.util.Map;

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
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.AccountPreferencesDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/accountpreferences")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/accountpreferences", description = ControllerDescription.ACCOUNT_PREFERENCES_DESCRIPTION, protocols = "http,https")
public interface AccountPreferencesEndpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AccountPreferencesDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.ACCOUNT_PREFERENCES_NOTES,
            nickname = "getAccountPreferencesEndpoint")
    AccountPreferencesJson get();

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AccountPreferencesDescription.PUT_PRIVATE, produces = ContentType.JSON, notes = Notes.ACCOUNT_PREFERENCES_NOTES,
            nickname = "putAccountPreferencesEndpoint")
    AccountPreferencesJson put(@Valid AccountPreferencesJson updateRequest);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AccountPreferencesDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.ACCOUNT_PREFERENCES_NOTES,
            nickname = "postAccountPreferencesEndpoint")
    AccountPreferencesJson post(@Valid AccountPreferencesJson updateRequest);

    @GET
    @Path("/isplatformselectiondisabled")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AccountPreferencesDescription.IS_PLATFORM_SELECTION_DISABLED, produces = ContentType.JSON,
            notes = Notes.ACCOUNT_PREFERENCES_NOTES)
    Map<String, Boolean> isPlatformSelectionDisabled();

    @GET
    @Path("validate")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AccountPreferencesDescription.VALIDATE, produces = ContentType.JSON, notes = Notes.ACCOUNT_PREFERENCES_NOTES,
        nickname = "validateAccountPreferencesEndpoint")
    Response validate();
}
