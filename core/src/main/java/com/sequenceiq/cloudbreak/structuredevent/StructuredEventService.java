package com.sequenceiq.cloudbreak.structuredevent;

import java.util.List;

import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventContainer;

public interface StructuredEventService {

    void storeStructuredEvent(StructuredEvent structuredEvent);

    <T extends StructuredEvent> List<T> getEventsForUserWithType(String userId, Class<T> eventClass);

    <T extends StructuredEvent> List<T> getEventsForUserWithTypeSince(String userId, Class<T> eventClass, Long since);

    <T extends StructuredEvent> List<T> getEventsForUserWithTypeAndResourceId(String userId, Class<T> eventClass, String resourceType, Long resourceId);

    StructuredEventContainer getEventsForUserWithStackId(String userId, Long stackId);
}
