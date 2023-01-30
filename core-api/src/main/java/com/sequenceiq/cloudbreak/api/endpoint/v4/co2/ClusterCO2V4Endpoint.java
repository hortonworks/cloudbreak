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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/v4/carbon_dioxide")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "/v4/carbon_dioxide", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface ClusterCO2V4Endpoint {

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.CO2OpDescription.LIST, produces = MediaType.APPLICATION_JSON,
            notes = Notes.CLUSTER_CO2_NOTES, nickname = "listClusterCO2V4")
    RealTimeCO2Response list(List<String> clusterCrns, @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @PUT
    @Path("environment")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.CO2OpDescription.LIST_BY_ENV, produces = MediaType.APPLICATION_JSON,
            notes = Notes.CLUSTER_CO2_NOTES, nickname = "listClusterCO2ByEnvV4")
    RealTimeCO2Response listByEnv(ClusterCO2V4Request clusterCO2V4Request, @QueryParam("initiatorUserCrn") String initiatorUserCrn);
}
