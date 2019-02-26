package com.sequenceiq.cloudbreak.api.endpoint.v3;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.users.UserProfileRequest;
import com.sequenceiq.cloudbreak.api.model.users.UserProfileResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.UserOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3/{workspaceId}/users")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{workspaceId}/users", description = ControllerDescription.USER_V3_DESCRIPTION, protocols = "http,https")
public interface UserV3Endpoint {

    @GET
    @Path("profile")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UserOpDescription.USER_GET_PROFILE_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.USER_NOTES,
            nickname = "getUserProfileInWorkspace")
    UserProfileResponse getProfileInWorkspace(@PathParam("workspaceId") Long workspaceId);

    @PUT
    @Path("profile")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UserOpDescription.USER_PUT_PROFILE_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.USER_NOTES,
            nickname = "modifyUserProfileInWorkspace")
    void modifyProfileInWorkspace(@PathParam("workspaceId") Long workspaceId, UserProfileRequest userProfileRequest);
}
