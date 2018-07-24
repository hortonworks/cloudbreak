package com.sequenceiq.cloudbreak.service.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.audit.AuditEvent;
import com.sequenceiq.cloudbreak.structuredevent.StructuredEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;
import com.sequenceiq.cloudbreak.util.comparator.AuditEventComparator;

@Service
public class AuditEventService {

    @Inject
    private StructuredEventService structuredEventService;

    public List<AuditEvent> getAuditEvents(String userId, String resourceType, Long resourceId) {

        List<AuditEvent> auditEvents = new ArrayList<>();
        appendAudit(auditEvents, structuredEventService.getEventsForUserWithTypeAndResourceId(userId, StructuredRestCallEvent.class, resourceType, resourceId));
        appendAudit(auditEvents, structuredEventService.getEventsForUserWithTypeAndResourceId(userId, StructuredFlowEvent.class, resourceType, resourceId));
        appendAudit(auditEvents, structuredEventService.getEventsForUserWithTypeAndResourceId(userId, StructuredNotificationEvent.class,
                resourceType, resourceId));

        Collections.sort(auditEvents, new AuditEventComparator());

        return auditEvents;
    }

    private <T extends StructuredEvent> List<AuditEvent> appendAudit(List<AuditEvent> auditEvents, List<T> events) {
        for (StructuredEvent structuredEvent : events) {
            auditEvents.add(new AuditEvent(structuredEvent));
        }
        return auditEvents;
    }

}
