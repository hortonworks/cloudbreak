package com.sequenceiq.cloudbreak.api.endpoint.v4.support;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.support.response.DataHubPlatformSupportRequirements;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Path("/v1/support")
@Tag(name = "/v1/support", description = ControllerDescription.SUPPORT_V1_DESCRIPTION)
public interface SupportV1Endpoint {

    @GET
    @Path("/internal/defaults/{cloudPlatform}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "get instances by platform",
            operationId = "getPlatformSupportRequirementsByPlatform",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DataHubPlatformSupportRequirements getInstanceTypesByPlatform(@PathParam("cloudPlatform") String cloudPlatform);

}
