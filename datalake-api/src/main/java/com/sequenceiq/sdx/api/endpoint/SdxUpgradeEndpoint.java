package com.sequenceiq.sdx.api.endpoint;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.validation.annotation.Validated;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeOptionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeOptionsV4Response;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Validated
@Path("/sdx")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/sdx", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface SdxUpgradeEndpoint {

    @GET
    @Path("{name}/check_for_upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "checks for upgrade options by name", nickname = "checkForUpgrade")
    UpgradeOptionV4Response checkForUpgradeByName(@PathParam("name") String name);

    @GET
    @Path("/crn/{crn}/check_for_upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "checks for upgrade options by crn", nickname = "checkForUpgradeByCrn")
    UpgradeOptionV4Response checkForUpgradeByCrn(@PathParam("crn") String crn);

    @POST
    @Path("{name}/upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "upgrades the datalake cluster to the latest images", nickname = "upgradeDatalakeCluster")
    FlowIdentifier upgradeClusterByName(@PathParam("name") String name);

    @POST
    @Path("/crn/{crn}/upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "upgrades the datalake cluster to the latest images", nickname = "upgradeDatalakeClusterByCrn")
    FlowIdentifier upgradeClusterByCrn(@PathParam("crn") String crn);

    @GET
    @Path("{name}/check_cluster_upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "checks for cluster upgrade options by name", nickname = "checkForClusterUpgradeByName")
    UpgradeOptionsV4Response checkForClusterUpgradeByName(@PathParam("name") String name);

    @GET
    @Path("/crn/{crn}/check_cluster_upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "checks for cluster upgrade options by crn", nickname = "checkForClusterUpgradeByCrn")
    UpgradeOptionsV4Response checkForClusterUpgradeByCrn(@PathParam("crn") String crn);

    @POST
    @Path("{name}/cluster_upgrade/image/{image}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "upgrades the datalake cluster by name", nickname = "upgradeClusterByName")
    FlowIdentifier upgradeClusterByName(@PathParam("name") String name, @PathParam("image") String imageId);

    @POST
    @Path("/crn/{crn}/cluster_upgrade/image/{image}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "upgrades the datalake cluster by crn", nickname = "upgradeClusterByCrn")
    FlowIdentifier upgradeClusterByCrn(@PathParam("crn") String crn, @PathParam("image") String imageId);
}
