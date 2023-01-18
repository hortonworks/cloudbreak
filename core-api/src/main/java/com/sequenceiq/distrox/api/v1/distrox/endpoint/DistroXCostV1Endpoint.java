package com.sequenceiq.distrox.api.v1.distrox.endpoint;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCostResponse;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v1/distrox/cost")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/distrox/cost")
public interface DistroXCostV1Endpoint {

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DistroXOpDescription.COST,
            description = Notes.CLUSTER_COST_NOTES, operationId = "listDistroXCostV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RealTimeCostResponse list(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) List<String> datahubCrns);
}
