package com.sequenceiq.cloudbreak.api.endpoint.v4.events;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.springframework.data.domain.Page;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Responses;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.EventOpDescription;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventContainer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/v4/events")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v4/events", description = ControllerDescription.EVENT_DESCRIPTION)
public interface EventV4Endpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EventOpDescription.GET_BY_TIMESTAMP, description = Notes.EVENT_NOTES,
            operationId = "getEventsInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CloudbreakEventV4Responses list(@QueryParam("since") Long since, @QueryParam("accountId") String accountId);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EventOpDescription.GET_BY_NAME, description = Notes.EVENT_NOTES,
            operationId = "getEventsByStackNameInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Page<CloudbreakEventV4Response> getCloudbreakEventsByStack(
            @PathParam("name") String name,
            @QueryParam("page") @DefaultValue("0") @Min(0) @Max(200) Integer page,
            @QueryParam("size") @DefaultValue("100") @Min(1) @Max(200) Integer size,
            @QueryParam("accountId") String accountId);

    @GET
    @Path("{name}/list")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EventOpDescription.GET_BY_NAME, description = Notes.EVENT_NOTES,
            operationId = "getCloudbreakEventsListByStack",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<CloudbreakEventV4Response> getPagedCloudbreakEventListByStack(
            @PathParam("name") String name,
            @QueryParam("page") @DefaultValue("0") @Min(0) @Max(200) Integer page,
            @QueryParam("size") @DefaultValue("100") @Min(1) @Max(200) Integer size,
            @QueryParam("accountId") String accountId);

    @GET
    @Path("crn/{crn}/list")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EventOpDescription.GET_BY_CRN, description = Notes.EVENT_NOTES,
            operationId = "getCloudbreakEventsListByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<CloudbreakEventV4Response> getPagedCloudbreakEventListByCrn(
            @PathParam("crn") String crn,
            @QueryParam("page") @DefaultValue("0") @Min(0) @Max(200) Integer page,
            @QueryParam("size") @DefaultValue("100") @Min(1) @Max(200) Integer size,
            @QueryParam("onlyAlive") @DefaultValue("true") boolean onlyAlive);

    @GET
    @Path("{name}/structured")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EventOpDescription.GET_EVENTS_BY_NAME, description = Notes.EVENT_NOTES,
            operationId = "getStructuredEventsInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StructuredEventContainer structured(@PathParam("name") String name, @QueryParam("accountId") String accountId);

    @GET
    @Path("crn/{crn}/structured")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EventOpDescription.GET_EVENTS_BY_CRN, description = Notes.EVENT_NOTES,
            operationId = "getStructuredEventsInWorkspaceByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StructuredEventContainer structuredByCrn(@PathParam("crn") String crn, @QueryParam("onlyAlive") @DefaultValue("true") boolean onlyAlive);

    @GET
    @Path("{name}/zip")
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    @Operation(summary = EventOpDescription.GET_EVENTS_ZIP_BY_NAME, description = Notes.EVENT_NOTES,
            operationId = "getStructuredEventsZipInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Response download(@PathParam("name") String name, @QueryParam("accountId") String accountId);
}
