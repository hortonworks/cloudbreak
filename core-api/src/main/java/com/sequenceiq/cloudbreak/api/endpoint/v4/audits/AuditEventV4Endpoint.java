package com.sequenceiq.cloudbreak.api.endpoint.v4.audits;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Responses;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.AuditOpDescription;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v4/{workspaceId}/audits")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v4/{workspaceId}/audits", description = ControllerDescription.AUDIT_V4_DESCRIPTION)
public interface AuditEventV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = AuditOpDescription.LIST_IN_WORKSPACE, description = Notes.AUDIT_EVENTS_NOTES,
            operationId = "getAuditEventsInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    AuditEventV4Responses getAuditEvents(@PathParam("workspaceId") Long workspaceId, @QueryParam("resourceType") String resourceType,
            @QueryParam("resourceId") Long resourceId, @QueryParam("resourceCrn") String resourceCrn);

    @GET
    @Path("zip")
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    @Operation(summary = AuditOpDescription.LIST_IN_WORKSPACE_ZIP, description = Notes.AUDIT_EVENTS_NOTES,
            operationId = "getAuditEventsZipInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Response getAuditEventsZip(@PathParam("workspaceId") Long workspaceId, @QueryParam("resourceType") String resourceType,
            @QueryParam("resourceId") Long resourceId, @QueryParam("resourceCrn") String resourceCrn);

    @GET
    @Path("{auditId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = AuditOpDescription.GET_BY_WORKSPACE, description = Notes.AUDIT_EVENTS_NOTES,
            operationId = "getAuditEventByWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    AuditEventV4Response getAuditEventById(@PathParam("workspaceId") Long workspaceId, @PathParam("auditId") Long auditId);

}
