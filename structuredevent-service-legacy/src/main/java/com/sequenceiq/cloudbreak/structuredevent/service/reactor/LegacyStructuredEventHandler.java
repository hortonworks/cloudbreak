package com.sequenceiq.cloudbreak.structuredevent.service.reactor;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.structuredevent.LegacyStructuredEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

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
