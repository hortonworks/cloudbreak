package com.sequenceiq.cloudbreak.structuredevent.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEventSenderService;

public interface CDPStructuredEventService extends CDPStructuredEventSenderService {

    <T extends CDPStructuredEvent> Page<T> getPagedEventsOfResource(List<StructuredEventType> eventType, String resourceCrn, Pageable pageable);

    <T extends CDPStructuredEvent> List<T> getEventsOfResource(List<StructuredEventType> eventTypes, String resourceCrn);

    <T extends CDPStructuredEvent> Page<T> getPagedEventsOfResources(List<StructuredEventType> eventTypes, List<String> resourceCrns, Pageable pageable);
}
