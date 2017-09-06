package com.sequenceiq.cloudbreak.structuredevent;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;

@Service
public class DefaultStructuredEventClient implements StructuredEventClient {
    @Inject
    private StructuredEventService structuredEventService;

    @Override
    public void sendStructuredEvent(StructuredEvent structuredEvent) {
        structuredEventService.storeStructuredEvent(structuredEvent);
    }
}
