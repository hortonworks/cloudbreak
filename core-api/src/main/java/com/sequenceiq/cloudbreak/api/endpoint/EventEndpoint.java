package com.sequenceiq.cloudbreak.api.endpoint;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/events")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/events", description = ControllerDescription.EVENT_DESCRIPTION, protocols = "http,https")
public interface EventEndpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.EventOpDescription.GET_BY_TIMESTAMP, produces = ContentType.JSON, notes = Notes.EVENT_NOTES,
            nickname = "getEvents")
    List<CloudbreakEventsJson> get(@QueryParam("since") Long since);

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.EventOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.EVENT_NOTES,
            nickname = "getEventsBySTackId")
    List<CloudbreakEventsJson> getByStack(@PathParam(value = "stackId") Long stackId);
}
