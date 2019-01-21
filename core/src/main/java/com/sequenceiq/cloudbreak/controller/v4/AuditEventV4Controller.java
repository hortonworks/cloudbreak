package com.sequenceiq.cloudbreak.controller.v4;

import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.AuditEventV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.filter.GetAuditEventRequestV4Filter;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Responses;
import com.sequenceiq.cloudbreak.service.audit.AuditEventService;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Controller
public class AuditEventV4Controller implements AuditEventV4Endpoint {

    @Inject
    private AuditEventService auditEventService;

    @Override
    public AuditEventV4Response getAuditEventById(Long workspaceId, Long auditId) {
        return auditEventService.getAuditEventByWorkspaceId(workspaceId, auditId);
    }

    @Override
    public AuditEventV4Responses getAuditEvents(Long workspaceId, GetAuditEventRequestV4Filter getAuditRequest) {
        List<AuditEventV4Response> auditEventsByWorkspaceId = auditEventService.getAuditEventsByWorkspaceId(workspaceId,
                getAuditRequest.getResourceType(), getAuditRequest.getResourceId());
        return new AuditEventV4Responses(auditEventsByWorkspaceId);

    }

    @Override
    public Response getAuditEventsZip(Long workspaceId, GetAuditEventRequestV4Filter getAuditRequest) {
        List<AuditEventV4Response> auditEvents = getAuditEvents(workspaceId, getAuditRequest).getResponses();
        return getAuditEventsZipResponse(auditEvents, getAuditRequest.getResourceType(), getAuditRequest.getResourceId());
    }

    private Response getAuditEventsZipResponse(List<AuditEventV4Response> auditEventV4Responses, String resourceType, Long resourceId) {
        StreamingOutput streamingOutput = output -> {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(output)) {
                zipOutputStream.putNextEntry(new ZipEntry("struct-events.json"));
                zipOutputStream.write(JsonUtil.writeValueAsString(auditEventV4Responses).getBytes());
                zipOutputStream.closeEntry();
            }
        };
        String fileName = String.format("audit-%s-%d.zip", resourceType, resourceId);
        return Response.ok(streamingOutput).header("content-disposition", String.format("attachment; filename = %s", fileName)).build();
    }
}
