package com.sequenceiq.cloudbreak.structuredevent;

import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;

import java.util.List;
import java.util.Map;

public interface StructuredEventService {
    void storeStructuredEvent(StructuredEvent structuredEvent);

    <T extends StructuredEvent> List<T> getEventsForUserWithType(String userId, String eventType);

    <T extends StructuredEvent> List<T> getEventsForUserWithTypeSince(String userId, String eventType, Long since);

    <T extends StructuredEvent> List<T> getEventsForUserWithTypeAndResourceId(String userId, String eventType, String resourceType, Long resourceId);

    List<StructuredEvent> getEventsForUser(String userId, List<String> eventTypes, Map<String, Long> resoureceIds);
}
