package com.sequenceiq.it.cloudbreak.assertion;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredRestCallEvent;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

@Component
public class EventAssertionCommon {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventAssertionCommon.class);

    private String generateRestId(CDPStructuredRestCallEvent event) {
        StringBuilder sb = new StringBuilder(event.getRestCall().getRestRequest().getMethod());
        sb.append("-").append(event.getOperation().getResourceType());
        if (event.getOperation().getResourceEvent() != null) {
            sb.append("-").append(event.getOperation().getResourceEvent());
        }
        String id = sb.toString().toLowerCase();
        LOGGER.debug("Generated rest id: {}", id);
        return id;
    }

    public void checkRestEvents(List<CDPStructuredEvent> auditEvents, List<String> expectedIds) {
        List<String> eventStates = auditEvents.stream()
                .filter(event -> "CDPStructuredRestCallEvent".equals(event.getType()))
                .map(event -> generateRestId((CDPStructuredRestCallEvent) event))
                .collect(Collectors.toList());
        boolean containsAll = eventStates.containsAll(expectedIds);
        if (!containsAll) {
            List<String> mutableList = new ArrayList<>(expectedIds);
            mutableList.removeAll(eventStates);
            throw new TestFailException("Cannot find all rest events: " + mutableList);
        }
    }

    public void checkNotificationEvents(List<CDPStructuredEvent> auditEvents, List<ResourceEvent> expectedStates) {
        List<ResourceEvent> eventStates = auditEvents.stream()
                .filter(event -> "CDPStructuredNotificationEvent".equals(event.getType()))
                .map(event -> ((CDPStructuredNotificationEvent) event).getNotificationDetails().getResourceEvent())
                .collect(Collectors.toList());
        boolean containsAll = eventStates.containsAll(expectedStates);
        if (!containsAll) {
            List<ResourceEvent> mutableList = new ArrayList<>(expectedStates);
            mutableList.removeAll(eventStates);
            throw new TestFailException("Cannot find all notification events: " + mutableList);
        }
    }

    public void checkFlowEvents(List<CDPStructuredEvent> auditEvents, List<String> expectedStates) {
        List<String> eventStates = auditEvents.stream()
                .filter(event -> "CDPStructuredFlowEvent".equals(event.getType()))
                .map(event -> ((CDPStructuredFlowEvent) event).getFlow().getFlowState())
                .collect(Collectors.toList());
        boolean containsAll = eventStates.containsAll(expectedStates);
        if (!containsAll) {
            List<String> mutableList = new ArrayList<>(expectedStates);
            mutableList.removeAll(eventStates);
            throw new TestFailException("Cannot find all flow state: " + mutableList);
        }
    }
}
