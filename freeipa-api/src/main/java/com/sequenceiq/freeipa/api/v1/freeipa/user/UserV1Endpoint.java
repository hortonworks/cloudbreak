package com.sequenceiq.freeipa.api.v1.freeipa.user;

import javax.validation.constraints.NotEmpty;
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
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.EnvironmentUserSyncState;
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
    SyncOperationStatus getSyncOperationStatus(@QueryParam("operationId") @NotNull String operationId);

    @GET
    @Path("syncState")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UserOperationDescriptions.ENVIRONMENT_USERSYNC_STATE, notes = UserNotes.USER_NOTES, produces = MediaType.APPLICATION_JSON,
            nickname = "getEnvironmentUserSyncStateV1")
    EnvironmentUserSyncState getUserSyncState(@QueryParam("environmentCrn") @NotEmpty String environmentCrn);

    @POST
    @Path("syncEnvironment")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UserOperationDescriptions.SYNC_ENVIRONMENT, notes = UserNotes.USER_NOTES, produces = MediaType.APPLICATION_JSON,
            nickname = "synchronizeEnvironmentV1")
    SyncOperationStatus synchronizeEnvironment(@QueryParam("environmentCrn") @NotEmpty String environmentCrn);

    @POST
    @Path("syncActor")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UserOperationDescriptions.SYNC_ACTOR, notes = UserNotes.USER_NOTES, produces = MediaType.APPLICATION_JSON,
            nickname = "synchronizeActorV1")
    SyncOperationStatus synchronizeActor(@QueryParam("actorCrn") @NotEmpty String actorCrn);
}
