package com.sequenceiq.redbeams.api.endpoint.v4.events;

import static com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventOperationDescriptions.AUDIT_EVENTS_NOTES;
import static com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventOperationDescriptions.AuditOpDescription.LIST_FOR_RESOURCE;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/v4/events")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RetryAndMetrics
@Tag(name = "/v4/events", description = "Query CDP structured events stored by redbeams for a database server resource")
public interface RedbeamsEventV4Endpoint {

    @GET
    @Path("/structured_events")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = LIST_FOR_RESOURCE, description = AUDIT_EVENTS_NOTES, operationId = "getRedbeamsAuditEventsForResource",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<CDPStructuredEvent> getAuditEvents(
            @QueryParam("resourceCrn") @NotEmpty(message = "The 'resourceCrn' query parameter must be specified.") String resourceCrn,
            @QueryParam("types") List<StructuredEventType> types,
            @QueryParam("page") @DefaultValue("0") @Min(0) @Max(200) Integer page,
            @QueryParam("size") @DefaultValue("100") @Min(1) @Max(200) Integer size);
}
