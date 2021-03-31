package com.sequenceiq.freeipa.api.v1.freeipa.test;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.freeipa.api.v1.freeipa.test.model.CheckGroupsV1Request;
import com.sequenceiq.freeipa.api.v1.freeipa.test.model.CheckUsersInGroupV1Request;
import com.sequenceiq.freeipa.api.v1.freeipa.test.model.CheckUsersV1Request;
import com.sequenceiq.freeipa.api.v1.freeipa.test.model.ClientTestBaseV1Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/v1/freeipa/test")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/freeipa/test", description = "Client test endpoint", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface ClientTestV1Endpoint {

    @GET
    @Path("{id}/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Retrieves user information", produces = MediaType.APPLICATION_JSON, nickname = "userShowV1")
    String userShow(@PathParam("id") Long id, @PathParam("name") String name);

    @POST
    @Path("check_users")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Checks if users exists in FreeIPA of the environment", produces = MediaType.APPLICATION_JSON, nickname = "checkUsers")
    ClientTestBaseV1Response checkUsers(@Valid CheckUsersV1Request checkUsersRequest);

    @POST
    @Path("check_groups")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Checks if groups exists in FreeIPA of the environment", produces = MediaType.APPLICATION_JSON, nickname = "checkGroups")
    ClientTestBaseV1Response checkGroups(@Valid CheckGroupsV1Request checkGroupsRequest);

    @POST
    @Path("check_users_in_group")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Checks if users exists in specific group of FreeIPA of the environment",
            produces = MediaType.APPLICATION_JSON, nickname = "checkUsersInGroup")
    ClientTestBaseV1Response checkUsersInGroup(@Valid CheckUsersInGroupV1Request checkUsersInGroupRequest);
}
