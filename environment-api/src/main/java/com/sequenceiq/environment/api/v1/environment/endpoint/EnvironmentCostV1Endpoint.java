package com.sequenceiq.environment.api.v1.environment.endpoint;

import static com.sequenceiq.environment.api.doc.environment.EnvironmentDescription.ENVIRONMENT_NOTES;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.common.cost.EnvironmentRealTimeCostResponse;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.environment.api.doc.environment.EnvironmentOpDescription;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRealTimeCostRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/v1/env/cost")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/env/cost")
public interface EnvironmentCostV1Endpoint {

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.LIST, description = ENVIRONMENT_NOTES, operationId = "listEnvironmentCostV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    EnvironmentRealTimeCostResponse list(EnvironmentRealTimeCostRequest request);

}
