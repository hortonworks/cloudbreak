package com.sequenceiq.sdx.api.endpoint;

import static com.sequenceiq.common.api.util.versionchecker.VersionCheckerModelDescription.CHECK_CLIENT_VERSION;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.common.api.util.UtilControllerDescription;
import com.sequenceiq.common.api.util.versionchecker.VersionCheckResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/sdx/utils")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/sdx/utils", description = UtilControllerDescription.UTIL_DESCRIPTION)
public interface UtilEndpoint {

    @GET
    @Path("client")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CHECK_CLIENT_VERSION, operationId = "checkClientVersionOfSdx",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    VersionCheckResult checkClientVersion(@QueryParam("version") String version);
}
