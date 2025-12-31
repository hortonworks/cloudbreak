package com.sequenceiq.freeipa.api.v1.support;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.freeipa.api.v1.support.response.FreeIpaPlatformSupportRequirements;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Path("/v1/support")
@Tag(name = "/v1/support")
public interface SupportV1Endpoint {

    @GET
    @Path("/internal/defaults/{cloudPlatform}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "get instances by platform",
            operationId = "getPlatformSupportRequirementsByPlatform",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FreeIpaPlatformSupportRequirements getInstanceTypesByPlatform(@PathParam("cloudPlatform") String cloudPlatform);
}
