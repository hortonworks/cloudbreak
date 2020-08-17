package com.sequenceiq.cloudbreak.structuredevent.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEventContainer;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEventSenderService;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;

public interface CDPStructuredEventService extends CDPStructuredEventSenderService {

    <T extends CDPStructuredEvent> List<T> getEventsForAccountWithType(String accountId, Class<T> eventClass);

    <T extends CDPStructuredEvent> List<T> getEventsForAccountWithTypeSince(String accountId, Class<T> eventClass, Long since);

    <T extends CDPStructuredEvent> List<T> getEventsWithTypeAndResourceId(Class<T> eventClass, String resourceType, Long resourceId);

    <T extends CDPStructuredEvent> Page<T> getEventsLimitedWithTypeAndResourceId(Class<T> eventClass, String resourceType, Long resourceId, Pageable pageable);

    CDPStructuredEventContainer getEventsForUserWithResourceId(String resourceType, Long resourceId);

    CDPStructuredEventContainer getStructuredEventsForObject(String name, String accountId);

    Page<CDPStructuredNotificationEvent> getPagedNotificationEventsOfResource(StructuredEventType eventType, String resourceCrn, Pageable pageable);
}
