package com.sequenceiq.freeipa.api.v1.co2;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.common.co2.RealTimeCO2Response;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaNotes;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v1/freeipa/carbon_dioxide")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/freeipa/carbon_dioxide")
public interface FreeIpaCO2V1Endpoint {

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.CO2, description = FreeIpaNotes.FREEIPA_NOTES, operationId = "listFreeIpaCO2V1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RealTimeCO2Response list(List<String> environmentCrns, @QueryParam("initiatorUserCrn") String initiatorUserCrn);
}
