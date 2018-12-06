package com.sequenceiq.cloudbreak.api.endpoint.v1;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.model.event.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.EventOpDescription;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventContainer;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/events")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/events", description = ControllerDescription.EVENT_DESCRIPTION, protocols = "http,https")
public interface EventEndpoint {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EventOpDescription.GET_BY_TIMESTAMP, produces = ContentType.JSON, notes = Notes.EVENT_NOTES,
            nickname = "getEvents")
    List<CloudbreakEventsJson> getCloudbreakEventsSince(@QueryParam("since") Long since);

    @GET
    @Path("{stackId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EventOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.EVENT_NOTES,
            nickname = "getEventsBySTackId")
    List<CloudbreakEventsJson> getCloudbreakEventsByStack(@PathParam("stackId") Long stackId);

    @GET
    @Path("struct/{stackId}")
    @Produces(MediaType.APPLICATION_JSON)
    StructuredEventContainer getStructuredEvents(@PathParam("stackId") Long stackId);

    @GET
    @Path("struct/zip/{stackId}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    Response getStructuredEventsZip(@PathParam("stackId") Long stackId);
}
