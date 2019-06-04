package com.sequenceiq.freeipa.api.v1.freeipa.user;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.freeipa.api.v1.freeipa.user.doc.UserNotes;
import com.sequenceiq.freeipa.api.v1.freeipa.user.doc.UserOperationDescriptions;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.CreateUsersRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.CreateUsersResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SetPasswordRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SetPasswordResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeUsersRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeUsersResponse;
import com.sequenceiq.service.api.doc.ContentType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/freeipa/user")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/freeipa/user", description = "Synchronize users to FreeIPA", protocols = "http,https")
public interface UserV1Endpoint {
    @POST
    @Path("sync")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UserOperationDescriptions.SYNC, notes = UserNotes.USER_NOTES, produces = ContentType.JSON, nickname = "synchronizeUsersV1")
    SynchronizeUsersResponse synchronizeUsers(SynchronizeUsersRequest request);

    @GET
    @Path("sync")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UserOperationDescriptions.SYNC_STATUS, notes = UserNotes.USER_NOTES, produces = ContentType.JSON, nickname = "getSyncUsersStatusV1")
    SynchronizeUsersResponse getStatus(@QueryParam("id") String syncId);

    @POST
    @Path("/password")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UserOperationDescriptions.SET_PASSWORD, notes = UserNotes.USER_NOTES, produces = ContentType.JSON, nickname = "setPasswordV1")
    SetPasswordResponse setPassword(SetPasswordRequest request);

    @POST
    @Path("create")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UserOperationDescriptions.CREATE, notes = UserNotes.USER_NOTES, produces = ContentType.JSON, nickname = "createUsersV1")
    CreateUsersResponse createUsers(CreateUsersRequest request);
}
