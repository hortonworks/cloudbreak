package com.sequenceiq.cloudbreak.api.endpoint.v4.co2;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.co2.requests.ClusterCO2V4Request;
import com.sequenceiq.cloudbreak.common.co2.RealTimeCO2Response;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v4/carbon_dioxide")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "/v4/carbon_dioxide")
public interface ClusterCO2V4Endpoint {

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = OperationDescriptions.CO2OpDescription.LIST, description = Notes.CLUSTER_CO2_NOTES, operationId = "listClusterCO2V4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RealTimeCO2Response list(List<String> clusterCrns, @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @PUT
    @Path("environment")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = OperationDescriptions.CO2OpDescription.LIST_BY_ENV, description = Notes.CLUSTER_CO2_NOTES, operationId = "listClusterCO2ByEnvV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RealTimeCO2Response listByEnv(ClusterCO2V4Request clusterCO2V4Request, @QueryParam("initiatorUserCrn") String initiatorUserCrn);
}
