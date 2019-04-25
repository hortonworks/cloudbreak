package com.sequenceiq.environment.api.endpoint;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.environment.api.model.WelcomeResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/env")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/env", protocols = "http,https")
public interface EnvironmentEndpoint {

    @GET
    @Path("welcome")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "welcome", produces = MediaType.APPLICATION_JSON, nickname = "getWelcomeMessage")
    WelcomeResponse welcome();

}
