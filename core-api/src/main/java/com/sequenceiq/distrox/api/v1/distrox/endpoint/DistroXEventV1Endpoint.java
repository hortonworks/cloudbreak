package com.sequenceiq.distrox.api.v1.distrox.endpoint;

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

import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventContainer;
import com.sequenceiq.distrox.api.v1.distrox.model.event.DistroXEventV1Response;
import com.sequenceiq.distrox.api.v1.distrox.model.event.DistroXEventV1Responses;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/v1/distrox/event")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "/v1/distrox/event", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface DistroXEventV1Endpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.EventOpDescription.GET_BY_TIMESTAMP, produces = MediaType.APPLICATION_JSON, notes = Notes.DISTROX_EVENT_NOTES,
            nickname = "getEvents")
    DistroXEventV1Responses list(@QueryParam("since") Long since);

    @GET
    @Path("{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.EventOpDescription.GET_BY_CRN, produces = MediaType.APPLICATION_JSON, notes = Notes.DISTROX_EVENT_NOTES,
            nickname = "getEventsByCrn")
    Page<DistroXEventV1Response> getEventsByStack(
            @PathParam("crn") String crn,
            @QueryParam("page") @DefaultValue("0") Integer page,
            @QueryParam("size") @DefaultValue("100") Integer size);

    @GET
    @Path("{crn}/structured")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.EventOpDescription.GET_EVENTS_BY_CRN, produces = MediaType.APPLICATION_JSON,
            notes = Notes.DISTROX_EVENT_NOTES, nickname = "getStructuredEvents")
    StructuredEventContainer structured(@PathParam("crn") String crn);

    @GET
    @Path("{crn}/zip")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiOperation(value = OperationDescriptions.EventOpDescription.GET_EVENTS_ZIP_BY_CRN, produces = MediaType.APPLICATION_JSON,
            notes = Notes.DISTROX_EVENT_NOTES, nickname = "getStructuredEventsZip")
    Response download(@PathParam("crn") String name);
}
