package com.sequenceiq.distrox.api.v1.distrox.endpoint;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.validation.annotation.Validated;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroxUpgradeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroxUpgradeV1Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Validated
@Path("/v1/distrox")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/distrox", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface DistroxUpgradeV1Endpoint {

    @POST
    @Path("{name}/upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "upgrades the distrox cluster", nickname = "upgradeDistroxCluster")
    DistroxUpgradeV1Response upgradeClusterByName(@PathParam("name") String name, @Valid DistroxUpgradeV1Request distroxUpgradeRequest);

    @POST
    @Path("/crn/{crn}/upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "upgrades the distrox cluster", nickname = "upgradeDistroxClusterByCrn")
    DistroxUpgradeV1Response upgradeClusterByCrn(@PathParam("crn") String crn,  @Valid DistroxUpgradeV1Request distroxUpgradeRequest);

}
