package com.sequenceiq.periscope.api.endpoint;

import static com.sequenceiq.periscope.doc.ApiDescription.HISTORY_DESCRIPTION;
import static com.sequenceiq.periscope.doc.ApiDescription.JSON;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.periscope.api.model.HistoryJson;
import com.sequenceiq.periscope.doc.ApiDescription;
import com.sequenceiq.periscope.doc.ApiDescription.HistoryNotes;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/clusters/{clusterId}/history")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/history", description = HISTORY_DESCRIPTION, protocols = "http,https")
public interface HistoryEndpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.HistoryOpDescription.HISTORY_GET_ALL, produces = JSON, notes = HistoryNotes.NOTES)
    List<HistoryJson> getHistory(@PathParam(value = "clusterId") Long clusterId);

    @GET
    @Path("{historyId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.HistoryOpDescription.HISTORY_GET, produces = JSON, notes = HistoryNotes.NOTES)
    HistoryJson getHistory(@PathParam(value = "clusterId") Long clusterId, @PathParam(value = "historyId") Long historyId);
}
