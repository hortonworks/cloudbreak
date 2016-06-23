package com.sequenceiq.cloudbreak.api.endpoint

import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

import com.sequenceiq.cloudbreak.doc.ContentType
import com.sequenceiq.cloudbreak.doc.ControllerDescription
import com.sequenceiq.cloudbreak.doc.Notes
import com.sequenceiq.cloudbreak.doc.OperationDescriptions
import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation

@Path("/events")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/events", description = ControllerDescription.EVENT_DESCRIPTION, position = 7)
interface EventEndpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.EventOpDescription.GET_BY_TIMESTAMP, produces = ContentType.JSON, notes = Notes.EVENT_NOTES)
    operator fun get(@QueryParam("since") since: Long?): List<CloudbreakEventsJson>
}
