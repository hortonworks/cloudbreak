package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CustomPermissionCheck;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.AuditEventV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Responses;
import com.sequenceiq.cloudbreak.audit.authz.AuditEventAuthorization;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.service.audit.AuditEventService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;

@Controller
public class AuditEventV4Controller implements AuditEventV4Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditEventV4Controller.class);

    @Inject
    private AuditEventService auditEventService;

    @Inject
    private CloudbreakRestRequestThreadLocalService threadLocalService;

    @Inject
    private AuditEventAuthorization eventAuthorization;

    @Override
    @CustomPermissionCheck
    public AuditEventV4Response getAuditEventById(Long workspaceId, Long auditId) {
        AuditEventV4Response event = auditEventService.getAuditEventByWorkspaceId(workspaceId, auditId);
        eventAuthorization.checkPermissions(List.of(event), event.getStructuredEvent().getOperation().getResourceType());
        return event;
    }

    @Override
    @CustomPermissionCheck
    public AuditEventV4Responses getAuditEvents(Long workspaceId, String resourceType, Long resourceId, String resourceCrn) {
        List<AuditEventV4Response> auditEventsByWorkspaceId = auditEventService.getAuditEventsByWorkspaceId(threadLocalService.getRequestedWorkspaceId(),
                resourceType, resourceId, resourceCrn);
        eventAuthorization.checkPermissions(auditEventsByWorkspaceId, resourceType);
        return new AuditEventV4Responses(auditEventsByWorkspaceId);

    }

    @Override
    @CustomPermissionCheck
    public Response getAuditEventsZip(Long workspaceId, String resourceType, Long resourceId, String resourceCrn) {
        Collection<AuditEventV4Response> auditEvents = getAuditEvents(threadLocalService.getRequestedWorkspaceId(),
                resourceType, resourceId, resourceCrn).getResponses();
        return getAuditEventsZipResponse(auditEvents, resourceType);
    }

    private Response getAuditEventsZipResponse(Collection<AuditEventV4Response> auditEventV4Responses, String resourceType) {
        StreamingOutput streamingOutput = output -> {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(output)) {
                zipOutputStream.putNextEntry(new ZipEntry("struct-events.json"));
                JsonUtil.writeValueToOutputStream(zipOutputStream, auditEventV4Responses);
            }
        };
        String fileName = String.format("audit-%s.zip", resourceType);
        return Response.ok(streamingOutput).header("content-disposition", String.format("attachment; filename = %s", fileName)).build();
    }

}
