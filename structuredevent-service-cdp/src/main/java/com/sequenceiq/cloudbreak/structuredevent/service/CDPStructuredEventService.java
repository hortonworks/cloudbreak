package com.sequenceiq.cloudbreak.structuredevent.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEventSenderService;

public interface CDPStructuredEventService extends CDPStructuredEventSenderService {

    <T extends CDPStructuredEvent> Page<T> getPagedNotificationEventsOfResource(StructuredEventType eventType, String resourceCrn, Pageable pageable);

    <T extends CDPStructuredEvent> Page<T> getPagedEventsOfResource(List<StructuredEventType> eventType, String resourceCrn, Pageable pageable);

    <T extends CDPStructuredEvent> List<T> getNotificationEventsOfResource(StructuredEventType eventType, String resourceCrn);
}
