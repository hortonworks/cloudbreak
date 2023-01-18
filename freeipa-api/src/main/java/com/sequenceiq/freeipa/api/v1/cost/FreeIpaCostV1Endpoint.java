package com.sequenceiq.freeipa.api.v1.cost;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.common.cost.RealTimeCostResponse;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaNotes;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v1/freeipa/cost")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/freeipa/cost")
public interface FreeIpaCostV1Endpoint {

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.COST, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "listFreeIpaCostV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RealTimeCostResponse list(List<String> environmentCrns, @QueryParam("initiatorUserCrn") String initiatorUserCrn);

}
