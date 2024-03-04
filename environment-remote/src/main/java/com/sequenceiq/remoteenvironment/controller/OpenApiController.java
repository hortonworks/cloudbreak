package com.sequenceiq.remoteenvironment.controller;


import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource;
import io.swagger.v3.oas.annotations.Operation;

@Path("/openapi.{type:json|yaml}")
public class OpenApiController extends BaseOpenApiResource {
    @GET
    @Produces({MediaType.APPLICATION_JSON, "application/yaml"})
    @Operation(hidden = true)
    public Response getOpenApi(@Context HttpHeaders headers, @Context UriInfo uriInfo, @PathParam("type") String type) throws Exception {
        return super.getOpenApi(headers, null, null, uriInfo, type);
    }
}
