package com.sequenceiq.sdx.api.endpoint;

import java.util.List;
import java.util.Set;

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

import com.sequenceiq.sdx.api.model.RedeploySdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/sdx")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/sdx", protocols = "http,https")
public interface SdxEndpoint {

    @POST
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "create SDX cluster", produces = "application/json", nickname = "createSdx")
    SdxClusterResponse create(@PathParam("name") String name, @Valid SdxClusterRequest createSdxClusterRequest);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "delete SDX cluster", produces = "application/json", nickname = "deleteSdx")
    void delete(@PathParam("name") String name);

    @POST
    @Path("{name}/redeploy")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "redeploy SDX cluster", produces = "application/json", nickname = "redeploySdx")
    void redeploy(@PathParam("name") String name, @Valid RedeploySdxClusterRequest redeploySdxClusterRequest);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "get SDX cluster", produces = "application/json", nickname = "getSdx")
    SdxClusterResponse get(@PathParam("name") String name);

    @GET
    @Path("{name}/detail")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "get SDX cluster detail", produces = "application/json", nickname = "getSdxDetail")
    SdxClusterDetailResponse getDetail(@PathParam("name") String name, @QueryParam("entries") Set<String> entries);

    @GET
    @Path("list")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "list SDX clusters", produces = "application/json", nickname = "listSdx")
    List<SdxClusterResponse> list(@QueryParam("envName") String envName);
}
