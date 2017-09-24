package com.sequenceiq.cloudbreak.structuredevent;

import java.util.List;

import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;

public interface StructuredEventService {
    void storeStructuredEvent(StructuredEvent structuredEvent);

    <T extends StructuredEvent> List<T> getEventsForUserWithType(String userId, String eventType);

    <T extends StructuredEvent> List<T> getEventsForUserWithTypeSince(String userId, String eventType, Long since);

    <T extends StructuredEvent> List<T> getEventsForUserWithTypeAndResourceId(String userId, String eventType, String resourceType, Long resourceId);
}
