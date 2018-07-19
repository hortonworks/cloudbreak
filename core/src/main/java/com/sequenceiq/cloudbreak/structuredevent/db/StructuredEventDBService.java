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
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventContainer;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;

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
    public <T extends StructuredEvent> List<T> getEventsForUserWithType(String userId, Class<T> eventClass) {
        List<StructuredEventEntity> events = structuredEventRepository.findByOwnerAndEventType(userId, StructuredEventType.getByClass(eventClass).name());
        return events != null ? (List<T>) conversionService.convert(events,
                TypeDescriptor.forObject(events),
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(StructuredEvent.class))) : Collections.emptyList();
    }

    @Override
    public <T extends StructuredEvent> List<T> getEventsForUserWithTypeSince(String userId, Class<T> eventClass, Long since) {
        List<StructuredEventEntity> events = structuredEventRepository.findByUserIdAndEventTypeSince(userId,
                StructuredEventType.getByClass(eventClass).name(), since);
        return events != null ? (List<T>) conversionService.convert(events,
                TypeDescriptor.forObject(events),
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(StructuredEvent.class))) : Collections.emptyList();
    }

    @Override
    public <T extends StructuredEvent> List<T> getEventsForUserWithTypeAndResourceId(String userId,
            Class<T> eventClass, String resourceType, Long resourceId) {
        List<StructuredEventEntity> events = structuredEventRepository.findByOwnerAndEventTypeAndResourceTypeAndResourceId(userId,
                StructuredEventType.getByClass(eventClass).name(), resourceType, resourceId);
        return events != null ? (List<T>) conversionService.convert(events,
                TypeDescriptor.forObject(events),
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(StructuredEvent.class))) : Collections.emptyList();
    }

    @Override
    public StructuredEventContainer getEventsForUserWithStackId(String userId, Long stackId) {

        List<StructuredRestCallEvent> rest = getEventsForUserWithTypeAndResourceId(userId, StructuredRestCallEvent.class, "stacks", stackId);
        List<StructuredFlowEvent> flow = getEventsForUserWithTypeAndResourceId(userId, StructuredFlowEvent.class, "STACK", stackId);
        List<StructuredNotificationEvent> notification = getEventsForUserWithTypeAndResourceId(userId,
                StructuredNotificationEvent.class, "STACK", stackId);

        return new StructuredEventContainer(flow, rest, notification);
    }
}
