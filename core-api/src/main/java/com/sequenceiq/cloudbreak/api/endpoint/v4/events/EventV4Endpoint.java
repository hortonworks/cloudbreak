package com.sequenceiq.cloudbreak.api.endpoint.v4.events;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.data.domain.Page;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Responses;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.EventOpDescription;
import com.sequenceiq.cloudbreak.jerseyclient.retry.RetryingRestClient;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventContainer;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v4/events")
@RetryingRestClient
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/events", description = ControllerDescription.EVENT_DESCRIPTION, protocols = "http,https")
public interface EventV4Endpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EventOpDescription.GET_BY_TIMESTAMP, produces = ContentType.JSON, notes = Notes.EVENT_NOTES,
            nickname = "getEventsInWorkspace")
    CloudbreakEventV4Responses list(@QueryParam("since") Long since);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EventOpDescription.GET_BY_NAME, produces = ContentType.JSON, notes = Notes.EVENT_NOTES,
            nickname = "getEventsByStackNameInWorkspace")
    Page<CloudbreakEventV4Response> getCloudbreakEventsByStack(
            @PathParam("name") String name,
            @QueryParam("page") @DefaultValue("0") Integer page,
            @QueryParam("size") @DefaultValue("100") Integer size);

    @GET
    @Path("{name}/structured")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EventOpDescription.GET_EVENTS_BY_NAME, produces = ContentType.JSON, notes = Notes.EVENT_NOTES,
            nickname = "getStructuredEventsInWorkspace")
    StructuredEventContainer structured(@PathParam("name") String name);

    @GET
    @Path("{name}/zip")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiOperation(value = EventOpDescription.GET_EVENTS_ZIP_BY_NAME, produces = ContentType.JSON, notes = Notes.EVENT_NOTES,
            nickname = "getStructuredEventsZipInWorkspace")
    Response download(@PathParam("name") String name);
}
