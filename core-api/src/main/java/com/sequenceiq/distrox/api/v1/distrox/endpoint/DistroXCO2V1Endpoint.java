package com.sequenceiq.distrox.api.v1.distrox.endpoint;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.common.co2.RealTimeCO2Response;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v1/distrox/carbon_dioxide")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/distrox/carbon_dioxide")
public interface DistroXCO2V1Endpoint {

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DistroXOpDescription.CO2, description = Notes.CLUSTER_CO2_NOTES, operationId = "listDistroXCO2V1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RealTimeCO2Response list(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) List<String> datahubCrns);
}
