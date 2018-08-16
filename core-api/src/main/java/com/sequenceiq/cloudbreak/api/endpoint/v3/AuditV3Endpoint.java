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
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3/{organizationId}/audits")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{organizationId}/audits", description = ControllerDescription.AUDIT_V3_DESCRIPTION, protocols = "http,https")
public interface AuditV3Endpoint {

    @GET
    @Path("event/{auditId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.AuditOpDescription.GET_BY_ORG, produces = ContentType.JSON, notes = Notes.AUDIT_EVENTS_NOTES,
            nickname = "getAuditEventByOrganization")
    AuditEvent getAuditEventByOrganization(@PathParam("organizationId") Long organizationId, @PathParam("auditId") Long auditId);

    @GET
    @Path("events/{resourceType}/{resourceId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.AuditOpDescription.LIST_IN_ORG, produces = ContentType.JSON, notes = Notes.AUDIT_EVENTS_NOTES,
            nickname = "getAuditEventsInOrganization")
    List<AuditEvent> getAuditEventsInOrganization(@PathParam("organizationId") Long organizationId,
            @PathParam("resourceType") String resourceType, @PathParam("resourceId") Long resourceId);

    @GET
    @Path("events/zip/{resourceType}/{resourceId}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiOperation(value = OperationDescriptions.AuditOpDescription.LIST_IN_ORG_ZIP, produces = ContentType.JSON, notes = Notes.AUDIT_EVENTS_NOTES,
            nickname = "getAuditEventsZipInOrganization")
    Response getAuditEventsZipInOrganization(@PathParam("organizationId") Long organizationId,
            @PathParam("resourceType") String resourceType, @PathParam("resourceId") Long resourceId);

}
