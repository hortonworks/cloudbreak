package com.sequenceiq.freeipa.api.model.endpoint;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.freeipa.api.model.ContentType;
import com.sequenceiq.freeipa.api.model.freeipa.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.model.freeipa.CreateFreeIpaResponse;
import com.sequenceiq.freeipa.api.model.freeipa.FreeIpaDetailsResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/freeipa/{environmentName}")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/freeipa/{environmentName}", description = "FreeIPA endpoint", protocols = "http,https")
public interface FreeIpaEndpoint {

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Creates FreeIPA instance", produces = ContentType.JSON, nickname = "createFreeIPA")
    CreateFreeIpaResponse create(@PathParam("environmentName") String environmentName, CreateFreeIpaRequest request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Delete FreeIPA instance", produces = ContentType.JSON, nickname = "deleteFreeIPA")
    void delete(@PathParam("environmentName") String environmentName, @PathParam("name") String name);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "gets get of FreeIPA for an environment", produces = ContentType.JSON, nickname = "getFreeIPADetails")
    FreeIpaDetailsResponse get(@PathParam("environmentName") String environmentName, @PathParam("name") String name);

}
