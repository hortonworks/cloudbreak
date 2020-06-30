package com.sequenceiq.periscope.api.endpoint.v1;

import static com.sequenceiq.periscope.doc.ApiDescription.HISTORY_DESCRIPTION;

import java.util.List;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.periscope.api.model.AutoscaleClusterHistoryResponse;
import com.sequenceiq.periscope.doc.ApiDescription.HistoryNotes;
import com.sequenceiq.periscope.doc.ApiDescription.HistoryOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Path("/v1/distrox")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/history", description = HISTORY_DESCRIPTION, protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface HistoryEndpoint {

    @GET
    @Path("crn/{crn}/history")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = HistoryOpDescription.HISTORY_GET_BY_CLUSTER, produces = MediaType.APPLICATION_JSON, notes = HistoryNotes.NOTES)
    List<AutoscaleClusterHistoryResponse> getHistoryByCrn(@PathParam("crn") String clusterCrn,
            @ApiParam(HistoryOpDescription.HISTORY_COUNT) @QueryParam("count") @DefaultValue("200")  @Min(1) @Max(2000) Integer historyCount);

    @GET
    @Path("name/{name}/history")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = HistoryOpDescription.HISTORY_GET_BY_CLUSTER, produces = MediaType.APPLICATION_JSON, notes = HistoryNotes.NOTES)
    List<AutoscaleClusterHistoryResponse> getHistoryByName(@PathParam("name") String clusterName,
            @ApiParam(HistoryOpDescription.HISTORY_COUNT) @QueryParam("count") @DefaultValue("200")  @Min(1) @Max(2000) Integer historyCount);
}
