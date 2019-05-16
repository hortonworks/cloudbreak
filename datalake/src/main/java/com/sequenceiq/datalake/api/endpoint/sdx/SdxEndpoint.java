package com.sequenceiq.datalake.api.endpoint.sdx;

import java.util.List;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/sdx")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/sdx", protocols = "http,https")
public interface SdxEndpoint {

    @POST
    @Path("{sdxName}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "create SDX cluster", produces = "application/json", nickname = "createSdx")
    SdxClusterResponse create(@PathParam("sdxName") String sdxName, @Valid SdxClusterRequest createSdxClusterRequest);

    @DELETE
    @Path("{sdxName}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "delete SDX cluster", produces = "application/json", nickname = "deleteSdx")
    void delete(@PathParam("sdxName") String sdxName);

    @POST
    @Path("{sdxName}/redeploy")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "redeploy SDX cluster", produces = "application/json", nickname = "redeploySdx")
    void redeploy(@PathParam("sdxName") String sdxName, @Valid RedeploySdxClusterRequest redeploySdxClusterRequest);

    @GET
    @Path("{sdxName}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "get SDX cluster", produces = "application/json", nickname = "getSdx")
    SdxClusterResponse get(@PathParam("sdxName") String sdxName);

    @GET
    @Path("list")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "list SDX clusters", produces = "application/json", nickname = "listSdx")
    List<SdxClusterResponse> list(@QueryParam("envName") String envName);
}
