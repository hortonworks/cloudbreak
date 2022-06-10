package com.sequenceiq.distrox.api.v1.distrox.endpoint;

import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_DATAHUB_AUDIT_EVENTS;

import java.util.List;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.validation.ValidCrn;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/v1/distrox")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "/v1/distrox", protocols = "http,https",
        consumes = MediaType.APPLICATION_JSON)
public interface DistroXV1EventEndpoint {

    @GET
    @Path("/structured_events")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = GET_DATAHUB_AUDIT_EVENTS, produces = MediaType.APPLICATION_JSON, notes = Notes.DATAHUB_AUDIT_EVENTS,
            nickname = "getAuditEvents")
    List<CDPStructuredEvent> getAuditEvents(
            @QueryParam("resourceCrn") @NotNull @ValidCrn(resource = CrnResourceDescriptor.DATAHUB) String resourceCrn,
            @QueryParam("page") @DefaultValue("0") Integer page,
            @QueryParam("size") @DefaultValue("100") Integer size);
}
