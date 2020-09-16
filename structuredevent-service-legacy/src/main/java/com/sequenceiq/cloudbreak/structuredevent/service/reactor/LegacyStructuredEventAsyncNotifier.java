package com.sequenceiq.cloudbreak.structuredevent.service.reactor;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.LegacyBaseStructuredEventClient;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component("legacyStructuredEventAsyncNotifier")
public class LegacyStructuredEventAsyncNotifier implements LegacyBaseStructuredEventClient {
    public static final String EVENT_LOG = "EVENT_LOG";

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private EventBus eventBus;

    @Override
    public void sendStructuredEvent(StructuredEvent structuredEvent) {
        sendAsyncEvent(EVENT_LOG, eventFactory.createEvent(structuredEvent));
    }

    private void sendAsyncEvent(String selector, Event<?> event) {
        eventBus.notify(selector, event);
    }
}
