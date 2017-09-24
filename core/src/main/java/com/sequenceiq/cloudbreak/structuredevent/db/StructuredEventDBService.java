package com.sequenceiq.cloudbreak.structuredevent.db;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.structuredevent.StructuredEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;

@Component
public class StructuredEventDBService implements StructuredEventService {
    @Inject
    private ConversionService conversionService;

    @Inject
    private StructuredEventRepository structuredEventRepository;

    @Override
    public void storeStructuredEvent(StructuredEvent structuredEvent) {
        StructuredEventEntity structuredEventEntityEntity = conversionService.convert(structuredEvent, StructuredEventEntity.class);
        structuredEventRepository.save(structuredEventEntityEntity);
    }

    @Override
    public <T extends StructuredEvent> List<T> getEventsForUserWithType(String userId, String eventType) {
        List<StructuredEventEntity> events = structuredEventRepository.findByUserIdAndEventType(userId, eventType);
        return events != null ? (List<T>) conversionService.convert(events,
                TypeDescriptor.forObject(events),
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(StructuredEvent.class))) : Collections.emptyList();
    }

    @Override
    public <T extends StructuredEvent> List<T> getEventsForUserWithTypeSince(String userId, String eventType, Long since) {
        List<StructuredEventEntity> events = structuredEventRepository.findByUserIdAndEventTypeSince(userId, eventType, since);
        return events != null ? (List<T>) conversionService.convert(events,
                TypeDescriptor.forObject(events),
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(StructuredEvent.class))) : Collections.emptyList();
    }

    @Override
    public <T extends StructuredEvent> List<T> getEventsForUserWithTypeAndResourceId(String userId, String eventType, String resourceType, Long resourceId) {
        List<StructuredEventEntity> events = structuredEventRepository.findByUserIdAndEventTypeAndResourceTypeAndResourceId(userId, eventType, resourceType,
                resourceId);
        return events != null ? (List<T>) conversionService.convert(events,
                TypeDescriptor.forObject(events),
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(StructuredEvent.class))) : Collections.emptyList();
    }
}
