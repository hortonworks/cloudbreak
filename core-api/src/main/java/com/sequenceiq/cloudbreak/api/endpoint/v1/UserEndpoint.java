package com.sequenceiq.cloudbreak.api.endpoint.v1;

import java.util.SortedSet;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.users.UserJson;
import com.sequenceiq.cloudbreak.api.model.users.UserProfileRequest;
import com.sequenceiq.cloudbreak.api.model.users.UserProfileResponse;
import com.sequenceiq.cloudbreak.api.model.users.UserResponseJson;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.UserOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/users")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/users", description = ControllerDescription.USER_DESCRIPTION, protocols = "http,https")
public interface UserEndpoint {

    @DELETE
    @Path("evict")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UserOpDescription.CURRENT_USER_DETAILS_EVICT, produces = ContentType.JSON, notes = Notes.USER_NOTES,
            nickname = "evictCurrentUserDetails")
    UserJson evictCurrentUserDetails();

    @GET
    @Path("profile")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UserOpDescription.USER_GET_PROFILE, produces = ContentType.JSON, notes = Notes.USER_NOTES,
            nickname = "getUserProfile")
    UserProfileResponse getProfile();

    @PUT
    @Path("profile")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UserOpDescription.USER_PUT_PROFILE, produces = ContentType.JSON, notes = Notes.USER_NOTES,
            nickname = "modifyProfile")
    void modifyProfile(UserProfileRequest userProfileRequest);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UserOpDescription.GET_TENANT_USERS, produces = ContentType.JSON, notes = Notes.USER_NOTES,
            nickname = "getAllUsers")
    SortedSet<UserResponseJson> getAll();
}
