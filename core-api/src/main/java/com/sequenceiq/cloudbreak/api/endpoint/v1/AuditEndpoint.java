package com.sequenceiq.cloudbreak.api.endpoint.v1;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.model.audit.AuditEvent;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;

import io.swagger.annotations.Api;

@Path("/v1/audits")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/audits", description = ControllerDescription.AUDIT_DESCRIPTION, protocols = "http,https")
public interface AuditEndpoint {

    @GET
    @Path("event/{auditId}")
    @Produces(MediaType.APPLICATION_JSON)
    AuditEvent getAuditEvent(@PathParam("auditId") Long auditId);

    @GET
    @Path("events/{resourceType}/{resourceId}")
    @Produces(MediaType.APPLICATION_JSON)
    List<AuditEvent> getAuditEvents(@PathParam("resourceType") String resourceType, @PathParam("resourceId") Long resourceId);

    @GET
    @Path("events/zip/{resourceType}/{resourceId}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    Response getAuditEventsZip(@PathParam("resourceType") String resourceType, @PathParam("resourceId") Long resourceId);

}
