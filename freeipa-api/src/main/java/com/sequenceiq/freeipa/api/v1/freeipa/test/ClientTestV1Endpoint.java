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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v1/freeipa/test")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/freeipa/test", description = "Client test endpoint")
public interface ClientTestV1Endpoint {

    @GET
    @Path("{id}/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieves user information", operationId = "userShowV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    String userShow(@PathParam("id") Long id, @PathParam("name") String name);

    @POST
    @Path("check_users")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Checks if users exists in FreeIPA of the environment", operationId = "checkUsers",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ClientTestBaseV1Response checkUsers(@Valid CheckUsersV1Request checkUsersRequest);

    @POST
    @Path("check_groups")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Checks if groups exists in FreeIPA of the environment", operationId = "checkGroups",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ClientTestBaseV1Response checkGroups(@Valid CheckGroupsV1Request checkGroupsRequest);

    @POST
    @Path("check_users_in_group")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Checks if users exists in specific group of FreeIPA of the environment", operationId = "checkUsersInGroup",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ClientTestBaseV1Response checkUsersInGroup(@Valid CheckUsersInGroupV1Request checkUsersInGroupRequest);
}
