package com.sequenceiq.environment.api.v1.environment.endpoint;

import static com.sequenceiq.environment.api.doc.environment.EnvironmentDescription.ENVIRONMENT_NOTES;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.common.co2.EnvironmentRealTimeCO2Response;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.environment.api.doc.environment.EnvironmentOpDescription;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRealTimeCO2Request;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/env/carbon_dioxide")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/env/carbon_dioxide", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface EnvironmentCO2V1Endpoint {

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.LIST, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES, nickname = "listEnvironmentCO2V1")
    EnvironmentRealTimeCO2Response list(EnvironmentRealTimeCO2Request request);
}
