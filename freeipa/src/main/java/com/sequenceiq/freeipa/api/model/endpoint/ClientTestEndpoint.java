package com.sequenceiq.freeipa.api.model.endpoint;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.freeipa.api.model.ContentType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/test")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/test", description = "Client test endpoint", protocols = "http,https")
public interface ClientTestEndpoint {
    @GET
    @Path("{id}/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Creates FreeIPA instance", produces = ContentType.JSON, nickname = "userShow")
    String userShow(@PathParam("id")Long id, @PathParam("name") String name);
}
