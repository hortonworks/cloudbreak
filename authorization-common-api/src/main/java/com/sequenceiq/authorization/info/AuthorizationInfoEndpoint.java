package com.sequenceiq.authorization.info;

import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.authorization.info.model.ApiAuthorizationInfo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/authorization")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/authorization", description = "API about authorization informations")
public interface AuthorizationInfoEndpoint {

    @GET
    @Path("info")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "list of required permissions for APIs", operationId = "authorizationInfo",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Set<ApiAuthorizationInfo> info();

}
