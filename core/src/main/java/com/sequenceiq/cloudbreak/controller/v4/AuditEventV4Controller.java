package com.sequenceiq.cloudbreak.controller.v4;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNullOtherwise;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.legacy.OperationDetails;

@Controller
public class AuditEventV4Controller implements AuditEventV4Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditEventV4Controller.class);

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
        checkPermissions(Set.of(event), getIfNotNullOtherwise(event.getStructuredEvent(), StructuredEvent::getType, null));
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
        LOGGER.debug("About to check permission for {} audit event(s) based on the following resource type: {}", auditEvents.size(), resourceType);
        Set<EventAuthorizationDto> dtos = collectEventsForDtoCreation(auditEvents).entrySet().stream()
                .map(entry -> new EventAuthorizationDto(entry.getKey(), entry.getValue().getKey(), entry.getValue().getValue()))
                .collect(Collectors.toSet());
        eventAuthorizationUtils.checkPermissionBasedOnResourceTypeAndCrn(dtos);
    }

    private Map<String, Pair<String, String>> collectEventsForDtoCreation(Collection<AuditEventV4Response> events) {
        Map<String, Pair<String, String>> eventsForDtoCreation = new LinkedHashMap<>();
        for (AuditEventV4Response event : events) {
            OperationDetails operation = event.getStructuredEvent().getOperation();
            if (!eventsForDtoCreation.containsKey(operation.getResourceCrn())) {
                eventsForDtoCreation.put(operation.getResourceCrn(),
                        Pair.of(operation.getResourceType(), operation.getEventType() != null ? operation.getEventType().name() : null));
            }
        }
        Set<String> logContent = eventsForDtoCreation.entrySet().stream()
                .map(entry -> String.format("[resourceCrn: %s, eventType: %s, resourceType: %s]", entry.getKey(), entry.getValue().getKey(),
                        entry.getValue().getValue()))
                .collect(Collectors.toSet());
        LOGGER.debug("The following audit entries will be checked for authz: {}", String.join(",", logContent));
        return eventsForDtoCreation;
    }

}
