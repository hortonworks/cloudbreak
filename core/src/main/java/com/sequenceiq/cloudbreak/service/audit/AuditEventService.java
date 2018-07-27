package com.sequenceiq.cloudbreak.service.audit;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.audit.AuditEvent;
import com.sequenceiq.cloudbreak.comparator.audit.AuditEventComparator;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.structuredevent.db.StructuredEventRepository;

@Service
public class AuditEventService {

    @Inject
    private ConversionService conversionService;

    @Inject
    private StructuredEventRepository structuredEventRepository;

    public AuditEvent getAuditEvent(String userId, Long auditId) {
        StructuredEventEntity event = structuredEventRepository.findByIdAndOwner(auditId, userId);
        return event != null ? conversionService.convert(event, AuditEvent.class) : null;
    }

    public List<AuditEvent> getAuditEvents(String userId, String resourceType, Long resourceId) {
        List<AuditEvent> auditEvents = getEventsForUserWithTypeAndResourceId(userId, resourceType, resourceId);
        Collections.sort(auditEvents, new AuditEventComparator());
        return auditEvents;
    }

    private List<AuditEvent> getEventsForUserWithTypeAndResourceId(String userId, String resourceType, Long resourceId) {
        List<StructuredEventEntity> events = structuredEventRepository.findByOwnerAndResourceTypeAndResourceId(userId, resourceType, resourceId);
        return events != null ? (List<AuditEvent>) conversionService.convert(events,
                TypeDescriptor.forObject(events),
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(AuditEvent.class))) : Collections.emptyList();
    }

}
