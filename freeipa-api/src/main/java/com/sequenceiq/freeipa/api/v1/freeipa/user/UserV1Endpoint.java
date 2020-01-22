package com.sequenceiq.freeipa.api.v1.freeipa.user;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.freeipa.api.v1.freeipa.user.doc.UserNotes;
import com.sequenceiq.freeipa.api.v1.freeipa.user.doc.UserOperationDescriptions;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SetPasswordRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeAllUsersRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeUserRequest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/v1/freeipa/user")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/freeipa/user", description = "Synchronize users to FreeIPA", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface UserV1Endpoint {
    @POST
    @Path("sync")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UserOperationDescriptions.SYNC_SINGLE, notes = UserNotes.USER_NOTES, produces = MediaType.APPLICATION_JSON,
            nickname = "synchronizeUserV1")
    SyncOperationStatus synchronizeUser(SynchronizeUserRequest request);

    @POST
    @Path("syncAll")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UserOperationDescriptions.SYNC_ALL, notes = UserNotes.USER_NOTES, produces = MediaType.APPLICATION_JSON,
            nickname = "synchronizeAllUsersV1")
    SyncOperationStatus synchronizeAllUsers(SynchronizeAllUsersRequest request);

    @POST
    @Path("password")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UserOperationDescriptions.SET_PASSWORD, notes = UserNotes.USER_NOTES, produces = MediaType.APPLICATION_JSON,
            nickname = "setPasswordV1")
    SyncOperationStatus setPassword(SetPasswordRequest request);

    @GET
    @Path("status")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UserOperationDescriptions.SYNC_OPERATION_STATUS, notes = UserNotes.USER_NOTES, produces = MediaType.APPLICATION_JSON,
            nickname = "getSyncOperationStatusV1")
    SyncOperationStatus getSyncOperationStatus(@NotNull @QueryParam("operationId") String operationId);
}
