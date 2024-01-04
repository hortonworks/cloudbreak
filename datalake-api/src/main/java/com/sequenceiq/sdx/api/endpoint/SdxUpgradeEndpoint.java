package com.sequenceiq.sdx.api.endpoint;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.springframework.validation.annotation.Validated;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.sdx.api.model.SdxCcmUpgradeResponse;
import com.sequenceiq.sdx.api.model.SdxUpgradeDatabaseServerRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeDatabaseServerResponse;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@Path("/sdx")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/sdx")
public interface SdxUpgradeEndpoint {

    @POST
    @Path("{name}/upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "upgrades the data lake cluster", operationId = "upgradeDatalakeCluster",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SdxUpgradeResponse upgradeClusterByName(@PathParam("name") String name, @Valid SdxUpgradeRequest upgradeSdxClusterRequest);

    @POST
    @Path("/crn/{crn}/upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "upgrades the data lake cluster", operationId = "upgradeDatalakeClusterByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SdxUpgradeResponse upgradeClusterByCrn(@PathParam("crn") String crn, @Valid SdxUpgradeRequest upgradeSdxClusterRequest);

    @POST
    @Path("{name}/prepare_upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "prepares the data lake cluster for upgrade", operationId = "prepareDatalakeClusterUpgrade",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SdxUpgradeResponse prepareClusterUpgradeByName(@PathParam("name") String name, @Valid SdxUpgradeRequest distroxUpgradeRequest);

    @POST
    @Path("/crn/{crn}/prepare_upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "prepares the data lake cluster for upgrade", operationId = "prepareDatalakeClusterUpgradeByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SdxUpgradeResponse prepareClusterUpgradeByCrn(@PathParam("crn") String crn, @Valid SdxUpgradeRequest distroxUpgradeRequest);

    @PUT
    @Path("/internal/upgrade_ccm")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Upgrade Cluster Connectivity Manager", operationId = "upgradeCcm",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SdxCcmUpgradeResponse upgradeCcm(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @QueryParam("environment") @NotEmpty String environmentCrn,
            @ValidCrn(resource = { CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER })
            @NotEmpty @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @PUT
    @Path("{name}/upgrade_rds")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Upgrades the database server of the data lake", operationId = "upgradeDatalakeDatabaseByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SdxUpgradeDatabaseServerResponse upgradeDatabaseServerByName(@PathParam("name") String clusterName,
            @Valid SdxUpgradeDatabaseServerRequest sdxUpgradeDatabaseServerRequest);

    @PUT
    @Path("/crn/{crn}/upgrade_rds")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Upgrades the database server of the data lake", operationId = "upgradeDatalakeDatabaseByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SdxUpgradeDatabaseServerResponse upgradeDatabaseServerByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATALAKE) @PathParam("crn") String clusterCrn,
            @Valid SdxUpgradeDatabaseServerRequest sdxUpgradeDatabaseServerRequest);
}
