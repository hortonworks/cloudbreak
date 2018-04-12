package com.sequenceiq.cloudbreak.structuredevent.db;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.structuredevent.StructuredEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

    @Override
    public List<StructuredEvent> getEventsForUser(String userId, List<String> eventTypes, Map<String, Long> resourceIds) {
        List<StructuredEventEntity> events = Lists.newArrayList();
        for (String eventType : eventTypes) {
            for (Entry<String, Long> resId : resourceIds.entrySet()) {
                events.addAll(structuredEventRepository.findByUserIdAndEventTypeAndResourceTypeAndResourceId(
                        userId, eventType, resId.getKey(), resId.getValue()));
            }
        }
        List<StructuredEvent> structEvents = events != null ? (List<StructuredEvent>) conversionService.convert(events,
                TypeDescriptor.forObject(events),
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(StructuredEvent.class))) : Collections.emptyList();
        structEvents.sort((StructuredEvent o1, StructuredEvent o2) -> (int) (o1.getOperation().getTimestamp() - o2.getOperation().getTimestamp()));
        return structEvents;
    }
}
