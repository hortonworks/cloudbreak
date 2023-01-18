package com.sequenceiq.sdx.api.endpoint;

import static com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventOperationDescriptions.AUDIT_EVENTS_NOTES;
import static com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventOperationDescriptions.AuditOpDescription.LIST_FOR_RESOURCE;
import static com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventOperationDescriptions.AuditOpDescription.LIST_FOR_RESOURCE_ZIP;

import java.util.List;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.validation.annotation.Validated;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@Path("/sdx")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/sdx")
public interface SdxEventEndpoint {

    @GET
    @Path("/structured_events")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = LIST_FOR_RESOURCE, description = AUDIT_EVENTS_NOTES, operationId = "getCDPAuditEventsForResource",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<CDPStructuredEvent> getAuditEvents(
            @QueryParam("environmentCrn") @NotNull(message = "The 'environmentCrn' query parameter must be specified.") String environmentCrn,
            @QueryParam("types") List<StructuredEventType> types,
            @QueryParam("page") @DefaultValue("0") Integer page,
            @QueryParam("size") @DefaultValue("100") Integer size);

    @GET
    @Path("zip")
    @Produces({ MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON })
    @Operation(summary = LIST_FOR_RESOURCE_ZIP, description = AUDIT_EVENTS_NOTES, operationId = "getDatalakeEventsZipForResource",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Response getDatalakeEventsZip(
            @QueryParam("environmentCrn") @NotNull(message = "The 'environmentCrn' query parameter must be specified.") String environmentCrn,
            @QueryParam("types") List<StructuredEventType> types);
}
