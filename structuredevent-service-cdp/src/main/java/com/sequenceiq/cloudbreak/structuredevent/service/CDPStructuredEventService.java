package com.sequenceiq.cloudbreak.structuredevent.service;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEventSenderService;

public interface CDPStructuredEventService extends CDPStructuredEventSenderService {

    <T extends CDPStructuredEvent> Slice<T> getPagedEventsOfResource(List<StructuredEventType> eventType, String resourceCrn, Pageable pageable);

    <T extends CDPStructuredEvent> List<T> getEventsOfResource(List<StructuredEventType> eventTypes, String resourceCrn);

    <T extends CDPStructuredEvent> Slice<T> getPagedEventsOfResources(List<StructuredEventType> eventTypes, List<String> resourceCrns, Pageable pageable);

    <T extends CDPStructuredEvent> List<T> getEventsOfResources(List<StructuredEventType> eventTypes, List<String> resourceCrns);
}
