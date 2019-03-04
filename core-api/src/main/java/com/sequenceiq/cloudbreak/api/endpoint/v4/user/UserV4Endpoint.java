package com.sequenceiq.cloudbreak.api.endpoint.v4.user;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.user.responses.UserEvictV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.user.responses.UserV4Responses;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.UserOpDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.UserProfileOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v4/users")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/users", description = ControllerDescription.USER_DESCRIPTION, protocols = "http,https")
public interface UserV4Endpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UserOpDescription.GET_TENANT_USERS, produces = ContentType.JSON, notes = Notes.USER_NOTES,
            nickname = "getAllUsers")
    UserV4Responses getAll();

    @DELETE
    @Path("evict")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UserProfileOpDescription.CURRENT_USER_DETAILS_EVICT, produces = ContentType.JSON,
            notes = Notes.USER_PROFILE_NOTES, nickname = "evictCurrentUserDetails")
    UserEvictV4Response evictCurrentUserDetails();

}
