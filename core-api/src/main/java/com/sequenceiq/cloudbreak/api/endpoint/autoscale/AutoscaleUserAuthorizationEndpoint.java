package com.sequenceiq.cloudbreak.api.endpoint.autoscale;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.doc.ControllerDescription;

import io.swagger.annotations.Api;

@Path("/autoscale/authorize")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/autoscale/authorize", description = ControllerDescription.AUTOSCALE_DESCRIPTION, protocols = "http,https")
public interface AutoscaleUserAuthorizationEndpoint {

    @GET
    @Path("/stack/{id}/{owner}/{permission}")
    @Produces(MediaType.APPLICATION_JSON)
    Boolean authorizeForAutoscale(@PathParam("id") Long id, @PathParam("owner") String owner, @PathParam("permission") String permission);

}
