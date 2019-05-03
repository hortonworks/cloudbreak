package com.sequenceiq.freeipa.api.model.users;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.freeipa.api.model.ContentType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/usersync")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/usersync", description = "Synchronize users to FreeIPA", protocols = "http,https")
public interface UsersyncEndpoint {

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Synchronize users to FreeIPA", produces = ContentType.JSON, nickname = "synchronizeUsers")
    SynchronizeUsersResponse synchronizeUsers(SynchronizeUsersRequest request);

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get status of latest synchronization", produces = ContentType.JSON, nickname = "getStatus")
    SynchronizeUsersStatus getStatus();
}
