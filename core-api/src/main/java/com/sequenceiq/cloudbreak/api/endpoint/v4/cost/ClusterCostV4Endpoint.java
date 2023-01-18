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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v4/cost")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "/v4/cost")
public interface ClusterCostV4Endpoint {

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = OperationDescriptions.CostOpDescription.LIST,
            description = Notes.CLUSTER_COST_NOTES, operationId = "listClusterCostV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RealTimeCostResponse list(List<String> clusterCrns, @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @PUT
    @Path("environment")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = OperationDescriptions.CostOpDescription.LIST_BY_ENV,
            description = Notes.CLUSTER_COST_NOTES, operationId = "listClusterCostByEnvV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RealTimeCostResponse listByEnv(ClusterCostV4Request clusterCostV4Request, @QueryParam("initiatorUserCrn") String initiatorUserCrn);
}
