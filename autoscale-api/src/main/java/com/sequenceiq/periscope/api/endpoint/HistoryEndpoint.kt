package com.sequenceiq.periscope.api.endpoint

import com.sequenceiq.periscope.doc.ApiDescription.HISTORY_DESCRIPTION
import com.sequenceiq.periscope.doc.ApiDescription.JSON

import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

import com.sequenceiq.periscope.doc.ApiDescription
import com.sequenceiq.periscope.api.model.HistoryJson

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation

@Path("/clusters/{clusterId}/history")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/history", description = HISTORY_DESCRIPTION, position = 3)
interface HistoryEndpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.HistoryOpDescription.HISTORY_GET_ALL, produces = JSON, notes = ApiDescription.HistoryNotes.NOTES)
    fun getHistory(@PathParam(value = "clusterId") clusterId: Long?): List<HistoryJson>

    @GET
    @Path("{historyId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.HistoryOpDescription.HISTORY_GET, produces = JSON, notes = ApiDescription.HistoryNotes.NOTES)
    fun getHistory(@PathParam(value = "clusterId") clusterId: Long?, @PathParam(value = "historyId") historyId: Long?): HistoryJson
}
