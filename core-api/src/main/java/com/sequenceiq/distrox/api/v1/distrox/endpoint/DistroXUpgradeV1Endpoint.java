package com.sequenceiq.distrox.api.v1.distrox.endpoint;

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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Validated
@Path("/v1/distrox")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/distrox", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface DistroXUpgradeV1Endpoint {

    @PUT
    @Path("{name}/rds_upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Upgrades the external database of the distrox cluster", nickname = "upgradeDistroXRdsByName")
    DistroXRdsUpgradeV1Response upgradeRdsByName(@PathParam("name") String clusterName, @Valid DistroXRdsUpgradeV1Request distroxRdsUpgradeRequest);

    @PUT
    @Path("/crn/{crn}/rds_upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Upgrades the external database of the distrox cluster", nickname = "upgradeDistroXRdsByCrn")
    DistroXRdsUpgradeV1Response upgradeRdsByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String clusterCrn,
            @Valid DistroXRdsUpgradeV1Request distroxRdsUpgradeRequest);

    @POST
    @Path("{name}/upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Upgrades the distrox cluster", nickname = "upgradeDistroxCluster")
    DistroXUpgradeV1Response upgradeClusterByName(@PathParam("name") String name, @Valid DistroXUpgradeV1Request distroxUpgradeRequest);

    @POST
    @Path("/crn/{crn}/upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Upgrades the distrox cluster", nickname = "upgradeDistroxClusterByCrn")
    DistroXUpgradeV1Response upgradeClusterByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn,
            @Valid DistroXUpgradeV1Request distroxUpgradeRequest);

    @POST
    @Path("{name}/prepare_upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "prepares the distrox cluster for upgrade", nickname = "prepareDistroxClusterUpgrade")
    DistroXUpgradeV1Response prepareClusterUpgradeByName(@PathParam("name") String name, @Valid DistroXUpgradeV1Request distroxUpgradeRequest);

    @POST
    @Path("/crn/{crn}/prepare_upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "prepares the distrox cluster for upgrade", nickname = "prepareDistroxClusterUpgradeByCrn")
    DistroXUpgradeV1Response prepareClusterUpgradeByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn,
            @Valid DistroXUpgradeV1Request distroxUpgradeRequest);

    @POST
    @Path("internal/{name}/upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Upgrades the distrox cluster internal", nickname = "upgradeDistroxClusterInternal")
    DistroXUpgradeV1Response upgradeClusterByNameInternal(@PathParam("name") String name, @Valid DistroXUpgradeV1Request distroxUpgradeRequest,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn, @QueryParam("rollingUpgradeEnabled") Boolean rollingUpgradeEnabled);

    @POST
    @Path("internal/crn/{crn}/upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Upgrades the distrox cluster internal", nickname = "upgradeDistroxClusterByCrnInternal")
    DistroXUpgradeV1Response upgradeClusterByCrnInternal(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn,
            @Valid DistroXUpgradeV1Request distroxUpgradeRequest,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn, @QueryParam("rollingUpgradeEnabled") Boolean rollingUpgradeEnabled);

    @PUT
    @Path("internal/crn/{crn}/upgrade_ccm")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Initiates the CCM tunnel type upgrade to the latest available version", nickname = "upgradeCcmByDatahubCrnInternal")
    DistroXCcmUpgradeV1Response upgradeCcmByCrnInternal(@NotEmpty @ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn,
            @ValidCrn(resource = { CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER })
            @NotEmpty @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @POST
    @Path("internal/{crn}/os_upgrade_by_upgrade_sets")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Upgrades distrox cluster OS by name and upgrades sets internal", nickname = "osUpgradeByUpgradeSetsInternal")
    FlowIdentifier osUpgradeByUpgradeSetsInternal(@PathParam("crn") String crn, OrderedOSUpgradeSetRequest orderedOsUpgradeSetRequest);

}
