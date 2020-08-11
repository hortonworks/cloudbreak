package com.sequenceiq.cloudbreak.structuredevent.controller.events;

import static com.sequenceiq.cloudbreak.structuredevent.controller.CDPStructuredEventOperationDescriptions.EVENT_NOTES;
import static com.sequenceiq.cloudbreak.structuredevent.controller.CDPStructuredEventOperationDescriptions.EventOpDescription.GET_BY_NAME;
import static com.sequenceiq.cloudbreak.structuredevent.controller.CDPStructuredEventOperationDescriptions.EventOpDescription.GET_BY_TIMESTAMP;
import static com.sequenceiq.cloudbreak.structuredevent.controller.CDPStructuredEventOperationDescriptions.EventOpDescription.GET_EVENTS_BY_NAME;
import static com.sequenceiq.cloudbreak.structuredevent.controller.CDPStructuredEventOperationDescriptions.EventOpDescription.GET_EVENTS_ZIP_BY_NAME;

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

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.structuredevent.controller.events.responses.CDPEventV4Response;
import com.sequenceiq.cloudbreak.structuredevent.controller.events.responses.CDPEventV4Responses;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEventContainer;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/events")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/events", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface CDPEventV4Endpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = GET_BY_TIMESTAMP, produces = MediaType.APPLICATION_JSON, notes = EVENT_NOTES,
            nickname = "getEventsInAccount")
    CDPEventV4Responses list(@QueryParam("since") Long since);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = GET_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = EVENT_NOTES,
            nickname = "getEventsByStackNameInAccount")
    Page<CDPEventV4Response> getCloudbreakEventsByStack(
            @PathParam("name") String name,
            @QueryParam("page") @DefaultValue("0") Integer page,
            @QueryParam("size") @DefaultValue("100") Integer size);

    @GET
    @Path("{name}/structured")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = GET_EVENTS_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = EVENT_NOTES,
            nickname = "getStructuredEventsInAccount")
    CDPStructuredEventContainer structured(@PathParam("name") String name);

    @GET
    @Path("{name}/zip")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiOperation(value = GET_EVENTS_ZIP_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = EVENT_NOTES,
            nickname = "getStructuredEventsZipInAccount")
    Response download(@PathParam("name") String name);
}
