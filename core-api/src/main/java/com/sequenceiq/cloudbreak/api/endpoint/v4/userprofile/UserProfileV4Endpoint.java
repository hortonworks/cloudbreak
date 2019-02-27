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
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.UserProfileOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v4/user_profiles")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/user_profiles", description = ControllerDescription.USER_PROFILES_V4_DESCRIPTION, protocols = "http,https")
public interface UserProfileV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UserProfileOpDescription.GET_USER_PROFILE, produces = ContentType.JSON,
            notes = Notes.USER_PROFILE_NOTES, nickname = "getUserProfileInWorkspace")
    UserProfileV4Response get();

    @GET
    @Path("/terminated_clusters_preferences")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UserProfileOpDescription.GET_TERMINATED_CLUSTERS_PREFERENCES, produces = ContentType.JSON,
            notes = Notes.SHOW_INSTANCES_PREFERENCES, nickname = "getTerminatedClustersPreferences")
    ShowTerminatedClusterPreferencesV4Response getShowClusterPreferences();

    @PUT
    @Path("/terminated_clusters_preferences")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UserProfileOpDescription.PUT_TERMINATED_CLUSTERS_PREFERENCES, produces = ContentType.JSON,
            notes = Notes.SHOW_INSTANCES_PREFERENCES, nickname = "putTerminatedClustersPreferences")
    void putTerminatedClustersPreferences(@Valid ShowTerminatedClustersPreferencesV4Request showInstancesPrefsV4Request);

    @DELETE
    @Path("/terminated_clusters_preferences")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UserProfileOpDescription.DELETE_TERMINATED_INSTANCES_PREFERENCES, produces = ContentType.JSON,
            notes = Notes.SHOW_INSTANCES_PREFERENCES, nickname = "deleteTerminatedClustersPreferences")
    void deleteTerminatedClustersPreferences();

}
