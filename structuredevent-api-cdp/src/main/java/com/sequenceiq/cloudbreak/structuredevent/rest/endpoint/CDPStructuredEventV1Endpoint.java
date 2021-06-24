package com.sequenceiq.cloudbreak.structuredevent.rest.endpoint;

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

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/v1/structured_events")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/structured_events", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface CDPStructuredEventV1Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LIST_FOR_RESOURCE, produces = MediaType.APPLICATION_JSON, notes = AUDIT_EVENTS_NOTES,
            nickname = "getCDPAuditEventsForResource")
    List<CDPStructuredEvent> getAuditEvents(
            @QueryParam("resourceCrn") @NotNull(message = "The 'resourceCrn' query parameter must be specified.") String resourceCrn,
            @QueryParam("types") List<StructuredEventType> types,
            @QueryParam("page") @DefaultValue("0") Integer page,
            @QueryParam("size") @DefaultValue("100") Integer size);

    @GET
    @Path("zip")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiOperation(value = LIST_FOR_RESOURCE_ZIP, produces = MediaType.APPLICATION_JSON, notes = AUDIT_EVENTS_NOTES,
            nickname = "getAuditEventsZipForResource")
    Response getAuditEventsZip(
            @QueryParam("resourceCrn") @NotNull(message = "The 'resourceCrn' query parameter must be specified.") String resourceCrn,
            @QueryParam("types") List<StructuredEventType> types);

}
