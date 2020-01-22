package com.sequenceiq.freeipa.api.v1.freeipa.test;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/v1/freeipa/test")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/freeipa/test", description = "Client test endpoint", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface ClientTestV1Endpoint {
    @GET
    @Path("{id}/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Retrieves user information", produces = MediaType.APPLICATION_JSON, nickname = "userShowV1")
    String userShow(@PathParam("id") Long id, @PathParam("name") String name);
}
