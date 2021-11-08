package com.sequenceiq.sdx.api.endpoint;


import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.validation.annotation.Validated;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.sdx.api.model.SdxRecoverableResponse;
import com.sequenceiq.sdx.api.model.SdxRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRecoveryResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Validated
@Path("/sdx")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/sdx", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface SdxRecoveryEndpoint {

    @POST
    @Path("{name}/recover")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "recovers the datalake upgrade", nickname = "recoverDatalakeCluster")
    SdxRecoveryResponse recoverClusterByName(@PathParam("name") String name, @Valid SdxRecoveryRequest recoverSdxClusterRequest);

    @POST
    @Path("/crn/{crn}/recover")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "recovers the datalake upgrade", nickname = "recoverDatalakeClusterByCrn")
    SdxRecoveryResponse recoverClusterByCrn(@PathParam("crn") String crn, @Valid SdxRecoveryRequest recoverSdxClusterRequest);

    @GET
    @Path("{name}/recoverable")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "validates if the data lake is recoverable or not", nickname = "getClusterRecoverableByName")
    SdxRecoverableResponse getClusterRecoverableByName(@PathParam("name") String name);

    @GET
    @Path("/crn/{crn}/recoverable")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "validates if the data lake is recoverable or not", nickname = "getClusterRecoverableByCrn")
    SdxRecoverableResponse getClusterRecoverableByCrn(@PathParam("crn") String crn);
}
