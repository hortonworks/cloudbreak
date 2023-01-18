package com.sequenceiq.cloudbreak.service.openapi;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource;
import io.swagger.v3.oas.annotations.Operation;

@Path("/openapi.{type:json|yaml}")
public class OpenApiController extends BaseOpenApiResource {
    @GET
    @Produces({MediaType.APPLICATION_JSON, "application/yaml"})
    @Operation(hidden = true)
    public Response getOpenApi(@Context HttpHeaders headers,
            @Context UriInfo uriInfo,
            @PathParam("type") String type) throws Exception {

        return super.getOpenApi(headers, null, null, uriInfo, type);
    }
}
