package com.sequenceiq.cloudbreak.structuredevent.rest.endpoint;

import static com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventOperationDescriptions.EVENT_NOTES;
import static com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventOperationDescriptions.EventOpDescription.GET_BY_NAME;
import static com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventOperationDescriptions.EventOpDescription.GET_BY_TIMESTAMP;
import static com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventOperationDescriptions.EventOpDescription.GET_EVENTS_BY_NAME;
import static com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventOperationDescriptions.EventOpDescription.GET_EVENTS_ZIP_BY_NAME;

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

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEventContainer;
import com.sequenceiq.cloudbreak.structuredevent.rest.model.CDPEventV1Response;
import com.sequenceiq.cloudbreak.structuredevent.rest.model.CDPEventV1Responses;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/v1/events")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/events")
public interface CDPEventV1Endpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = GET_BY_TIMESTAMP, description = EVENT_NOTES,
            operationId = "getEventsInAccount",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CDPEventV1Responses list(@QueryParam("since") Long since);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = GET_BY_NAME, description = EVENT_NOTES,
            operationId = "getEventsByResourceNameInAccount",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Page<CDPEventV1Response> getCloudbreakEventsByResource(
            @PathParam("name") String name,
            @QueryParam("page") @DefaultValue("0") @Min(0) @Max(200) Integer page,
            @QueryParam("size") @DefaultValue("100") @Min(1) @Max(200) Integer size);

    @GET
    @Path("{name}/structured")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = GET_EVENTS_BY_NAME, description = EVENT_NOTES,
            operationId = "getStructuredEventsInAccount",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CDPStructuredEventContainer structured(@PathParam("name") String name);

    @GET
    @Path("{name}/zip")
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    @Operation(summary = GET_EVENTS_ZIP_BY_NAME, description = EVENT_NOTES, operationId = "getStructuredEventsZipInAccount",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Response download(@PathParam("name") String name);
}
