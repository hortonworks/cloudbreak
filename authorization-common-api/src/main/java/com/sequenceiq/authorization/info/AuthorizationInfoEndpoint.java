package com.sequenceiq.authorization.info;

import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.authorization.info.model.ApiAuthorizationInfo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/authorization")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/authorization", description = "API about authorization informations",
        protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface AuthorizationInfoEndpoint {

    @GET
    @Path("info")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "list of required permissions for APIs", produces = MediaType.APPLICATION_JSON,
            nickname = "authorizationInfo")
    Set<ApiAuthorizationInfo> info();

}
