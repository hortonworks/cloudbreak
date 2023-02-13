package com.sequenceiq.cloudbreak.api.endpoint.v4.events;

import java.util.List;

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
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.EventOpDescription;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventContainer;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@Path("/v4/events")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v4/events", description = ControllerDescription.EVENT_DESCRIPTION)
public interface EventV4Endpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  EventOpDescription.GET_BY_TIMESTAMP, description =  Notes.EVENT_NOTES,
            operationId = "getEventsInWorkspace")
    CloudbreakEventV4Responses list(@QueryParam("since") Long since, @AccountId @QueryParam("accountId") String accountId);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  EventOpDescription.GET_BY_NAME, description =  Notes.EVENT_NOTES,
            operationId = "getEventsByStackNameInWorkspace")
    Page<CloudbreakEventV4Response> getCloudbreakEventsByStack(
            @PathParam("name") String name,
            @QueryParam("page") @DefaultValue("0") Integer page,
            @QueryParam("size") @DefaultValue("100") Integer size,
            @AccountId @QueryParam("accountId") String accountId);

    @GET
    @Path("{name}/list")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  EventOpDescription.GET_BY_NAME, description =  Notes.EVENT_NOTES,
            operationId = "getCloudbreakEventsListByStack")
    List<CloudbreakEventV4Response> getPagedCloudbreakEventListByStack(
            @PathParam("name") String name,
            @QueryParam("page") @DefaultValue("0") Integer page,
            @QueryParam("size") @DefaultValue("100") Integer size,
            @AccountId @QueryParam("accountId") String accountId);

    @GET
    @Path("crn/{crn}/list")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  EventOpDescription.GET_BY_CRN, description =  Notes.EVENT_NOTES,
            operationId = "getCloudbreakEventsListByCrn")
    List<CloudbreakEventV4Response> getPagedCloudbreakEventListByCrn(
            @PathParam("crn") String crn,
            @QueryParam("page") @DefaultValue("0") Integer page,
            @QueryParam("size") @DefaultValue("100") Integer size,
            @QueryParam("onlyAlive") @DefaultValue("true") boolean onlyAlive);

    @GET
    @Path("{name}/structured")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  EventOpDescription.GET_EVENTS_BY_NAME, description =  Notes.EVENT_NOTES,
            operationId = "getStructuredEventsInWorkspace")
    StructuredEventContainer structured(@PathParam("name") String name, @AccountId @QueryParam("accountId") String accountId);

    @GET
    @Path("crn/{crn}/structured")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  EventOpDescription.GET_EVENTS_BY_CRN, description =  Notes.EVENT_NOTES,
            operationId = "getStructuredEventsInWorkspaceByCrn")
    StructuredEventContainer structuredByCrn(@PathParam("crn") String crn, @QueryParam("onlyAlive") @DefaultValue("true") boolean onlyAlive);

    @GET
    @Path("{name}/zip")
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    @Operation(summary =  EventOpDescription.GET_EVENTS_ZIP_BY_NAME, description =  Notes.EVENT_NOTES,
            operationId = "getStructuredEventsZipInWorkspace")
    Response download(@PathParam("name") String name, @AccountId @QueryParam("accountId") String accountId);
}
