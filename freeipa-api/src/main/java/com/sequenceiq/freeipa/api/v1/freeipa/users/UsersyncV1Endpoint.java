package com.sequenceiq.freeipa.api.v1.freeipa.users;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.freeipa.api.v1.freeipa.users.model.SynchronizeUsersRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.users.model.SynchronizeUsersResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.users.model.SynchronizeUsersStatus;
import com.sequenceiq.service.api.doc.ContentType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/freeipa/usersync")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/freeipa/usersync", description = "Synchronize users to FreeIPA", protocols = "http,https")
public interface UsersyncV1Endpoint {
    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Synchronize users to FreeIPA", produces = ContentType.JSON, nickname = "synchronizeUsersV1")
    SynchronizeUsersResponse synchronizeUsers(SynchronizeUsersRequest request);

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get status of latest synchronization", produces = ContentType.JSON, nickname = "getSyncUsersStatusV1")
    SynchronizeUsersStatus getStatus();
}
