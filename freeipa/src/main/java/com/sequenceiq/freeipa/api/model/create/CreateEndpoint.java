package com.sequenceiq.freeipa.api.model.create;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.freeipa.api.model.ContentType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/create")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/create", description = "Create FreeIPA", protocols = "http,https")
public interface CreateEndpoint {

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Creates FreeIPA instance", produces = ContentType.JSON, nickname = "createFreeIPA")
    CreateFreeIpaResponse create(CreateFreeIpaRequest request);
}
