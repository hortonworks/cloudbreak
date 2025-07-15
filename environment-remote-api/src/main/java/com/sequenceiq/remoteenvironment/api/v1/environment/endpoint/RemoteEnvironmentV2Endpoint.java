package com.sequenceiq.remoteenvironment.api.v1.environment.endpoint;

import static com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentOpDescription.DESCRIBE_BY_CRN;
import static com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentOpDescription.ENVIRONMENT_NOTES;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.springframework.validation.annotation.Validated;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.remoteenvironment.DescribeEnvironmentV2Response;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.DescribeRemoteEnvironment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@Path("/v2/env")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v2/env", description = "Remote Environment V2 operations")
public interface RemoteEnvironmentV2Endpoint {

    @POST
    @Path("/crn")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DESCRIBE_BY_CRN, description = ENVIRONMENT_NOTES, operationId = "getRemoteEnvironmentV2ByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DescribeEnvironmentV2Response getByCrn(@Valid DescribeRemoteEnvironment request);
}