package com.sequenceiq.cloudbreak.structuredevent.service.reactor;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.service.CDPStructuredEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;

@Component
public class CDPStructuredEventHandler<T extends CDPStructuredEvent> implements EventHandler<T> {
    @Inject
    private CDPStructuredEventService cdpStructuredEventService;

    @Override
    public String selector() {
        return CDPStructuredEventAsyncNotifier.EVENT_LOG;
    }

    @Override
    public void accept(Event<T> structuredEvent) {
        cdpStructuredEventService.create(structuredEvent.getData());
    }
}
