package com.sequenceiq.distrox.api.v1.distrox.endpoint;

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

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSetRequest;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXCcmUpgradeV1Response;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Response;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.rds.DistroXRdsUpgradeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.rds.DistroXRdsUpgradeV1Response;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@Path("/v1/distrox")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/distrox")
public interface DistroXUpgradeV1Endpoint {

    @PUT
    @Path("{name}/rds_upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Upgrades the external database of the distrox cluster", operationId = "upgradeDistroXRdsByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DistroXRdsUpgradeV1Response upgradeRdsByName(@PathParam("name") String clusterName, @Valid DistroXRdsUpgradeV1Request distroxRdsUpgradeRequest);

    @PUT
    @Path("/crn/{crn}/rds_upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Upgrades the external database of the distrox cluster", operationId = "upgradeDistroXRdsByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DistroXRdsUpgradeV1Response upgradeRdsByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String clusterCrn,
            @Valid DistroXRdsUpgradeV1Request distroxRdsUpgradeRequest);

    @POST
    @Path("{name}/upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Upgrades the distrox cluster", operationId = "upgradeDistroxCluster",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DistroXUpgradeV1Response upgradeClusterByName(@PathParam("name") String name, @Valid DistroXUpgradeV1Request distroxUpgradeRequest);

    @POST
    @Path("/crn/{crn}/upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Upgrades the distrox cluster", operationId = "upgradeDistroxClusterByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DistroXUpgradeV1Response upgradeClusterByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn,
            @Valid DistroXUpgradeV1Request distroxUpgradeRequest);

    @POST
    @Path("{name}/prepare_upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "prepares the distrox cluster for upgrade", operationId = "prepareDistroxClusterUpgrade",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DistroXUpgradeV1Response prepareClusterUpgradeByName(@PathParam("name") String name, @Valid DistroXUpgradeV1Request distroxUpgradeRequest);

    @POST
    @Path("/crn/{crn}/prepare_upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "prepares the distrox cluster for upgrade", operationId = "prepareDistroxClusterUpgradeByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DistroXUpgradeV1Response prepareClusterUpgradeByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn,
            @Valid DistroXUpgradeV1Request distroxUpgradeRequest);

    @POST
    @Path("internal/{name}/upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Upgrades the distrox cluster internal", operationId = "upgradeDistroxClusterInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DistroXUpgradeV1Response upgradeClusterByNameInternal(@PathParam("name") String name, @Valid DistroXUpgradeV1Request distroxUpgradeRequest,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn, @QueryParam("rollingUpgradeEnabled") Boolean rollingUpgradeEnabled);

    @POST
    @Path("internal/crn/{crn}/upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Upgrades the distrox cluster internal", operationId = "upgradeDistroxClusterByCrnInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DistroXUpgradeV1Response upgradeClusterByCrnInternal(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn,
            @Valid DistroXUpgradeV1Request distroxUpgradeRequest,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn, @QueryParam("rollingUpgradeEnabled") Boolean rollingUpgradeEnabled);

    @PUT
    @Path("internal/crn/{crn}/upgrade_ccm")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Initiates the CCM tunnel type upgrade to the latest available version", operationId = "upgradeCcmByDatahubCrnInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DistroXCcmUpgradeV1Response upgradeCcmByCrnInternal(@NotEmpty @ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn,
            @ValidCrn(resource = { CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER })
            @NotEmpty @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @POST
    @Path("internal/{crn}/os_upgrade_by_upgrade_sets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Upgrades distrox cluster OS by name and upgrades sets internal", operationId = "osUpgradeByUpgradeSetsInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier osUpgradeByUpgradeSetsInternal(@PathParam("crn") String crn, OrderedOSUpgradeSetRequest orderedOsUpgradeSetRequest);

}
