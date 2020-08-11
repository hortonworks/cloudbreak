package com.sequenceiq.cloudbreak.structuredevent.reactor;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.CDPStructuredEventClient;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class CDPStructuredEventAsyncNotifier implements CDPStructuredEventClient {
    public static final String EVENT_LOG = "EVENT_LOG";

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private EventBus eventBus;

    @Override
    public void sendStructuredEvent(CDPStructuredEvent structuredEvent) {
        sendAsyncEvent(EVENT_LOG, eventFactory.createEvent(structuredEvent));
    }

    private void sendAsyncEvent(String selector, Event<?> event) {
        eventBus.notify(selector, event);
    }
}
