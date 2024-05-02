package com.sequenceiq.remoteenvironment.api.v1.environment.endpoint;

import static com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentOpDescription.ENVIRONMENT_NOTES;
import static com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentOpDescription.GET_BY_CRN;
import static com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentOpDescription.LIST;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.springframework.validation.annotation.Validated;

import com.cloudera.cdp.environments2.model.DescribeEnvironmentResponse;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponses;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@Path("/v1/env")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/env", description = "Remote Environment operations")
public interface RemoteEnvironmentEndpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = LIST, description = ENVIRONMENT_NOTES, operationId = "listRemoteEnvironmentsV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SimpleRemoteEnvironmentResponses list();

    @GET
    @Path("/crn/{crn:.+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = GET_BY_CRN, description = ENVIRONMENT_NOTES, operationId = "getRemoteEnvironmentV1ByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DescribeEnvironmentResponse getByCrn(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("crn") String crn);

}