package com.sequenceiq.environment.api.v1.expressonboarding;

import static com.sequenceiq.environment.api.doc.expressonboarding.ExpressOnboardingOpDescription.DESCRIPTION;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.environment.api.v1.expressonboarding.model.response.DetailedExpressOnboardingRegionResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/v1/express_onboarding")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/express_onboarding")
public interface ExpressOnboardingEndpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get express onboarding response.", description = DESCRIPTION,
            operationId = "getExpressOnboardingV1Response",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DetailedExpressOnboardingRegionResponse get();

}
