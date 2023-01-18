package com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.requests.ShowTerminatedClustersPreferencesV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses.ShowTerminatedClusterPreferencesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses.UserProfileV4Response;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.UserProfileOpDescription;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v4/user_profiles")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v4/user_profiles", description = ControllerDescription.USER_PROFILES_V4_DESCRIPTION)
public interface UserProfileV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UserProfileOpDescription.GET_USER_PROFILE,
            description = Notes.USER_PROFILE_NOTES, operationId = "getUserProfileInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    UserProfileV4Response get();

    @GET
    @Path("/terminated_clusters_preferences")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UserProfileOpDescription.GET_TERMINATED_CLUSTERS_PREFERENCES,
            description = Notes.SHOW_INSTANCES_PREFERENCES, operationId = "getTerminatedClustersPreferences",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ShowTerminatedClusterPreferencesV4Response getShowClusterPreferences();

    @PUT
    @Path("/terminated_clusters_preferences")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UserProfileOpDescription.PUT_TERMINATED_CLUSTERS_PREFERENCES,
            description = Notes.SHOW_INSTANCES_PREFERENCES, operationId = "putTerminatedClustersPreferences",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void putTerminatedClustersPreferences(@Valid ShowTerminatedClustersPreferencesV4Request showInstancesPrefsV4Request);

    @DELETE
    @Path("/terminated_clusters_preferences")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UserProfileOpDescription.DELETE_TERMINATED_INSTANCES_PREFERENCES,
            description = Notes.SHOW_INSTANCES_PREFERENCES, operationId = "deleteTerminatedClustersPreferences",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void deleteTerminatedClustersPreferences();

}
