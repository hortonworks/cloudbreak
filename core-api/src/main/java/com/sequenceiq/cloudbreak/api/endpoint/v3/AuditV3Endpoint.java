package com.sequenceiq.cloudbreak.api.endpoint.v3;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.model.audit.AuditEvent;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.AuditOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3/{workspaceId}/audits")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{workspaceId}/audits", description = ControllerDescription.AUDIT_V3_DESCRIPTION, protocols = "http,https")
public interface AuditV3Endpoint {

    @GET
    @Path("event/{auditId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AuditOpDescription.GET_BY_ORG, produces = ContentType.JSON, notes = Notes.AUDIT_EVENTS_NOTES,
            nickname = "getAuditEventByWorkspace")
    AuditEvent getAuditEventByWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("auditId") Long auditId);

    @GET
    @Path("events/{resourceType}/{resourceId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AuditOpDescription.LIST_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.AUDIT_EVENTS_NOTES,
            nickname = "getAuditEventsInWorkspace")
    List<AuditEvent> getAuditEventsInWorkspace(@PathParam("workspaceId") Long workspaceId,
            @PathParam("resourceType") String resourceType, @PathParam("resourceId") Long resourceId);

    @GET
    @Path("events/zip/{resourceType}/{resourceId}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiOperation(value = AuditOpDescription.LIST_IN_WORKSPACE_ZIP, produces = ContentType.JSON, notes = Notes.AUDIT_EVENTS_NOTES,
            nickname = "getAuditEventsZipInWorkspace")
    Response getAuditEventsZipInWorkspace(@PathParam("workspaceId") Long workspaceId,
            @PathParam("resourceType") String resourceType, @PathParam("resourceId") Long resourceId);

}
