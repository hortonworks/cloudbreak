package com.sequenceiq.periscope.api.endpoint.v1;

import static com.sequenceiq.periscope.doc.ApiDescription.HISTORY_DESCRIPTION;
import static com.sequenceiq.periscope.doc.ApiDescription.JSON;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.periscope.api.model.AutoscaleClusterHistoryResponse;
import com.sequenceiq.periscope.doc.ApiDescription.HistoryNotes;
import com.sequenceiq.periscope.doc.ApiDescription.HistoryOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/clusters/{clusterId}/history")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/history", description = HISTORY_DESCRIPTION, protocols = "http,https")
public interface HistoryEndpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = HistoryOpDescription.HISTORY_GET_ALL, produces = JSON, notes = HistoryNotes.NOTES)
    List<AutoscaleClusterHistoryResponse> getHistory(@PathParam("clusterId") Long clusterId);

    @GET
    @Path("{historyId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = HistoryOpDescription.HISTORY_GET, produces = JSON, notes = HistoryNotes.NOTES)
    AutoscaleClusterHistoryResponse getHistoryById(@PathParam("clusterId") Long clusterId, @PathParam("historyId") Long historyId);
}
