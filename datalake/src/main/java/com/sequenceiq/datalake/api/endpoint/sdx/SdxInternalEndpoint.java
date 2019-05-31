package com.sequenceiq.datalake.api.endpoint.sdx;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/sdxinternal")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/sdxinternal", protocols = "http,https")
public interface SdxInternalEndpoint {

    @POST
    @Path("{sdxName}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "create internal SDX cluster", produces = "application/json", nickname = "createInternalSdx")
    SdxClusterResponse create(@PathParam("sdxName") String sdxName, @Valid SdxInternalClusterRequest createSdxClusterRequest);

}
