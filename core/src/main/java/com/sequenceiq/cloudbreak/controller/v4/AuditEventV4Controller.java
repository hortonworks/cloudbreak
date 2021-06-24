package com.sequenceiq.cloudbreak.controller.v4;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNullOtherwise;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CustomPermissionCheck;
import com.sequenceiq.authorization.utils.EventAuthorizationDto;
import com.sequenceiq.authorization.utils.EventAuthorizationUtils;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.AuditEventV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Responses;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.service.audit.AuditEventService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;

@Controller
public class AuditEventV4Controller implements AuditEventV4Endpoint {

    @Inject
    private AuditEventService auditEventService;

    @Inject
    private CloudbreakRestRequestThreadLocalService threadLocalService;

    @Inject
    private EventAuthorizationUtils eventAuthorizationUtils;

    @Override
    @CustomPermissionCheck
    public AuditEventV4Response getAuditEventById(Long workspaceId, Long auditId) {
        AuditEventV4Response event = auditEventService.getAuditEventByWorkspaceId(workspaceId, auditId);
        checkPermissions(Set.of(event), getIfNotNullOtherwise(event.getStructuredEvent(), structuredEvent -> structuredEvent.getType(), null));
        return event;
    }

    @Override
    @CustomPermissionCheck
    public AuditEventV4Responses getAuditEvents(Long workspaceId, String resourceType, Long resourceId, String resourceCrn) {
        List<AuditEventV4Response> auditEventsByWorkspaceId = auditEventService.getAuditEventsByWorkspaceId(threadLocalService.getRequestedWorkspaceId(),
                resourceType, resourceId, resourceCrn);
        checkPermissions(auditEventsByWorkspaceId, resourceType);
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
                zipOutputStream.write(JsonUtil.writeValueAsString(auditEventV4Responses).getBytes());
                zipOutputStream.closeEntry();
            }
        };
        String fileName = String.format("audit-%s.zip", resourceType);
        return Response.ok(streamingOutput).header("content-disposition", String.format("attachment; filename = %s", fileName)).build();
    }

    private void checkPermissions(Collection<AuditEventV4Response> auditEvents, String resourceType) {
        Set<EventAuthorizationDto> dtos = auditEvents.stream()
                .map(auditEventV4Response -> auditEventV4Response.getStructuredEvent())
                .map(event -> new EventAuthorizationDto(event.getOperation().getResourceCrn(), resourceType))
                .collect(Collectors.toSet());
        eventAuthorizationUtils.checkPermissionBasedOnResourceTypeAndCrn(dtos);
    }

}
