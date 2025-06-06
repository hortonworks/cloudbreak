package com.sequenceiq.freeipa.api.v1.freeipa.user;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.freeipa.api.v1.freeipa.user.doc.UserNotes;
import com.sequenceiq.freeipa.api.v1.freeipa.user.doc.UserOperationDescriptions;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.EnvironmentUserSyncState;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SetPasswordRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeAllUsersRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeUserRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v1/freeipa/user")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/freeipa/user", description = "Synchronize users to FreeIPA")
public interface UserV1Endpoint {
    @POST
    @Path("sync")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UserOperationDescriptions.SYNC_SINGLE, description = UserNotes.USER_NOTES,
            operationId = "synchronizeUserV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SyncOperationStatus synchronizeUser(SynchronizeUserRequest request);

    @POST
    @Path("syncAll")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UserOperationDescriptions.SYNC_ALL, description = UserNotes.USER_NOTES,
            operationId = "synchronizeAllUsersV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SyncOperationStatus synchronizeAllUsers(SynchronizeAllUsersRequest request);

    @POST
    @Path("password")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UserOperationDescriptions.SET_PASSWORD, description = UserNotes.USER_NOTES,
            operationId = "setPasswordV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SyncOperationStatus setPassword(SetPasswordRequest request);

    @GET
    @Path("status")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UserOperationDescriptions.SYNC_OPERATION_STATUS, description = UserNotes.USER_NOTES,
            operationId = "getSyncOperationStatusV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SyncOperationStatus getSyncOperationStatus(@QueryParam("operationId") @NotNull String operationId);

    @GET
    @Path("internal/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UserOperationDescriptions.INTERNAL_SYNC_OPERATION_STATUS, description = UserNotes.USER_NOTES,
            operationId = "internalGetSyncOperationStatusV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SyncOperationStatus getSyncOperationStatusInternal(
            @QueryParam("accountId") String accountId, @QueryParam("operationId") @NotNull String operationId);

    @GET
    @Path("lastStatus")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UserOperationDescriptions.LAST_SYNC_OPERATION_STATUS, description = UserNotes.USER_NOTES,
            operationId = "getLastSyncOperationStatusV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SyncOperationStatus getLastSyncOperationStatus(@QueryParam("environmentCrn") @NotEmpty String environmentCrn);

    @GET
    @Path("syncState")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UserOperationDescriptions.ENVIRONMENT_USERSYNC_STATE, description = UserNotes.USER_NOTES,
            operationId = "getEnvironmentUserSyncStateV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    EnvironmentUserSyncState getUserSyncState(@QueryParam("environmentCrn") @NotEmpty String environmentCrn);

    @POST
    @Path("presync")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UserOperationDescriptions.PRE_SYNC, description = UserNotes.USER_NOTES,
            operationId = "preSynchronizeV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SyncOperationStatus preSynchronize(@QueryParam("environmentCrn") @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @NotEmpty String environmentCrn);

    @POST
    @Path("postsync")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UserOperationDescriptions.POST_SYNC, description = UserNotes.USER_NOTES,
            operationId = "postSynchronizeV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SyncOperationStatus postSynchronize(@QueryParam("environmentCrn") @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @NotEmpty String environmentCrn);
}
