package com.sequenceiq.cloudbreak.structuredevent;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventContainer;

public interface StructuredEventService extends StructuredEventSenderService {

    <T extends StructuredEvent> List<T> getEventsForAccountWithType(String accountId, Class<T> eventClass);

    <T extends StructuredEvent> List<T> getEventsForAccountWithTypeSince(String accountId, Class<T> eventClass, Long since);

    <T extends StructuredEvent> List<T> getEventsWithTypeAndResourceId(Class<T> eventClass, String resourceType, Long resourceId);

    <T extends StructuredEvent> Page<T> getEventsLimitedWithTypeAndResourceId(Class<T> eventClass, String resourceType, Long resourceId, Pageable pageable);

    StructuredEventContainer getEventsForUserWithResourceId(String resourceType, Long resourceId);

    StructuredEventContainer getStructuredEventsForStack(String name, Long workspaceId);
}
