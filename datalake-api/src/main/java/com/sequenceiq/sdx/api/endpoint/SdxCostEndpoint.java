package com.sequenceiq.sdx.api.endpoint;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCostResponse;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/sdx/cost")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "/sdx/cost")
public interface SdxCostEndpoint {

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "cost of SDX clusters", operationId = "listSdxCost",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RealTimeCostResponse list(@ValidCrn(resource = CrnResourceDescriptor.DATALAKE) List<String> sdxCrns);
}
