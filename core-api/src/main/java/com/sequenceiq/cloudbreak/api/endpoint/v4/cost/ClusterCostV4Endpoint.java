package com.sequenceiq.cloudbreak.api.endpoint.v4.cost;

import static com.sequenceiq.environment.api.doc.environment.EnvironmentDescription.ENVIRONMENT_NOTES;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.common.cost.RealTimeCostResponse;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.environment.api.doc.environment.EnvironmentOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/v4/cost")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "/v4/cost", protocols = "http,https",
        consumes = MediaType.APPLICATION_JSON)
public interface ClusterCostV4Endpoint {

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.LIST, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES, nickname = "listDistroXCostV1")
    RealTimeCostResponse list(List<String> clusterCrns, @QueryParam("initiatorUserCrn") String initiatorUserCrn);
}
