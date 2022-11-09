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

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
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
    @Path("internal/status")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UserOperationDescriptions.INTERNAL_SYNC_OPERATION_STATUS, notes = UserNotes.USER_NOTES,
            produces = MediaType.APPLICATION_JSON, nickname = "internalGetSyncOperationStatusV1")
    SyncOperationStatus getSyncOperationStatusInternal(
            @QueryParam("accountId") @AccountId String accountId, @QueryParam("operationId") @NotNull String operationId);

    @GET
    @Path("lastStatus")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UserOperationDescriptions.LAST_SYNC_OPERATION_STATUS, notes = UserNotes.USER_NOTES, produces = MediaType.APPLICATION_JSON,
            nickname = "getLastSyncOperationStatusV1")
    SyncOperationStatus getLastSyncOperationStatus(@QueryParam("environmentCrn") @NotEmpty String environmentCrn);

    @GET
    @Path("syncState")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UserOperationDescriptions.ENVIRONMENT_USERSYNC_STATE, notes = UserNotes.USER_NOTES, produces = MediaType.APPLICATION_JSON,
            nickname = "getEnvironmentUserSyncStateV1")
    EnvironmentUserSyncState getUserSyncState(@QueryParam("environmentCrn") @NotEmpty String environmentCrn);

    @POST
    @Path("presync")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UserOperationDescriptions.PRE_SYNC, notes = UserNotes.USER_NOTES, produces = MediaType.APPLICATION_JSON,
            nickname = "preSynchronizeV1")
    SyncOperationStatus preSynchronize(@QueryParam("environmentCrn") @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @NotEmpty String environmentCrn);

    @POST
    @Path("postsync")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UserOperationDescriptions.POST_SYNC, notes = UserNotes.USER_NOTES, produces = MediaType.APPLICATION_JSON,
            nickname = "postSynchronizeV1")
    SyncOperationStatus postSynchronize(@QueryParam("environmentCrn") @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @NotEmpty String environmentCrn);
}
