package com.sequenceiq.cloudbreak.audit.authz;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.utils.EventAuthorizationDto;
import com.sequenceiq.authorization.utils.EventAuthorizationUtils;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Response;
import com.sequenceiq.cloudbreak.structuredevent.event.legacy.OperationDetails;

@Component
public class AuditEventAuthorization {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditEventAuthorization.class);

    private final EventAuthorizationUtils eventAuthorizationUtils;

    public AuditEventAuthorization(EventAuthorizationUtils eventAuthorizationUtils) {
        this.eventAuthorizationUtils = eventAuthorizationUtils;
    }

    public void checkPermissions(Collection<AuditEventV4Response> auditEvents, String resourceType) {
        if (CollectionUtils.isEmpty(auditEvents)) {
            LOGGER.debug("No audit events to check permission for.");
            return;
        }
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
