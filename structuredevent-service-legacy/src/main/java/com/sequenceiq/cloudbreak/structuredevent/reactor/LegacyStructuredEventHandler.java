package com.sequenceiq.cloudbreak.structuredevent.reactor;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.cloudbreak.structuredevent.LegacyStructuredEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;

import reactor.bus.Event;

@Component
public class LegacyStructuredEventHandler<T extends StructuredEvent> implements EventHandler<T> {
    @Inject
    private LegacyStructuredEventService legacyStructuredEventService;

    @Override
    public String selector() {
        return LegacyStructuredEventAsyncNotifier.EVENT_LOG;
    }

    @Override
    public void accept(Event<T> structuredEvent) {
        legacyStructuredEventService.create(structuredEvent.getData());
    }
}
