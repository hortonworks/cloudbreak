package com.sequenceiq.sdx.api.endpoint;

import static com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventOperationDescriptions.AUDIT_EVENTS_NOTES;
import static com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventOperationDescriptions.AuditOpDescription.LIST_FOR_RESOURCE;

import java.util.List;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.springframework.validation.annotation.Validated;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Validated
@Path("/sdx")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/sdx", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface SdxEventEndpoint {

    @GET
    @Path("/structured_events")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LIST_FOR_RESOURCE, produces = MediaType.APPLICATION_JSON, notes = AUDIT_EVENTS_NOTES,
            nickname = "getCDPAuditEventsForResource")
    List<CDPStructuredEvent> getAuditEvents(
            @QueryParam("environmentCrn") @NotNull(message = "The 'environmentCrn' query parameter must be specified.") String environmentCrn,
            @QueryParam("types") List<StructuredEventType> types,
            @QueryParam("page") @DefaultValue("0") Integer page,
            @QueryParam("size") @DefaultValue("100") Integer size);
}
