package com.sequenceiq.freeipa.api.v1.freeipa.user;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.freeipa.api.v1.freeipa.user.doc.UsersyncOperationDescriptions;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SetPasswordRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SetPasswordResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeUsersRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeUsersResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeUsersStatus;
import com.sequenceiq.service.api.doc.ContentType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/freeipa/user")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/freeipa/user", description = "Synchronize users to FreeIPA", protocols = "http,https")
public interface UserV1Endpoint {
    @POST
    @Path("sync")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UsersyncOperationDescriptions.SYNC, produces = ContentType.JSON, nickname = "synchronizeUsersV1")
    SynchronizeUsersResponse synchronizeUsers(SynchronizeUsersRequest request);

    @GET
    @Path("sync")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get status of latest synchronization", produces = ContentType.JSON, nickname = "getSyncUsersStatusV1")
    SynchronizeUsersStatus getStatus();

    @POST
    @Path("/{username}/password")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UsersyncOperationDescriptions.SET_PASSWORD, produces = ContentType.JSON, nickname = "setPasswordV1")
    SetPasswordResponse setPassword(@PathParam("username") String username, SetPasswordRequest request);
}
