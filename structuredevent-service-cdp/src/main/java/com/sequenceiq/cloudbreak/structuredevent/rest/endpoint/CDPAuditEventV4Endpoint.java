package com.sequenceiq.cloudbreak.structuredevent.rest.endpoint;

import static com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventOperationDescriptions.AUDIT_EVENTS_NOTES;
import static com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventOperationDescriptions.AuditOpDescription.GET_BY_WORKSPACE;
import static com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventOperationDescriptions.AuditOpDescription.LIST_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventOperationDescriptions.AuditOpDescription.LIST_IN_WORKSPACE_ZIP;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.structuredevent.rest.model.CDPAuditEventV4Response;
import com.sequenceiq.cloudbreak.structuredevent.rest.model.CDPAuditEventV4Responses;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/v1/audits")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/audits", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface CDPAuditEventV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LIST_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = AUDIT_EVENTS_NOTES,
            nickname = "getAuditEventsInAccount")
    CDPAuditEventV4Responses getAuditEvents(@QueryParam("resourceType") String resourceType,
        @QueryParam("resourceId") Long resourceId, @QueryParam("resourceCrn") String resourceCrn);

    @GET
    @Path("zip")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiOperation(value = LIST_IN_WORKSPACE_ZIP, produces = MediaType.APPLICATION_OCTET_STREAM, notes = AUDIT_EVENTS_NOTES,
            nickname = "getAuditEventsZipInAccount")
    Response getAuditEventsZip(@QueryParam("resourceType") String resourceType,
        @QueryParam("resourceId") Long resourceId, @QueryParam("resourceCrn") String resourceCrn);

    @GET
    @Path("{auditId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = GET_BY_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = AUDIT_EVENTS_NOTES,
            nickname = "getAuditEventByWorkspace")
    CDPAuditEventV4Response getAuditEventById(@PathParam("auditId") Long auditId);
}
