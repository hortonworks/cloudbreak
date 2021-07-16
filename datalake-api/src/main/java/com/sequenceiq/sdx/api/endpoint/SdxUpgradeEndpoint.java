package com.sequenceiq.sdx.api.endpoint;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.validation.annotation.Validated;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.sdx.api.model.SdxRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRecoveryResponse;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Validated
@Path("/sdx")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/sdx", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface SdxUpgradeEndpoint {

    @POST
    @Path("{name}/upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "upgrades the datalake cluster", nickname = "upgradeDatalakeCluster")
    SdxUpgradeResponse upgradeClusterByName(@PathParam("name") String name, @Valid SdxUpgradeRequest upgradeSdxClusterRequest);

    @POST
    @Path("/crn/{crn}/upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "upgrades the datalake cluster", nickname = "upgradeDatalakeClusterByCrn")
    SdxUpgradeResponse upgradeClusterByCrn(@PathParam("crn") String crn,  @Valid SdxUpgradeRequest upgradeSdxClusterRequest);

    @POST
    @Path("{name}/recover_failed_upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "recovers the failed datalake upgrade", nickname = "recoverDatalakeCluster")
    SdxRecoveryResponse recoverClusterByName(@PathParam("name") String name, @Valid SdxRecoveryRequest recoverSdxClusterRequest);

    @POST
    @Path("/crn/{crn}/recover_failed_upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "recovers the failed datalake upgrade", nickname = "recoverDatalakeClusterByCrn")
    SdxRecoveryResponse recoverClusterByCrn(@PathParam("crn") String crn,  @Valid SdxRecoveryRequest recoverSdxClusterRequest);

}
