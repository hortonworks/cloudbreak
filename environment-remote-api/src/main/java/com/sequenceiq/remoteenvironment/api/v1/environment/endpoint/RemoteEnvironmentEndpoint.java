package com.sequenceiq.remoteenvironment.api.v1.environment.endpoint;

import static com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentOpDescription.DATALAKE_SERVICES_NOTES;
import static com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentOpDescription.ENVIRONMENT_NOTES;
import static com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentOpDescription.LIST;
import static com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentOpDescription.POST_BY_CRN;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.springframework.validation.annotation.Validated;

import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextResponse;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesRequest;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesResponse;
import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.DescribeRemoteEnvironment;
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

    @POST
    @Path("/crn")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = POST_BY_CRN, description = ENVIRONMENT_NOTES, operationId = "getRemoteEnvironmentV1ByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DescribeEnvironmentResponse getByCrn(@Valid DescribeRemoteEnvironment request);

    @POST
    @Path("/rdc")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = POST_BY_CRN, description = ENVIRONMENT_NOTES, operationId = "getRdcV1ByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DescribeDatalakeAsApiRemoteDataContextResponse getRdcByCrn(@Valid DescribeRemoteEnvironment request);

    @POST
    @Path("/datalakeServices")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = POST_BY_CRN, description = DATALAKE_SERVICES_NOTES, operationId = "getDatalakeByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DescribeDatalakeServicesResponse getDatalakeServicesByCrn(@Valid DescribeDatalakeServicesRequest request);

}