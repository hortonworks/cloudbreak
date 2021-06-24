package com.sequenceiq.cloudbreak.api.endpoint.v4.audits;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Responses;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.AuditOpDescription;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/v4/{workspaceId}/audits")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/{workspaceId}/audits", description = ControllerDescription.AUDIT_V4_DESCRIPTION, protocols = "http,https",
        consumes = MediaType.APPLICATION_JSON)
public interface AuditEventV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AuditOpDescription.LIST_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = Notes.AUDIT_EVENTS_NOTES,
            nickname = "getAuditEventsInWorkspace")
    AuditEventV4Responses getAuditEvents(@PathParam("workspaceId") Long workspaceId, @QueryParam("resourceType") String resourceType,
            @QueryParam("resourceId") Long resourceId, @QueryParam("resourceCrn") String resourceCrn);

    @GET
    @Path("zip")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiOperation(value = AuditOpDescription.LIST_IN_WORKSPACE_ZIP, produces = MediaType.APPLICATION_OCTET_STREAM, notes = Notes.AUDIT_EVENTS_NOTES,
            nickname = "getAuditEventsZipInWorkspace")
    Response getAuditEventsZip(@PathParam("workspaceId") Long workspaceId, @QueryParam("resourceType") String resourceType,
            @QueryParam("resourceId") Long resourceId, @QueryParam("resourceCrn") String resourceCrn);

    @GET
    @Path("{auditId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AuditOpDescription.GET_BY_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = Notes.AUDIT_EVENTS_NOTES,
            nickname = "getAuditEventByWorkspace")
    AuditEventV4Response getAuditEventById(@PathParam("workspaceId") Long workspaceId, @PathParam("auditId") Long auditId);

}
