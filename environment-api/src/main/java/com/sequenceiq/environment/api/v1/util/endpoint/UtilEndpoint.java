package com.sequenceiq.environment.api.v1.util.endpoint;

import static com.sequenceiq.common.api.util.versionchecker.VersionCheckerModelDescription.CHECK_CLIENT_VERSION;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.common.api.util.UtilControllerDescription;
import com.sequenceiq.common.api.util.versionchecker.VersionCheckResult;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/utils")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/utils", description = UtilControllerDescription.UTIL_DESCRIPTION, protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface UtilEndpoint {

    @GET
    @Path("client")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CHECK_CLIENT_VERSION, produces = MediaType.APPLICATION_JSON, nickname = "checkClientVersionOfEnvironmentV1")
    VersionCheckResult checkClientVersion(@QueryParam("version") String version);
}
