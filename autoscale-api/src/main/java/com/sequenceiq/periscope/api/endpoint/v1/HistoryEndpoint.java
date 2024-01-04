package com.sequenceiq.periscope.api.endpoint.v1;

import static com.sequenceiq.periscope.doc.ApiDescription.HISTORY_DESCRIPTION;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.periscope.api.model.AutoscaleClusterHistoryResponse;
import com.sequenceiq.periscope.doc.ApiDescription.HistoryNotes;
import com.sequenceiq.periscope.doc.ApiDescription.HistoryOpDescription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/v1/distrox")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/history", description = HISTORY_DESCRIPTION)
public interface HistoryEndpoint {

    @GET
    @Path("crn/{crn}/history")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = HistoryOpDescription.HISTORY_GET_BY_CLUSTER, description = HistoryNotes.NOTES, operationId = "getHistoryByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<AutoscaleClusterHistoryResponse> getHistoryByCrn(@PathParam("crn") String clusterCrn,
            @Parameter(description = HistoryOpDescription.HISTORY_COUNT) @QueryParam("count") @DefaultValue("200")  @Min(1) @Max(2000) Integer historyCount);

    @GET
    @Path("name/{name}/history")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = HistoryOpDescription.HISTORY_GET_BY_CLUSTER, description = HistoryNotes.NOTES, operationId = "getHistoryByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<AutoscaleClusterHistoryResponse> getHistoryByName(@PathParam("name") String clusterName,
            @Parameter(description = HistoryOpDescription.HISTORY_COUNT) @QueryParam("count") @DefaultValue("200")  @Min(1) @Max(2000) Integer historyCount);
}
