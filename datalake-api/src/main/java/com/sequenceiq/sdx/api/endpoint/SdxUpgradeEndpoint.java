package com.sequenceiq.sdx.api.endpoint;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.springframework.validation.annotation.Validated;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.sdx.api.model.SdxCcmUpgradeResponse;
import com.sequenceiq.sdx.api.model.SdxUpgradeDatabaseServerRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeDatabaseServerResponse;
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
    @ApiOperation(value = "upgrades the data lake cluster", nickname = "upgradeDatalakeCluster")
    SdxUpgradeResponse upgradeClusterByName(@PathParam("name") String name, @Valid SdxUpgradeRequest upgradeSdxClusterRequest);

    @POST
    @Path("/crn/{crn}/upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "upgrades the data lake cluster", nickname = "upgradeDatalakeClusterByCrn")
    SdxUpgradeResponse upgradeClusterByCrn(@PathParam("crn") String crn,
            @Valid SdxUpgradeRequest upgradeSdxClusterRequest);

    @POST
    @Path("{name}/prepare_upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "prepares the data lake cluster for upgrade", nickname = "prepareDatalakeClusterUpgrade")
    SdxUpgradeResponse prepareClusterUpgradeByName(@PathParam("name") String name, @Valid SdxUpgradeRequest distroxUpgradeRequest);

    @POST
    @Path("/crn/{crn}/prepare_upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "prepares the data lake cluster for upgrade", nickname = "prepareDatalakeClusterUpgradeByCrn")
    SdxUpgradeResponse prepareClusterUpgradeByCrn(@PathParam("crn") String crn, @Valid SdxUpgradeRequest distroxUpgradeRequest);

    @PUT
    @Path("/internal/upgrade_ccm")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Upgrade Cluster Connectivity Manager", nickname = "upgradeCcm")
    SdxCcmUpgradeResponse upgradeCcm(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @QueryParam("environment") @NotEmpty String environmentCrn,
            @ValidCrn(resource = { CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER })
            @NotEmpty @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @PUT
    @Path("{name}/upgrade_rds")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Upgrades the database server of the data lake", nickname = "upgradeDatalakeDatabaseByName")
    SdxUpgradeDatabaseServerResponse upgradeDatabaseServerByName(@PathParam("name") String clusterName, @Valid SdxUpgradeDatabaseServerRequest
            sdxUpgradeDatabaseServerRequest);

    @PUT
    @Path("/crn/{crn}/upgrade_rds")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Upgrades the database server of the data lake", nickname = "upgradeDatalakeDatabaseByCrn")
    SdxUpgradeDatabaseServerResponse upgradeDatabaseServerByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATALAKE) @PathParam("crn") String clusterCrn,
            @Valid SdxUpgradeDatabaseServerRequest sdxUpgradeDatabaseServerRequest);

}
