package com.sequenceiq.cloudbreak.structuredevent.service;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

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
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;

@Component
public class CdpStructuredEventAuthorization {

    private static final Logger LOGGER = LoggerFactory.getLogger(CdpStructuredEventAuthorization.class);

    private final EventAuthorizationUtils eventAuthorizationUtils;

    public CdpStructuredEventAuthorization(EventAuthorizationUtils eventAuthorizationUtils) {
        this.eventAuthorizationUtils = eventAuthorizationUtils;
    }

    public void checkPermissions(Collection<? extends CDPStructuredEvent> events) {
        if (CollectionUtils.isEmpty(events)) {
            LOGGER.debug("No structured events to check permission for.");
            return;
        }
        LOGGER.debug("About to check permission for {} structured event(s).", events.size());
        Set<EventAuthorizationDto> dtos = collectEventsForDtoCreation(events).entrySet().stream()
                .map(entry -> new EventAuthorizationDto(entry.getKey(), entry.getValue().getKey(), entry.getValue().getValue()))
                .collect(Collectors.toSet());
        eventAuthorizationUtils.checkPermissionBasedOnResourceTypeAndCrn(dtos);
    }

    private Map<String, Pair<String, String>> collectEventsForDtoCreation(Collection<? extends CDPStructuredEvent> events) {
        Map<String, Pair<String, String>> eventsForDtoCreation = new LinkedHashMap<>();
        for (CDPStructuredEvent event : events) {
            CDPOperationDetails operation = event.getOperation();
            if (!eventsForDtoCreation.containsKey(operation.getResourceCrn())) {
                eventsForDtoCreation.put(operation.getResourceCrn(), Pair.of(operation.getResourceType(), getIfNotNull(operation.getEventType(), Enum::name)));
            }
        }
        Set<String> logContent = eventsForDtoCreation.entrySet().stream()
                .map(entry -> String.format("[resourceCrn: %s, resourceType: %s, eventType: %s]", entry.getKey(), entry.getValue().getKey(),
                        entry.getValue().getValue()))
                .collect(Collectors.toSet());
        LOGGER.debug("The following audit entries will be checked for authorization: {}", String.join(",", logContent));
        return eventsForDtoCreation;
    }

}
