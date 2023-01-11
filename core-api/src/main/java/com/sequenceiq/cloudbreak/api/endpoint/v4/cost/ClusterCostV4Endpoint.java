package com.sequenceiq.cloudbreak.api.endpoint.v4.cost;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.cost.requests.ClusterCostV4Request;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCostResponse;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;

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
    @ApiOperation(value = OperationDescriptions.CostOpDescription.LIST, produces = MediaType.APPLICATION_JSON,
            notes = Notes.CLUSTER_COST_NOTES, nickname = "listClusterCostV4")
    RealTimeCostResponse list(List<String> clusterCrns, @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @PUT
    @Path("environment")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.CostOpDescription.LIST_BY_ENV, produces = MediaType.APPLICATION_JSON,
            notes = Notes.CLUSTER_COST_NOTES, nickname = "listClusterCostByEnvV4")
    RealTimeCostResponse listByEnv(ClusterCostV4Request clusterCostV4Request, @QueryParam("initiatorUserCrn") String initiatorUserCrn);
}
