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

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@RetryAndMetrics
@Path("/v1/freeipa/user")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/freeipa/user", description = "Synchronize users to FreeIPA")
public interface UserV1Endpoint {
    @POST
    @Path("sync")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  UserOperationDescriptions.SYNC_SINGLE, description =  UserNotes.USER_NOTES,
            operationId = "synchronizeUserV1")
    SyncOperationStatus synchronizeUser(SynchronizeUserRequest request);

    @POST
    @Path("syncAll")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  UserOperationDescriptions.SYNC_ALL, description =  UserNotes.USER_NOTES,
            operationId = "synchronizeAllUsersV1")
    SyncOperationStatus synchronizeAllUsers(SynchronizeAllUsersRequest request);

    @POST
    @Path("password")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  UserOperationDescriptions.SET_PASSWORD, description =  UserNotes.USER_NOTES,
            operationId = "setPasswordV1")
    SyncOperationStatus setPassword(SetPasswordRequest request);

    @GET
    @Path("status")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  UserOperationDescriptions.SYNC_OPERATION_STATUS, description =  UserNotes.USER_NOTES,
            operationId = "getSyncOperationStatusV1")
    SyncOperationStatus getSyncOperationStatus(@QueryParam("operationId") @NotNull String operationId);

    @GET
    @Path("internal/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  UserOperationDescriptions.INTERNAL_SYNC_OPERATION_STATUS, description =  UserNotes.USER_NOTES,
            operationId ="internalGetSyncOperationStatusV1")
    SyncOperationStatus getSyncOperationStatusInternal(
            @QueryParam("accountId") @AccountId String accountId, @QueryParam("operationId") @NotNull String operationId);

    @GET
    @Path("lastStatus")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  UserOperationDescriptions.LAST_SYNC_OPERATION_STATUS, description =  UserNotes.USER_NOTES,
            operationId = "getLastSyncOperationStatusV1")
    SyncOperationStatus getLastSyncOperationStatus(@QueryParam("environmentCrn") @NotEmpty String environmentCrn);

    @GET
    @Path("syncState")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  UserOperationDescriptions.ENVIRONMENT_USERSYNC_STATE, description =  UserNotes.USER_NOTES,
            operationId = "getEnvironmentUserSyncStateV1")
    EnvironmentUserSyncState getUserSyncState(@QueryParam("environmentCrn") @NotEmpty String environmentCrn);

    @POST
    @Path("presync")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  UserOperationDescriptions.PRE_SYNC, description =  UserNotes.USER_NOTES,
            operationId = "preSynchronizeV1")
    SyncOperationStatus preSynchronize(@QueryParam("environmentCrn") @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @NotEmpty String environmentCrn);

    @POST
    @Path("postsync")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  UserOperationDescriptions.POST_SYNC, description =  UserNotes.USER_NOTES,
            operationId = "postSynchronizeV1")
    SyncOperationStatus postSynchronize(@QueryParam("environmentCrn") @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @NotEmpty String environmentCrn);
}
